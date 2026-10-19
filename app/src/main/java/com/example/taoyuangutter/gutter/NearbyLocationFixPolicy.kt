package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.map.MapCoordinatePolicy

/** Eligibility rule for the legacy Nearby query location callback. */
internal object NearbyLocationFixPolicy {
    fun shouldAccept(
        latitude: Double?,
        longitude: Double?,
        locationTimeMillis: Long,
        accuracyMeters: Float?,
        nowMillis: Long,
        maxAgeMillis: Long,
        maxAccuracyMeters: Float
    ): Boolean {
        if (!MapCoordinatePolicy.isUsableCoordinate(latitude, longitude)) return false
        val accuracy = accuracyMeters ?: return false
        if (locationTimeMillis <= 0L || nowMillis < locationTimeMillis) return false
        return nowMillis - locationTimeMillis <= maxAgeMillis && accuracy <= maxAccuracyMeters
    }
}
