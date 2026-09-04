package com.example.taoyuangutter.common

import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.gutter.PhotoUploadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadFailureClassifierTest {
    @Test
    fun failedToConnectIsNetworkFailureButNotTimeout() {
        val message = "Failed to connect to demo.srgeo.com.tw"

        assertTrue(UploadFailureClassifier.isNetworkFailureMessage(message))
        assertFalse(UploadFailureClassifier.isTimeoutFailureMessage(message))
        assertEquals(
            "網路連線失敗",
            UploadFailureClassifier.forStoreDitchNetworkFailure(
                ApiResult.Error(message = message, code = null)
            ).categoryLabel
        )
    }

    @Test
    fun timeoutMessageIsClassifiedAsTimeout() {
        val message = "timeout"

        assertTrue(UploadFailureClassifier.isNetworkFailureMessage(message))
        assertTrue(UploadFailureClassifier.isTimeoutFailureMessage(message))
        assertEquals(
            "網路連線逾時",
            UploadFailureClassifier.forStoreDitchNetworkTimeout(
                ApiResult.Error(message = message, code = null)
            ).categoryLabel
        )
    }

    @Test
    fun storeDitch401UsesAuthReferenceCode() {
        val result = UploadFailureClassifier.forStoreDitchError(
            ApiResult.Error(message = "Unauthorized", code = 401)
        )

        assertEquals("STORE_DITCH_AUTH_FAILED", result.referenceCode)
        assertTrue(result.userMessage.contains("請重新登入"))
    }

    @Test
    fun photoApi401UsesAuthReferenceCode() {
        val result = UploadFailureClassifier.forPhotoApiError(
            ApiResult.Error(message = "Unauthorized", code = 401)
        )

        assertEquals("PHOTO_AUTH_FAILED", result.referenceCode)
        assertTrue(result.userMessage.contains("請重新登入"))
    }

    @Test
    fun photoBatch401UsesAuthReferenceCode() {
        val result = UploadFailureClassifier.forPhotoBatchFailures(
            listOf(
                PhotoUploadManager.PhotoUploadFailure(
                    nodeId = 12,
                    fileCategory = 1,
                    attempt = 1,
                    message = "Unauthorized",
                    code = 401
                )
            )
        )

        assertEquals("PHOTO_AUTH_FAILED", result.referenceCode)
        assertTrue(result.userMessage.contains("請重新登入"))
    }
}
