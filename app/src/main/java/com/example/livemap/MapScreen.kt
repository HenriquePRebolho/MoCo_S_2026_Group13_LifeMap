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


private data class RouteResult(val points: List<GeoPoint>, val distanceM: Double, val durationS: Double)

private val SEVILLA = GeoPoint(37.3886, -5.9823)

private const val MAX_CACHE_AGE_MS = 5 * 60 * 1000L


@Composable
fun MapScreen(vm: CounterViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf(SEVILLA) }
    var searchQuery  by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }

    var driveRoute by remember { mutableStateOf<Polyline?>(null) }
    var walkRoute  by remember { mutableStateOf<Polyline?>(null) }
    var driveLabel by remember { mutableStateOf("") }
    var walkLabel  by remember { mutableStateOf("") }
    var destMarker by remember { mutableStateOf<Marker?>(null) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tienePermiso = granted }

    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(SEVILLA)
        }
    }

    val userMarker = remember {
        Marker(mapView).apply {
            title = "Your location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            position = SEVILLA
            mapView.overlays.add(this)
        }
    }

    LaunchedEffect(tienePermiso) {
        if (!tienePermiso) {
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

    // Place a marker for every event that has coordinates
    LaunchedEffect(Unit) {
        vm.events.filter { it.locationLat != 0.0 || it.locationLng != 0.0 }.forEach { event ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(event.locationLat, event.locationLng)
                title = event.name
                snippet = event.location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    selectedEvent = event
                    true
                }
            }
            mapView.overlays.add(0, marker) // insert below user marker
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }


    fun clearRoutes() {
        driveRoute?.let { mapView.overlays.remove(it) }
        walkRoute?.let  { mapView.overlays.remove(it) }
        destMarker?.let { mapView.overlays.remove(it) }
        driveRoute = null
        walkRoute  = null
        destMarker = null
        driveLabel = ""
        walkLabel  = ""
        errorMsg   = ""
        mapView.invalidate()
    }

    // Single-mode route to a known coordinate (used by event marker buttons)
    fun routeToPoint(dest: GeoPoint, label: String, profile: String) {
        scope.launch {
            isLoading = true
            clearRoutes()
            try {
                val marker = Marker(mapView).apply {
                    position = dest
                    title = label
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
                destMarker = marker

                val result = fetchRoute(userLocation, dest, profile)
                if (result != null) {
                    val lineColor = if (profile == "driving") "#2196F3" else "#4CAF50"
                    val line = Polyline(mapView).apply {
                        setPoints(result.points)
                        outlinePaint.color = android.graphics.Color.parseColor(lineColor)
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(0, line)
                    if (profile == "driving") {
                        driveRoute = line
                        driveLabel = formatRouteLabel(result.distanceM, result.durationS)
                    } else {
                        walkRoute = line
                        walkLabel = formatRouteLabel(result.distanceM, result.durationS)
                    }
                }

                val bbox = BoundingBox(
                    maxOf(userLocation.latitude, dest.latitude),
                    maxOf(userLocation.longitude, dest.longitude),
                    minOf(userLocation.latitude, dest.latitude),
                    minOf(userLocation.longitude, dest.longitude)
                )
                mapView.zoomToBoundingBox(bbox.increaseByScale(1.4f), true)
                mapView.invalidate()
            } finally {
                isLoading = false
            }
        }
    }

    // Geocode a text address then calculate both routes (used by search bar)
    fun searchAndRoute(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isLoading = true
            clearRoutes()
            try {
                val destination = geocodeAddress(query)
                if (destination == null) {
                    errorMsg = "Could not find \"$query\""
                    return@launch
                }

                val marker = Marker(mapView).apply {
                    position = destination
                    title = query
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
                destMarker = marker

                val driveResult = fetchRoute(userLocation, destination, "driving")
                val walkResult  = fetchRoute(userLocation, destination, "foot")

                if (driveResult != null) {
                    val line = Polyline(mapView).apply {
                        setPoints(driveResult.points)
                        outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(0, line)
                    driveRoute = line
                    driveLabel = formatRouteLabel(driveResult.distanceM, driveResult.durationS)
                }

                if (walkResult != null) {
                    val line = Polyline(mapView).apply {
                        setPoints(walkResult.points)
                        outlinePaint.color = android.graphics.Color.parseColor("#4CAF50")
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(0, line)
                    walkRoute = line
                    walkLabel = formatRouteLabel(walkResult.distanceM, walkResult.durationS)
                }

                val bbox = BoundingBox(
                    maxOf(userLocation.latitude, destination.latitude),
                    maxOf(userLocation.longitude, destination.longitude),
                    minOf(userLocation.latitude, destination.latitude),
                    minOf(userLocation.longitude, destination.longitude)
                )
                mapView.zoomToBoundingBox(bbox.increaseByScale(1.4f), true)
                mapView.invalidate()

            } finally {
                isLoading = false
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {

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

        // FAB buttons: center, zoom+, zoom− (bottom right)
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
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
            FloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = Color.White, contentColor = Color.DarkGray, modifier = Modifier.size(48.dp)
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
        }

        // Route legend (bottom left) — visible only when a route is drawn
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
                        if (driveRoute != null) LegendRow(Color(0xFF2196F3), "By car · $driveLabel")
                        if (walkRoute  != null) LegendRow(Color(0xFF4CAF50), "On foot · $walkLabel")
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

        // Event info card — appears when an event marker is tapped
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "${event.date}  ·  ${event.timeStart}–${event.timeEnd}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { selectedEvent = null }) {
                            Icon(
                                painter = painterResource(R.drawable.cancel),
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                routeToPoint(
                                    GeoPoint(event.locationLat, event.locationLng),
                                    event.name,
                                    "driving"
                                )
                                selectedEvent = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("By Car", color = Color.White)
                        }
                        Button(
                            onClick = {
                                routeToPoint(
                                    GeoPoint(event.locationLat, event.locationLng),
                                    event.name,
                                    "foot"
                                )
                                selectedEvent = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("On Foot", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Surface(
            modifier = Modifier.size(width = 16.dp, height = 4.dp),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Text(text = "  $label", style = MaterialTheme.typography.bodySmall)
    }
}


private fun formatRouteLabel(distanceM: Double, durationS: Double): String {
    val dist = if (distanceM < 1000) "${distanceM.toInt()} m"
               else "${"%.1f".format(distanceM / 1000)} km"
    val mins = (durationS / 60).toInt()
    val time = if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}min"
    return "$dist · $time"
}

@SuppressLint("MissingPermission")
private suspend fun fetchLocation(context: Context): GeoPoint? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val now = System.currentTimeMillis()
    val gpsCache = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    val netCache = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    val cached = when {
        gpsCache != null && now - gpsCache.time < MAX_CACHE_AGE_MS -> gpsCache
        netCache != null && now - netCache.time < MAX_CACHE_AGE_MS -> netCache
        else -> null
    }
    if (cached != null) return GeoPoint(cached.latitude, cached.longitude)

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

private suspend fun geocodeAddress(query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            .openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)")
        val arr = JSONArray(conn.getInputStream().bufferedReader().readText())
        if (arr.length() == 0) return@withContext null
        val obj = arr.getJSONObject(0)
        GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
    } catch (e: Exception) { null }
}

private suspend fun fetchRoute(origin: GeoPoint, dest: GeoPoint, profile: String): RouteResult? = withContext(Dispatchers.IO) {
    try {
        val server = if (profile == "foot")
            "https://routing.openstreetmap.de/routed-foot/route/v1/driving"
        else
            "https://routing.openstreetmap.de/routed-car/route/v1/driving"

        val url = "$server/${origin.longitude},${origin.latitude}" +
                ";${dest.longitude},${dest.latitude}" +
                "?overview=full&geometries=geojson"

        val conn = URL(url).openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)")

        val json      = JSONObject(conn.getInputStream().bufferedReader().readText())
        val routes    = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext null

        val route     = routes.getJSONObject(0)
        val distanceM = route.getDouble("distance")
        val durationS = route.getDouble("duration")
        val coords    = route.getJSONObject("geometry").getJSONArray("coordinates")

        val points = (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }

        RouteResult(points = points, distanceM = distanceM, durationS = durationS)

    } catch (e: Exception) {
        null
    }
}
