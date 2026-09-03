package com.example.taoyuangutter.main

import org.junit.Assert.assertEquals
import org.junit.Test

class MainMapLoadIndicatorStateMachineTest {
    @Test
    fun prepareLoadingAndFinishFollowZoomRules() {
        val machine = MainMapLoadIndicatorStateMachine()
        val belowMinZoom = MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM - 0.1f

        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.prepareForNewOperation(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.syncZoom(MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM).mode)
    }

    @Test
    fun errorStatePersistsUntilNextOperation() {
        val machine = MainMapLoadIndicatorStateMachine()
        val belowMinZoom = MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM - 0.1f

        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.failLoading(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.ERROR, machine.syncZoom(MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.prepareForNewOperation(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.HIDDEN, machine.prepareForNewOperation(MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM).mode)
    }

    @Test
    fun loadingStateKeepsPriorityOverZoomUpdates() {
        val machine = MainMapLoadIndicatorStateMachine()
        val belowMinZoom = MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM - 0.1f

        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.beginLoading(MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM).mode)
        assertEquals(MainMapLoadIndicatorMode.LOADING, machine.syncZoom(belowMinZoom).mode)
        assertEquals(MainMapLoadIndicatorMode.LOW_ZOOM, machine.finishLoading(belowMinZoom).mode)
    }
}
