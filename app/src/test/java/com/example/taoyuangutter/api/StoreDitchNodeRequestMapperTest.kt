package com.example.taoyuangutter.api

import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreDitchNodeRequestMapperTest {
    private val gson = Gson()

    @Test
    fun existingNodeIncludesUpdatedPhotoIdsButOmitsCapturedAt() {
        val waypoint = waypoint(
            "_nodeId" to "42",
            "photo1" to "content://photo-1",
            "photo1CapturedAt" to "2026-09-11T01:00:00Z",
            "photo1ImgId" to "101"
        )
        val request = StoreDitchNodeRequestMapper.map(waypoint, requestNodeId = 42, nodeSequence = 1)
        val json = gson.toJson(request)

        assertFalse(json.contains("\"captured_at\""))
        assertTrue(json.contains("\"img_ids\""))
        assertTrue(json.contains("101"))
        assertEquals("content://photo-1", waypoint.basicData["photo1"])
        assertEquals("101", waypoint.basicData["photo1ImgId"])
    }

    @Test
    fun newNodeRetainsPhotoMetadataInJson() {
        val waypoint = waypoint(
            "photo1" to "content://photo-1",
            "photo1CapturedAt" to "2026-09-11T01:00:00Z",
            "photo1ImgId" to "101",
            "photo2" to "content://photo-2",
            "photo2CapturedAt" to "2026-09-11T01:01:00Z"
        )
        val request = StoreDitchNodeRequestMapper.map(waypoint, requestNodeId = null, nodeSequence = 1)
        val json = gson.toJson(request)

        assertTrue(json.contains("\"captured_at\""))
        assertTrue(json.contains("2026-09-11T01:00:00Z"))
        assertTrue(json.contains("\"img_ids\""))
        assertTrue(json.contains("101"))
    }

    private fun waypoint(vararg entries: Pair<String, String>): Waypoint = Waypoint(
        type = WaypointType.NODE,
        label = "節點1",
        latLng = LatLng(24.99, 121.31),
        basicData = hashMapOf(*entries)
    )
}
