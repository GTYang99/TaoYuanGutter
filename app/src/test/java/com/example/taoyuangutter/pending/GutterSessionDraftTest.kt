package com.example.taoyuangutter.pending

import com.example.taoyuangutter.common.PhotoUploadSlotState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterSessionDraftTest {
    @Test
    fun serializationPreservesPendingDraftPhotoPathsAndUploadState() {
        val draft = GutterSessionDraft(
            id = 904L,
            savedAt = 1_725_408_000_000L,
            spiTyp = "1",
            waypoints = listOf(
                WaypointSnapshot(
                    type = "NODE",
                    label = "節點1",
                    latitude = 24.99,
                    longitude = 121.31,
                    basicData = hashMapOf(
                        "SPI_NUM" to "TYG-0904",
                        "photo1" to "/data/user/0/com.example.taoyuangutter/files/GUTTER_EXT_photo1.jpg",
                        "_pending_photo_1_path" to "/storage/emulated/0/DCIM/pending-photo.jpg",
                        PhotoUploadSlotState.stateKey(1) to PhotoUploadSlotState.STATE_FAILED,
                        PhotoUploadSlotState.errorKey(1) to "Unauthorized"
                    ),
                    uid = "stable-node"
                )
            )
        )

        val json = Gson().toJson(listOf(draft))
        val type = object : TypeToken<List<GutterSessionDraft>>() {}.type
        val restored = Gson().fromJson<List<GutterSessionDraft>>(json, type).single()
        val restoredBasicData = restored.waypoints.single().basicData

        assertTrue(restored.waypoints.isNotEmpty())
        assertEquals("TYG-0904", restoredBasicData["SPI_NUM"])
        assertEquals(
            "/data/user/0/com.example.taoyuangutter/files/GUTTER_EXT_photo1.jpg",
            restoredBasicData["photo1"]
        )
        assertEquals(
            "/storage/emulated/0/DCIM/pending-photo.jpg",
            restoredBasicData["_pending_photo_1_path"]
        )
        assertEquals(PhotoUploadSlotState.STATE_FAILED, restoredBasicData[PhotoUploadSlotState.stateKey(1)])
        assertEquals("Unauthorized", restoredBasicData[PhotoUploadSlotState.errorKey(1)])
    }
}
