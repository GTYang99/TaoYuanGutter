package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft

/**
 * Policy shared by the pending list and the restore flow.
 * Existing server gutters (identified by SPI_NUM) keep their legacy inspect/edit
 * behavior; only ordinary drafts with a persisted submission marker become read-only.
 */
object SubmittedDraftResumePolicy {
    fun isReadOnly(draft: GutterSessionDraft): Boolean {
        if (!draft.hasSubmittedStoreDitch) return false
        return draft.waypoints
            .firstOrNull { it.type == WaypointType.START.name }
            ?.basicData
            ?.get("SPI_NUM")
            .isNullOrBlank()
    }
}
