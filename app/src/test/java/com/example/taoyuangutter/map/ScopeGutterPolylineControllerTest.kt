package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.GeoFeature
import com.example.taoyuangutter.api.GeoGeometry
import com.example.taoyuangutter.api.GeoProperties
import com.google.android.gms.maps.model.PolylineOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeGutterPolylineControllerTest {
    @Test
    fun replaceFeaturesRemovesPreviousPolylinesAndRetainsLatestResponseOnly() {
        val renderer = FakeRenderer()
        val controller = ScopeGutterPolylineController(renderer = renderer)

        controller.drawFeatures(
            features = listOf(feature("A"), feature("B")),
            savedGroupId = 7
        )
        val oldHandles = renderer.handles.toList()

        controller.replaceFeatures(
            features = listOf(feature("C")),
            savedGroupId = 7
        )

        assertTrue(oldHandles.all { it.removed })
        assertEquals(setOf("C"), controller.entries().map { it.key }.toSet())
        assertFalse((controller.entries().single().value.inner as FakeHandle).removed)
    }

    @Test
    fun replaceFeaturesAppliesHiddenStateToInnerAndPendingDeployOutline() {
        val renderer = FakeRenderer()
        val controller = ScopeGutterPolylineController(renderer = renderer)

        controller.setVisible(false)
        controller.replaceFeatures(
            features = listOf(feature("A", isPendingDeploy = 1)),
            savedGroupId = 7
        )

        val polylineSet = controller.entries().single().value
        assertFalse(polylineSet.inner.isVisible)
        assertFalse(polylineSet.outline?.isVisible ?: true)
    }

    private class FakeRenderer : ScopeGutterPolylineController.ScopePolylineRenderer {
        val handles = mutableListOf<FakeHandle>()

        override fun isReady(): Boolean = true

        override fun addPolyline(options: PolylineOptions): ScopeGutterPolylineController.ScopePolylineHandle {
            return FakeHandle(
                color = options.color,
                isVisible = options.isVisible
            ).also(handles::add)
        }
    }

    private class FakeHandle(
        override var color: Int,
        override var isVisible: Boolean
    ) : ScopeGutterPolylineController.ScopePolylineHandle {
        override var tag: Any? = null
        var removed: Boolean = false

        override fun remove() {
            removed = true
        }
    }

    private fun feature(
        spiNum: String,
        isPendingDeploy: Int = 0
    ): GeoFeature {
        return GeoFeature(
            type = "Feature",
            geometry = GeoGeometry(
                type = "LineString",
                coordinates = listOf(
                    listOf(121.0, 24.9),
                    listOf(121.1, 25.0)
                )
            ),
            properties = GeoProperties(
                spiNum = spiNum,
                groupId = "7",
                spiState = 1,
                isPendingDeploy = isPendingDeploy
            )
        )
    }
}
