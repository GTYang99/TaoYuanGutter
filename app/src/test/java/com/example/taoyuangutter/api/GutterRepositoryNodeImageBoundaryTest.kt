package com.example.taoyuangutter.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterRepositoryNodeImageBoundaryTest {
    private val gson = Gson()

    @Test
    fun positiveImageIdRemainsSuccessful() {
        val result = validateNodeImageUploadSuccess(
            gson.fromJson(
                """{"success":true,"message":"上傳成功","data":{"url":"https://example.test/1.jpg","img_id":51863}}""",
                NodeImageUploadResponse::class.java
            )
        )

        assertTrue(result is ApiResult.Success)
        assertEquals(51863, (result as ApiResult.Success).data.data?.imgId)
    }

    @Test
    fun successWithoutImageIdBecomesAnError() {
        val result = validateNodeImageUploadSuccess(
            gson.fromJson(
                """{"success":true,"message":"上傳成功","data":{"url":"https://example.test/1.jpg"}}""",
                NodeImageUploadResponse::class.java
            )
        )

        assertTrue(result is ApiResult.Error)
        assertEquals(NODE_IMAGE_MISSING_ID_MESSAGE, (result as ApiResult.Error).message)
    }

    @Test
    fun nonPositiveImageIdBecomesAnError() {
        val result = validateNodeImageUploadSuccess(
            gson.fromJson(
                """{"success":true,"message":"上傳成功","data":{"url":"https://example.test/1.jpg","img_id":0}}""",
                NodeImageUploadResponse::class.java
            )
        )

        assertTrue(result is ApiResult.Error)
        assertEquals(NODE_IMAGE_MISSING_ID_MESSAGE, (result as ApiResult.Error).message)
    }
}
