package com.osscabinet.ossWallet.domain.util

/**
 * Converts a "lat,lon" string back to a coordinate pair.
 * Returns null if the string is null, blank, or malformed.
 */
fun String?.toGpsCoords(): Pair<Double, Double>? {
    if (isNullOrBlank()) return null
    val parts = split(",")
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    return lat to lon
}

/** Formats a coordinate pair as the canonical "lat,lon" storage string. */
fun Pair<Double, Double>.toGpsString(): String = "${first},${second}"

/** Short display label, e.g. "37.7749, -122.4194" (5 decimal places). */
fun String.formatGpsForDisplay(): String {
    val (lat, lon) = toGpsCoords() ?: return this
    return "${"%.5f".format(lat)}, ${"%.5f".format(lon)}"
}
