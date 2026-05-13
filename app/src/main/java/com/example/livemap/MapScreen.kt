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


// Resultado de una ruta: puntos para dibujar en el mapa, distancia (m) y duración (s)
private data class RouteResult(val points: List<GeoPoint>, val distanceM: Double, val durationS: Double)

// Ubicación por defecto cuando el usuario deniega el permiso de localización
private val SEVILLA = GeoPoint(37.3886, -5.9823)

// Tiempo máximo para considerar válida una posición cacheada (5 minutos)
private const val MAX_CACHE_AGE_MS = 5 * 60 * 1000L


// ─── PANTALLA DEL MAPA ───────────────────────────────────────────────────────
// @Composable: describe la interfaz. Compose la re-ejecuta cuando cambia algún estado.

@Composable
fun MapScreen(vm: CounterViewModel) {
    val context = LocalContext.current
    // scope ligado a esta pantalla para lanzar corrutinas sin bloquear la UI
    val scope = rememberCoroutineScope()


    // ── Estado ───────────────────────────────────────────────────────────────
    // remember { mutableStateOf(...) }: conserva el valor entre recomposiciones
    // y redibuja la pantalla automáticamente cuando cambia.

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf(SEVILLA) }  // posición actual del usuario
    var searchQuery  by remember { mutableStateOf("") }        // texto del campo de búsqueda
    var isLoading    by remember { mutableStateOf(false) }     // muestra/oculta el spinner
    var errorMsg     by remember { mutableStateOf("") }        // mensaje cuando la dirección no existe

    var driveRoute by remember { mutableStateOf<Polyline?>(null) } // línea azul en el mapa
    var walkRoute  by remember { mutableStateOf<Polyline?>(null) } // línea verde en el mapa
    var driveLabel by remember { mutableStateOf("") }              // p.ej. "3.2 km · 8 min"
    var walkLabel  by remember { mutableStateOf("") }
    var destMarker by remember { mutableStateOf<Marker?>(null) }   // marcador del destino buscado


    // ── Permiso de localización ───────────────────────────────────────────────
    // Prepara el diálogo del sistema. El bloque se ejecuta cuando el usuario responde.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tienePermiso = granted }


    // ── Configuración del mapa ────────────────────────────────────────────────
    // User-Agent obligatorio: OpenStreetMap rechaza peticiones sin identificador
    Configuration.getInstance().userAgentValue = context.packageName

    // remember { } = el MapView se crea UNA SOLA VEZ y sobrevive a las recomposiciones
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK) // tiles de OpenStreetMap
            setMultiTouchControls(true)             // pellizcar para hacer zoom
            controller.setZoom(15.0)               // nivel de calle
            controller.setCenter(SEVILLA)
        }
    }

    // Marcador del usuario: se añade al mapa una sola vez
    val userMarker = remember {
        Marker(mapView).apply {
            title = "Tu ubicación"
            // ANCHOR_CENTER/BOTTOM: la punta del icono toca exactamente la coordenada
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            position = SEVILLA
            mapView.overlays.add(this) // overlays = elementos visibles sobre el mapa
        }
    }


    // ── LaunchedEffect: permisos y ubicación ─────────────────────────────────
    // Se ejecuta al entrar a la pantalla y cada vez que tienePermiso cambia.
    LaunchedEffect(tienePermiso) {
        if (!tienePermiso) {
            // Sin permiso → mostrar el diálogo del sistema
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Con permiso → obtener ubicación real. fetchLocation es suspend:
            // espera la respuesta del GPS en segundo plano sin bloquear la pantalla.
            val loc = fetchLocation(context)
            if (loc != null) {
                userLocation = loc
                userMarker.position = loc          // mueve el marcador al punto real
                mapView.controller.animateTo(loc)  // animación suave al punto real
                mapView.invalidate()               // redibuja el mapa
            }
            // loc == null → GPS sin fix todavía, queda centrado en Sevilla
        }
    }

    // ── DisposableEffect: ciclo de vida ──────────────────────────────────────
    // osmdroid necesita onResume/onPause para gestionar la descarga de tiles
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }


    // ── Limpiar rutas ─────────────────────────────────────────────────────────
    // Borra del mapa las rutas, el marcador de destino y los mensajes
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


    // ── Buscar dirección y calcular rutas ────────────────────────────────────
    // 1. Geocodifica el texto a coordenadas (Nominatim)
    // 2. Calcula ruta en coche y a pie (OSRM, routing.openstreetmap.de)
    // 3. Dibuja ambas rutas en el mapa
    fun searchAndRoute(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isLoading = true
            clearRoutes()
            // try/finally: garantiza que el spinner se apague aunque haya errores
            try {
                // 1. Geocodificación: texto → coordenadas
                val destination = geocodeAddress(query)
                if (destination == null) {
                    errorMsg = "No se encontró \"$query\""
                    return@launch
                }

                // 2. Marcador en el destino
                val marker = Marker(mapView).apply {
                    position = destination
                    title = query
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
                destMarker = marker

                // 3. Calcular ambas rutas
                val driveResult = fetchRoute(userLocation, destination, "driving")
                val walkResult  = fetchRoute(userLocation, destination, "foot")

                // 4. Dibujar ruta en coche (azul)
                if (driveResult != null) {
                    val line = Polyline(mapView).apply {
                        setPoints(driveResult.points)
                        outlinePaint.color = android.graphics.Color.parseColor("#2196F3") // azul
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(0, line) // índice 0 = queda debajo de los marcadores
                    driveRoute = line
                    driveLabel = formatRouteLabel(driveResult.distanceM, driveResult.durationS)
                }

                // 5. Dibujar ruta a pie (verde)
                if (walkResult != null) {
                    val line = Polyline(mapView).apply {
                        setPoints(walkResult.points)
                        outlinePaint.color = android.graphics.Color.parseColor("#4CAF50") // verde
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(0, line)
                    walkRoute = line
                    walkLabel = formatRouteLabel(walkResult.distanceM, walkResult.durationS)
                }

                // 6. Zoom automático para ver origen y destino
                // BoundingBox(norte, este, sur, oeste) encierra los dos puntos
                // increaseByScale(1.4f) añade un 40% de margen para no quedar pegado a los bordes
                val bbox = BoundingBox(
                    maxOf(userLocation.latitude, destination.latitude),
                    maxOf(userLocation.longitude, destination.longitude),
                    minOf(userLocation.latitude, destination.latitude),
                    minOf(userLocation.longitude, destination.longitude)
                )
                mapView.zoomToBoundingBox(bbox.increaseByScale(1.4f), true)
                mapView.invalidate()

            } finally {
                isLoading = false // siempre apagar el spinner, aunque haya fallado algo
            }
        }
    }


    // ─── UI ──────────────────────────────────────────────────────────────────
    // Box: apila sus hijos en capas. El mapa va de fondo y los controles flotan encima.

    Box(modifier = Modifier.fillMaxSize()) {

        // Mapa — ocupa toda la pantalla
        // AndroidView: puente para usar una View clásica de Android dentro de Compose
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())


        // Barra de búsqueda + error (arriba, flotando sobre el mapa)
        // Column: coloca la barra y el posible error en vertical, uno debajo del otro
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
                            if (errorMsg.isNotEmpty()) errorMsg = "" // borra el error al escribir
                        },
                        placeholder = { Text("Buscar dirección...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f), // ocupa todo el espacio sobrante de la fila
                        colors = TextFieldDefaults.colors( // fondo transparente: oculta el gris por defecto
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor  = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // botón "buscar" en el teclado
                        keyboardActions = KeyboardActions(onSearch = { searchAndRoute(searchQuery) })
                    )
                    // Spinner mientras espera la respuesta de la API; X si hay texto escrito
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; clearRoutes() }) {
                            Icon(painter = painterResource(R.drawable.cancel), contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                }
            }

            // Mensaje de error — aparece justo debajo de la barra si la búsqueda falla
            if (errorMsg.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),    // rojo muy claro de fondo
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFC62828), // rojo oscuro para el texto
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }


        // Botones flotantes: centrar, zoom+, zoom− (abajo a la derecha)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { mapView.controller.animateTo(userLocation); mapView.controller.setZoom(15.0) },
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2), // azul oscuro para el icono
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painter = painterResource(R.drawable.location_on), contentDescription = "Centrar")
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


        // Leyenda de rutas (abajo a la izquierda) — visible solo cuando hay rutas dibujadas
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
                        if (driveRoute != null) LegendRow(Color(0xFF2196F3), "En coche · $driveLabel")
                        if (walkRoute  != null) LegendRow(Color(0xFF4CAF50), "Andando  · $walkLabel")
                    }
                    // Botón X para borrar las rutas directamente desde la leyenda
                    IconButton(onClick = { clearRoutes() }) {
                        Icon(
                            painter = painterResource(R.drawable.cancel),
                            contentDescription = "Borrar rutas",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


// ─── COMPOSABLE AUXILIAR ─────────────────────────────────────────────────────
// Una fila de la leyenda: rectángulo de color + texto "modo · distancia · tiempo"

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        // Rectángulo pequeño que simula visualmente la línea de ruta
        Surface(
            modifier = Modifier.size(width = 16.dp, height = 4.dp),
            color = color,
            shape = RoundedCornerShape(2.dp)
        ) {}
        Text(text = "  $label", style = MaterialTheme.typography.bodySmall)
    }
}


// ─── FUNCIONES AUXILIARES ────────────────────────────────────────────────────

// Convierte metros y segundos en texto legible para la leyenda.
// Ejemplos: "450 m · 6 min"  /  "12.3 km · 1h 8min"
private fun formatRouteLabel(distanceM: Double, durationS: Double): String {
    val dist = if (distanceM < 1000) "${distanceM.toInt()} m"
               else "${"%.1f".format(distanceM / 1000)} km"
    val mins = (durationS / 60).toInt()
    val time = if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}min"
    return "$dist · $time"
}

// Obtiene la ubicación actual del dispositivo.
// 1. Intenta la última posición cacheada (respuesta instantánea).
// 2. Si no hay caché (p.ej. primer uso tras dar permiso), solicita una lectura fresca y espera.
@SuppressLint("MissingPermission")
private suspend fun fetchLocation(context: Context): GeoPoint? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Paso 1: caché — solo válido si tiene menos de 5 minutos.
    // getLastKnownLocation puede devolver posiciones de horas atrás (p.ej. EE.UU. si
    // el emulador o el dispositivo tenía una localización antigua guardada).
    val now = System.currentTimeMillis()
    val gpsCache = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    val netCache = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    val cached = when {
        gpsCache != null && now - gpsCache.time < MAX_CACHE_AGE_MS -> gpsCache
        netCache != null && now - netCache.time < MAX_CACHE_AGE_MS -> netCache
        else -> null
    }
    if (cached != null) return GeoPoint(cached.latitude, cached.longitude)

    // Paso 2: sin caché → solicita una lectura fresca y suspende la corrutina hasta recibirla.
    // suspendCancellableCoroutine convierte el callback de Android en una función suspend:
    // la pantalla no se congela, simplemente espera en segundo plano.
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lm.removeUpdates(this) // una sola lectura es suficiente
                if (cont.isActive) cont.resume(GeoPoint(loc.latitude, loc.longitude))
            }
        }
        // Red (WiFi/móvil) responde antes que el GPS; si no hay red disponible, usa GPS
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationManager.GPS_PROVIDER
            else -> { cont.resume(null); return@suspendCancellableCoroutine }
        }
        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        // Si el usuario sale de la pantalla antes de recibir la ubicación, limpia el listener
        cont.invokeOnCancellation { lm.removeUpdates(listener) }
    }
}

// Convierte una dirección en texto a coordenadas geográficas.
// Usa Nominatim (OpenStreetMap) — gratuito, sin API key.
// "suspend": puede pausarse esperando la red sin bloquear la UI.
private suspend fun geocodeAddress(query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8") // "Plaza Nueva" → "Plaza+Nueva"
        val conn = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            .openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)") // obligatorio por Nominatim
        val arr = JSONArray(conn.getInputStream().bufferedReader().readText())
        if (arr.length() == 0) return@withContext null
        val obj = arr.getJSONObject(0) // primer resultado (el más relevante)
        GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
    } catch (e: Exception) { null }
}

// Calcula la ruta entre dos puntos usando OSRM (routing.openstreetmap.de).
// Gratuito, sin API key. Cada perfil usa un servidor con su propio grafo:
//   "driving" → routed-car  (red de carreteras)
//   "foot"    → routed-foot (aceras, caminos, puentes peatonales)
// Devuelve los puntos del trazado + distancia y duración, o null si falla.
private suspend fun fetchRoute(origin: GeoPoint, dest: GeoPoint, profile: String): RouteResult? = withContext(Dispatchers.IO) {
    try {
        // Un servidor diferente por perfil — cada uno tiene cargado su propio grafo
        val server = if (profile == "foot")
            "https://routing.openstreetmap.de/routed-foot/route/v1/driving"
        else
            "https://routing.openstreetmap.de/routed-car/route/v1/driving"

        // OSRM espera "longitud,latitud" — al revés que GeoPoint(latitud, longitud)
        val url = "$server/${origin.longitude},${origin.latitude}" +
                ";${dest.longitude},${dest.latitude}" +
                "?overview=full&geometries=geojson"

        val conn = URL(url).openConnection()
        conn.setRequestProperty("User-Agent", "LifeMap/1.0 (Android)")

        val json      = JSONObject(conn.getInputStream().bufferedReader().readText())
        val routes    = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext null

        val route     = routes.getJSONObject(0)
        val distanceM = route.getDouble("distance") // metros totales
        val durationS = route.getDouble("duration") // segundos estimados
        val coords    = route.getJSONObject("geometry").getJSONArray("coordinates")

        // GeoJSON devuelve [longitud, latitud] — hay que invertir a GeoPoint(latitud, longitud)
        val points = (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }

        RouteResult(points = points, distanceM = distanceM, durationS = durationS)

    } catch (e: Exception) {
        null
    }
}
