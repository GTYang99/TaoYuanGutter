package com.example.taoyuangutter.pending

enum class PendingDraftTagKind {
    SUBMITTED,
    UNSUBMITTED
}

object PendingDraftTagPolicy {
    fun kindFor(draft: GutterSessionDraft): PendingDraftTagKind? {
        val spiNum = draft.waypoints
            .firstOrNull { it.type == "START" }
            ?.basicData
            ?.get("SPI_NUM")
            ?.trim()

        if (!spiNum.isNullOrEmpty()) return null
        return if (draft.hasSubmittedStoreDitch) {
            PendingDraftTagKind.SUBMITTED
        } else {
            PendingDraftTagKind.UNSUBMITTED
        }
    }
}
