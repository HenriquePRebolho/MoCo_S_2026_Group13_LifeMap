package com.example.livemap.aux_files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Shared geocoding helpers built on top of Nominatim (OpenStreetMap, free, no
 * API key). Centralised here so the event-creation flow, the location picker
 * and MapScreen all reuse the SAME network logic instead of duplicating it.
 *
 * Both functions return null on failure (no match, no network, timeout, parse
 * error), which the callers translate into "Invalid location" / a coordinate
 * fallback.
 */

private const val NOMINATIM_BASE = "https://nominatim.openstreetmap.org"
private const val USER_AGENT = "LifeMap/1.0 (Android)"

// Bounded so a hung connection can't keep the "Create event" button stuck in
// the Submitting state forever — on timeout we just report "Invalid location".
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 10_000

/** Result of a geocode lookup: coordinates + a human readable address. */
data class GeoLocation(
    val lat: Double,
    val lng: Double,
    val displayName: String
)

/** Performs a GET against Nominatim with a User-Agent and bounded timeouts. */
private fun fetchJson(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/**
 * Forward geocoding: turns a free-text address/city into coordinates.
 * Returns null when Nominatim finds no match (treated as "Invalid location").
 */
suspend fun geocodeLocation(query: String): GeoLocation? = withContext(Dispatchers.IO) {
    if (query.isBlank()) return@withContext null
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val arr = JSONArray(fetchJson("$NOMINATIM_BASE/search?q=$encoded&format=json&limit=1"))
        if (arr.length() == 0) return@withContext null
        val obj = arr.getJSONObject(0)
        GeoLocation(
            lat = obj.getDouble("lat"),
            lng = obj.getDouble("lon"),
            displayName = obj.optString("display_name", query)
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Reverse geocoding: turns coordinates (e.g. tapped on the map) into a readable
 * address. Returns null on failure so the caller can fall back to raw coords.
 */
suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    try {
        val obj = JSONObject(fetchJson("$NOMINATIM_BASE/reverse?lat=$lat&lon=$lng&format=json"))
        obj.optString("display_name").ifBlank { null }
    } catch (e: Exception) {
        null
    }
}
