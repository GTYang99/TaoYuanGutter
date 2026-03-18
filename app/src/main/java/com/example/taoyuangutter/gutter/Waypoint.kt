package com.example.taoyuangutter.gutter

import com.google.android.gms.maps.model.LatLng

enum class WaypointType { START, NODE, END }

data class Waypoint(
    var type: WaypointType,
    var label: String,
    var latLng: LatLng? = null
)
