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
    fun newNodeOmitsCapturedAtButRetainsImgIdsInJson() {
        val waypoint = waypoint(
            "photo1" to "content://photo-1",
            "photo1CapturedAt" to "2026-09-11T01:00:00Z",
            "photo1ImgId" to "101",
            "photo2" to "content://photo-2",
            "photo2CapturedAt" to "2026-09-11T01:01:00Z"
        )
        val request = StoreDitchNodeRequestMapper.map(waypoint, requestNodeId = null, nodeSequence = 1)
        val json = gson.toJson(request)

        assertFalse(json.contains("\"captured_at\""))
        assertFalse(json.contains("2026-09-11T01:00:00Z"))
        assertTrue(json.contains("\"img_ids\""))
        assertTrue(json.contains("101"))
    }

    @Test
    fun normalNewNodeOmitsXyNumAndSendsUppercaseConnectionFlagsAsBooleans() {
        val waypoint = waypoint(
            "IS_TIEINPOINT" to "0",
            "IS_CONNECTING" to "0"
        )
        val json = gson.toJson(StoreDitchNodeRequestMapper.map(waypoint, null, 1))

        assertFalse(json.contains("\"XY_NUM\""))
        assertTrue(json.contains("\"IS_CANTOPEN\":false"))
        assertTrue(json.contains("\"IS_TIEINPOINT\":false"))
        assertTrue(json.contains("\"IS_CONNECTING\":false"))
        assertFalse(json.contains("is_connect_point"))
        assertFalse(json.contains("is_connect_pipe"))
    }

    @Test
    fun tieInPointOmitsExemptDetailAndConnectingParameters() {
        val waypoint = waypoint(
            "IS_TIEINPOINT" to "1",
            "IS_CONNECTING" to "1",
            "MAT_TYP" to "2",
            "NODE_DEP" to "89",
            "NODE_WID" to "78",
            "COVER_DEP" to "5",
            "IS_BROKEN" to "1",
            "IS_HANGING" to "1",
            "IS_SILT" to "2",
            "photo1ImgId" to "101",
            "photo2ImgId" to "102",
            "photo3ImgId" to "103"
        )
        val json = gson.toJson(StoreDitchNodeRequestMapper.map(waypoint, null, 1))

        assertTrue(json.contains("\"IS_CANTOPEN\":false"))
        assertTrue(json.contains("\"IS_TIEINPOINT\":true"))
        assertFalse(json.contains("\"IS_CONNECTING\""))
        assertFalse(json.contains("\"MAT_TYP\""))
        assertFalse(json.contains("\"NODE_DEP\""))
        assertFalse(json.contains("\"NODE_WID\""))
        assertFalse(json.contains("\"COVER_DEP\""))
        assertFalse(json.contains("\"IS_BROKEN\""))
        assertFalse(json.contains("\"IS_HANGING\""))
        assertFalse(json.contains("\"IS_SILT\""))
        assertTrue(json.contains("\"img_ids\":[101]"))
    }

    @Test
    fun cantOpenOmitsExemptDetailAndConnectingParameters() {
        val waypoint = waypoint(
            "IS_CANTOPEN" to "1",
            "IS_TIEINPOINT" to "1",
            "IS_CONNECTING" to "1",
            "MAT_TYP" to "2",
            "NODE_DEP" to "89",
            "NODE_WID" to "78",
            "COVER_DEP" to "5",
            "IS_BROKEN" to "1",
            "IS_HANGING" to "1",
            "IS_SILT" to "2",
            "photo1ImgId" to "101",
            "photo2ImgId" to "102",
            "photo3ImgId" to "103"
        )
        val json = gson.toJson(StoreDitchNodeRequestMapper.map(waypoint, null, 1))

        assertTrue(json.contains("\"IS_CANTOPEN\":true"))
        assertTrue(json.contains("\"IS_TIEINPOINT\":false"))
        assertFalse(json.contains("\"IS_CONNECTING\""))
        assertFalse(json.contains("\"MAT_TYP\""))
        assertFalse(json.contains("\"NODE_DEP\""))
        assertFalse(json.contains("\"NODE_WID\""))
        assertFalse(json.contains("\"COVER_DEP\""))
        assertFalse(json.contains("\"IS_BROKEN\""))
        assertFalse(json.contains("\"IS_HANGING\""))
        assertFalse(json.contains("\"IS_SILT\""))
        assertTrue(json.contains("\"img_ids\":[101]"))
    }

    @Test
    fun virtualNodeOmitsCantOpenAndConnectionFlags() {
        val waypoint = waypoint(
            "IS_CANTOPEN" to "1",
            "IS_TIEINPOINT" to "1",
            "IS_CONNECTING" to "1",
            "is_virtual" to "1"
        )
        val json = gson.toJson(StoreDitchNodeRequestMapper.map(waypoint, null, 1))

        assertFalse(json.contains("\"IS_CANTOPEN\""))
        assertFalse(json.contains("\"IS_TIEINPOINT\""))
        assertFalse(json.contains("\"IS_CONNECTING\""))
        assertFalse(json.contains("is_connect_point"))
        assertFalse(json.contains("is_connect_pipe"))
    }

    @Test
    fun editNodePreservesExistingXyNum() {
        val waypoint = waypoint("XY_NUM" to "E0001")
        val json = gson.toJson(StoreDitchNodeRequestMapper.map(waypoint, 42, 1))

        assertTrue(json.contains("\"XY_NUM\":\"E0001\""))
    }

    private fun waypoint(vararg entries: Pair<String, String>): Waypoint = Waypoint(
        type = WaypointType.NODE,
        label = "節點1",
        latLng = LatLng(24.99, 121.31),
        basicData = hashMapOf(*entries)
    )
}
