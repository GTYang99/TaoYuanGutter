package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.common.PhotoUploadSlotState
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingPhotoUploadCandidatePlannerTest {

    @Test
    fun importedServerImagesProduceNoBatchUploadCandidates() {
        val waypoint = waypoint("photo1" to "content://imported")
        PhotoUploadSlotState.writeState(
            waypoint.basicData,
            slot = 1,
            state = PhotoUploadSlotState.STATE_SUCCESS,
            imgId = 101
        )

        assertEquals(emptyList<PendingPhotoUploadCandidatePlanner.Candidate>(), candidates(waypoint))
    }

    @Test
    fun coordinatorResolvedSlotProducesNoBatchUploadCandidate() {
        val waypoint = waypoint("photo1" to "content://coordinator-result")
        PhotoUploadSlotState.writeState(
            waypoint.basicData,
            slot = 1,
            state = PhotoUploadSlotState.STATE_SUCCESS,
            imgId = 202
        )

        assertEquals(emptyList<PendingPhotoUploadCandidatePlanner.Candidate>(), candidates(waypoint))
    }

    @Test
    fun newUsablePhotoProducesOneBatchUploadCandidate() {
        val waypoint = waypoint("photo1" to "content://new-photo")

        assertEquals(
            listOf(PendingPhotoUploadCandidatePlanner.Candidate(0, 1, "content://new-photo")),
            candidates(waypoint)
        )
    }

    private fun candidates(waypoint: Waypoint) = PendingPhotoUploadCandidatePlanner.resolve(
        waypoints = listOf(waypoint),
        isUnchangedPhoto = { _, _ -> false },
        isUsableForUpload = { !it.isNullOrBlank() }
    )

    private fun waypoint(vararg data: Pair<String, String>) = Waypoint(
        type = WaypointType.NODE,
        label = "節點1",
        basicData = hashMapOf(*data)
    )
}
