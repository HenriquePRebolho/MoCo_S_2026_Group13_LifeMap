package com.example.livemap.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.AuthState
import com.example.livemap.data.repository.FormState
import com.example.livemap.data.repository.FriendshipRepository
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
    private val userRepository: UserRepository = UserRepository(),
    private val friendshipRepository: FriendshipRepository = FriendshipRepository()
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

    /**
     * Permanently deletes the current account.
     *
     * Steps (in order, all while still authenticated):
     *   1. Re-authenticate with the password — Firebase requires a recent login
     *      for delete(), otherwise it throws RecentLoginRequired.
     *   2. Delete the user's friendship docs so friends don't keep ghost entries.
     *   3. Delete the Firestore /users/{uid} profile.
     *   4. Delete the Auth account — the auth-state listener then drives the app
     *      back to the login screen.
     *
     * The outcome is reported through [onResult] so the UI can show success/error
     * instead of failing silently (the old version returned early and did nothing,
     * because authState.value was Loading in the screen-scoped ViewModel).
     */
    fun deleteAccount(password: String, onResult: (Result<Unit>) -> Unit = {}) {
        val uid = authRepository.currentUser()?.uid
        if (uid == null) {
            onResult(Result.failure(IllegalStateException("No signed-in user")))
            return
        }
        if (password.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Please enter your password to confirm")))
            return
        }

        viewModelScope.launch {
            val reauth = authRepository.reauthenticate(password)
            if (reauth.isFailure) {
                onResult(Result.failure(reauth.exceptionOrNull() ?: Exception("Re-authentication failed")))
                return@launch
            }

            // Best-effort cleanup of related data; don't abort the deletion if it fails.
            friendshipRepository.deleteAllForUser(uid)

            val profileResult = userRepository.deleteUserProfile(uid)
            if (profileResult.isFailure) {
                onResult(Result.failure(profileResult.exceptionOrNull() ?: Exception("Failed to delete profile")))
                return@launch
            }

            onResult(authRepository.deleteCurrentUser())
        }
    }
}