package com.example.taoyuangutter.main

class MainMapLocationRecenterReloadTracker {
    private var shouldReloadAfterLocationMove = false

    fun expectReloadAfterLocationMove() {
        shouldReloadAfterLocationMove = true
    }

    fun cancelPendingLocationMove() {
        shouldReloadAfterLocationMove = false
    }

    fun consumeReloadOnLocationUpdated(): Boolean {
        if (!shouldReloadAfterLocationMove) return false
        shouldReloadAfterLocationMove = false
        return true
    }
}
