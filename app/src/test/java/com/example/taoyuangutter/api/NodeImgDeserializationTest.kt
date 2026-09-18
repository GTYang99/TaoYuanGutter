package com.example.taoyuangutter.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class NodeImgDeserializationTest {
    private val gson = Gson()

    @Test
    fun nodeImgReadsDitchDetailsId() {
        val image = gson.fromJson(
            """{"url":"https://example.test/1.jpg","fileCategory":"1","id":12043}""",
            NodeImg::class.java
        )

        assertEquals(12043, image.id)
    }

    @Test
    fun nodeImgReadsUploadStyleImgIdAlias() {
        val image = gson.fromJson(
            """{"url":"https://example.test/1.jpg","fileCategory":"1","img_id":12043}""",
            NodeImg::class.java
        )

        assertEquals(12043, image.id)
    }

    @Test
    fun nodeDetailsReadsStoreDitchUrlShapeAndUsesItsImageId() {
        val details = gson.fromJson(
            """{
                "node_id": 10979,
                "url": [{
                    "url": "https://example.test/1.jpg",
                    "node_id": "10979",
                    "fileCategory": "1",
                    "id": 12339
                }]
            }""".trimIndent(),
            NodeDetails::class.java
        )

        assertEquals("https://example.test/1.jpg", details.photoImage("1")?.url)
        assertEquals(12339, details.photoImage("1")?.id)
    }
}
