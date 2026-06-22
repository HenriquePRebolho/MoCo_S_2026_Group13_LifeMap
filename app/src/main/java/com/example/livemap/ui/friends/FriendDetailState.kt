package com.example.livemap.ui.friends


import com.example.livemap.data.model.User

/**
 * Describes the relationship between the current user and the friend being
 * viewed. The action button at the bottom of the detail screen depends on this:
 *   NotConnected     → "Add Friend"
 *   Pending          → "Request Sent" (disabled)
 *   IncomingRequest  → "Decline" + "Accept"
 *   Friends          → "Remove Friend"
 *   Self             → no button (you're viewing your own profile)
 */
sealed class RelationshipStatus {
    data object NotConnected : RelationshipStatus()
    data object Pending : RelationshipStatus()
    data object IncomingRequest : RelationshipStatus()
    data object Friends : RelationshipStatus()
    data object Self : RelationshipStatus()
}

/**
 * State of the FriendDetailScreen.
 *
 * Loaded carries:
 *   - the friend's User document
 *   - the relationship with the current user
 *   - the interests both share (precomputed in the VM to keep the UI dumb)
 */
sealed class FriendDetailState {
    data object Loading : FriendDetailState()
    data class Loaded(
        val user: User,
        val relationship: RelationshipStatus,
        val commonInterests: List<String>
    ) : FriendDetailState()
    data class Error(val message: String) : FriendDetailState()
}