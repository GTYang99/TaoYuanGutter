package com.example.taoyuangutter.api

import com.example.taoyuangutter.gutter.StoreDitchResponseWaypointMapper
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StoreDitchResponseParsingTest {
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
