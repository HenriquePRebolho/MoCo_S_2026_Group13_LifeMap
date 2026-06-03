package com.example.livemap.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Represents a friendship/relationship between two users.
 *
 * Stored in the Firestore "friendships" collection.
 *
 * Document ID structure:
 *   The document ID is constructed deterministically as "{smallerUid}_{largerUid}",
 *   ensuring there can NEVER be two documents for the same pair of users.
 *   See Friendship.buildId() below.
 */
data class Friendship(
    @DocumentId
    val id: String = "",

    // Always exactly 2 UIDs, sorted alphabetically (smaller first).
    val userIds: List<String> = emptyList(),

    // "pending"  — request sent, awaiting acceptance
    // "accepted" — both users are friends
    // "blocked"  — one user blocked the other
    val status: String = "pending",

    // Used to distinguish "incoming requests" from "outgoing requests".
    val requestedBy: String = "",

    // If status == "blocked", stores the UID who did the blocking.
    val blockedBy: String? = null,

    val createdAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null
) {
    companion object {
        /**
         * Builds the deterministic document ID for the friendship between
         * two users, regardless of the order in which they're passed.
         */
        fun buildId(uid1: String, uid2: String): String =
            if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
}