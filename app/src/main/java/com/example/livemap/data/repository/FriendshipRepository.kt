package com.example.livemap.data.repository

import com.example.livemap.data.model.Friendship
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Single source of truth for the /friendships collection.
 *
 * Document ID convention:
 *   friendshipId = "{smallerUid}_{largerUid}"  (see Friendship.buildId)
 *   This guarantees that there cannot be two documents for the same pair,
 *   regardless of who sent the request first.
 *
 * Status field:
 *   "pending"  → request sent, awaiting acceptance from the other user
 *   "accepted" → both users are friends
 *   "blocked"  → one user blocked the other (see blockedBy)
 */
class FriendshipRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val friendshipsCollection = db.collection("friendships")

    /**
     * Emits all friendships involving the given user, regardless of status.
     * The ViewModel partitions them into "accepted" and "pending requests".
     */
    fun observeFriendships(uid: String): Flow<List<Friendship>> = callbackFlow {
        val registration = friendshipsCollection
            .whereArrayContains("userIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                // Guard toObject so a single malformed friendship document doesn't
                // throw on the main thread and crash the screen (see UserRepository).
                val friendships = snapshot?.documents
                    ?.mapNotNull { runCatching { it.toObject(Friendship::class.java) }.getOrNull() }
                    ?: emptyList()
                trySend(friendships)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Sends a friend request from `fromUid` to `toUid`.
     * If a friendship document already exists between these two users
     * (regardless of order), this overwrites it with the new pending state.
     * Using set() with merge=false is intentional: we want to reset accepted/blocked
     * states if the user re-requests after a removal.
     */
    suspend fun sendRequest(fromUid: String, toUid: String): Result<Unit> = runCatching {
        val docId = Friendship.buildId(fromUid, toUid)
        val data = mapOf(
            // userIds is stored sorted for consistent queries (whereArrayContains works regardless,
            // but having a canonical order helps clients).
            "userIds" to listOf(fromUid, toUid).sorted(),
            "status" to "pending",
            "requestedBy" to fromUid,
            "blockedBy" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "acceptedAt" to null
        )
        friendshipsCollection.document(docId).set(data).await()
    }

    /**
     * Accepts an incoming friend request.
     * Updates status to "accepted" and sets acceptedAt.
     */
    suspend fun acceptRequest(friendshipId: String): Result<Unit> = runCatching {
        friendshipsCollection.document(friendshipId)
            .update(
                mapOf(
                    "status" to "accepted",
                    "acceptedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    /** Cancels a pending request or removes an existing friendship. */
    suspend fun removeFriendship(friendshipId: String): Result<Unit> = runCatching {
        friendshipsCollection.document(friendshipId).delete().await()
    }
}