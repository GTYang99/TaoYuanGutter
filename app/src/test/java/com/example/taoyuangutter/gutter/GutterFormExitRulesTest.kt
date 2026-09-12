package com.example.taoyuangutter.gutter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterFormExitRulesTest {
    @Test
    fun cantOpenConfirmationIsSkippedWhenNoClearableContentExists() {
        assertFalse(
            GutterFormExitRules.shouldConfirmCantOpenClear(
                coverThickness = "", depth = "", topWidth = "",
                materialSelected = false, brokenSelected = false,
                hangingSelected = false, siltSelected = false,
                hasPhoto2 = false, hasPhoto3 = false
            )
        )
    }

    @Test
    fun cantOpenConfirmationAppearsForEveryClearableContentType() {
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("1", "", "", false, false, false, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "1", "", false, false, false, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "1", false, false, false, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", true, false, false, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", false, true, false, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", false, false, true, false, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", false, false, false, true, false, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", false, false, false, false, true, false))
        assertTrue(GutterFormExitRules.shouldConfirmCantOpenClear("", "", "", false, false, false, false, false, true))
    }

    @Test
    fun incompleteExitWarningFollowsExistingBasicAndPhotoRules() {
        assertTrue(GutterFormExitRules.shouldShowIncompleteExitWarning(true, false, false))
        assertTrue(GutterFormExitRules.shouldShowIncompleteExitWarning(false, false, true))
        assertFalse(GutterFormExitRules.shouldShowIncompleteExitWarning(false, false, false))
        assertFalse(GutterFormExitRules.shouldShowIncompleteExitWarning(false, true, true))
    }
}
