package com.example.livemap.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livemap.aux_files.LatLng
import com.example.livemap.aux_files.event_types
import com.example.livemap.aux_files.haversineKm
import com.example.livemap.aux_files.matchesDistanceFilter
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

    // The user's current location, pushed in by the screen (which owns the
    // Context + permission). null until resolved — distance filtering and the
    // distance label fall back gracefully while it's null.
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    /** Called by the screen once it has the device location (or to update it). */
    fun setUserLocation(location: LatLng?) { _userLocation.value = location }

    /**
     * Main state. Emits Loaded whenever:
     *   - The events list changes (Firestore snapshot)
     *   - The filters change
     *   - The recentlyJoined list changes
     *   - The user location changes (recomputes distances + distance filter)
     */
    val state: StateFlow<EventsState> = if (currentUid == null) {
        MutableStateFlow<EventsState>(EventsState.Error("Not signed in")).asStateFlow()
    } else {
        kotlinx.coroutines.flow.combine(
            eventRepository.observeAllEvents(),
            _filters,
            _recentlyJoinedIds,
            _userLocation
        ) { events, filters, recentIds, userLoc ->
            val mapped = events.map { it.toUi(currentUid, userLoc) }
            val filteredAll = mapped.applyFilters(filters, userLoc != null)
            // Index by id once so "recently joined" lookups are O(1) instead of
            // scanning the whole list per id (was O(recent × events)).
            val byId = mapped.associateBy { it.id }

            // Real categories present among events (category + tags), in configs.kt
            // order, plus per-category event counts — same approach as the map.
            val present = buildSet { mapped.forEach { add(it.category); addAll(it.tags) } }
            val availableCategories = event_types.filter { it in present }
            val categoryCounts = mapped
                .flatMap { (listOf(it.category) + it.tags).distinct() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()

            // Past events are pulled out of the active sections and grouped on
            // their own, newest first.
            EventsState.Loaded(
                nearby = filteredAll.filter { !it.ownedByMe && !it.joinedByMe && !it.isPast },
                joined = mapped.filter { it.joinedByMe && !it.isPast },
                recentlyJoined = recentIds.mapNotNull { byId[it] }.filter { !it.isPast },
                owned = mapped.filter { it.ownedByMe && !it.isPast },
                past = mapped.filter { it.isPast }.sortedByDescending { it.startMillis ?: 0L },
                availableCategories = availableCategories,
                categoryCounts = categoryCounts
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

    private fun Event.toUi(uid: String, userLoc: LatLng?): EventUi = EventUi(
        id = id,
        name = name,
        category = category,
        date = dateTime.toDisplayDate(),
        time = dateTime.toDisplayTime(),
        location = locationText,
        // Real distance from the user, when we have both the user's location and
        // coordinates for the event. null otherwise (unknown).
        distanceKm = distanceFromUser(userLoc),
        ageRange = ageRange,
        genderPref = genderPref,
        joined = participantIds.size,
        maxPeople = limitPeople,
        ownedByMe = ownerId == uid,
        joinedByMe = participantIds.contains(uid),
        timeBucket = dateTime.toTimeBucket(),
        tags = tags,
        isPast = dateTime.isBeforeToday(),
        startMillis = dateTime?.toDate()?.time
    )

    /** True when the timestamp falls on a day earlier than today. */
    private fun Timestamp?.isBeforeToday(): Boolean {
        val date = this?.toDate() ?: return false
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return date.before(todayStart)
    }

    /**
     * Computes the event's distance from the user, or null when it can't be known
     * (no user location yet, or the event has no real coordinates).
     */
    private fun Event.distanceFromUser(userLoc: LatLng?): Double? {
        if (userLoc == null) return null
        val hasCoords = locationLat != 0.0 || locationLng != 0.0
        if (!hasCoords) return null
        return haversineKm(userLoc.lat, userLoc.lng, locationLat, locationLng)
    }

    private fun List<EventUi>.applyFilters(f: EventFilters, hasLocation: Boolean): List<EventUi> = filter { ev ->
        // Match the event's main category OR any of its tags, like the map filter.
        val byCategory  = f.category  == null || ev.category == f.category || ev.tags.contains(f.category)
        val byAge       = f.ageRange  == null || f.ageRange == "Any" || ev.ageRange == f.ageRange
        val byGender    = f.gender    == null || f.gender == "Any" || ev.genderPref == f.gender || ev.genderPref == "Any"
        val byTime      = f.time      == null || f.time == "Any" || ev.timeBucket == f.time
        val byMaxPeople = when (f.maxPeople) {
            "5"   -> ev.maxPeople <= 5
            "10"  -> ev.maxPeople <= 10
            "20"  -> ev.maxPeople <= 20
            else  -> true
        }
        // Distance filter:
        //  - no filter selected            → pass.
        //  - no user location available    → pass (UI shows "location unavailable").
        //  - event distance unknown        → only the widest "50+ km" option keeps it.
        //  - otherwise                     → within the selected radius.
        val byDistance = when {
            f.distance == null -> true
            !hasLocation       -> true
            ev.distanceKm == null -> f.distance.contains("+")
            else               -> matchesDistanceFilter(f.distance, ev.distanceKm)
        }
        byCategory && byAge && byGender && byTime && byMaxPeople && byDistance
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