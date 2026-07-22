package com.example.taoyuangutter.common

object PhotoUploadSlotState {
    private const val IMG_ID_SUFFIX = "ImgId"
    private const val STATE_SUFFIX = "UploadState"
    private const val ERROR_SUFFIX = "UploadError"

    const val STATE_IDLE = "idle"
    const val STATE_UPLOADING = "uploading"
    const val STATE_SUCCESS = "success"
    const val STATE_FAILED = "failed"

    fun imgIdKey(slot: Int): String = "photo${slot}$IMG_ID_SUFFIX"
    fun stateKey(slot: Int): String = "photo${slot}$STATE_SUFFIX"
    fun errorKey(slot: Int): String = "photo${slot}$ERROR_SUFFIX"

    fun readImgId(data: Map<String, String>, slot: Int): Int? =
        data[imgIdKey(slot)]?.trim()?.toIntOrNull()

    fun readState(data: Map<String, String>, slot: Int): String =
        data[stateKey(slot)]?.trim()?.takeIf { it.isNotEmpty() } ?: STATE_IDLE

    fun readError(data: Map<String, String>, slot: Int): String? =
        data[errorKey(slot)]?.trim()?.takeIf { it.isNotEmpty() }

    fun writeState(
        data: MutableMap<String, String>,
        slot: Int,
        state: String,
        imgId: Int? = null,
        error: String? = null
    ) {
        data[stateKey(slot)] = state
        if (imgId == null) {
            data.remove(imgIdKey(slot))
        } else {
            data[imgIdKey(slot)] = imgId.toString()
        }
        if (error.isNullOrBlank()) {
            data.remove(errorKey(slot))
        } else {
            data[errorKey(slot)] = error
        }
    }

    fun clear(data: MutableMap<String, String>, slot: Int) {
        data.remove(stateKey(slot))
        data.remove(imgIdKey(slot))
        data.remove(errorKey(slot))
    }
}
