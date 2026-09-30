package com.example.taoyuangutter.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoUploadSlotStateTest {
    @Test
    fun uploadResponseRequiresPositiveImageId() {
        listOf(null, 0, -1).forEach { imgId ->
            val outcome = PhotoUploadSlotState.outcomeForUploadResponse(imgId)

            assertEquals(PhotoUploadSlotState.STATE_FAILED, outcome.state)
            assertNull(outcome.imgId)
            assertEquals(PhotoUploadSlotState.ERROR_MISSING_IMG_ID, outcome.error)
        }
    }

    @Test
    fun positiveImageIdIsSuccessfulUploadOutcome() {
        val outcome = PhotoUploadSlotState.outcomeForUploadResponse(51863)

        assertEquals(PhotoUploadSlotState.STATE_SUCCESS, outcome.state)
        assertEquals(51863, outcome.imgId)
        assertNull(outcome.error)
    }

    @Test
    fun storedImageIdParserRejectsNonPositiveValues() {
        assertNull(PhotoUploadSlotState.readImgId(mapOf("photo1ImgId" to "0"), 1))
        assertNull(PhotoUploadSlotState.readImgId(mapOf("photo1ImgId" to "-3"), 1))
        assertEquals(21, PhotoUploadSlotState.readImgId(mapOf("photo1ImgId" to "21"), 1))
    }
}
