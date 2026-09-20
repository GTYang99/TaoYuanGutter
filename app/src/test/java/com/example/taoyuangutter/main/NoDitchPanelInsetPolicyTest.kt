package com.example.taoyuangutter.main

import org.junit.Assert.assertEquals
import org.junit.Test

class NoDitchPanelInsetPolicyTest {

    @Test
    fun imeHeightDoesNotBecomeAnAdditionalPanelBottomMargin() {
        assertEquals(
            112,
            NoDitchPanelInsetPolicy.bottomMargin(
                baseBottomMarginPx = 12,
                systemBarsBottomPx = 100
            )
        )
    }

    @Test
    fun marginIsCalculatedFromBaseOnEveryInsetUpdate() {
        val base = 12

        assertEquals(112, NoDitchPanelInsetPolicy.bottomMargin(base, 100))
        assertEquals(12, NoDitchPanelInsetPolicy.bottomMargin(base, 0))
    }

    @Test
    fun negativeInsetsCannotMovePanelBeyondItsBaseMargin() {
        assertEquals(12, NoDitchPanelInsetPolicy.bottomMargin(12, -80))
    }
}
