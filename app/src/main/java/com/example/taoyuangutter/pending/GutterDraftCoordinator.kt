package com.example.taoyuangutter.pending

import android.content.Context
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import java.io.File

/**
 * 封裝地圖流程草稿的儲存規則，讓 Activity 不必直接操作
 * 「SPI_NUM 去重、session draft id 沿用、照片清理」等細節。
 */
class GutterDraftCoordinator(
    context: Context,
    private val repository: GutterSessionRepository = GutterSessionRepository(context)
) {
    private val appContext = context.applicationContext

    data class SaveResult(
        val draftId: Long,
        val snapshots: List<WaypointSnapshot>
    )

    fun getDraft(id: Long): GutterSessionDraft? = repository.getById(id)

    fun getAllDrafts(): List<GutterSessionDraft> = repository.getAll()

    fun deleteDraft(id: Long) {
        repository.delete(id)
    }

    fun ensureDraftExists(
        draftId: Long,
        waypoints: List<Waypoint>,
        spiTyp: String? = null,
        isOffline: Boolean,
        isCurve: Boolean = false
    ) {
        if (repository.getById(draftId) != null) return
        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type = wp.type.name,
                label = wp.label,
                latitude = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = HashMap(wp.basicData),
                uid = wp.uid
            )
        }
        repository.save(
            GutterSessionDraft(
                id = draftId,
                savedAt = System.currentTimeMillis(),
                spiTyp = spiTyp,
                kind = if (isCurve) KIND_CURVE else KIND_GUTTER,
                isOffline = isOffline,
                waypoints = snapshots
            )
        )
    }

    fun autoSaveSessionDraft(
        waypoints: List<Waypoint>,
        currentSessionDraftId: Long?,
        spiTyp: String? = null,
        isOffline: Boolean,
        isCurve: Boolean = false
    ): SaveResult? {
        if (waypoints.isEmpty()) return null

        val hasAnyLatLng = waypoints.any { it.latLng != null }
        val hasAnyBasicData = waypoints.any { wp -> hasMeaningfulBasicData(wp.basicData) }
        val hasSpiTyp = spiTyp.isNullOrBlank().not()
        if (!hasAnyLatLng && !hasAnyBasicData && !hasSpiTyp) {
            currentSessionDraftId?.let {
                deleteDraftAndLocalPhotos(appContext, it, waypoints)
            }
            return null
        }

        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type = wp.type.name,
                label = wp.label,
                latitude = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = wp.basicData,
                uid = wp.uid
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

        val draftId = existingId ?: currentSessionDraftId ?: run {
            android.util.Log.w(
                "GutterDraftCoordinator",
                "skip auto-save because currentSessionDraftId is missing"
            )
            return null
        }

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
                spiTyp = spiTyp,
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

    private fun hasMeaningfulBasicData(basicData: Map<String, String>): Boolean {
        return basicData.any { (key, value) ->
            when (key) {
                "is_virtual", "_isImported" -> false
                "IS_PENDING_DEPLOY" -> value.equals("1", ignoreCase = true) ||
                    value.equals("true", ignoreCase = true) ||
                    value.equals("y", ignoreCase = true) ||
                    value.equals("yes", ignoreCase = true)
                else -> value.isNotBlank()
            }
        }
    }

    fun deleteDraftIfEffectivelyEmpty(draftId: Long) {
        val draft = repository.getById(draftId) ?: return
        if (!hasRetainableContent(draft)) {
            DraftPhotoCleaner.deleteDraftLocalPhotos(appContext, draft)
            repository.delete(draftId)
        }
    }

    fun cleanupEmptyDrafts() {
        repository.getAll().forEach { draft ->
            if (!hasRetainableContent(draft)) {
                DraftPhotoCleaner.deleteDraftLocalPhotos(appContext, draft)
                repository.delete(draft.id)
            }
        }
    }

    private fun hasRetainableContent(draft: GutterSessionDraft): Boolean {
        return draft.spiTyp.isNullOrBlank().not() || draft.waypoints.any { snapshot ->
            (snapshot.latitude != null && snapshot.longitude != null) ||
                hasRetainableBasicData(snapshot.basicData)
        }
    }

    private fun hasRetainableBasicData(basicData: Map<String, String>): Boolean {
        return basicData.any { (key, value) ->
            when {
                key == "is_virtual" || key == "_isImported" -> false
                key == "IS_PENDING_DEPLOY" -> value.equals("1", ignoreCase = true) ||
                    value.equals("true", ignoreCase = true) ||
                    value.equals("y", ignoreCase = true) ||
                    value.equals("yes", ignoreCase = true)
                key.startsWith("_pending_photo_") -> hasUsablePendingPhoto(value)
                else -> value.isNotBlank()
            }
        }
    }

    private fun hasUsablePendingPhoto(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val file = File(path)
        return file.exists() && file.length() > 0L
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
