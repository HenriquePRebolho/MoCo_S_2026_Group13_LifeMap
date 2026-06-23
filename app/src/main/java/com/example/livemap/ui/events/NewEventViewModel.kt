package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
import com.example.livemap.aux_files.geocodeLocation
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.EventRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class NewEventState {
    object Idle : NewEventState()
    object Submitting : NewEventState()
    data class Success(val eventId: String) : NewEventState()
    data class Error(val message: String) : NewEventState()
}

class NewEventViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<NewEventState>(NewEventState.Idle)
    val state: StateFlow<NewEventState> = _state.asStateFlow()

    /**
     * Creates an event.
     *
     * Location handling:
     *   - If [locationLat]/[locationLng] are provided (the user picked a point on
     *     the map), they are used as-is.
     *   - Otherwise the [locationText] typed manually is geocoded to validate it.
     *     If Nominatim finds no match, the event is NOT created and the state
     *     becomes Error("Invalid location") so the screen can show it.
     */
    fun createEvent(
        name: String,
        description: String,
        locationText: String,
        dateTime: Date?,
        isPublic: Boolean,
        limitPeople: Int,
        tags: List<String>,
        invitedFriends: List<String>,
        locationLat: Double? = null,
        locationLng: Double? = null
    ) {
        val currentUser = authRepository.currentUser()
        if (currentUser == null) {
            _state.value = NewEventState.Error("User not authenticated")
            return
        }

        _state.value = NewEventState.Submitting

        viewModelScope.launch {
            // Resolve coordinates: prefer the ones picked on the map; otherwise
            // geocode the manually typed address to validate it.
            var lat = locationLat
            var lng = locationLng
            if (lat == null || lng == null) {
                val geo = geocodeLocation(locationText)
                if (geo == null) {
                    _state.value = NewEventState.Error("Invalid location")
                    return@launch
                }
                lat = geo.lat
                lng = geo.lng
            }

            val event = Event(
                name = name,
                description = description,
                ownerId = currentUser.uid,
                dateTime = dateTime?.let { Timestamp(it) },
                locationText = locationText,
                locationLat = lat,
                locationLng = lng,
                geohash = "",
                isPublic = isPublic,
                limitPeople = limitPeople,
                // Store the invited friends' strings in participantIds as requested
                participantIds = invitedFriends,
                tags = tags,
                category = tags.firstOrNull() ?: ""
            )

            eventRepository.createEvent(event)
                .onSuccess { id ->
                    _state.value = NewEventState.Success(id)
                }
                .onFailure { e ->
                    _state.value = NewEventState.Error(e.localizedMessage ?: "Failed to create event")
                }
        }
    }

    fun resetState() {
        _state.value = NewEventState.Idle
    }
}
