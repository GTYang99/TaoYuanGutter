package com.example.taoyuangutter.pending

enum class PendingDraftTagKind {
    EXISTING_GUTTER,
    SUBMITTED,
    UNSUBMITTED
}

object PendingDraftTagPolicy {
    /**
     * Existing-gutter identity is intentionally limited to the START waypoint.
     * The normalized value is shared by list rendering, title/delete identity,
     * and submitted-draft resume policy so those entry points cannot diverge.
     */
    fun spiNumFor(draft: GutterSessionDraft): String? = draft.waypoints
            .firstOrNull { it.type == "START" }
            ?.basicData
            ?.get("SPI_NUM")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun hasValidSpiNum(draft: GutterSessionDraft): Boolean = spiNumFor(draft) != null

    fun kindFor(draft: GutterSessionDraft): PendingDraftTagKind = when {
        hasValidSpiNum(draft) -> PendingDraftTagKind.EXISTING_GUTTER
        draft.hasSubmittedStoreDitch -> PendingDraftTagKind.SUBMITTED
        else -> PendingDraftTagKind.UNSUBMITTED
    }
}
