package com.example.taoyuangutter.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainMapLocationRecenterReloadTrackerTest {
    @Test
    fun expectedLocationMoveConsumesOneReload() {
        val tracker = MainMapLocationRecenterReloadTracker()

        tracker.expectReloadAfterLocationMove()

        assertTrue(tracker.consumeReloadOnLocationUpdated())
        assertFalse(tracker.consumeReloadOnLocationUpdated())
    }

    @Test
    fun cancelledLocationMoveDoesNotReload() {
        val tracker = MainMapLocationRecenterReloadTracker()

        tracker.expectReloadAfterLocationMove()
        tracker.cancelPendingLocationMove()

        assertFalse(tracker.consumeReloadOnLocationUpdated())
    }
}
