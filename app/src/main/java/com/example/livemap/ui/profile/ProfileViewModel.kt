package com.example.livemap.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.User
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.StorageRepository
import com.example.livemap.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
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
     *
     * The result is reported back through [onResult] so the UI can tell the
     * user whether the write actually persisted. Previously the Result was
     * dropped, so a rejected write (e.g. by security rules) failed silently
     * and the screen looked like "changes don't save".
     */
    fun updateProfile(
        updates: Map<String, Any?>,
        newProfilePictureUri: android.net.Uri? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        val uid = user.value?.uid
        if (uid == null) {
            onResult(Result.failure(IllegalStateException("No signed-in user")))
            return
        }
        viewModelScope.launch {
            var finalUpdates = updates
            if (newProfilePictureUri != null) {
                val uploadResult = storageRepository.uploadProfilePicture(uid, newProfilePictureUri)
                uploadResult.onSuccess { url ->
                    finalUpdates = updates + ("photoUrl" to url)
                }.onFailure {
                    onResult(Result.failure(it))
                    return@launch
                }
            }
            onResult(userRepository.updateUser(uid, finalUpdates))
        }
    }
}
