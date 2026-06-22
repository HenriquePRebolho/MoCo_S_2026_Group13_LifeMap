package com.example.livemap.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Friendship
import com.example.livemap.data.model.User
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.FriendshipRepository
import com.example.livemap.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Drives FriendsScreen.
 *
 * Combines THREE Flows into one screen state:
 *   1. observeAllUsers — for "Suggested" and current user's info
 *   2. observeFriendships(currentUid) — to know who's accepted/pending
 *   3. _sentRequestIds — session-local set of friendship IDs we just sent
 *
 * Partition logic:
 *   myFriends        = users in accepted friendships
 *   incomingRequests = users in pending friendships where requestedBy != me
 *   suggested        = users that are NOT me, NOT in any friendship with me
 */
class FriendsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val friendshipRepository: FriendshipRepository = FriendshipRepository()
) : ViewModel() {

    private val currentUid: String? = authRepository.currentUser()?.uid

    private val _sentRequestIds = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<FriendsState> = if (currentUid == null) {
        MutableStateFlow<FriendsState>(FriendsState.Error("Not signed in")).asStateFlow()
    } else {
        combine(
            userRepository.observeAllUsers(),
            friendshipRepository.observeFriendships(currentUid),
            _sentRequestIds
        ) { allUsers, friendships, sentIds ->
            val me = allUsers.firstOrNull { it.uid == currentUid }

            // Map friendships by the OTHER user's uid for quick lookup.
            val friendshipByOtherUid: Map<String, Friendship> = friendships
                .associateBy { friendship ->
                    friendship.userIds.firstOrNull { it != currentUid } ?: ""
                }

            val accepted = allUsers.filter { user ->
                user.uid != currentUid &&
                        friendshipByOtherUid[user.uid]?.status == "accepted"
            }

            val incoming = allUsers.filter { user ->
                user.uid != currentUid &&
                        friendshipByOtherUid[user.uid]?.let { f ->
                            f.status == "pending" && f.requestedBy != currentUid
                        } == true
            }

            val suggested = allUsers.filter { user ->
                user.uid != currentUid &&
                        friendshipByOtherUid[user.uid] == null
            }

            FriendsState.Loaded(
                myFriends = accepted.map { it.toFriendUi() },
                incomingRequests = incoming.map { it.toFriendUi() },
                suggested = suggested.map { it.toFriendUi() },
                sentRequestFriendshipIds = sentIds,
                myInterests = me?.hobbies ?: emptyList()
            ) as FriendsState
        }
            .catch { e -> emit(FriendsState.Error(e.localizedMessage ?: "Failed to load friends")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FriendsState.Loading
            )
    }

    /** Sends a friend request to the given user. */
    fun sendFriendRequest(toUid: String) {
        val uid = currentUid ?: return
        val friendshipId = Friendship.buildId(uid, toUid)
        viewModelScope.launch {
            friendshipRepository.sendRequest(uid, toUid).onSuccess {
                _sentRequestIds.value = _sentRequestIds.value + friendshipId
            }
        }
    }

    /** Accepts an incoming friend request from the given user. */
    fun acceptFriendRequest(fromUid: String) {
        val uid = currentUid ?: return
        val friendshipId = Friendship.buildId(uid, fromUid)
        viewModelScope.launch {
            friendshipRepository.acceptRequest(friendshipId)
        }
    }

    // ── Mapping ──

    private fun User.toFriendUi(): FriendUi = FriendUi(
        id = uid,
        name = displayName.ifBlank { "(no name)" },
        age = birthday.toAge(),
        location = location.ifBlank { "—" },
        bio = description.ifBlank { "No bio yet" },
        interests = hobbies
    )

    private fun Timestamp?.toAge(): Int {
        if (this == null) return 0
        val birth = Calendar.getInstance().apply { time = toDate() }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }
}