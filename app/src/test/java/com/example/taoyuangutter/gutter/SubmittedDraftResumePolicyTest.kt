package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmittedDraftResumePolicyTest {
    @Test
    fun submittedOrdinaryDraftEntersReadOnlyMode() {
        val draft = GutterSessionDraft(
            hasSubmittedStoreDitch = true,
            waypoints = listOf(WaypointSnapshot(type = WaypointType.START.name))
        )

        assertTrue(SubmittedDraftResumePolicy.isReadOnly(draft))
    }

    @Test
    fun unsubmittedDraftStaysEditable() {
        val draft = GutterSessionDraft(
            hasSubmittedStoreDitch = false,
            waypoints = listOf(WaypointSnapshot(type = WaypointType.START.name))
        )

        assertFalse(SubmittedDraftResumePolicy.isReadOnly(draft))
    }

    @Test
    fun existingSpiNumDraftKeepsLegacyEditFlow() {
        val draft = GutterSessionDraft(
            hasSubmittedStoreDitch = true,
            waypoints = listOf(
                WaypointSnapshot(
                    type = WaypointType.START.name,
                    basicData = hashMapOf("SPI_NUM" to "SPI-1")
                )
            )
        )

        assertFalse(SubmittedDraftResumePolicy.isReadOnly(draft))
    }
}
