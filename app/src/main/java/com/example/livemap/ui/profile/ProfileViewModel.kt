package com.example.livemap.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.User
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    /**
     * The profile of the currently logged-in user.
     * It automatically updates whenever the auth state changes or the Firestore document changes.
     */
    val user: StateFlow<User?> = authRepository.observeAuthState()
        .flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                userRepository.observeUser(firebaseUser.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * Updates the current user's profile in Firestore.
     */
    fun updateProfile(updates: Map<String, Any?>) {
        val uid = user.value?.uid ?: return
        viewModelScope.launch {
            userRepository.updateUser(uid, updates)
        }
    }
}
