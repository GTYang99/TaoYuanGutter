package com.example.taoyuangutter.main

import org.junit.Assert.assertEquals
import org.junit.Test

class MainMapLoadIndicatorStateMachineTest {
    @Test
    fun prepareLoadingAndFinishFollowZoomRules() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.prepareForNewOperation(9.5f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(9.5f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(9.5f).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.syncZoom(11f).mode)
    }

    @Test
    fun errorStatePersistsUntilNextOperation() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.failLoading(12f).mode)
        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.syncZoom(13f).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.prepareForNewOperation(13f).mode)
    }

    @Test
    fun loadingStateKeepsPriorityOverZoomUpdates() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(12f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.syncZoom(9.2f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(9.2f).mode)
    }
}
