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

sealed class EventDetailState {
    object Loading : EventDetailState()
    data class Loaded(val event: Event, val isOwner: Boolean, val isJoined: Boolean) : EventDetailState()
    data class Error(val message: String) : EventDetailState()
}

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository = EventRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<EventDetailState>(EventDetailState.Loading)
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    private val currentUid = authRepository.currentUser()?.uid

    init {
        loadEvent()
    }

    fun loadEvent() {
        if (currentUid == null) {
            _state.value = EventDetailState.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            eventRepository.getEvent(eventId).onSuccess { event ->
                if (event != null) {
                    _state.value = EventDetailState.Loaded(
                        event = event,
                        isOwner = event.ownerId == currentUid,
                        isJoined = event.participantIds.contains(currentUid)
                    )
                } else {
                    _state.value = EventDetailState.Error("Event not found")
                }
            }.onFailure { e ->
                _state.value = EventDetailState.Error(e.localizedMessage ?: "Failed to load event")
            }
        }
    }

    fun toggleJoin() {
        val uid = currentUid ?: return
        val currentState = _state.value as? EventDetailState.Loaded ?: return
        
        viewModelScope.launch {
            if (currentState.isJoined) {
                eventRepository.leaveEvent(eventId, uid).onSuccess { loadEvent() }
            } else {
                eventRepository.joinEvent(eventId, uid).onSuccess { loadEvent() }
            }
        }
    }

    fun updateEvent(updatedEvent: Event) {
        if (currentUid == null || (_state.value as? EventDetailState.Loaded)?.isOwner != true) return

        viewModelScope.launch {
            // Re-using createEvent logic but for update. 
            // Better to have a dedicated updateEvent in repository, let's check if it exists or add it.
            // For now, let's assume we need to add updateEvent to EventRepository or use a generic update.
            eventRepository.updateEvent(eventId, updatedEvent).onSuccess {
                loadEvent()
            }.onFailure { e ->
                _state.value = EventDetailState.Error(e.localizedMessage ?: "Failed to update event")
            }
        }
    }
}
