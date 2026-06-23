package com.example.livemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.livemap.aux_files.reverseGeocode
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

// Default center when opening the picker (same reference point MapScreen uses).
private val PICKER_DEFAULT = GeoPoint(37.3886, -5.9823) // Sevilla

/**
 * Full-screen map where the user taps to choose a location for a new event.
 *
 * On confirm we reverse-geocode the tapped point to a readable address and hand
 * back (lat, lng, address) to the caller (NewScreen, via savedStateHandle).
 * Reuses the same osmdroid + Nominatim stack already present in the project.
 */
@Composable
fun LocationPickerScreen(
    onConfirm: (lat: Double, lng: Double, address: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<GeoPoint?>(null) }
    var isResolving by remember { mutableStateOf(false) }

    // Created once and kept alive across recompositions.
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(PICKER_DEFAULT)
        }
    }

    // The pin that marks the chosen point. Added to the map on the first tap.
    val pinMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected location"
        }
    }

    DisposableEffect(Unit) {
        // osmdroid needs a User-Agent before loading tiles (OSM policy).
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.onResume()

        // Tap handler: place/move the pin where the user taps.
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selected = p
                    pinMarker.position = p
                    if (!mapView.overlays.contains(pinMarker)) mapView.overlays.add(pinMarker)
                    mapView.invalidate()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(receiver))

        onDispose { mapView.onPause() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Top bar: back/cancel + hint
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onCancel) {
                    Icon(painterResource(R.drawable.back), contentDescription = "Cancel")
                }
                Text(
                    "Tap the map to choose a location",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
            }
        }

        // Confirm button (enabled once a point is selected)
        Button(
            onClick = {
                val point = selected ?: return@Button
                isResolving = true
                scope.launch {
                    val address = reverseGeocode(point.latitude, point.longitude)
                        ?: "%.5f, %.5f".format(point.latitude, point.longitude)
                    isResolving = false
                    onConfirm(point.latitude, point.longitude, address)
                }
            },
            enabled = selected != null && !isResolving,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Confirm location")
            }
        }
    }
}
