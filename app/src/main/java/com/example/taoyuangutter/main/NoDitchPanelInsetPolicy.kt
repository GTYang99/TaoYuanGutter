package com.example.taoyuangutter.main

internal object NoDitchPanelInsetPolicy {
    fun bottomMargin(baseBottomMarginPx: Int, systemBarsBottomPx: Int): Int =
        baseBottomMarginPx.coerceAtLeast(0) + systemBarsBottomPx.coerceAtLeast(0)
}
