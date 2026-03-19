package com.example.taoyuangutter.gutter

import com.google.android.gms.maps.model.LatLng

enum class WaypointType { START, NODE, END }

data class Waypoint(
    var type: WaypointType,
    var label: String,
    var latLng: LatLng? = null,
    /** 已填寫並儲存的表單資料（供檢視/編輯模式預填用） */
    var basicData: HashMap<String, String> = hashMapOf()
)
