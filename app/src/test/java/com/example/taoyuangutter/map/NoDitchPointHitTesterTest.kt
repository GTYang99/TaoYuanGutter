package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.NoDitchPoint
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoDitchPointHitTesterTest {
    @Test
    fun findNearestReturnsClosestPointWithinRadius() {
        val target = LatLng(24.992900, 121.301100)
        val farPoint = NoDitchPoint(
            id = "far",
            latitude = 24.993300,
            longitude = 121.301500,
            note = "far note"
        )
        val nearPoint = NoDitchPoint(
            id = "near",
            latitude = 24.992930,
            longitude = 121.301120,
            note = "near note"
        )

        val result = NoDitchPointHitTester.findNearest(
            targetLatLng = target,
            candidates = listOf(farPoint, nearPoint),
            radiusMeters = 12f
        )

        assertEquals("near", result?.id)
    }

    @Test
    fun findNearestReturnsNullWhenAllPointsAreOutsideRadius() {
        val result = NoDitchPointHitTester.findNearest(
            targetLatLng = LatLng(24.992900, 121.301100),
            candidates = listOf(
                NoDitchPoint(
                    id = "outside",
                    latitude = 24.994000,
                    longitude = 121.302000,
                    note = "outside note"
                )
            ),
            radiusMeters = 8f
        )

        assertNull(result)
    }
}
