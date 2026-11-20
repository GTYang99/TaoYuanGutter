package com.example.taoyuangutter.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOverlayControllerStateTest {
    @Test
    fun deletedAreaIsDisabledByDefault() {
        val controller = MapOverlayController(mapProvider = { null })

        assertFalse(controller.currentState().showDeletedArea)
    }

    @Test
    fun allDistrictsAreSelectedByDefault() {
        val controller = MapOverlayController(mapProvider = { null })

        assertEquals(13, controller.currentState().selectedDistricts.size)
    }

    @Test
    fun overlayStateCarriesDistrictSelection() {
        val controller = MapOverlayController(mapProvider = { null })

        controller.applyState(
            MapOverlayController.OverlayState(
                selectedLayer = "EMAP",
                showPlan = true,
                showWaterOld = true,
                showPossible = true,
                showRegion = true,
                showNoDitchPoints = false,
                showDeletedArea = false,
                selectedDistricts = setOf("桃園區", "中壢區"),
            )
        )

        assertEquals(setOf("桃園區", "中壢區"), controller.currentState().selectedDistricts)
        assertTrue(controller.currentState().showPlan)
    }

    @Test
    fun updateOverlayTogglesIncrementsGeneration() {
        val controller = MapOverlayController(mapProvider = { null })
        val initialGeneration = controller.currentState().generation

        controller.updateOverlayToggles(
            showPlan = true,
            showWaterOld = true,
            showPossible = true,
            showRegion = true,
            showNoDitchPoints = false,
            showDeletedArea = false,
            selectedDistricts = setOf("桃園區")
        )

        assertTrue(controller.currentState().generation > initialGeneration)
    }
}
