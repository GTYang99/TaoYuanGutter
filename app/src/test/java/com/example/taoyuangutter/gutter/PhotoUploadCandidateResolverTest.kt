package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.WaypointSnapshot
import com.example.taoyuangutter.common.PhotoUploadSlotState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class PhotoUploadCandidateResolverTest {
    @Test
    fun unchangedExistingPhotosAreRemovedFromUploadCopyForAllSlots() {
        val original = waypoint(
            "photo1" to "content://one", "photo1CapturedAt" to "at-1",
            "photo2" to "content://two", "photo2CapturedAt" to "at-2",
            "photo3" to "content://three", "photo3CapturedAt" to "at-3"
        )
        val current = original.copy(basicData = HashMap(original.basicData))
        val candidate = PhotoUploadCandidateResolver.resolve(
            listOf(current), listOf(snapshot(original)), resumedFromDraft = false
        ).single()

        assertEquals("", candidate.basicData["photo1"])
        assertEquals("", candidate.basicData["photo2"])
        assertEquals("", candidate.basicData["photo3"])
        assertEquals("at-1", current.basicData["photo1CapturedAt"])
        assertEquals("content://two", current.basicData["photo2"])
        assertNotSame(current, candidate)
    }

    @Test
    fun replacingOneSlotKeepsOnlyThatSlotAsUploadCandidate() {
        val original = waypoint(
            "photo1" to "content://one", "photo1CapturedAt" to "at-1",
            "photo2" to "content://two", "photo2CapturedAt" to "at-2",
            "photo3" to "content://three", "photo3CapturedAt" to "at-3"
        )
        val currentData = HashMap(original.basicData).apply {
            this["photo2"] = "content://new-two"
            this["photo2CapturedAt"] = "new-at-2"
        }
        val candidate = PhotoUploadCandidateResolver.resolve(
            listOf(original.copy(basicData = currentData)),
            listOf(snapshot(original)),
            resumedFromDraft = false
        ).single()

        assertEquals("", candidate.basicData["photo1"])
        assertEquals("content://new-two", candidate.basicData["photo2"])
        assertEquals("", candidate.basicData["photo3"])
    }

    @Test
    fun draftResumePreservesAllPhotoCandidatesAndMetadata() {
        val current = waypoint(
            "photo1" to "content://one", "photo1CapturedAt" to "at-1", "photo1ImgId" to "11",
            "photo2" to "content://two", "photo2CapturedAt" to "at-2", "photo2ImgId" to "22",
            "photo3" to "content://three", "photo3CapturedAt" to "at-3", "photo3ImgId" to "33"
        )
        val result = PhotoUploadCandidateResolver.resolve(
            listOf(current), emptyList(), resumedFromDraft = true
        )

        assertEquals(current, result.single())
        assertEquals("22", result.single().basicData["photo2ImgId"])
    }

    @Test
    fun importedExistingPhotoIdsRemainAvailableToUploadGuard() {
        val imported = waypoint(
            "photo1" to "content://imported-one",
            "photo1CapturedAt" to "at-1",
            "photo1ImgId" to "101",
            "photo1UploadState" to "success"
        )

        val result = PhotoUploadCandidateResolver.resolve(
            listOf(imported), originalWaypoints = emptyList(), resumedFromDraft = false
        ).single()

        assertEquals("101", result.basicData["photo1ImgId"])
        assertEquals("success", result.basicData["photo1UploadState"])
        assertEquals("content://imported-one", result.basicData["photo1"])
    }

    @Test
    fun importedExistingPhotoWithoutImageIdIsStillMarkedAsUploaded() {
        val imported = waypoint(
            "photo1" to "content://imported-one",
            "photo1UploadState" to PhotoUploadSlotState.STATE_SUCCESS
        )

        assertEquals(true, PhotoUploadSlotState.isAlreadyUploaded(imported.basicData, 1))
    }

    @Test
    fun successfulImportedPhotoWithoutImageIdIsExcludedBySubmitUploadGuard() {
        val imported = waypoint(
            "photo1" to "content://imported-one",
            "photo1UploadState" to PhotoUploadSlotState.STATE_SUCCESS
        )

        // AddGutterBottomSheet's pre-submit guard must use this shared rule.
        assertEquals(false, !PhotoUploadSlotState.isAlreadyUploaded(imported.basicData, 1))
    }

    @Test
    fun replacedPhotoWithoutSuccessfulStateRemainsEligibleForSubmitUpload() {
        val replacement = waypoint(
            "photo1" to "content://new-photo",
            "photo1UploadState" to PhotoUploadSlotState.STATE_IDLE
        )

        assertEquals(false, PhotoUploadSlotState.isAlreadyUploaded(replacement.basicData, 1))
    }

    @Test
    fun clearingFormerServerMetadataMakesReplacementUploadEligible() {
        val replacement = waypoint(
            "photo1" to "content://new-photo",
            "photo1ImgId" to "101",
            "photo1UploadState" to PhotoUploadSlotState.STATE_SUCCESS
        )

        PhotoUploadSlotState.clear(replacement.basicData, 1)

        assertEquals("content://new-photo", replacement.basicData["photo1"])
        assertEquals(false, PhotoUploadSlotState.isAlreadyUploaded(replacement.basicData, 1))
    }

    private fun waypoint(vararg entries: Pair<String, String>): Waypoint = Waypoint(
        type = WaypointType.NODE,
        label = "節點1",
        basicData = hashMapOf("_nodeId" to "42", *entries),
        uid = "uid-42"
    )

    private fun snapshot(waypoint: Waypoint) = WaypointSnapshot(
        type = waypoint.type.name,
        label = waypoint.label,
        latitude = waypoint.latLng?.latitude,
        longitude = waypoint.latLng?.longitude,
        basicData = HashMap(waypoint.basicData),
        uid = waypoint.uid
    )
}
