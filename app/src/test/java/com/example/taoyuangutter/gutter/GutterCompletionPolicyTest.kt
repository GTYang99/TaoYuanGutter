package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterCompletionPolicyTest {
    @Test
    fun connectPointUsesCantOpenDetailExemption() {
        val data = mapOf("IS_TIEINPOINT" to "1")

        assertTrue(GutterCompletionPolicy.isDetailExempt(data))
        assertEquals(listOf(1), GutterCompletionPolicy.requiredPhotoSlots(false, true))
    }

    @Test
    fun normalPointRequiresAllThreePhotos() {
        assertEquals(
            listOf(1, 2, 3),
            GutterCompletionPolicy.requiredPhotoSlots(false, false)
        )
    }

    @Test
    fun virtualPointRequiresOnlyIdentityFieldsAndNoPhotos() {
        assertEquals(
            listOf("NODE_X", "NODE_Y", "XY_NUM"),
            GutterCompletionPolicy.requiredBasicKeys(isVirtual = true, requiresMeasureId = true)
        )
        assertTrue(GutterCompletionPolicy.requiredPhotoSlots(true, false).isEmpty())
    }

    @Test
    fun backendGeneratedMeasureIdIsNotRequiredWhenTheFlowDisablesManualEntry() {
        val requiredKeys = GutterCompletionPolicy.requiredBasicKeys(
            isVirtual = false,
            requiresMeasureId = false
        )

        assertFalse(requiredKeys.contains("XY_NUM"))
    }

    @Test
    fun requiredValuesRejectPartialData() {
        val data = mapOf("NODE_TYP" to "1", "NODE_X" to "25.0")

        assertFalse(
            GutterCompletionPolicy.hasRequiredValues(
                data,
                GutterCompletionPolicy.requiredBasicKeys(isVirtual = false, requiresMeasureId = false)
            )
        )
    }

    @Test
    fun completeNormalPointRequiresEveryFieldAndPhoto() {
        val data = mutableMapOf(
            "NODE_TYP" to "1",
            "NODE_X" to "25.0",
            "NODE_Y" to "121.0"
        )
        GutterCompletionPolicy.detailRequiredKeys.forEach { data[it] = "1" }

        assertFalse(
            GutterCompletionPolicy.isComplete(
                data = data,
                isVirtual = false,
                requiresMeasureId = false,
                hasCoordinates = true,
                photoUsable = { it != 3 }
            )
        )
        assertTrue(
            GutterCompletionPolicy.isComplete(
                data = data,
                isVirtual = false,
                requiresMeasureId = false,
                hasCoordinates = true,
                photoUsable = { true }
            )
        )
    }
}
