package com.example.taoyuangutter.api

import android.util.Log

fun NodeDetails.safeCapturedAt(index: Int, tag: String? = null, reason: String = ""): String? {
    return try {
        capturedAt.getOrNull(index)
    } catch (t: Throwable) {
        if (tag != null) {
            Log.w(
                tag,
                "captured_at unavailable nodeId=$nodeId index=$index reason=$reason",
                t
            )
        }
        null
    }
}
