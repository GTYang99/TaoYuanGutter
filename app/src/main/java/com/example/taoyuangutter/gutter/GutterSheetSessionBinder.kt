package com.example.taoyuangutter.gutter

/**
 * 集中 AddGutterBottomSheet 的 waypoint 變更綁定，避免 MainActivity
 * 在新增 / 編輯 / 草稿恢復等流程各自維護一份近似邏輯。
 */
class GutterSheetSessionBinder {

    data class Config(
        val initialWaypointCount: Int = 0,
        val refitOnGrowth: Boolean = true
    )

    data class Hooks(
        val onWaypointsUpdated: (List<Waypoint>) -> Unit,
        val onWaypointsCleared: () -> Unit,
        val onAutoSaveRequested: (List<Waypoint>) -> Unit,
        val onRefitRequested: (List<Waypoint>) -> Unit
    )

    fun bind(
        sheet: AddGutterBottomSheet,
        config: Config = Config(),
        hooks: Hooks
    ) {
        var lastWaypointsSize = config.initialWaypointCount
        sheet.onWaypointsChanged = waypointChanges@ { waypoints ->
            if (waypoints == null) {
                hooks.onWaypointsCleared()
                return@waypointChanges
            }

            val shouldRefit = config.refitOnGrowth && waypoints.size > lastWaypointsSize
            lastWaypointsSize = waypoints.size
            hooks.onWaypointsUpdated(waypoints)
            hooks.onAutoSaveRequested(waypoints)
            if (shouldRefit) {
                hooks.onRefitRequested(waypoints)
            }
        }
    }
}
