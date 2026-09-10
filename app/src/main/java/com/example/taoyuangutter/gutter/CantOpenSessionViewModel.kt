package com.example.taoyuangutter.gutter

import androidx.lifecycle.ViewModel
import com.example.taoyuangutter.common.PendingPhotoDraftState

/** Temporary, non-persisted state for one editable form session. */
class CantOpenSessionViewModel : ViewModel() {
    data class PhotoState(
        val uri: String,
        val capturedAt: String,
        val uploadState: String,
        val imgId: String,
        val uploadError: String,
        val pendingPath: String
    )

    data class Snapshot(
        val fields: Map<String, String>,
        val photos: Map<Int, PhotoState>,
        val clearedFields: Map<String, String>,
        val clearedPhotos: Map<Int, PhotoState>
    )

    private var snapshot: Snapshot? = null
    private val fieldGenerations = mutableMapOf<String, Int>()
    private val photoGenerations = mutableMapOf<Int, Int>()
    private val activeCaptureTokens = mutableMapOf<Int, Long>()
    private var generation = 0L

    fun capture(data: Map<String, String>): Snapshot {
        val fields = CANT_OPEN_FIELDS.associateWith { data[it].orEmpty() }
        val photos = (2..3).associateWith { slot -> photoState(data, slot) }
        val result = Snapshot(fields, photos, emptyMap(), emptyMap())
        snapshot = result
        fieldGenerations.clear()
        photoGenerations.clear()
        return result
    }

    fun currentSnapshot(): Snapshot? = snapshot

    fun markCleared(data: Map<String, String>) {
        val saved = snapshot ?: return
        snapshot = saved.copy(
            clearedFields = CANT_OPEN_FIELDS.associateWith { data[it].orEmpty() },
            clearedPhotos = (2..3).associateWith { slot -> photoState(data, slot) }
        )
    }

    fun markFieldChanged(key: String) { fieldGenerations[key] = generation.toInt() }
    fun markPhotoChanged(slot: Int) { photoGenerations[slot] = generation.toInt() }

    fun restore(data: Map<String, String>): Map<String, String> {
        val saved = snapshot ?: return data
        val out = data.toMutableMap()
        saved.fields.forEach { (key, value) ->
            if (fieldGenerations[key] == null && data[key].orEmpty() == saved.clearedFields[key].orEmpty()) out[key] = value
        }
        saved.photos.forEach { (slot, value) ->
            if (photoGenerations[slot] == null && photoState(data, slot) == saved.clearedPhotos[slot]) writePhoto(out, slot, value)
        }
        snapshot = null
        return out
    }

    fun discard() {
        snapshot = null
        fieldGenerations.clear()
        photoGenerations.clear()
        activeCaptureTokens.clear()
    }

    fun newCaptureToken(slot: Int): Long = (++generation).also { activeCaptureTokens[slot] = it }
    fun invalidateCapture(slot: Int) { activeCaptureTokens.remove(slot); markPhotoChanged(slot) }
    fun acceptsCapture(slot: Int, token: Long): Boolean = activeCaptureTokens[slot] == token

    private fun photoState(data: Map<String, String>, slot: Int) = PhotoState(
        data["photo$slot"].orEmpty(), data["photo${slot}CapturedAt"].orEmpty(),
        data["photo${slot}UploadState"].orEmpty(), data["photo${slot}ImgId"].orEmpty(),
        data["photo${slot}UploadError"].orEmpty(), PendingPhotoDraftState.readPath(data, slot).orEmpty()
    )

    private fun writePhoto(data: MutableMap<String, String>, slot: Int, value: PhotoState) {
        data["photo$slot"] = value.uri
        data["photo${slot}CapturedAt"] = value.capturedAt
        data["photo${slot}UploadState"] = value.uploadState
        data["photo${slot}ImgId"] = value.imgId
        data["photo${slot}UploadError"] = value.uploadError
        PendingPhotoDraftState.writePath(data, slot, value.pendingPath)
    }

    companion object {
        val CANT_OPEN_FIELDS = listOf("COVER_DEP", "NODE_DEP", "NODE_WID", "MAT_TYP", "IS_BROKEN", "IS_HANGING", "IS_SILT")
    }
}
