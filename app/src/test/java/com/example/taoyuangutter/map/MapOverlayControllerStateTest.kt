package com.example.taoyuangutter.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOverlayControllerStateTest {
    @Test
    fun deletedAreaIsEnabledByDefault() {
        val controller = MapOverlayController(mapProvider = { null })

        assertTrue(controller.currentState().showDeletedArea)
    }

    @Test
    fun deletedAreaStateCanBeTurnedOffWithoutChangingOtherState() {
        val controller = MapOverlayController(mapProvider = { null })

        controller.updateOverlayToggles(
            showPlan = true,
            showWaterOld = true,
            showPossible = true,
            showRegion = true,
            showNoDitchPoints = false,
            showDeletedArea = false
        )

        assertFalse(controller.currentState().showDeletedArea)
        assertTrue(controller.currentState().showPlan)
    }
}
