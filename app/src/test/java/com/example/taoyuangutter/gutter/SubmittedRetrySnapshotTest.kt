package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmittedRetrySnapshotTest {
    @Test
    fun transportCopyDoesNotMutateRoomSnapshot() {
        val draft = GutterSessionDraft(
            hasSubmittedStoreDitch = true,
            spiTyp = "2",
            waypoints = listOf(
                WaypointSnapshot(
                    type = WaypointType.START.name,
                    basicData = hashMapOf("photo1" to "content://photo")
                )
            )
        )

        val snapshot = SubmittedRetrySnapshot.fromDraft(draft)
        val transport = snapshot.toTransportWaypoints()
        transport.single().basicData["photo1UploadState"] = "success"
        transport.single().basicData["photo1ImgId"] = "901"

        assertNull(draft.waypoints.single().basicData["photo1UploadState"])
        assertNull(draft.waypoints.single().basicData["photo1ImgId"])
        assertEquals("2", snapshot.spiTyp)
        assertFalse(snapshot.isCurve)
    }

    @Test
    fun submittedRetryCallbackPolicyBlocksMutableWaypointUpdates() {
        assertFalse(GutterSheetSessionBinder.shouldForwardWaypointUpdate(true))
        assertTrue(GutterSheetSessionBinder.shouldForwardWaypointUpdate(false))
    }
}
