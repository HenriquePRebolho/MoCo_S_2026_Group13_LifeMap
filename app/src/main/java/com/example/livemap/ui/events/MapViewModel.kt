package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the markers on MapScreen.
 *
 * Reuses the existing EventRepository (single source of truth for /events) so
 * the map shows the SAME events as the Events tab — including the ones the user
 * creates, which now carry real coordinates. No parallel data source.
 *
 * Visibility: private events (isPublic = false) are only shown to people who
 * can already see them — the owner and the participants — so "invite only"
 * events don't leak onto everyone's map.
 *
 * Filtering by event type happens in the UI (MapScreen) over this list, using
 * the categories defined in aux_files/configs.kt.
 */
class MapViewModel(
    authRepository: AuthRepository = AuthRepository(),
    eventRepository: EventRepository = EventRepository()
) : ViewModel() {

    private val currentUid: String? = authRepository.currentUser()?.uid

    /** Visible events from Firestore; the screen filters/places markers from this. */
    val events: StateFlow<List<Event>> = eventRepository.observeAllEvents()
        .map { all ->
            all.filter { event ->
                event.isPublic ||
                        event.ownerId == currentUid ||
                        event.participantIds.contains(currentUid)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
