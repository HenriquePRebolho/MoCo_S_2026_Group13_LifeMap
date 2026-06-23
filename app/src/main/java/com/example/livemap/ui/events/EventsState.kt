package com.example.livemap.ui.events

/** Result handed back from the map location picker (see LocationPickerScreen). */
data class PickedLocation(val lat: Double, val lng: Double, val address: String)

/**
 * The aesthetic UI model from the team's design. We map data.model.Event → EventUi
 * inside the ViewModel so EventsScreen doesn't need to know about Firestore types.
 *
 * IMPORTANT: id changed from Int to String to match Firestore document IDs.
 */
data class EventUi(
    val id: String,
    val name: String,
    val category: String,
    val date: String,
    val time: String,
    val location: String,
    val distanceKm: Double,
    val ageRange: String,
    val genderPref: String,
    val joined: Int,
    val maxPeople: Int,
    val ownedByMe: Boolean = false,
    val joinedByMe: Boolean = false,
    val timeBucket: String = "Today",
    val tags: List<String> = emptyList()
)

/**
 * Filters held in the UI; passed to the ViewModel for application.
 * All null means "no filter applied".
 */
data class EventFilters(
    val distance: String? = null,
    val category: String? = null,
    val ageRange: String? = null,
    val gender: String? = null,
    val maxPeople: String? = null,
    val time: String? = null
)

/**
 * The screen state. We emit Loading until the first snapshot arrives, then Loaded.
 * Error covers "no signed-in user" or read failures.
 */
sealed class EventsState {
    data object Loading : EventsState()
    data class Loaded(
        val nearby: List<EventUi>,
        val joined: List<EventUi>,
        val recentlyJoined: List<EventUi>,
        val owned: List<EventUi>
    ) : EventsState()
    data class Error(val message: String) : EventsState()
}