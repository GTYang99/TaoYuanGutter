package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.NoDitchPoint
import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NoDitchPointHitTester {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun findNearest(
        targetLatLng: LatLng,
        candidates: List<NoDitchPoint>,
        radiusMeters: Float
    ): NoDitchPoint? {
        if (radiusMeters <= 0f) return null
        var nearest: NoDitchPoint? = null
        var nearestDistance = Float.MAX_VALUE
        candidates.forEach { point ->
            val distance = distanceMeters(targetLatLng, point.latLng)
            if (distance <= radiusMeters && distance < nearestDistance) {
                nearest = point
                nearestDistance = distance
            }
        }
        return nearest
    }

    private fun distanceMeters(from: LatLng, to: LatLng): Float {
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLng = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(fromLat) * cos(toLat) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).toFloat()
    }
}
