package com.example.taoyuangutter.gutter

/** Eligibility rule for the legacy Nearby query location callback. */
internal object NearbyLocationFixPolicy {
    fun shouldAccept(
        locationTimeMillis: Long,
        accuracyMeters: Float?,
        nowMillis: Long,
        maxAgeMillis: Long,
        maxAccuracyMeters: Float
    ): Boolean {
        val accuracy = accuracyMeters ?: return false
        if (locationTimeMillis <= 0L || nowMillis < locationTimeMillis) return false
        return nowMillis - locationTimeMillis <= maxAgeMillis && accuracy <= maxAccuracyMeters
    }
}
