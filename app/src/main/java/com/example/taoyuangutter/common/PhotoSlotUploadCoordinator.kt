package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.pending.GutterSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object PhotoSlotUploadCoordinator {
    data class Snapshot(
        val draftId: Long,
        val waypointIndex: Int,
        val slot: Int,
        val photoPath: String,
        val state: String,
        val imgId: Int? = null,
        val error: String? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = ConcurrentHashMap<String, MutableSet<(Snapshot) -> Unit>>()
    private val inFlight = ConcurrentHashMap<String, Unit>()
    private val completed = ConcurrentHashMap<String, Snapshot>()

    private fun taskKey(draftId: Long, waypointIndex: Int, slot: Int): String =
        "$draftId:$waypointIndex:$slot"

    fun isUploading(draftId: Long, waypointIndex: Int, slot: Int): Boolean {
        if (draftId <= 0L || slot !in 1..3) return false
        return inFlight.containsKey(taskKey(draftId, waypointIndex, slot))
    }

    fun completedFor(
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        photoPath: String
    ): Snapshot? {
        val snapshot = completed[taskKey(draftId, waypointIndex, slot)] ?: return null
        return snapshot.takeIf { it.photoPath == photoPath.trim() }
    }

    /**
     * Waits for an already queued upload instead of starting a second upload.
     * The draft is the source of truth because the form can be recreated while
     * the coordinator continues in its application-scoped worker.
     */
    suspend fun awaitCompletion(
        context: Context,
        draftId: Long,
        waypointIndex: Int,
        slot: Int,
        timeoutMs: Long = 30_000L
    ): Snapshot? {
        val key = taskKey(draftId, waypointIndex, slot)
        val deadline = System.currentTimeMillis() + timeoutMs
        val repository = GutterSessionRepository(context.applicationContext)
        while (inFlight.containsKey(key) && System.currentTimeMillis() < deadline) {
            delay(50L)
        }
        val waypoint = repository.getById(draftId)?.waypoints?.getOrNull(waypointIndex)
            ?: return null
        val state = PhotoUploadSlotState.readState(waypoint.basicData, slot)
        return Snapshot(
            draftId = draftId,
            waypointIndex = waypointIndex,
            slot = slot,
            photoPath = waypoint.basicData["photo$slot"]?.trim().orEmpty(),
            state = state,
            imgId = PhotoUploadSlotState.readImgId(waypoint.basicData, slot),
            error = PhotoUploadSlotState.readError(waypoint.basicData, slot)
        )
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
        completed.remove(key)

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
                    completed[key] = Snapshot(
                        draftId = draftId,
                        waypointIndex = waypointIndex,
                        slot = slot,
                        photoPath = photoPath.trim(),
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
                    completed[key] = Snapshot(
                        draftId = draftId,
                        waypointIndex = waypointIndex,
                        slot = slot,
                        photoPath = photoPath.trim(),
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
    ): Snapshot? {
        val repository = GutterSessionRepository(context.applicationContext)
        val draft = repository.getById(draftId) ?: return null
        if (waypointIndex !in draft.waypoints.indices) return null

        val target = draft.waypoints[waypointIndex]
        val currentPhotoPath = target.basicData["photo$slot"]?.trim().orEmpty()
        if (currentPhotoPath.isEmpty() || currentPhotoPath != expectedPhotoPath.trim()) {
            return null
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
            photoPath = expectedPhotoPath.trim(),
            state = state,
            imgId = resolvedImgId,
            error = error
        )
        listeners[taskKey(draftId, waypointIndex, slot)]?.toList()?.forEach { it(snapshot) }
        return snapshot
    }
}
