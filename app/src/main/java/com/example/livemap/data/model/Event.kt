package com.example.livemap.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Represents a social event hosted by a user.
 *
 * Mapped 1:1 with documents in the Firestore "events" collection.
 */
data class Event(
    val id: String = "",

    val name: String = "",
    //val nameLower: String = "",          // for case-insensitive search
    val description: String = "",

    // Owner (= creator). UID references /users/{uid}
    val ownerId: String = "",
//    val ownerName: String = "",          // denormalized
//    val ownerPhotoUrl: String? = null,   // denormalized

    // Start/end combine date and time into a single sortable value.
    val dateTime: Timestamp? = null,
//    val endAt: Timestamp? = null,

    // Location - text + coordinates. Required for placing markers on the map.
    val locationText: String = "",
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,

    // Geohash for "events near me" queries.
    val geohash: String = "",

    // Event cover photo (Firebase Storage URL)
    //val photoUrl: String? = null,

    // Visibility: true = anyone can see | false = invite only (future)
    val isPublic: Boolean = true,

    // 0 = no participant limit
    val limitPeople: Int = 0,

    // Denormalized counter - kept in sync with participantIds.size to enable fast filtering
    //val participantsCount: Int = 0,

    // Array of user UIDs who joined this event.
    val participantIds: List<String> = emptyList(),

    val tags: List<String> = emptyList(),
//    val languages: List<String> = emptyList(),
//    val restrictions: List<String> = emptyList(),
//    val itemsToBring: List<String> = emptyList(),

    // Contact info shown on the event detail screen (set by the organizer)
//    val contactPhone: String = "",
//    val contactInstagram: String = "",

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,

    // ── FIELDS for the EventsScreen design ──────────────────────────
    // Single category used by the filter chips on the Events screen.
    // Expected values: "Sport", "Study", "Social", "Art", "Food", "Music".
    // Empty string = uncategorized (shown as "Other" in the UI).
    val category: String = "",

    // Target age range for the event: "18-25", "26-35", "36-45", "Any".
    val ageRange: String = "Any",

    // Gender preference of the organizer: "Any", "Male", "Female", "Mixed".
    // The filter treats "Any" as matching everything.
    val genderPref: String = "Any",
    // ────────────────────────────────────────────────────────────────────
)