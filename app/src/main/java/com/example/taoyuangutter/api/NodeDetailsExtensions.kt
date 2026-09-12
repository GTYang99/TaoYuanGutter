package com.example.taoyuangutter.api

import android.util.Log

fun NodeDetails.safeCapturedAt(index: Int, tag: String? = null, reason: String = ""): String? {
    return try {
        // Gson may assign null to the non-null Kotlin default when the API
        // explicitly returns captured_at: null. Treat that as no timestamp.
        capturedAt?.getOrNull(index)
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
