package com.example.taoyuangutter.map

import android.location.Location

/** Keeps fast cached positioning useful without accepting stale or negligible refinements. */
object LocationFixQualityPolicy {
    const val MAX_CACHED_AGE_MS = 5 * 60 * 1000L
    const val MIN_ACCURACY_IMPROVEMENT_METERS = 10f

    fun isUsableCached(location: Location?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val candidate = location ?: return false
        return isUsableCached(candidate.time, nowMillis)
    }

    fun isUsableCached(locationTimeMillis: Long, nowMillis: Long): Boolean =
        locationTimeMillis > 0L && nowMillis >= locationTimeMillis &&
            nowMillis - locationTimeMillis <= MAX_CACHED_AGE_MS

    fun shouldUseRefinement(accepted: Location?, candidate: Location?): Boolean {
        val refinement = candidate ?: return false
        val current = accepted ?: return refinement.hasAccuracy()
        return shouldUseRefinement(
            acceptedTimeMillis = current.time,
            acceptedAccuracyMeters = current.accuracy.takeIf { current.hasAccuracy() },
            candidateTimeMillis = refinement.time,
            candidateAccuracyMeters = refinement.accuracy.takeIf { refinement.hasAccuracy() }
        )
    }

    fun shouldUseRefinement(
        acceptedTimeMillis: Long,
        acceptedAccuracyMeters: Float?,
        candidateTimeMillis: Long,
        candidateAccuracyMeters: Float?
    ): Boolean {
        if (candidateTimeMillis <= acceptedTimeMillis || candidateAccuracyMeters == null) return false
        return acceptedAccuracyMeters == null ||
            acceptedAccuracyMeters - candidateAccuracyMeters >= MIN_ACCURACY_IMPROVEMENT_METERS
    }
}
