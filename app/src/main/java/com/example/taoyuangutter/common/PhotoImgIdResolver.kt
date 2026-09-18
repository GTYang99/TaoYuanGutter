package com.example.taoyuangutter.common

/** Resolves an imported photo ID without discarding a server ID already in form state. */
object PhotoImgIdResolver {
    fun resolve(
        responseImgId: Int?,
        data: Map<String, String>,
        slot: Int
    ): Int? = responseImgId ?: PhotoUploadSlotState.readImgId(data, slot)
}
