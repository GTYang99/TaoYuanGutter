package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoResultMetadataMergerTest {
    @Test
    fun newUploadedPhotoKeepsReturnedIdAndSuccessState() {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf("photo1" to ""),
            incoming = mapOf(
                "photo1" to "content://new-photo",
                "photo1ImgId" to "123",
                "photo1UploadState" to "success",
                "photo1CapturedAt" to "2026-09-20T10:00:00Z"
            )
        )

        assertEquals("content://new-photo", merged["photo1"])
        assertEquals("123", merged["photo1ImgId"])
        assertEquals("success", merged["photo1UploadState"])
        assertEquals("2026-09-20T10:00:00Z", merged["photo1CapturedAt"])
    }

    @Test
    fun replacementKeepsNewIdAndDoesNotKeepOldId() {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf(
                "photo1" to "content://old-photo",
                "photo1ImgId" to "101",
                "photo1UploadState" to "success"
            ),
            incoming = mapOf(
                "photo1" to "content://new-photo",
                "photo1ImgId" to "202",
                "photo1UploadState" to "success"
            )
        )

        assertEquals("202", merged["photo1ImgId"])
        assertEquals("success", merged["photo1UploadState"])
        assertFalse(merged["photo1ImgId"] == "101")
    }

    @Test
    fun changedPhotoKeepsCurrentUploadingStateButClearsOldId() {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf(
                "photo1" to "content://old-photo",
                "photo1ImgId" to "101",
                "photo1UploadState" to "success"
            ),
            incoming = mapOf(
                "photo1" to "content://new-photo",
                "photo1UploadState" to "uploading"
            )
        )

        assertNull(merged["photo1ImgId"])
        assertEquals("uploading", merged["photo1UploadState"])
    }

    @Test
    fun deletedPhotoClearsFormerServerMetadata() {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf(
                "photo1" to "content://old-photo",
                "photo1ImgId" to "101",
                "photo1UploadState" to "success",
                "photo1CapturedAt" to "old-time"
            ),
            incoming = mapOf("photo1" to "")
        )

        assertEquals("", merged["photo1"])
        assertNull(merged["photo1ImgId"])
        assertNull(merged["photo1UploadState"])
        assertNull(merged["photo1CapturedAt"])
    }

    @Test
    fun samePhotoPreservesExistingMetadataWhenResultOmitsIt() {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf(
                "photo1" to "content://same-photo",
                "photo1ImgId" to "101",
                "photo1UploadState" to "success"
            ),
            incoming = mapOf("photo1" to "content://same-photo")
        )

        assertTrue(merged["photo1UploadState"] == "success")
        assertEquals("101", merged["photo1ImgId"])
    }

    @Test
    fun reversingWaypointsKeepsPhotoIdWithItsWaypoint() {
        val first = Waypoint(
            type = WaypointType.START,
            label = "起點",
            basicData = hashMapOf("photo1ImgId" to "101"),
            uid = "uid-first"
        )
        val second = Waypoint(
            type = WaypointType.END,
            label = "終點",
            basicData = hashMapOf("photo1ImgId" to "202"),
            uid = "uid-second"
        )

        val reversed = mutableListOf(first, second).apply { reverse() }

        assertEquals("uid-second", reversed[0].uid)
        assertEquals("202", reversed[0].basicData["photo1ImgId"])
        assertEquals("uid-first", reversed[1].uid)
        assertEquals("101", reversed[1].basicData["photo1ImgId"])
    }
}
