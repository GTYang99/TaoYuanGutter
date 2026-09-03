package com.example.taoyuangutter.main

import org.junit.Assert.assertEquals
import org.junit.Test

class MainMapLoadIndicatorStateMachineTest {
    @Test
    fun prepareLoadingAndFinishFollowZoomRules() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.prepareForNewOperation(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.syncZoom(18f).mode)
    }

    @Test
    fun errorStatePersistsUntilNextOperation() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.failLoading(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.syncZoom(18f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.prepareForNewOperation(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.prepareForNewOperation(18f).mode)
    }

    @Test
    fun loadingStateKeepsPriorityOverZoomUpdates() {
        val machine = MainMapLoadIndicatorStateMachine()

        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(18f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.syncZoom(17.9f).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(17.9f).mode)
    }
}
