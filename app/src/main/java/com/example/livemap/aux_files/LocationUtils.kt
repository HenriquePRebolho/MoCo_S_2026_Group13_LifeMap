package com.example.livemap.aux_files

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared location + distance helpers.
 *
 * Centralised here so BOTH the Events tab and the Map tab compute the user's
 * position and event distances the same way, instead of each screen rolling its
 * own. MapScreen kept a private osmdroid-flavoured copy of fetchLocation; it now
 * delegates here so there is a single source of truth.
 */

/** Plain lat/lng pair, free of any map-library type (osmdroid, etc.). */
data class LatLng(val lat: Double, val lng: Double)

/**
 * Distance radius options shown in the filters, shared by the Events screen and
 * the Map filter menu so both stay in sync. "50+ km" is the widest option and
 * acts as "no upper limit" (see [matchesDistanceFilter]).
 */
val DistanceFilterOptions = listOf("1 km", "5 km", "10 km", "25 km", "50+ km")

// Cached GPS positions older than this are considered stale.
private const val MAX_CACHE_AGE_MS = 5 * 60 * 1000L

/**
 * Gets the device's current GPS position as a plain [LatLng], or null if the
 * permission is missing / no provider is available.
 *
 * Tries the cached position first (instant); if it's too old, waits for a single
 * fresh fix without blocking the UI thread.
 */
@SuppressLint("MissingPermission")
suspend fun fetchUserLatLng(context: Context): LatLng? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val now = System.currentTimeMillis()

    // Try cached position first (much faster than requesting a new fix).
    val gpsCache = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    val netCache = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    val cached = when {
        gpsCache != null && now - gpsCache.time < MAX_CACHE_AGE_MS -> gpsCache
        netCache != null && now - netCache.time < MAX_CACHE_AGE_MS -> netCache
        else -> null
    }
    if (cached != null) return LatLng(cached.latitude, cached.longitude)

    // No fresh cache — suspend and wait for a new fix.
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(LatLng(loc.latitude, loc.longitude))
            }
        }
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> {
                cont.resume(null); return@suspendCancellableCoroutine
            }
        }
        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        cont.invokeOnCancellation { lm.removeUpdates(listener) }
    }
}

/**
 * Great-circle distance between two coordinates in kilometres (haversine).
 */
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Whether a distance (in km) is within the selected radius option.
 *
 *  - null option         → no filter, everything passes.
 *  - "1 km".."25 km"     → within that many km.
 *  - "50+ km"            → no upper limit (the widest option), everything passes.
 */
fun matchesDistanceFilter(option: String?, km: Double): Boolean {
    if (option == null) return true
    if (option.contains("+")) return true // "50+ km" = no upper bound
    val limit = option.filter { it.isDigit() }.toIntOrNull() ?: return true
    return km <= limit
}
