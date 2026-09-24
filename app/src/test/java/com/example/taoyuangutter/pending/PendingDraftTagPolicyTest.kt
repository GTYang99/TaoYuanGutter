package com.example.taoyuangutter.pending

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingDraftTagPolicyTest {
    @Test
    fun validStartSpiNumTakesPrecedenceOverSubmissionState() {
        val draft = draft(
            submitted = true,
            waypoints = listOf(start("  SPI-1  "))
        )

        assertTrue(PendingDraftTagPolicy.hasValidSpiNum(draft))
        assertEquals(PendingDraftTagKind.EXISTING_GUTTER, PendingDraftTagPolicy.kindFor(draft))
    }

    @Test
    fun emptyAndWhitespaceStartSpiNumAreInvalid() {
        listOf("", "   ").forEach { value ->
            val draft = draft(waypoints = listOf(start(value)))

            assertFalse(PendingDraftTagPolicy.hasValidSpiNum(draft))
            assertEquals(PendingDraftTagKind.UNSUBMITTED, PendingDraftTagPolicy.kindFor(draft))
        }
    }

    @Test
    fun missingStartOrNonStartSpiNumAreInvalid() {
        val missingStart = draft(
            waypoints = listOf(WaypointSnapshot(type = "NODE", basicData = hashMapOf("SPI_NUM" to "SPI-1")))
        )
        val nonStartOnly = draft(
            waypoints = listOf(WaypointSnapshot(type = "END", basicData = hashMapOf("SPI_NUM" to "SPI-2")))
        )

        assertFalse(PendingDraftTagPolicy.hasValidSpiNum(missingStart))
        assertFalse(PendingDraftTagPolicy.hasValidSpiNum(nonStartOnly))
    }

    @Test
    fun generalSubmittedAndUnsubmittedDraftsUseFallbackTags() {
        assertEquals(
            PendingDraftTagKind.SUBMITTED,
            PendingDraftTagPolicy.kindFor(draft(submitted = true))
        )
        assertEquals(
            PendingDraftTagKind.UNSUBMITTED,
            PendingDraftTagPolicy.kindFor(draft(submitted = false))
        )
    }

    private fun draft(
        submitted: Boolean = false,
        waypoints: List<WaypointSnapshot> = emptyList()
    ) = GutterSessionDraft(
        hasSubmittedStoreDitch = submitted,
        waypoints = waypoints
    )

    private fun start(spiNum: String) = WaypointSnapshot(
        type = "START",
        basicData = hashMapOf("SPI_NUM" to spiNum)
    )
}
