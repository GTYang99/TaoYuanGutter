package com.example.taoyuangutter.map

/** Validates geographic coordinates before using them as a map target or location fix. */
object MapCoordinatePolicy {
    fun isUsableCoordinate(latitude: Double?, longitude: Double?): Boolean {
        val lat = latitude ?: return false
        val lng = longitude ?: return false
        return lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0 &&
            (lat != 0.0 || lng != 0.0)
    }
}
