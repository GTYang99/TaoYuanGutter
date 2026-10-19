package com.example.taoyuangutter.gutter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyLocationFixPolicyTest {
    @Test
    fun acceptsFreshAccurateFixWithoutTenMeterImprovementRequirement() {
        // The host/cache fix may already be 5 m accurate; a 4.417 m fresh fix
        // still needs to move the Nearby marker/camera and trigger a B query.
        assertTrue(
            NearbyLocationFixPolicy.shouldAccept(
                latitude = 25.03,
                longitude = 121.5,
                locationTimeMillis = 9_950L,
                accuracyMeters = 4.417f,
                nowMillis = 10_000L,
                maxAgeMillis = 30_000L,
                maxAccuracyMeters = 30f
            )
        )
    }

    @Test
    fun rejectsStaleFutureOrOverThresholdFixes() {
        assertFalse(accept(time = 1_000L, accuracy = 5f, now = 40_001L))
        assertFalse(accept(time = 10_001L, accuracy = 5f, now = 10_000L))
        assertFalse(accept(time = 9_950L, accuracy = 30.1f, now = 10_000L))
        assertFalse(accept(time = 9_950L, accuracy = null, now = 10_000L))
    }

    @Test
    fun rejectsFreshAccurateZeroZeroFix() {
        assertFalse(
            NearbyLocationFixPolicy.shouldAccept(
                latitude = 0.0,
                longitude = 0.0,
                locationTimeMillis = 9_950L,
                accuracyMeters = 4.417f,
                nowMillis = 10_000L,
                maxAgeMillis = 30_000L,
                maxAccuracyMeters = 30f
            )
        )
    }

    private fun accept(time: Long, accuracy: Float?, now: Long): Boolean =
        NearbyLocationFixPolicy.shouldAccept(
            latitude = 25.03,
            longitude = 121.5,
            locationTimeMillis = time,
            accuracyMeters = accuracy,
            nowMillis = now,
            maxAgeMillis = 30_000L,
            maxAccuracyMeters = 30f
        )
}
