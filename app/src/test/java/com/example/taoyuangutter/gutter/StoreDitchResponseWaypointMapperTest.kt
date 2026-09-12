package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.NodeImageUrl
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreDitchResponseWaypointMapperTest {
    @Test
    fun mapsStoreDitchPhotoIdsByFileCategoryAndPreservesMissingIds() {
        val waypoint = Waypoint(
            type = WaypointType.START,
            label = "起點",
            latLng = LatLng(25.0, 121.0),
            basicData = hashMapOf("photo1ImgId" to "old-ignored")
        )
        val node = DitchNode(
            nodeId = 10888,
            nodeAtt = "1",
            nodeNum = null,
            url = listOf(
                image(category = "1", id = 12135),
                image(category = "2", id = 12134),
                image(category = "3", id = 12133)
            )
        )

        val mapped = StoreDitchResponseWaypointMapper.apply(listOf(waypoint), listOf(node)).single()

        assertEquals("10888", mapped.basicData["_nodeId"])
        assertEquals("12135", mapped.basicData["photo1ImgId"])
        assertEquals("12134", mapped.basicData["photo2ImgId"])
        assertEquals("12133", mapped.basicData["photo3ImgId"])
    }

    @Test
    fun doesNotEraseExistingPhotoIdWhenResponseHasNoId() {
        val waypoint = Waypoint(
            type = WaypointType.NODE,
            label = "節點",
            basicData = hashMapOf("photo2ImgId" to "12044")
        )
        val node = DitchNode(
            nodeId = 10840,
            nodeAtt = "2",
            nodeNum = "1",
            url = listOf(image(category = "2", id = null))
        )

        val mapped = StoreDitchResponseWaypointMapper.apply(listOf(waypoint), listOf(node)).single()

        assertEquals("12044", mapped.basicData["photo2ImgId"])
    }

    private fun image(category: String, id: Int?): NodeImageUrl = NodeImageUrl(
        url = "https://example.test/$category.jpg",
        nodeId = "10888",
        fileCategory = category,
        id = id
    )
}
