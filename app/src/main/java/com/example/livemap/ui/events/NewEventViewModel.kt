package com.example.livemap.ui.events

import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Date

sealed class NewEventState {
    object Idle : NewEventState()
    object Submitting : NewEventState()
    data class Success(val eventId: String) : NewEventState()
    data class Error(val message: String) : NewEventState()
}

class NewEventViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val friendshipRepository: FriendshipRepository = FriendshipRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<NewEventState>(NewEventState.Idle)
    val state: StateFlow<NewEventState> = _state.asStateFlow()

    // ── Form state ────────────────────────────────────────────────────────────
    // Held in the ViewModel (not in the screen's remember) so it survives
    // navigating to the map location picker and back: that navigation disposes
    // NewScreen's composition, which would otherwise wipe plain remember values.
    val eventName = mutableStateOf("")
    val eventDescription = mutableStateOf("")
    val eventLocation = mutableStateOf("")
    val selectedDateTime = mutableStateOf<LocalDateTime?>(null)
    val peopleLimit = mutableStateOf("")
    val isPublic = mutableStateOf(true)
    val addedEvents = mutableStateOf<List<String>>(emptyList())
    val addedFriendIds = mutableStateOf<List<String>>(emptyList())
    val eventLat = mutableStateOf<Double?>(null)
    val eventLng = mutableStateOf<Double?>(null)

    /** Clears the whole form (used after a successful creation). */
    fun resetForm() {
        eventName.value = ""
        eventDescription.value = ""
        eventLocation.value = ""
        selectedDateTime.value = null
        peopleLimit.value = ""
        isPublic.value = true
        addedEvents.value = emptyList()
        addedFriendIds.value = emptyList()
        eventLat.value = null
        eventLng.value = null
    }

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
