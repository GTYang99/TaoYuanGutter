package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.WaypointSnapshot

object GutterImportValidator {
    fun hasExistingXyNum(
        importedXyNum: String?,
        currentWaypoints: List<WaypointSnapshot>
    ): Boolean {
        val normalizedImported = importedXyNum?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return currentWaypoints.any { waypoint ->
            waypoint.basicData["XY_NUM"]?.trim() == normalizedImported
        }
    }
}
