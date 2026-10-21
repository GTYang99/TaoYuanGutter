package com.example.taoyuangutter.gutter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPointPickerInitialCameraPolicyTest {
    @Test
    fun blankOrInvalidCoordinateWaitsForLocation() {
        assertTrue(MapPointPickerInitialCameraPolicy.shouldWaitForLocation(0.0, 0.0))
        assertTrue(MapPointPickerInitialCameraPolicy.shouldWaitForLocation(Double.NaN, 121.5))
    }

    @Test
    fun usableSavedCoordinateShowsImmediately() {
        assertFalse(MapPointPickerInitialCameraPolicy.shouldWaitForLocation(24.99, 121.30))
    }
}
