package com.example.livemap.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.AuthState
import com.example.livemap.data.repository.FormState
import com.example.livemap.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel that bridges Login/Register screens with the repositories.
 *
 * Exposes:
 *   - authState: Drives navigation between the auth-gate (AuthRepository) and the main app.
 *   - formState: State for the currently-visible auth screen | Reset to Idle every time the user starts a new attempt.
 *
 * Coordinates:
 *   - register(): signs up via AuthRepository, then creates the Firestore profile via UserRepository
 *      If profile creation fails, the user is left as auth-only without a profile doc (edge case).
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    /**
     * Auth state derived from the repository's auth-state Flow.
     *
     * stateIn() converts the cold Flow into a hot StateFlow that:
     *   - starts collecting when the first subscriber appears
     *   - keeps emitting for 5s after the last subscriber leaves
     *   - has Loading as the initial value until Firebase responds
     */
    val authState: StateFlow<AuthState> = authRepository
        .observeAuthState()
        .map { firebaseUser ->
            if (firebaseUser != null) AuthState.Authenticated(firebaseUser.uid)
            else AuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = AuthState.Loading
        )

    // formState is exposed as read-only outside (only mutable in the VM).
    private val _formState = MutableStateFlow<FormState>(FormState.Idle)
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    /**
     * Signs in an existing user.
     *   - On success: formState → Success, authState will transition to Authenticated automatically.
     *   - On failure: formState → Error(message). UI shows the message.
     */
    fun login(email: String, password: String) {
        // local validation - Backend validates again (real validation).
        if (email.isBlank() || password.isBlank()) {
            _formState.value = FormState.Error("Please fill in all fields.")
            return
        }

        _formState.value = FormState.Submitting

        viewModelScope.launch {
            authRepository.signIn(email.trim(), password)
                .onSuccess {
                    _formState.value = FormState.Success
                }
                .onFailure { e ->
                    _formState.value = FormState.Error(
                        e.localizedMessage ?: "Login failed. Check your credentials."
                    )
                }
        }
    }

    /**
     * Creates a new account + profile document.
     *
     * Process:
     *   1. AuthRepository.signUp() - creates Firebase Auth user.
     *   2. UserRepository.createUserProfile() - creates /users/{uid} doc.
     */
    fun register(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _formState.value = FormState.Error("Please fill in all fields.")
            return
        }
        if (password.length < 6) {
            // Firebase Auth also enforces this, but failing fast is nicer UX.
            _formState.value = FormState.Error("Password must be at least 6 characters.")
            return
        }

        _formState.value = FormState.Submitting

        viewModelScope.launch {
            authRepository.signUp(email.trim(), password)
                .onSuccess { uid ->
                    // Auth account is created. Now create the profile doc.
                    userRepository
                        .createUserProfile(
                            uid = uid,
                            email = email.trim(),
                            displayName = displayName.trim()
                        )
                        .onSuccess {
                            _formState.value = FormState.Success
                        }
                        .onFailure { e ->
                            // Auth succeeded but profile didn't. Not fatal.
                            _formState.value = FormState.Error(
                                "Account created but profile setup failed: " +
                                        (e.localizedMessage ?: "unknown error") +
                                        ". You can complete it later from Profile."
                            )
                        }
                }
                .onFailure { e ->
                    _formState.value = FormState.Error(
                        e.localizedMessage ?: "Registration failed."
                    )
                }
        }
    }

    // Back to Idle state - resets the form so the UI can start fresh.
    fun resetForm() {
        _formState.value = FormState.Idle
    }

    // Signs the current user out - authState will transition to Unauthenticated.
    fun logout() {
        authRepository.signOut()
    }

    /**
     * Sends a password reset email.
     */
    fun resetPassword(email: String) {

        println("RESET PASSWORD CALLED")

        if (email.isBlank()) {
            _formState.value = FormState.Error("Please enter your email.")
            return
        }

        _formState.value = FormState.Submitting

        viewModelScope.launch {

            authRepository
                .sendPasswordResetEmail(email.trim())
                .onSuccess {
                    _formState.value = FormState.Success
                }
                .onFailure { e ->
                    _formState.value = FormState.Error(
                        e.localizedMessage ?: "Unable to send reset email."
                    )
                }
        }
    }

    fun deleteAccount() {

        _formState.value = FormState.Submitting

        viewModelScope.launch {

            val currentState = authState.value
            val uid = (currentState as? AuthState.Authenticated)?.uid ?: run {
                _formState.value = FormState.Error("No authenticated user.")
                return@launch
            }

            val profileResult = userRepository.deleteUserProfile(uid)

            if (profileResult.isFailure) {
                _formState.value = FormState.Error("Failed to delete profile.")
                return@launch
            }

            val authResult = authRepository.deleteCurrentUser()

            if (authResult.isFailure) {
                _formState.value = FormState.Error(
                    authResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to delete account."
                )
                return@launch
            }

            // Account successfully deleted
            _formState.value = FormState.Success
        }
    }
}