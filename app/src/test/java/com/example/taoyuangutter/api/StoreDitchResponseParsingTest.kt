package com.example.taoyuangutter.api

import com.example.taoyuangutter.gutter.StoreDitchResponseWaypointMapper
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreDitchResponseParsingTest {
    @Test
    fun parsesNodeDetailsUppercaseConnectionStringsAndNormalizesCantOpenPriority() {
        val response = Gson().fromJson(
            """
            {
              "success": true,
              "message": "OK",
              "data": [
                {"node_id": 1, "IS_CANTOPEN": "0", "IS_TIEINPOINT": "1", "IS_CONNECTING": "1"},
                {"node_id": 2, "IS_CANTOPEN": "1", "IS_TIEINPOINT": "1", "IS_CONNECTING": "0"},
                {"node_id": 3, "IS_CANTOPEN": "0"}
              ]
            }
            """.trimIndent(),
            NodeDetailsResponse::class.java
        )

        val normal = response.data!![0]
        assertTrue(normal.isTieInPointAsBoolean)
        assertTrue(normal.isConnectingAsBoolean)

        val cantOpenWins = response.data!![1]
        assertTrue(cantOpenWins.isCantOpenAsBoolean)
        assertFalse(cantOpenWins.isTieInPointAsBoolean)
        assertFalse(cantOpenWins.isConnectingAsBoolean)

        val missingDefaults = response.data!![2]
        assertFalse(missingDefaults.isTieInPointAsBoolean)
        assertFalse(missingDefaults.isConnectingAsBoolean)
    }

    @Test
    fun parsesGeneratedXyNumbersAndConnectionFlagsFromStoreResponse() {
        val response = Gson().fromJson(
            """
            {
              "success": true,
              "message": "新增成功",
              "data": {
                "ditch_id": 10233,
                "SPI_NUM": "3207-ZZ-00208",
                "XY_NUM": {"起點": "A-START", "節點": ["A-NODE"], "終點": "A-END"},
                "nodes": [
                  {
                    "node_id": 10889,
                    "NODE_ATT": "1",
                    "NODE_NUM": "A-NODE",
                    "url": []
                  }
                ]
              }
            }
            """.trimIndent(),
            StoreDitchResponse::class.java
        )

        assertEquals("A-START", response.data?.xyNum?.start)
        assertEquals(listOf("A-NODE"), response.data?.xyNum?.nodes)
        assertEquals("A-END", response.data?.xyNum?.end)
    }

    @Test
    fun parsesStoreDitchUrlIdsAndMapsThemToDraftPhotoIds() {
        val response = Gson().fromJson(
            """
            {
              "success": true,
              "message": "新增成功",
              "data": {
                "ditch_id": 10232,
                "SPI_NUM": "3207-ZZ-00207",
                "nodes": [
                  {
                    "node_id": 10888,
                    "NODE_ATT": "1",
                    "NODE_NUM": null,
                    "url": [
                      {"url": "https://example.test/1.jpg", "node_id": "10888", "fileCategory": "1", "id": 12135},
                      {"url": "https://example.test/2.jpg", "node_id": "10888", "fileCategory": "2", "id": 12134},
                      {"url": "https://example.test/3.jpg", "node_id": "10888", "fileCategory": "3", "id": 12133}
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),
            StoreDitchResponse::class.java
        )

        val node = response.data?.nodes?.single()
        assertNotNull(node)
        assertEquals(listOf(12135, 12134, 12133), node?.url?.map { it.id })

        val waypoint = Waypoint(
            type = WaypointType.START,
            label = "起點",
            latLng = LatLng(25.0, 121.0)
        )
        val mapped = StoreDitchResponseWaypointMapper.apply(listOf(waypoint), listOf(node!!)).single()

        assertEquals("10888", mapped.basicData["_nodeId"])
        assertEquals("12135", mapped.basicData["photo1ImgId"])
        assertEquals("12134", mapped.basicData["photo2ImgId"])
        assertEquals("12133", mapped.basicData["photo3ImgId"])
    }
}
