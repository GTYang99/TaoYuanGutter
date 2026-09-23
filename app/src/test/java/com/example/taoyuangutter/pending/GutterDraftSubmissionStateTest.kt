package com.example.taoyuangutter.pending

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GutterDraftSubmissionStateTest {
    @Test
    fun generalDraftWithSubmittedMarkerUsesSubmittedTag() {
        val draft = draft(hasSubmittedStoreDitch = true)

        assertEquals(PendingDraftTagKind.SUBMITTED, PendingDraftTagPolicy.kindFor(draft))
    }

    @Test
    fun generalDraftWithoutSubmittedMarkerUsesUnsubmittedTag() {
        val draft = draft(hasSubmittedStoreDitch = false)

        assertEquals(PendingDraftTagKind.UNSUBMITTED, PendingDraftTagPolicy.kindFor(draft))
    }

    @Test
    fun existingGutterDraftWithSpiNumHidesSubmissionTag() {
        val draft = draft(
            hasSubmittedStoreDitch = true,
            waypoints = listOf(
                WaypointSnapshot(
                    type = "START",
                    basicData = hashMapOf("SPI_NUM" to "TYG-EXISTING")
                )
            )
        )

        assertNull(PendingDraftTagPolicy.kindFor(draft))
    }

    private fun draft(
        hasSubmittedStoreDitch: Boolean,
        waypoints: List<WaypointSnapshot> = emptyList()
    ) = GutterSessionDraft(
        id = 923L,
        createdAt = 1L,
        savedAt = 2L,
        hasSubmittedStoreDitch = hasSubmittedStoreDitch,
        waypoints = waypoints
    )
}
