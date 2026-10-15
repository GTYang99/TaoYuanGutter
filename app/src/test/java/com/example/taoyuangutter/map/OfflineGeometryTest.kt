package com.example.taoyuangutter.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

class OfflineGeometryTest {
    @Test
    fun `offline polygon outline gets thicker at broad scale and thinner at close scale`() {
        assertEquals(2.5f, OfflinePolygonStyle.outlineWidthPxAtZoom(15), 0f)
        assertEquals(2.5f, OfflinePolygonStyle.outlineWidthPxAtZoom(16), 0f)
        assertEquals(2f, OfflinePolygonStyle.outlineWidthPxAtZoom(18), 0f)
        assertEquals(1.5f, OfflinePolygonStyle.outlineWidthPxAtZoom(20), 0f)
        assertEquals(1.5f, OfflinePolygonStyle.outlineWidthPxAtZoom(21), 0f)
    }

    @Test
    fun `offline polygon outline profile values are adjustable`() {
        assertEquals(
            3f,
            OfflinePolygonStyle.outlineWidthPxAtZoom(
                zoom = 14,
                broadScaleWidthPx = 4f,
                closeScaleWidthPx = 2f,
                broadScaleZoom = 12,
                closeScaleZoom = 16
            ),
            0f
        )
    }

    @Test
    fun `projection maps central meridian and round trips Taoyuan coordinates`() {
        val centralMeridian = Twd97Projection.longitudeLatitudeToTwd97(121.0, 0.0)
        assertEquals(250000.0, centralMeridian.x, 0.001)
        assertEquals(0.0, centralMeridian.y, 0.001)

        val projected = Twd97Projection.longitudeLatitudeToTwd97(121.3011, 24.9929)
        val (longitude, latitude) = Twd97Projection.twd97ToLongitudeLatitude(projected.x, projected.y)
        assertEquals(121.3011, longitude, 0.00001)
        assertEquals(24.9929, latitude, 0.00001)
    }

    @Test
    fun `administrative GPKG coordinates normalize from GCS TWD97 2020 to EPSG 3826`() {
        val firstControlPoint = Twd97Projection.longitudeLatitudeToTwd97(121.0, 25.0)
        assertEquals(250000.0, firstControlPoint.x, 0.01)
        assertEquals(2765777.564, firstControlPoint.y, 0.01)
        val firstTilePixel = WebMercatorTileMath.tilePixel(firstControlPoint.x, firstControlPoint.y, 54795, 28065, 16)
        assertEquals(96.711111, firstTilePixel.x, 0.01)
        assertEquals(51.128269, firstTilePixel.y, 0.01)

        val projected = Twd97Projection.longitudeLatitudeToTwd97(121.3011, 24.9929)
        assertEquals(280394.869, projected.x, 0.01)
        assertEquals(2765024.899, projected.y, 0.01)
        val tilePixel = WebMercatorTileMath.tilePixel(projected.x, projected.y, 54850, 28066, 16)
        assertEquals(48.988160, tilePixel.x, 0.01)
        assertEquals(160.207802, tilePixel.y, 0.01)

        val source = OfflineGeometry(
            listOf(OfflinePolygon(listOf(listOf(OfflineCoordinate(121.0, 25.0)))))
        )
        val normalized = OfflineGeometryCodec.transformToTwd97(source)
        assertEquals(firstControlPoint, normalized.polygons.single().rings.single().single())
        assertEquals(0xff762e77L, OfflinePolygonColors.REGION_ARGB)
        assertEquals(2.5f, OfflinePolygonStyle.REGION_STROKE_WIDTH_PX, 0f)
    }

    @Test
    fun `GeoPackage geometry header and WKB preserve polygon exterior and hole rings`() {
        val polygon = polygonWkb(
            listOf(
                listOf(OfflineCoordinate(10.0, 20.0), OfflineCoordinate(15.0, 20.0), OfflineCoordinate(15.0, 25.0), OfflineCoordinate(10.0, 20.0)),
                listOf(OfflineCoordinate(11.0, 21.0), OfflineCoordinate(12.0, 21.0), OfflineCoordinate(12.0, 22.0), OfflineCoordinate(11.0, 21.0))
            )
        )
        val blob = ByteBuffer.allocate(8 + polygon.size).order(ByteOrder.LITTLE_ENDIAN)
            .put('G'.code.toByte()).put('P'.code.toByte()).put(0).put(1)
            .putInt(3826).put(polygon).array()

        val decoded = OfflineGeometryCodec.fromGeoPackage(blob)
        assertEquals(1, decoded.polygons.size)
        assertEquals(2, decoded.polygons.single().rings.size)
        assertEquals(10.0, decoded.polygons.single().rings.first().first().x, 0.0)
        assertEquals(11.0, decoded.polygons.single().rings.last().first().x, 0.0)
    }

    @Test
    fun `multi polygon WKB keeps each polygon and its rings`() {
        val polygon = OfflinePolygon(
            listOf(
                listOf(OfflineCoordinate(1.0, 2.0), OfflineCoordinate(3.0, 2.0), OfflineCoordinate(1.0, 2.0)),
                listOf(OfflineCoordinate(1.5, 2.1), OfflineCoordinate(1.7, 2.1), OfflineCoordinate(1.5, 2.1))
            )
        )
        val bytes = OfflineGeometryCodec.fromGeoJsonMultiPolygon(listOf(polygon, polygon))
        val decoded = OfflineGeometryCodec.fromWkb(bytes)
        assertEquals(2, decoded.polygons.size)
        assertEquals(2, decoded.polygons[0].rings.size)
        assertEquals(2, decoded.polygons[1].rings.size)
    }

    @Test
    fun `GeoPackage MultiPolygon decoding keeps all polygon members and holes`() {
        val polygon = OfflinePolygon(
            listOf(
                listOf(OfflineCoordinate(1.0, 2.0), OfflineCoordinate(3.0, 2.0), OfflineCoordinate(1.0, 2.0)),
                listOf(OfflineCoordinate(1.5, 2.1), OfflineCoordinate(1.7, 2.1), OfflineCoordinate(1.5, 2.1))
            )
        )
        val wkb = OfflineGeometryCodec.fromGeoJsonMultiPolygon(listOf(polygon, polygon))
        val geoPackage = ByteBuffer.allocate(8 + wkb.size).order(ByteOrder.LITTLE_ENDIAN)
            .put('G'.code.toByte()).put('P'.code.toByte()).put(0).put(1)
            .putInt(3826).put(wkb).array()

        val decoded = OfflineGeometryCodec.fromGeoPackage(geoPackage)
        assertEquals(2, decoded.polygons.size)
        assertEquals(2, decoded.polygons[0].rings.size)
        assertEquals(2, decoded.polygons[1].rings.size)
        assertEquals(1.5, decoded.polygons[1].rings.last().first().x, 0.0)
    }

    @Test
    fun `tile bounds contain a known Taoyuan coordinate`() {
        val longitude = 121.3011
        val latitude = 24.9929
        val zoom = 16
        val count = 1 shl zoom
        val x = floor((longitude + 180.0) / 360.0 * count).toInt()
        val y = floor((1.0 - ln(tan(Math.toRadians(latitude)) + 1.0 / kotlin.math.cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * count).toInt()
        val coordinate = Twd97Projection.longitudeLatitudeToTwd97(longitude, latitude)
        val bounds = WebMercatorTileMath.bounds3826(x, y, zoom)
        assertTrue(coordinate.x in bounds[0]..bounds[2])
        assertTrue(coordinate.y in bounds[1]..bounds[3])
    }

    @Test
    fun `offline layer fills use the required opaque colors`() {
        assertEquals(0xff156d1dL, OfflinePolygonColors.LEGACY_ARGB)
        assertEquals(0xfffa0000L, OfflinePolygonColors.POSSIBLE_ARGB)
    }

    private fun polygonWkb(rings: List<List<OfflineCoordinate>>): ByteArray {
        val pointCount = rings.sumOf { it.size }
        val buffer = ByteBuffer.allocate(1 + 4 + 4 + rings.size * 4 + pointCount * 16).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(1).putInt(3).putInt(rings.size)
        rings.forEach { ring ->
            buffer.putInt(ring.size)
            ring.forEach { buffer.putDouble(it.x).putDouble(it.y) }
        }
        return buffer.array()
    }
}
