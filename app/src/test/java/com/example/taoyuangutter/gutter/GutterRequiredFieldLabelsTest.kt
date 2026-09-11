package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Test

class GutterRequiredFieldLabelsTest {
    @Test
    fun labelForMapsRequiredSubmitValidationKeysToChinese() {
        val expectedLabels = mapOf(
            "NODE_TYP" to "側溝形式",
            "XY_NUM" to "測量座標編號",
            "COVER_DEP" to "溝蓋板厚度",
            "NODE_DEP" to "側溝測量深度",
            "NODE_WID" to "側溝頂寬度",
            "MAT_TYP" to "側溝材質",
            "IS_BROKEN" to "溝體結構受損",
            "IS_HANGING" to "附掛或過路管線",
            "IS_SILT" to "淤積程度"
        )

        expectedLabels.forEach { (key, label) ->
            assertEquals(label, GutterRequiredFieldLabels.labelFor(key))
        }
    }

    @Test
    fun labelForKeepsUnknownKeysVisibleForDebugging() {
        assertEquals("UNKNOWN_FIELD", GutterRequiredFieldLabels.labelFor("UNKNOWN_FIELD"))
    }
}
