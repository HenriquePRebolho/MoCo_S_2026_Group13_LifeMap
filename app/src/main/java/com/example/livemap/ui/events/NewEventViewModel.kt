package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
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
