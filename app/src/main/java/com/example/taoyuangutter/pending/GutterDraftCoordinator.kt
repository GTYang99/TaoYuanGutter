package com.example.taoyuangutter.pending

import android.content.Context
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType

/**
 * 封裝地圖流程草稿的儲存規則，讓 Activity 不必直接操作
 * 「SPI_NUM 去重、session draft id 沿用、照片清理」等細節。
 */
class GutterDraftCoordinator(
    context: Context,
    private val repository: GutterSessionRepository = GutterSessionRepository(context)
) {

    data class SaveResult(
        val draftId: Long,
        val snapshots: List<WaypointSnapshot>
    )

    fun getDraft(id: Long): GutterSessionDraft? = repository.getById(id)

    fun getAllDrafts(): List<GutterSessionDraft> = repository.getAll()

    fun deleteDraft(id: Long) {
        repository.delete(id)
    }

    fun autoSaveSessionDraft(
        waypoints: List<Waypoint>,
        currentSessionDraftId: Long?,
        isOffline: Boolean,
        isCurve: Boolean = false
    ): SaveResult? {
        if (waypoints.isEmpty()) return null

        val hasAnyLatLng = waypoints.any { it.latLng != null }
        val hasAnyBasicData = waypoints.any { wp ->
            wp.basicData.any { (_, v) -> v.isNotBlank() }
        }
        if (!hasAnyLatLng && !hasAnyBasicData) return null

        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type = wp.type.name,
                label = wp.label,
                latitude = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = wp.basicData
            )
        }

        val spiNum = waypoints.firstOrNull { it.type == WaypointType.START }
            ?.basicData?.get("SPI_NUM")
            ?.takeIf { it.isNotEmpty() }

        val previousSessionId = currentSessionDraftId
        val existingId = if (!spiNum.isNullOrEmpty()) {
            repository.getAll().firstOrNull { draft ->
                draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                    ?.basicData?.get("SPI_NUM") == spiNum &&
                    draft.id != currentSessionDraftId
            }?.id
        } else {
            null
        }

        if (existingId != null && previousSessionId != null && previousSessionId != existingId) {
            val previousDraft = repository.getById(previousSessionId)
            val previousSpiNum = previousDraft?.waypoints
                ?.firstOrNull { it.type == WaypointType.START.name }
                ?.basicData
                ?.get("SPI_NUM")
                ?.takeIf { it.isNotEmpty() }
            if (previousSpiNum.isNullOrEmpty()) {
                repository.delete(previousSessionId)
            }
        }

        val draftId = existingId ?: currentSessionDraftId ?: System.currentTimeMillis()

        if (!spiNum.isNullOrEmpty()) {
            repository.getAll()
                .filter { draft ->
                    draft.id != draftId &&
                        draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                            ?.basicData?.get("SPI_NUM") == spiNum
                }
                .forEach { duplicate -> repository.delete(duplicate.id) }
        }

        repository.save(
            GutterSessionDraft(
                id = draftId,
                savedAt = System.currentTimeMillis(),
                kind = if (isCurve) KIND_CURVE else KIND_GUTTER,
                isOffline = isOffline,
                waypoints = snapshots
            )
        )

        return SaveResult(
            draftId = draftId,
            snapshots = snapshots
        )
    }

    fun deleteDraftAndLocalPhotos(context: Context, draftId: Long, fallbackWaypoints: List<Waypoint>) {
        repository.getById(draftId)?.let { draft ->
            DraftPhotoCleaner.deleteDraftLocalPhotos(context, draft)
        } ?: run {
            DraftPhotoCleaner.deleteWaypointsLocalPhotos(
                context = context,
                waypoints = fallbackWaypoints.map { it.basicData }
            )
        }
        repository.delete(draftId)
    }

    fun deleteDraftsBySpiNum(context: Context, spiNum: String) {
        repository.getAll()
            .filter { draft ->
                draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                    ?.basicData?.get("SPI_NUM") == spiNum
            }
            .forEach { draft ->
                DraftPhotoCleaner.deleteDraftLocalPhotos(context, draft)
                repository.delete(draft.id)
            }
    }
}
