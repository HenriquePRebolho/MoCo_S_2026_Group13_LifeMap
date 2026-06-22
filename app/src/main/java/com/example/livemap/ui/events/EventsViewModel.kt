package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.data.model.Event
import com.example.livemap.data.repository.AuthRepository
import com.example.livemap.data.repository.EventRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Drives the EventsScreen.
 *
 * Responsibilities:
 *   - Observe ALL events from EventRepository and partition them into:
 *       nearby   = available to join (not owned, not joined)
 *       joined   = events I joined
 *       owned    = events I created
 *   - Track "recently joined" as session-local state
 *   - Apply UI filters
 *   - Expose join/leave actions
 */
class EventsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val eventRepository: EventRepository = EventRepository()
) : ViewModel() {

    private val currentUid: String? = authRepository.currentUser()?.uid

    // Filter state, owned by the VM so it survives recompositions.
    // The screen reads/writes this via updateFilters().
    private val _filters = MutableStateFlow(EventFilters())
    val filters: StateFlow<EventFilters> = _filters.asStateFlow()

    // Session-only tracker for "Recently joined" section.
    // Stores event IDs in chronological order (newest first).
    private val _recentlyJoinedIds = MutableStateFlow<List<String>>(emptyList())

    /**
     * Main state. Emits Loaded whenever:
     *   - The events list changes (Firestore snapshot)
     *   - The filters change
     *   - The recentlyJoined list changes
     * Limitation: distance filtering is a no-op for now — we don't have the
     * user's current location wired in. Will be a follow-up when we connect
     * the location service from MapScreen.
     */
    val state: StateFlow<EventsState> = if (currentUid == null) {
        MutableStateFlow<EventsState>(EventsState.Error("Not signed in")).asStateFlow()
    } else {
        kotlinx.coroutines.flow.combine(
            eventRepository.observeAllEvents(),
            _filters,
            _recentlyJoinedIds
        ) { events, filters, recentIds ->
            val mapped = events.map { it.toUi(currentUid) }
            val filteredAll = mapped.applyFilters(filters)

            EventsState.Loaded(
                nearby = filteredAll.filter { !it.ownedByMe && !it.joinedByMe },
                joined = mapped.filter { it.joinedByMe },
                recentlyJoined = recentIds.mapNotNull { id -> mapped.find { it.id == id } },
                owned = mapped.filter { it.ownedByMe }
            ) as EventsState
        }
            .catch { e -> emit(EventsState.Error(e.localizedMessage ?: "Failed to load events")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EventsState.Loading
            )
    }

    fun setDistance(value: String?)  { _filters.value = _filters.value.copy(distance = value) }
    fun setCategory(value: String?)  { _filters.value = _filters.value.copy(category = value) }
    fun setAge(value: String?)       { _filters.value = _filters.value.copy(ageRange = value) }
    fun setGender(value: String?)    { _filters.value = _filters.value.copy(gender = value) }
    fun setMaxPeople(value: String?) { _filters.value = _filters.value.copy(maxPeople = value) }
    fun setTime(value: String?)      { _filters.value = _filters.value.copy(time = value) }
    fun clearFilters()               { _filters.value = EventFilters() }

    /**
     * Joins an event. On success, also adds it to the session-local
     * "recentlyJoined" list so the UI section shows it immediately.
     */
    fun joinEvent(eventId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            eventRepository.joinEvent(eventId, uid).onSuccess {
                _recentlyJoinedIds.value =
                    listOf(eventId) + _recentlyJoinedIds.value.filter { it != eventId }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            eventRepository.leaveEvent(eventId, uid)
        }
    }

    // ── Mapping helpers (Firestore Event → UI EventUi) ──

    private fun Event.toUi(uid: String): EventUi = EventUi(
        id = id,
        name = name,
        category = category,
        date = startAt.toDisplayDate(),
        time = startAt.toDisplayTime(),
        location = locationText,
        // Placeholder until we wire in the user's GPS location.
        distanceKm = 0.0,
        ageRange = ageRange,
        genderPref = genderPref,
        joined = participantIds.size,
        maxPeople = limitPeople,
        ownedByMe = ownerId == uid,
        joinedByMe = participantIds.contains(uid),
        timeBucket = startAt.toTimeBucket()
    )

    private fun List<EventUi>.applyFilters(f: EventFilters): List<EventUi> = filter { ev ->
        val byCategory  = f.category  == null || ev.category  == f.category
        val byAge       = f.ageRange  == null || f.ageRange == "Any" || ev.ageRange == f.ageRange
        val byGender    = f.gender    == null || f.gender == "Any" || ev.genderPref == f.gender || ev.genderPref == "Any"
        val byTime      = f.time      == null || f.time == "Any" || ev.timeBucket == f.time
        val byMaxPeople = when (f.maxPeople) {
            "5"   -> ev.maxPeople <= 5
            "10"  -> ev.maxPeople <= 10
            "20"  -> ev.maxPeople <= 20
            else  -> true
        }
        // Distance filter is a no-op until we add user location.
        byCategory && byAge && byGender && byTime && byMaxPeople
    }

    private fun Timestamp?.toDisplayDate(): String {
        if (this == null) return "TBD"
        val cal = Calendar.getInstance().apply { time = toDate() }
        val today = Calendar.getInstance()
        return when {
            cal.isSameDay(today) -> "Today"
            cal.isSameDay(today.apply { add(Calendar.DAY_OF_YEAR, 1) }) -> "Tomorrow"
            else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(toDate())
        }
    }

    private fun Timestamp?.toDisplayTime(): String =
        this?.toDate()?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--"

    private fun Timestamp?.toTimeBucket(): String {
        if (this == null) return "Any"
        val cal = Calendar.getInstance().apply { time = toDate() }
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
        return when {
            cal.isSameDay(today) -> "Today"
            cal.isSameDay(tomorrow) -> "Tomorrow"
            cal.before(weekEnd) -> "This week"
            else -> "Any"
        }
    }

    private fun Calendar.isSameDay(other: Calendar): Boolean =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}