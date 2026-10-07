package com.example.taoyuangutter.map

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.google.android.gms.maps.model.TileProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

class OfflinePolygonDataStoreInstrumentedTest {
    @Test
    fun packagedSourcesReturnLocalPolygonGeometryForTaoyuanExtent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bounds = doubleArrayOf(274000.0, 2755000.0, 276000.0, 2760000.0)

        listOf(OfflinePolygonLayer.LEGACY, OfflinePolygonLayer.POSSIBLE).forEach { layer ->
            val source = OfflinePolygonDataStore.load(context, layer).get()
            val geometries = source.query(bounds)
            assertFalse("${layer.name} should have packaged features in the selected extent", geometries.isEmpty())
            geometries.forEach { blob ->
                val geometry = if (layer == OfflinePolygonLayer.LEGACY) {
                    OfflineGeometryCodec.fromGeoPackage(blob)
                } else {
                    OfflineGeometryCodec.fromWkb(blob)
                }
                assertFalse("${layer.name} geometry should contain polygon rings", geometry.polygons.isEmpty())
                assertFalse("${layer.name} geometry should contain a ring", geometry.polygons.first().rings.isEmpty())
            }
        }
    }

    @Test
    fun releasedProviderRejectsFurtherTileRequests() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val zoom = 16
        val tileCount = 1 shl zoom
        val longitude = 121.3011
        val latitude = 24.9929
        val x = floor((longitude + 180.0) / 360.0 * tileCount).toInt()
        val y = floor(
            (1.0 - ln(tan(Math.toRadians(latitude)) + 1.0 / kotlin.math.cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * tileCount
        ).toInt()
        val provider = OfflinePolygonTileProvider(context, OfflinePolygonLayer.LEGACY)

        try {
            assertNotSame(TileProvider.NO_TILE, provider.getTile(x, y, zoom))
        } finally {
            provider.release()
        }
        assertSame(TileProvider.NO_TILE, provider.getTile(x, y, zoom))
    }
}
