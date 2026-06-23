package com.example.livemap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.aux_files.DistanceFilterOptions
import com.example.livemap.aux_files.event_types
import com.example.livemap.aux_files.fetchUserLatLng
import com.example.livemap.aux_files.geocodeLocation
import com.example.livemap.aux_files.haversineKm
import com.example.livemap.aux_files.matchesDistanceFilter
import com.example.livemap.data.model.Event
import com.example.livemap.ui.events.MapViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL


// ─── ROUTE MODE ──────────────────────────────────────────────────────────────
// Groups everything related to a route type in one place.
// Using an enum avoids repeating "driving" / "foot" and the color codes everywhere.
enum class RouteMode(
    val profile: String,       // parameter sent to the OSRM routing API
    val colorHex: String,      // hex color used when drawing the line on the map
    val colorCompose: Color    // same color in Compose format, used in buttons and legend
) {
    DRIVING("driving", "#2196F3", Color(0xFF2196F3)),  // blue  — car route
    FOOT   ("foot",    "#4CAF50", Color(0xFF4CAF50))   // green — walking route
}


// ─── CONSTANTS ───────────────────────────────────────────────────────────────

// Default location shown when the user denies location permission
private val DEFAULT_LOCATION = GeoPoint(37.3886, -5.9823) // Sevilla

// Data returned by the OSRM routing API
private data class RouteResult(
    val points: List<GeoPoint>,
    val distanceM: Double,
    val durationS: Double
)


// ─── MAP SCREEN ──────────────────────────────────────────────────────────────

// Participation filter: how the current user relates to an event.
// Mirrors the partitions used on the Events tab.
enum class ParticipationFilter(val label: String) {
    JOINED("Joined"),
    AVAILABLE("Can join"),
    OWNED("Created by me")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Real events from Firestore (same source as the Events tab). Three combinable
    // filters live in the "⋮" menu: category (type), distance and participation.
    val events by mapViewModel.events.collectAsStateWithLifecycle()
    val uid = mapViewModel.uid
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedDistance by remember { mutableStateOf<String?>(null) }
    var selectedParticipation by remember { mutableStateOf<ParticipationFilter?>(null) }
    var filterMenuOpen by remember { mutableStateOf(false) }

    // Events that actually have coordinates — the only ones that produce markers.
    val locatedEvents = remember(events) {
        events.filter { it.locationLat != 0.0 || it.locationLng != 0.0 }
    }
    // Type chips: only the configs.kt types present among located events,
    // keeping configs.kt order. Avoids showing 46 mostly-empty chips.
    val availableTypes = remember(locatedEvents) {
        val present = buildSet {
            locatedEvents.forEach { add(it.category); addAll(it.tags) }
        }
        event_types.filter { it in present }
    }
    // If the active filter is no longer available (events changed), clear it.
    LaunchedEffect(availableTypes) {
        if (selectedType != null && selectedType !in availableTypes) selectedType = null
    }
    // Number of (located) events per type, so the filter menu can show "type (N)".
    // distinct() per event avoids double-counting when category == first tag.
    val typeCounts = remember(locatedEvents) {
        locatedEvents
            .flatMap { (listOf(it.category) + it.tags).distinct() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
    }
    // Number of (located) events per participation status, relative to the current
    // user, so the participation filter can show "Joined (N)" etc.
    val participationCounts = remember(locatedEvents, uid) {
        ParticipationFilter.entries.associateWith { pf ->
            if (uid == null) 0 else locatedEvents.count { ev ->
                when (pf) {
                    ParticipationFilter.JOINED -> ev.participantIds.contains(uid)
                    ParticipationFilter.OWNED -> ev.ownerId == uid
                    ParticipationFilter.AVAILABLE -> ev.ownerId != uid && !ev.participantIds.contains(uid)
                }
            }
        }
    }

    // ── State variables ──────────────────────────────────────────────────────
    // remember { } keeps the value alive across recompositions (screen redraws).
    // mutableStateOf triggers a redraw whenever the value changes.

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation  by remember { mutableStateOf(DEFAULT_LOCATION) }
    var searchQuery   by remember { mutableStateOf("") }
    var isLoading     by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }

    var driveRoute    by remember { mutableStateOf<Polyline?>(null) }
    var walkRoute     by remember { mutableStateOf<Polyline?>(null) }
    var driveLabel    by remember { mutableStateOf("") }
    var walkLabel     by remember { mutableStateOf("") }
    var destMarker    by remember { mutableStateOf<Marker?>(null) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Keeps track of the event markers so we can remove them if the list changes
    val eventMarkers  = remember { mutableListOf<Marker>() }

    // ── Permission launcher ──────────────────────────────────────────────────
    // Shows the system "Allow location?" dialog. The result updates hasPermission.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // ── Map view ─────────────────────────────────────────────────────────────
    // remember { } ensures the MapView is created only once, not on every redraw.
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK) // OpenStreetMap tiles
            setMultiTouchControls(true)             // pinch to zoom
            controller.setZoom(15.0)
            controller.setCenter(DEFAULT_LOCATION)
        }
    }

    // The blue dot showing the user's position. Created once and kept alive.
    val userMarker = remember {
        Marker(mapView).apply {
            title = "Your location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            position = DEFAULT_LOCATION
            mapView.overlays.add(this)
        }
    }

    // ── Effects ──────────────────────────────────────────────────────────────
    // LaunchedEffect runs a coroutine (background task) tied to this screen.
    // DisposableEffect handles setup/cleanup tied to this screen's lifecycle.

    // Map setup + osmdroid User-Agent (required by OpenStreetMap)
    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // Ask for permission, then move the map to the real user location
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val loc = fetchLocation(context)
            if (loc != null) {
                userLocation = loc
                userMarker.position = loc
                mapView.controller.animateTo(loc)
                mapView.invalidate()
            }
        }
    }

    // Place a map marker for each real event that has coordinates and matches the
    // active filters (category + distance + participation, combined with AND).
    // Re-runs whenever the events list, any filter OR the user location changes,
    // so markers update in real time.
    LaunchedEffect(locatedEvents, selectedType, selectedDistance, selectedParticipation, userLocation) {
        // Remove old event markers before adding the new ones
        eventMarkers.forEach { mapView.overlays.remove(it) }
        eventMarkers.clear()

        locatedEvents.filter { ev ->
            val byType = selectedType == null ||
                    ev.category == selectedType ||
                    ev.tags.contains(selectedType)

            val byDistance = selectedDistance == null || matchesDistanceFilter(
                selectedDistance,
                haversineKm(userLocation.latitude, userLocation.longitude, ev.locationLat, ev.locationLng)
            )

            val byParticipation = when (selectedParticipation) {
                null -> true
                ParticipationFilter.JOINED -> uid != null && ev.participantIds.contains(uid)
                ParticipationFilter.OWNED  -> uid != null && ev.ownerId == uid
                ParticipationFilter.AVAILABLE ->
                    uid != null && ev.ownerId != uid && !ev.participantIds.contains(uid)
            }

            byType && byDistance && byParticipation
        }.forEach { event ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(event.locationLat, event.locationLng)
                title    = event.name
                snippet  = event.locationText
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Tapping the marker shows the event info card instead of the default popup
                setOnMarkerClickListener { _, _ ->
                    selectedEvent = event
                    true // true = we handled the click, don't show the default bubble
                }
            }
            mapView.overlays.add(0, marker) // add behind the user marker
            eventMarkers.add(marker)
        }
        mapView.invalidate()
    }


    // ── Helper functions ─────────────────────────────────────────────────────

    // Removes all drawn routes and resets the labels
    fun clearRoutes() {
        driveRoute?.let { mapView.overlays.remove(it) }
        walkRoute?.let  { mapView.overlays.remove(it) }
        destMarker?.let { mapView.overlays.remove(it) }
        driveRoute = null;  walkRoute  = null;  destMarker = null
        driveLabel = "";    walkLabel  = "";    errorMsg   = ""
        mapView.invalidate()
    }

    // Calculates and draws a single route (car OR foot) to a known coordinate.
    // Used when the user taps an event marker and picks a travel mode.
    fun routeToPoint(dest: GeoPoint, label: String, mode: RouteMode) {
        scope.launch {
            isLoading = true
            clearRoutes()
            try {
                // Pin the destination on the map
                destMarker = Marker(mapView).apply {
                    position = dest; title = label
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }

                // Fetch and draw the route
                val result = fetchRoute(userLocation, dest, mode.profile)
                if (result != null) {
                    val line = addPolyline(mapView, result.points, mode.colorHex)
                    val routeLabel = formatRouteLabel(result.distanceM, result.durationS)
                    if (mode == RouteMode.DRIVING) { driveRoute = line; driveLabel = routeLabel }
                    else                           { walkRoute  = line; walkLabel  = routeLabel }
                }

                fitMap(mapView, userLocation, dest)
            } finally {
                isLoading = false
            }
        }
    }

    // Geocodes a text address, then calculates both routes at the same time (parallel).
    // Used when the user types an address in the search bar.
    fun searchAndRoute(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isLoading = true
            clearRoutes()
            try {
                // Convert the text address to GPS coordinates
                val dest = geocodeAddress(query)
                if (dest == null) {
                    errorMsg = "Could not find \"$query\""
                    return@launch
                }

                // Pin the destination on the map
                destMarker = Marker(mapView).apply {
                    position = dest; title = query
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }

                // Fetch car and foot routes in parallel — twice as fast as one after the other
                val driveDeferred = async { fetchRoute(userLocation, dest, RouteMode.DRIVING.profile) }
                val walkDeferred  = async { fetchRoute(userLocation, dest, RouteMode.FOOT.profile) }
                val driveResult   = driveDeferred.await()
                val walkResult    = walkDeferred.await()

                driveResult?.let {
                    driveRoute = addPolyline(mapView, it.points, RouteMode.DRIVING.colorHex)
                    driveLabel = formatRouteLabel(it.distanceM, it.durationS)
                }
                walkResult?.let {
                    walkRoute = addPolyline(mapView, it.points, RouteMode.FOOT.colorHex)
                    walkLabel = formatRouteLabel(it.distanceM, it.durationS)
                }

                fitMap(mapView, userLocation, dest)
            } finally {
                isLoading = false
            }
        }
    }


    // ── UI ───────────────────────────────────────────────────────────────────
    // Box stacks its children in layers. The map is the background;
    // search bar, buttons, legend and event card float on top.

    Box(modifier = Modifier.fillMaxSize()) {

        // Full-screen map
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Search bar (top)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 6.dp,
                color = Color.White
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (errorMsg.isNotEmpty()) errorMsg = ""
                        },
                        placeholder = { Text("Search address...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor  = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { searchAndRoute(searchQuery) })
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; clearRoutes() }) {
                            Icon(painter = painterResource(R.drawable.cancel), contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                    // Three-dot menu opening the filters (category / distance /
                    // participation). Highlighted when any filter is active.
                    val activeFilters =
                        (if (selectedType != null) 1 else 0) +
                        (if (selectedDistance != null) 1 else 0) +
                        (if (selectedParticipation != null) 1 else 0)
                    IconButton(onClick = { filterMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Filters",
                            tint = if (activeFilters > 0) Color(0xFF1976D2) else Color.Gray
                        )
                    }
                }
            }

            // Error message shown below the search bar
            if (errorMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // FAB buttons: center on user, zoom in, zoom out (bottom right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { mapView.controller.animateTo(userLocation); mapView.controller.setZoom(15.0) },
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painter = painterResource(R.drawable.location_on), contentDescription = "Center")
            }
            FloatingActionButton(
                onClick = { mapView.controller.zoomIn() },
                containerColor = Color.White, contentColor = Color.DarkGray, modifier = Modifier.size(48.dp)
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            FloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = Color.White, contentColor = Color.DarkGray, modifier = Modifier.size(48.dp)
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        }

        // Route legend (bottom left) — only visible when at least one route is drawn
        if (driveRoute != null || walkRoute != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                        if (driveRoute != null) LegendRow(RouteMode.DRIVING.colorCompose, "By car · $driveLabel")
                        if (walkRoute  != null) LegendRow(RouteMode.FOOT.colorCompose,    "On foot · $walkLabel")
                    }
                    IconButton(onClick = { clearRoutes() }) {
                        Icon(
                            painter = painterResource(R.drawable.cancel),
                            contentDescription = "Clear routes",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Event info card (bottom center) — appears when the user taps an event marker
        selectedEvent?.let { event ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Event info: name, place, date & time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val whenText = event.dateTime?.toDate()?.let {
                                SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(it)
                            } ?: "TBD"
                            Text(text = event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = event.locationText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = whenText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { selectedEvent = null }) {
                            Icon(painter = painterResource(R.drawable.cancel), contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Route buttons: the user picks car or foot
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        RouteMode.entries.forEach { mode ->
                            Button(
                                onClick = {
                                    routeToPoint(GeoPoint(event.locationLat, event.locationLng), event.name, mode)
                                    selectedEvent = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = mode.colorCompose)
                            ) {
                                Text(if (mode == RouteMode.DRIVING) "By Car" else "On Foot", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Open the full event detail screen, passing the event id through
                    // the existing navigation (events/{eventId}).
                    OutlinedButton(
                        onClick = {
                            val id = event.id
                            selectedEvent = null
                            if (id.isNotBlank()) onNavigateToDetail(id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.event),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("See event information")
                    }
                }
            }
        }

        // Filters bottom sheet, opened from the "⋮" menu. Holds the three
        // combinable filters; state lives in the composable so it stays
        // consistent across open/close.
        if (filterMenuOpen) {
            ModalBottomSheet(onDismissRequest = { filterMenuOpen = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            selectedType = null
                            selectedDistance = null
                            selectedParticipation = null
                        }) { Text("Clear all") }
                    }
                    Spacer(Modifier.height(4.dp))

                    // 1) Category — reuses the existing type list + filter logic,
                    // each chip annotated with its event count "type (N)".
                    FilterAccordion(
                        label = "Category",
                        options = availableTypes,
                        selected = selectedType,
                        onSelect = { selectedType = if (selectedType == it) null else it },
                        enabled = availableTypes.isNotEmpty(),
                        disabledMessage = "No categories available",
                        displayLabel = { "$it (${typeCounts[it] ?: 0})" }
                    )

                    // 2) Distance from the user (same options as the Events tab).
                    FilterAccordion(
                        label = "Distance",
                        options = DistanceFilterOptions,
                        selected = selectedDistance,
                        onSelect = { selectedDistance = if (selectedDistance == it) null else it }
                    )

                    // 3) Participation status relative to the current user, each
                    // chip annotated with its event count "Joined (N)" etc.
                    FilterAccordion(
                        label = "Participation",
                        options = ParticipationFilter.entries.map { it.label },
                        selected = selectedParticipation?.label,
                        onSelect = { label ->
                            val picked = ParticipationFilter.entries.first { it.label == label }
                            selectedParticipation = if (selectedParticipation == picked) null else picked
                        },
                        displayLabel = { label ->
                            val pf = ParticipationFilter.entries.first { it.label == label }
                            "$label (${participationCounts[pf] ?: 0})"
                        }
                    )
                }
            }
        }
    }
}


// ─── SMALL COMPOSABLES ───────────────────────────────────────────────────────

// One row in the route legend: a colored rectangle + a text label
@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Surface(modifier = Modifier.size(width = 16.dp, height = 4.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
        Text(text = "  $label", style = MaterialTheme.typography.bodySmall)
    }
}


// ─── MAP HELPERS ─────────────────────────────────────────────────────────────

// Creates a colored line on the map from a list of GPS points and returns it.
// Index 0 means it renders below markers (markers appear on top).
private fun addPolyline(mapView: MapView, points: List<GeoPoint>, colorHex: String): Polyline {
    return Polyline(mapView).apply {
        setPoints(points)
        outlinePaint.color = android.graphics.Color.parseColor(colorHex)
        outlinePaint.strokeWidth = 10f
        mapView.overlays.add(0, this)
    }
}

// Zooms and pans the map so both origin and destination are visible with padding
private fun fitMap(mapView: MapView, origin: GeoPoint, dest: GeoPoint) {
    val bbox = BoundingBox(
        maxOf(origin.latitude, dest.latitude),
        maxOf(origin.longitude, dest.longitude),
        minOf(origin.latitude, dest.latitude),
        minOf(origin.longitude, dest.longitude)
    )
    mapView.zoomToBoundingBox(bbox.increaseByScale(1.4f), true)
    mapView.invalidate()
}

// Converts meters + seconds into a readable label: e.g. "3.2 km · 8 min"
private fun formatRouteLabel(distanceM: Double, durationS: Double): String {
    val dist = if (distanceM < 1000) "${distanceM.toInt()} m" else "${"%.1f".format(distanceM / 1000)} km"
    val mins = (durationS / 60).toInt()
    val time = if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}min"
    return "$dist · $time"
}


// ─── NETWORK / LOCATION FUNCTIONS ────────────────────────────────────────────

// Gets the device's current GPS position as a GeoPoint.
// Delegates to the shared fetchUserLatLng helper (aux_files/LocationUtils) so the
// Map and Events tabs resolve the user location with identical logic.
private suspend fun fetchLocation(context: Context): GeoPoint? =
    fetchUserLatLng(context)?.let { GeoPoint(it.lat, it.lng) }

// Converts a text address into GPS coordinates. Delegates to the shared
// geocoding helper (Nominatim) so the network logic lives in one place.
private suspend fun geocodeAddress(query: String): GeoPoint? {
    val loc = geocodeLocation(query) ?: return null
    return GeoPoint(loc.lat, loc.lng)
}

// Fetches a driving or walking route between two points using OSRM (free, no API key).
// withContext(Dispatchers.IO) runs this on a background thread so the UI stays responsive.
private suspend fun fetchRoute(origin: GeoPoint, dest: GeoPoint, profile: String): RouteResult? = withContext(Dispatchers.IO) {
    try {
        // routing.openstreetmap.de hosts separate servers for car and foot profiles
        val server = if (profile == "foot")
            "https://routing.openstreetmap.de/routed-foot/route/v1/driving"
        else
            "https://routing.openstreetmap.de/routed-car/route/v1/driving"

        // OSRM expects longitude THEN latitude (opposite of GeoPoint)
        val url  = "$server/${origin.longitude},${origin.latitude};${dest.longitude},${dest.latitude}?overview=full&geometries=geojson"
        val conn = URL(url).openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)")

        val json   = JSONObject(conn.getInputStream().bufferedReader().readText())
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext null

        val route  = routes.getJSONObject(0)
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")

        // GeoJSON gives [longitude, latitude] — we flip it to GeoPoint(latitude, longitude)
        val points = (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }

        RouteResult(
            points    = points,
            distanceM = route.getDouble("distance"),
            durationS = route.getDouble("duration")
        )
    } catch (e: Exception) { null }
}
