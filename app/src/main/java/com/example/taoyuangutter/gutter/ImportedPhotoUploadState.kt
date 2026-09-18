package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.api.NodeImg
import com.example.taoyuangutter.common.PhotoUploadSlotState

/**
 * Records server photo ownership as soon as an existing point is imported.
 * Local downloads are for preview only and must not make a server image appear
 * pending for upload.
 */
internal fun applyImportedPhotoUploadState(
    formData: MutableMap<String, String>,
    images: List<NodeImg>
) {
    images.forEach { image ->
        val slot = image.fileCategory?.toIntOrNull()?.takeIf { it in 1..3 } ?: return@forEach
        val imgId = image.id ?: return@forEach
        PhotoUploadSlotState.writeState(
            data = formData,
            slot = slot,
            state = PhotoUploadSlotState.STATE_SUCCESS,
            imgId = imgId,
            error = null
        )
    }
}
