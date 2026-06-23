package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.google.android.gms.maps.model.LatLng

/**
 * 封裝「開始新增 session」與「從草稿恢復 session」的流程決策，
 * 讓 Activity 只處理畫面層的地圖與 FragmentTransaction。
 */
class GutterSessionFlowCoordinator {

    data class AddSessionStart(
        val draftId: Long,
        val isOffline: Boolean,
        val sheet: AddGutterBottomSheet
    )

    sealed interface ResumeAction {
        data class ResumeMapSheet(
            val draftId: Long,
            val isOffline: Boolean,
            val initialWaypointCount: Int,
            val sheet: AddGutterBottomSheet,
            val waypoints: List<Waypoint>
        ) : ResumeAction
    }

    fun createAddSessionStart(isOfflineMainMode: Boolean): AddSessionStart {
        return AddSessionStart(
            // 先在 session 開始時就分配固定 draftId，讓 sheet 與表單共用同一筆草稿。
            draftId = System.currentTimeMillis(),
            isOffline = isOfflineMainMode,
            sheet = if (isOfflineMainMode) {
                AddGutterBottomSheet.newOfflineInstance()
            } else {
                AddGutterBottomSheet.newInstance()
            }
        )
    }

    fun createResumeAction(
        draft: GutterSessionDraft,
        isOfflineMainMode: Boolean
    ): ResumeAction {
        return ResumeAction.ResumeMapSheet(
            draftId = draft.id,
            isOffline = isOfflineMainMode,
            initialWaypointCount = draft.waypoints.size,
            sheet = AddGutterBottomSheet.newInstanceFromDraft(
                draft = draft,
                forceOffline = isOfflineMainMode
            ),
            waypoints = restoreDraftWaypoints(draft)
        )
    }

    private fun restoreDraftWaypoints(draft: GutterSessionDraft): List<Waypoint> {
        return draft.waypoints.map { snap ->
            Waypoint(
                type = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE,
                label = snap.label,
                latLng = if (snap.latitude != null && snap.longitude != null) {
                    LatLng(snap.latitude, snap.longitude)
                } else {
                    null
                },
                basicData = HashMap(snap.basicData)
            )
        }
    }
}
