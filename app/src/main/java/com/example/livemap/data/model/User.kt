package com.example.livemap.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Represents a LifeMap user.
 *
 * Mapped 1:1 with documents in the Firestore "users" collection.
 * The document ID is the Firebase Auth uid - same value as the `uid` field.
 *
 * IMPORTANT for Firestore serialization:
 *   - All fields must have default values (no-argument constructor required).
 *   - All fields are `var` or have defaults so Firestore can populate them
 *     via reflection when deserializing documents.
 *   - Password is NOT stored here, Firebase Auth handles credentials.
 */
data class User(
    // The Firestore document ID (== Firebase Auth uid). It is NOT stored as a
    // field in the document, so without @DocumentId every User deserialized to
    // uid = "". That made all FriendUi keys equal "" and crashed FriendsScreen's
    // LazyColumn ("Key \"\" was already used"). @DocumentId tells Firestore to
    // populate this from the document ID on read.
    @DocumentId
    val uid: String = "",

    // Display name shown across the app
    val displayName: String = "",

    // Lowercase copy of displayName - used for case-insensitive search.
    // Firestore doesn't support case-insensitive queries.
    val displayNameLower: String = "",

    val email: String = "",

    val description: String = "",

    // Optional fields (null = "not set yet")
    val birthday: Timestamp? = null,

    // "Male" | "Female" | "Other" | "" (not set)
    val sex: String = "",

    // URL pointing to Firebase Storage. Null = use default avatar.
    val photoUrl: String? = null,

    val languages: List<String> = emptyList(),

    val hobbies: List<String> = emptyList(),

    // Human-readable address
    val location: String = "",

    // Coordinates - null if user hasn't set a location yet
    val locationLat: Double? = null,
    val locationLng: Double? = null,

    val phone: String = "",

    val instagram: String = "",

    // Set by the server using FieldValue.serverTimestamp() on creation
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
