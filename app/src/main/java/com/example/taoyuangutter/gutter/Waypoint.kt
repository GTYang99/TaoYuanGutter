package com.example.taoyuangutter.gutter

import com.google.android.gms.maps.model.LatLng
import java.util.UUID

enum class WaypointType { START, NODE, END }

data class Waypoint(
    var type: WaypointType,
    var label: String,
    var latLng: LatLng? = null,
    /** 已填寫並儲存的表單資料（供檢視/編輯模式預填用） */
    var basicData: HashMap<String, String> = hashMapOf(),
    /** 穩定識別碼：不會因拖拉、重排或刪除鄰近點位而改變。 */
    var uid: String = UUID.randomUUID().toString()
) {
    val isVirtual: Boolean
        get() {
            val v = (basicData["is_virtual"] ?: basicData["IS_VIRTUAL"])?.trim()?.lowercase()
            return v == "1" || v == "true"
        }
}
