package com.example.livemap.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Friendship
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.FriendshipRepository
import com.example.livemap.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives FriendDetailScreen.
 *
 * Key new concept: SavedStateHandle.
 * When this VM is created inside a NavHost composable that has a "{uid}"
 * argument, Compose Navigation automatically populates SavedStateHandle
 * with that argument. So friendUid is read from the navigation route — no
 * factory, no manual wiring needed.
 *
 * Combines THREE flows:
 *   - the friend's user document
 *   - the current user's document (for computing common interests)
 *   - all friendships involving the current user (to determine relationship)
 */
class FriendDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val friendshipRepository: FriendshipRepository = FriendshipRepository()
) : ViewModel() {

    // Pulled from the navigation route. If the route was "friends/abc123",
    // then friendUid == "abc123".
    private val friendUid: String = savedStateHandle.get<String>("uid") ?: ""
    private val currentUid: String? = authRepository.currentUser()?.uid

    val state: StateFlow<FriendDetailState> = if (currentUid == null || friendUid.isBlank()) {
        MutableStateFlow<FriendDetailState>(
            FriendDetailState.Error("Invalid user reference")
        ).asStateFlow()
    } else {
        combine(
            userRepository.observeUser(friendUid),
            userRepository.observeUser(currentUid),
            friendshipRepository.observeFriendships(currentUid)
        ) { friend, me, friendships ->
            if (friend == null) {
                FriendDetailState.Error("User not found")
            } else {
                val friendship = friendships.firstOrNull { it.userIds.contains(friendUid) }
                val relationship = when {
                    friendUid == currentUid -> RelationshipStatus.Self
                    friendship == null -> RelationshipStatus.NotConnected
                    friendship.status == "accepted" -> RelationshipStatus.Friends
                    friendship.status == "pending" && friendship.requestedBy == currentUid -> RelationshipStatus.Pending
                    friendship.status == "pending" -> RelationshipStatus.IncomingRequest
                    else -> RelationshipStatus.NotConnected
                }
                val common = (me?.hobbies ?: emptyList())
                    .intersect(friend.hobbies.toSet())
                    .toList()
                FriendDetailState.Loaded(friend, relationship, common) as FriendDetailState
            }
        }
            .catch { e -> emit(FriendDetailState.Error(e.localizedMessage ?: "Failed to load")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FriendDetailState.Loading
            )
    }

    fun sendFriendRequest() {
        val uid = currentUid ?: return
        if (friendUid.isBlank() || friendUid == uid) return
        viewModelScope.launch {
            friendshipRepository.sendRequest(uid, friendUid)
        }
    }

    fun acceptFriendRequest() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            friendshipRepository.acceptRequest(Friendship.buildId(uid, friendUid))
        }
    }

    /** Used for "Remove Friend" AND "Decline incoming request" — both delete the doc. */
    fun removeFriendship() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            friendshipRepository.removeFriendship(Friendship.buildId(uid, friendUid))
        }
    }
}