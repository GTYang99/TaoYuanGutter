package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.pending.GutterSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object PhotoSlotUploadCoordinator {
    data class Snapshot(
        val draftId: Long,
        val waypointIndex: Int,
        val slot: Int,
        val state: String,
        val imgId: Int? = null,
        val error: String? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = ConcurrentHashMap<String, MutableSet<(Snapshot) -> Unit>>()
    private val inFlight = ConcurrentHashMap<String, Unit>()

    private fun taskKey(draftId: Long, waypointIndex: Int, slot: Int): String =
        "$draftId:$waypointIndex:$slot"

    fun isUploading(draftId: Long, waypointIndex: Int, slot: Int): Boolean {
        if (draftId <= 0L || slot !in 1..3) return false
        return inFlight.containsKey(taskKey(draftId, waypointIndex, slot))
    }

    fun registerListener(
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        listener: (Snapshot) -> Unit
    ) {
        val key = taskKey(draftId, waypointIndex, slot)
        listeners.getOrPut(key) { LinkedHashSet() }.add(listener)
    }

    fun unregisterListener(
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        listener: (Snapshot) -> Unit
    ) {
        val key = taskKey(draftId, waypointIndex, slot)
        listeners[key]?.let { set ->
            set.remove(listener)
            if (set.isEmpty()) listeners.remove(key)
        }
    }

    fun enqueueUpload(
        context: Context,
        repository: GutterRepository,
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        photoPath: String,
        token: String
    ) {
        val key = taskKey(draftId, waypointIndex, slot)
        if (inFlight.putIfAbsent(key, Unit) != null) return

        scope.launch {
            updateDraftAndNotify(
                context = context,
                draftId = draftId,
                waypointIndex = waypointIndex,
                slot = slot,
                expectedPhotoPath = photoPath,
                state = PhotoUploadSlotState.STATE_UPLOADING,
                imgId = null,
                error = null
            )

            val result = repository.uploadNodeImage(
                context = context,
                nodeId = null,
                fileCategory = slot,
                imageUri = Uri.parse(photoPath),
                token = token
            )

            when (result) {
                is ApiResult.Success -> {
                    updateDraftAndNotify(
                        context = context,
                        draftId = draftId,
                        waypointIndex = waypointIndex,
                        slot = slot,
                        expectedPhotoPath = photoPath,
                        state = PhotoUploadSlotState.STATE_SUCCESS,
                        imgId = result.data.data?.imgId,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    updateDraftAndNotify(
                        context = context,
                        draftId = draftId,
                        waypointIndex = waypointIndex,
                        slot = slot,
                        expectedPhotoPath = photoPath,
                        state = PhotoUploadSlotState.STATE_FAILED,
                        imgId = null,
                        error = result.message
                    )
                }
            }
            inFlight.remove(key)
        }
    }

    private fun updateDraftAndNotify(
        context: Context,
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        expectedPhotoPath: String,
        state: String,
        imgId: Int?,
        error: String?
    ) {
        val repository = GutterSessionRepository(context.applicationContext)
        val draft = repository.getById(draftId) ?: return
        if (waypointIndex !in draft.waypoints.indices) return

        val target = draft.waypoints[waypointIndex]
        val currentPhotoPath = target.basicData["photo$slot"]?.trim().orEmpty()
        if (currentPhotoPath.isEmpty() || currentPhotoPath != expectedPhotoPath.trim()) {
            return
        }

        val updatedWaypoints = draft.waypoints.toMutableList()
        val updatedBasicData = HashMap(target.basicData)
        val resolvedImgId = if (state == PhotoUploadSlotState.STATE_UPLOADING && imgId == null) {
            PhotoUploadSlotState.readImgId(target.basicData, slot)
        } else {
            imgId
        }
        PhotoUploadSlotState.writeState(updatedBasicData, slot, state = state, imgId = resolvedImgId, error = error)
        updatedWaypoints[waypointIndex] = target.copy(basicData = updatedBasicData)
        repository.save(draft.copy(savedAt = System.currentTimeMillis(), waypoints = updatedWaypoints))

        val snapshot = Snapshot(
            draftId = draftId,
            waypointIndex = waypointIndex,
            slot = slot,
            state = state,
            imgId = resolvedImgId,
            error = error
        )
        listeners[taskKey(draftId, waypointIndex, slot)]?.toList()?.forEach { it(snapshot) }
    }
}
