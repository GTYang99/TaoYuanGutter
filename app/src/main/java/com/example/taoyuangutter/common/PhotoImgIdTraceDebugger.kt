package com.example.taoyuangutter.common

import android.net.Uri
import android.util.Log
import java.util.Locale

object PhotoImgIdTraceDebugger {
    private val lastNonEmptyImgIds = mutableMapOf<Int, String>()
    private val firstDropStages = mutableSetOf<Int>()

    @Synchronized
    fun record(owner: String, stage: String, imgIds: List<String?>, summary: String) {
        Log.d(owner, "imgId-trace[$stage] $summary")
        imgIds.forEachIndexed { index, rawValue ->
            val slot = index + 1
            val value = rawValue?.trim().orEmpty()
            val isEmpty = value.isEmpty() || value == "-" || value.equals("null", ignoreCase = true)
            if (!isEmpty) {
                lastNonEmptyImgIds[slot] = value
                return@forEachIndexed
            }
            val previous = lastNonEmptyImgIds[slot] ?: return@forEachIndexed
            if (firstDropStages.add(slot)) {
                Log.w(
                    owner,
                    "imgId-trace-first-drop[slot=$slot stage=$stage previous=$previous]"
                )
            }
        }
    }

    fun logPhotoUriState(owner: String, stage: String, data: Map<String, String>, label: String? = null) {
        val summary = buildString {
            append("uri-trace[$stage]")
            if (!label.isNullOrBlank()) {
                append(" label=")
                append(label)
            }
            (1..3).forEach { slot ->
                val photoKey = "photo$slot"
                val pendingKey = "_pending_photo_${slot}_path"
                val photoValue = data[photoKey].orEmpty()
                val pendingValue = data[pendingKey].orEmpty()
                append(" | p")
                append(slot)
                append("(scheme=")
                append(uriScheme(photoValue))
                append(",value=")
                append(shorten(photoValue))
                append(",pendingScheme=")
                append(uriScheme(pendingValue))
                append(",pending=")
                append(shorten(pendingValue))
                append(')')
            }
        }
        Log.d(owner, summary)
    }

    fun logPhotoSubmitSourceState(
        owner: String,
        stage: String,
        label: String?,
        slot: Int,
        photoValue: String?,
        pendingValue: String?,
        usable: Boolean
    ) {
        val summary = buildString {
            append("submit-uri-trace[$stage]")
            if (!label.isNullOrBlank()) {
                append(" label=")
                append(label)
            }
            append(" slot=")
            append(slot)
            append(" photoScheme=")
            append(uriScheme(photoValue.orEmpty()))
            append(" photo=")
            append(shorten(photoValue.orEmpty()))
            append(" pendingScheme=")
            append(uriScheme(pendingValue.orEmpty()))
            append(" pending=")
            append(shorten(pendingValue.orEmpty()))
            append(" usable=")
            append(usable)
        }
        Log.d(owner, summary)
    }

    private fun uriScheme(value: String): String {
        if (value.isBlank()) return "-"
        val scheme = runCatching { Uri.parse(value).scheme }.getOrNull()?.lowercase(Locale.US)
        return scheme ?: "(none)"
    }

    private fun shorten(value: String, maxLength: Int = 96): String {
        if (value.isBlank()) return "-"
        return if (value.length <= maxLength) value else value.take(maxLength - 3) + "..."
    }
}
