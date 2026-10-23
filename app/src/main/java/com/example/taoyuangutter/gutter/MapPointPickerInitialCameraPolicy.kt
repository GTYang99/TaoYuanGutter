package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.map.MapCoordinatePolicy

/** A blank picker must wait for location before exposing its fallback camera. */
internal object MapPointPickerInitialCameraPolicy {
    fun shouldWaitForLocation(latitude: Double, longitude: Double): Boolean =
        !MapCoordinatePolicy.isUsableCoordinate(latitude, longitude)
}
