package com.example.taoyuangutter.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixQualityPolicyTest {
    @Test
    fun cachedLocationIsUsableOnlyWithinFiveMinutes() {
        val now = 1_000_000L
        assertTrue(LocationFixQualityPolicy.isUsableCached(now - 300_000L, now))
        assertFalse(LocationFixQualityPolicy.isUsableCached(now - 300_001L, now))
    }

    @Test
    fun refinementRequiresNewerTenMeterAccuracyImprovement() {
        assertTrue(LocationFixQualityPolicy.shouldUseRefinement(1_000L, 30f, 2_000L, 20f))
        assertFalse(LocationFixQualityPolicy.shouldUseRefinement(1_000L, 30f, 2_000L, 21f))
        assertFalse(LocationFixQualityPolicy.shouldUseRefinement(1_000L, 30f, 1_000L, 10f))
    }

    @Test
    fun firstHighAccuracyResultIsAcceptedWithoutCache() {
        assertTrue(LocationFixQualityPolicy.shouldUseRefinement(0L, null, 1_000L, 25f))
        assertFalse(LocationFixQualityPolicy.shouldUseRefinement(0L, null, 1_000L, null))
    }
}
