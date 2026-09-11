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
}
