package com.example.taoyuangutter.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePolygonSourceTest {
    @Test
    fun fullSelectionUsesSinglePackagedSource() {
        val source = OfflinePolygonSourceCatalog.full(OfflinePolygonLayer.POSSIBLE)

        assertEquals(OfflinePolygonLayer.POSSIBLE.assetName, source.assetName)
        assertEquals(MapOverlayController.DEFAULT_DISTRICTS, source.queryDistricts)
        assertNotNull(source.expectedSha256)
    }

    @Test
    fun districtSelectionUsesDistrictAssetAndFullFallback() {
        val source = OfflinePolygonSourceCatalog.district(OfflinePolygonLayer.LEGACY, "桃園區")

        assertEquals("TOWN_桃園區.gpkg", source.assetName)
        assertEquals(setOf("桃園區"), source.queryDistricts)
        assertEquals("桃園區", source.fallback?.queryDistricts?.single())
        assertTrue(source.fallback?.assetName == OfflinePolygonLayer.LEGACY.assetName)
    }
}
