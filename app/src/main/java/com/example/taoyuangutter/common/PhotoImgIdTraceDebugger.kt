package com.example.taoyuangutter.common

import android.util.Log

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
}
