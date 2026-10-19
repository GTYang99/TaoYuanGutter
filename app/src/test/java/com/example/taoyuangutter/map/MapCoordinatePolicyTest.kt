package com.example.taoyuangutter.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCoordinatePolicyTest {
    @Test
    fun rejectsMissingInvalidAndZeroZeroCoordinates() {
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(null, 121.5))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(25.0, null))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(Double.NaN, 121.5))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(25.0, Double.POSITIVE_INFINITY))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(91.0, 121.5))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(25.0, 181.0))
        assertFalse(MapCoordinatePolicy.isUsableCoordinate(0.0, 0.0))
    }

    @Test
    fun acceptsValidCoordinatesIncludingOneZeroAxis() {
        assertTrue(MapCoordinatePolicy.isUsableCoordinate(25.03, 121.5))
        assertTrue(MapCoordinatePolicy.isUsableCoordinate(0.0, 121.5))
        assertTrue(MapCoordinatePolicy.isUsableCoordinate(25.03, 0.0))
    }
}
