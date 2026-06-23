package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
import com.example.livemap.aux_files.geocodeLocation
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.EventRepository
import com.example.livemap.data.model.User
import com.example.livemap.data.repository.UserRepository
import com.example.livemap.data.repository.FriendshipRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class EventDetailState {
    object Loading : EventDetailState()
    data class Loaded(val event: Event, val isOwner: Boolean, val isJoined: Boolean) : EventDetailState()
    data class Error(val message: String) : EventDetailState()
}

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository = EventRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val friendshipRepository: FriendshipRepository = FriendshipRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<EventDetailState>(EventDetailState.Loading)
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    private val currentUid = authRepository.currentUser()?.uid

    val allUsers: StateFlow<List<User>> = userRepository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<User>> = if (currentUid == null) {
        MutableStateFlow(emptyList())
    } else {
        combine(
            allUsers,
            friendshipRepository.observeFriendships(currentUid)
        ) { users, friendships ->
            val friendIds = friendships
                .filter { it.status == "accepted" }
                .flatMap { it.userIds }
                .filter { it != currentUid }
                .toSet()
            users.filter { it.uid in friendIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }


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

    /**
     * Updates the event. Mirrors the location handling of NewEventViewModel:
     *   - If [pickedLat]/[pickedLng] are provided (the owner picked a point on the
     *     map), they are used as-is.
     *   - Otherwise, if the address text changed, it is geocoded to validate it.
     *     A failed lookup leaves the event untouched and reports "Invalid location".
     *   - If the address text is unchanged, the existing coordinates are kept.
     */
    fun updateEvent(updatedEvent: Event, pickedLat: Double? = null, pickedLng: Double? = null) {
        if (currentUid == null) return
        val loaded = _state.value as? EventDetailState.Loaded ?: return
        if (!loaded.isOwner) return
        val original = loaded.event

        viewModelScope.launch {
            // Resolve coordinates: prefer the ones picked on the map; otherwise
            // geocode the typed address only if it changed, else keep the old ones.
            var lat = pickedLat
            var lng = pickedLng
            if (lat == null || lng == null) {
                if (updatedEvent.locationText != original.locationText) {
                    val geo = geocodeLocation(updatedEvent.locationText)
                    if (geo == null) {
                        _state.value = EventDetailState.Error("Invalid location")
                        return@launch
                    }
                    lat = geo.lat
                    lng = geo.lng
                } else {
                    lat = original.locationLat
                    lng = original.locationLng
                }
            }

            val eventToSave = updatedEvent.copy(locationLat = lat, locationLng = lng)
            eventRepository.updateEvent(eventId, eventToSave).onSuccess {
                loadEvent()
            }.onFailure { e ->
                _state.value = EventDetailState.Error(e.localizedMessage ?: "Failed to update event")
            }
        }
    }

    fun deleteEvent(onSuccess: () -> Unit) {
        if (currentUid == null || (_state.value as? EventDetailState.Loaded)?.isOwner != true) return

        viewModelScope.launch {
            eventRepository.deleteEvent(eventId).onSuccess {
                onSuccess()
            }.onFailure { e ->
                _state.value = EventDetailState.Error(e.localizedMessage ?: "Failed to delete event")
            }
        }
    }
}
