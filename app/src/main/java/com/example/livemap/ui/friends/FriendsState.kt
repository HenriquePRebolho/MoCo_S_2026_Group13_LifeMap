package com.example.livemap.ui.friends

/**
 * UI model for the FriendsScreen design. Mapped from data.model.User + data.model.Friendship
 * inside FriendsViewModel.
 *
 * IMPORTANT: id changed from Int to String to match Firestore uids.
 */
data class FriendUi(
    val id: String,                 // = Firestore uid
    val name: String,
    val age: Int,                   // computed from User.birthday; 0 if unknown
    val location: String,
    val bio: String,
    val interests: List<String>     // = User.hobbies
)

sealed class FriendsState {
    data object Loading : FriendsState()
    data class Loaded(
        val myFriends: List<FriendUi>,
        val incomingRequests: List<FriendUi>,
        // Pending requests the current user SENT (status pending, requestedBy == me).
        // Rendered as "Request Sent" so the relationship is visible to the sender.
        val outgoingRequests: List<FriendUi>,
        val suggested: List<FriendUi>,
        val myInterests: List<String>
    ) : FriendsState()
    data class Error(val message: String) : FriendsState()
}