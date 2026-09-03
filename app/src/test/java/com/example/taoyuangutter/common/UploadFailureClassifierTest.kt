package com.example.taoyuangutter.common

import com.example.taoyuangutter.api.ApiResult
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
}
