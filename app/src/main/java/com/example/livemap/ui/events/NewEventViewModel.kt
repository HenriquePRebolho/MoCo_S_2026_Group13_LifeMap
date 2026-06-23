package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
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

    fun createEvent(
        name: String,
        description: String,
        locationText: String,
        dateTime: Date?,
        isPublic: Boolean,
        limitPeople: Int,
        tags: List<String>,
        invitedFriends: List<String>
    ) {
        val currentUser = authRepository.currentUser()
        if (currentUser == null) {
            _state.value = NewEventState.Error("User not authenticated")
            return
        }

        _state.value = NewEventState.Submitting

        viewModelScope.launch {
            val event = Event(
                name = name,
                description = description,
                ownerId = currentUser.uid,
                dateTime = dateTime?.let { Timestamp(it) },
                locationText = locationText,
                // Default coordinates for now, or we could geocode if we had a service ready
                locationLat = 0.0,
                locationLng = 0.0,
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
