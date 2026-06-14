package com.example.livemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL
import java.net.URLEncoder


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

// Cached GPS positions older than 5 minutes are considered stale
private const val MAX_CACHE_AGE_MS = 5 * 60 * 1000L

// Data returned by the OSRM routing API
private data class RouteResult(
    val points: List<GeoPoint>,
    val distanceM: Double,
    val durationS: Double
)


// ─── MAP SCREEN ──────────────────────────────────────────────────────────────

@Composable
fun MapScreen(vm: CounterViewModel) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

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
    var selectedEvent by remember { mutableStateOf<UiEvent?>(null) }

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

    // Place a map marker for each event that has coordinates.
    // Uses vm.events as key so this re-runs if the events list changes (e.g. from Firebase).
    LaunchedEffect(vm.events) {
        // Remove old event markers before adding the new ones
        eventMarkers.forEach { mapView.overlays.remove(it) }
        eventMarkers.clear()

        vm.events.filter { it.hasLocation }.forEach { event ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(event.locationLat, event.locationLng)
                title    = event.name
                snippet  = event.location
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
                            Text(text = event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = event.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "${event.date}  ·  ${event.timeStart}–${event.timeEnd}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

// Gets the device's current GPS position.
// First tries the cached position (instant). If it's too old, requests a fresh one.
@SuppressLint("MissingPermission")
private suspend fun fetchLocation(context: Context): GeoPoint? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) return null

    val lm  = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val now = System.currentTimeMillis()

    // Try cached position first (much faster than requesting a new fix)
    val gpsCache = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    val netCache = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    val cached   = when {
        gpsCache != null && now - gpsCache.time < MAX_CACHE_AGE_MS -> gpsCache
        netCache != null && now - netCache.time < MAX_CACHE_AGE_MS -> netCache
        else -> null
    }
    if (cached != null) return GeoPoint(cached.latitude, cached.longitude)

    // No fresh cache — suspend and wait for a new GPS fix without blocking the UI
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(GeoPoint(loc.latitude, loc.longitude))
            }
        }
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationManager.GPS_PROVIDER
            else -> { cont.resume(null); return@suspendCancellableCoroutine }
        }
        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        cont.invokeOnCancellation { lm.removeUpdates(listener) }
    }
}

// Converts a text address into GPS coordinates using Nominatim (OpenStreetMap, free, no API key)
private suspend fun geocodeAddress(query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn    = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1").openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)")
        val arr = JSONArray(conn.getInputStream().bufferedReader().readText())
        if (arr.length() == 0) return@withContext null
        val obj = arr.getJSONObject(0)
        GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
    } catch (e: Exception) { null }
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
