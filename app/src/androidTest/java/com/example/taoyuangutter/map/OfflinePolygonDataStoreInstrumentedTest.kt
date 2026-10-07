package com.example.taoyuangutter.map

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.graphics.BitmapFactory
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.example.taoyuangutter.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.gms.maps.model.TileProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

class OfflinePolygonDataStoreInstrumentedTest {
    @Test
    fun packagedSourcesReturnLocalPolygonGeometryForTaoyuanExtent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bounds = doubleArrayOf(274000.0, 2755000.0, 276000.0, 2760000.0)
        assertEquals(2f, OfflinePolygonStyle.OFFLINE_POLYGON_STROKE_WIDTH_PX, 0f)

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

            val interiorPoint = geometries.asSequence()
                .map { blob ->
                    if (layer == OfflinePolygonLayer.LEGACY) OfflineGeometryCodec.fromGeoPackage(blob)
                    else OfflineGeometryCodec.fromWkb(blob)
                }
                .mapNotNull(::findInteriorPoint)
                .firstOrNull()
                ?: throw AssertionError("${layer.name} query results should contain a polygon interior point")
            val center = Twd97Projection.twd97ToLongitudeLatitude(
                interiorPoint.x,
                interiorPoint.y
            )
            listOf(16, 20).forEach { zoom ->
                val tile = tileCoordinate(center.first, center.second, zoom)
                val provider = OfflinePolygonTileProvider(context, layer)
                val alternateWidthProvider = OfflinePolygonTileProvider(context, layer, outlineWidthPx = 5f)
                try {
                    val rendered = provider.getTile(tile.first, tile.second, tile.third)
                        ?: throw AssertionError("${layer.name} provider returned null instead of TileProvider.NO_TILE or a tile")
                    assertNotSame("${layer.name} should render a populated offline tile", TileProvider.NO_TILE, rendered)
                    val png = rendered.data ?: throw AssertionError("${layer.name} tile has no PNG data")
                    val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
                    assertNotNull("${layer.name} tile should contain a decodable PNG", bitmap)
                    val alternateTile = alternateWidthProvider.getTile(tile.first, tile.second, tile.third)
                        ?: throw AssertionError("${layer.name} alternate-width provider returned null")
                    assertNotSame(TileProvider.NO_TILE, alternateTile)
                    val alternatePng = alternateTile.data
                        ?: throw AssertionError("${layer.name} alternate-width tile has no PNG data")
                    val alternateBitmap = BitmapFactory.decodeByteArray(alternatePng, 0, alternatePng.size)
                    assertNotNull("${layer.name} alternate-width tile should contain a decodable PNG", alternateBitmap)
                    bitmap!!.let { image ->
                        val pixels = IntArray(image.width * image.height)
                        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                        assertTrue(
                            "${layer.name} tile should contain its opaque fill and outline color",
                            pixels.any { it == layer.color }
                        )
                        val sample = WebMercatorTileMath.tilePixel(
                            interiorPoint.x,
                            interiorPoint.y,
                            tile.first,
                            tile.second,
                            tile.third
                        )
                        val sampleX = sample.x.toInt().coerceIn(0, image.width - 1)
                        val sampleY = sample.y.toInt().coerceIn(0, image.height - 1)
                        assertEquals("${layer.name} interior fill must remain unchanged", layer.color, image.getPixel(sampleX, sampleY))
                        val alternatePixels = IntArray(alternateBitmap!!.width * alternateBitmap.height)
                        alternateBitmap.getPixels(alternatePixels, 0, alternateBitmap.width, 0, 0, alternateBitmap.width, alternateBitmap.height)
                        assertTrue("${layer.name} fill must remain intact with an alternate outline width", alternatePixels.any { it == layer.color })
                        assertEquals("${layer.name} alternate width must not affect the interior fill", layer.color, alternateBitmap.getPixel(sampleX, sampleY))
                        assertFalse("${layer.name} alternate outline width should affect rendered edge pixels", pixels.contentEquals(alternatePixels))
                        image.recycle()
                        alternateBitmap.recycle()
                    }
                } finally {
                    provider.release()
                    alternateWidthProvider.release()
                }
            }
        }
    }

    @Test
    fun releasedProviderRejectsFurtherTileRequests() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tile = tileCoordinate(121.3011, 24.9929, zoom = 16)
        val provider = OfflinePolygonTileProvider(context, OfflinePolygonLayer.LEGACY)

        try {
            assertNotSame(TileProvider.NO_TILE, provider.getTile(tile.first, tile.second, tile.third))
        } finally {
            provider.release()
        }
        assertSame(TileProvider.NO_TILE, provider.getTile(tile.first, tile.second, tile.third))
    }

    @Test
    fun layerSheetInflatesExactOfflineSourceLabels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_TaoYuanGutter)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.sheet_layers, null, false)

        assertEquals("水務局舊資料（.gpkg）", view.findViewById<MaterialCheckBox>(R.id.cbWaterOld).text.toString())
        assertEquals("可能側溝位置（.geojson）", view.findViewById<MaterialCheckBox>(R.id.cbPossible).text.toString())
    }

    private fun tileCoordinate(longitude: Double, latitude: Double, zoom: Int): Triple<Int, Int, Int> {
        val tileCount = 1 shl zoom
        val x = floor((longitude + 180.0) / 360.0 * tileCount).toInt()
        val y = floor(
            (1.0 - ln(tan(Math.toRadians(latitude)) + 1.0 / kotlin.math.cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * tileCount
        ).toInt()
        return Triple(x, y, zoom)
    }

    private fun findInteriorPoint(geometry: OfflineGeometry): OfflineCoordinate? {
        var bestPoint: OfflineCoordinate? = null
        var bestClearance = 0.0
        geometry.polygons.forEach { polygon ->
            val shell = polygon.rings.firstOrNull()?.takeIf { it.size >= 4 } ?: return@forEach
            val minX = shell.minOf { it.x }
            val maxX = shell.maxOf { it.x }
            val minY = shell.minOf { it.y }
            val maxY = shell.maxOf { it.y }
            for (xIndex in 0 until 32) {
                val x = minX + (maxX - minX) * (xIndex + 0.5) / 32.0
                for (yIndex in 0 until 32) {
                    val y = minY + (maxY - minY) * (yIndex + 0.5) / 32.0
                    val point = OfflineCoordinate(x, y)
                    if (!pointInRing(point, shell) || polygon.rings.drop(1).any { pointInRing(point, it) }) continue
                    val clearance = polygon.rings.minOf { ring -> distanceToRing(point, ring) }
                    if (clearance > bestClearance) {
                        bestClearance = clearance
                        bestPoint = point
                    }
                }
            }
        }
        return bestPoint
    }

    private fun pointInRing(point: OfflineCoordinate, ring: List<OfflineCoordinate>): Boolean {
        var inside = false
        var previous = ring.lastIndex
        for (current in ring.indices) {
            val a = ring[current]
            val b = ring[previous]
            if ((a.y > point.y) != (b.y > point.y) &&
                point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
            ) inside = !inside
            previous = current
        }
        return inside
    }

    private fun distanceToRing(point: OfflineCoordinate, ring: List<OfflineCoordinate>): Double {
        var minimum = Double.POSITIVE_INFINITY
        for (index in ring.indices) {
            val start = ring[index]
            val end = ring[(index + 1) % ring.size]
            val dx = end.x - start.x
            val dy = end.y - start.y
            val lengthSquared = dx * dx + dy * dy
            val ratio = if (lengthSquared == 0.0) 0.0 else
                (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val closestX = start.x + ratio * dx
            val closestY = start.y + ratio * dy
            val distanceX = point.x - closestX
            val distanceY = point.y - closestY
            minimum = minOf(minimum, kotlin.math.sqrt(distanceX * distanceX + distanceY * distanceY))
        }
        return minimum
    }
}
