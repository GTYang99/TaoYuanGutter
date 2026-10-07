package com.example.taoyuangutter.map

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

internal data class OfflineCoordinate(val x: Double, val y: Double)
internal data class OfflinePolygon(val rings: List<List<OfflineCoordinate>>)
internal data class OfflineGeometry(val polygons: List<OfflinePolygon>)

internal object OfflinePolygonColors {
    const val LEGACY_ARGB: Long = 0xff156d1dL
    const val POSSIBLE_ARGB: Long = 0xfffa0000L
}

internal object OfflineGeometryCodec {
    fun fromGeoJsonMultiPolygon(coordinates: List<OfflinePolygon>): ByteArray {
        val size = 1 + 4 + 4 + coordinates.sumOf { polygon ->
            1 + 4 + 4 + polygon.rings.sumOf { ring -> 4 + ring.size * 16 }
        }
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(1)
        buffer.putInt(6)
        buffer.putInt(coordinates.size)
        coordinates.forEach { polygon ->
            buffer.put(1)
            buffer.putInt(3)
            buffer.putInt(polygon.rings.size)
            polygon.rings.forEach { ring ->
                buffer.putInt(ring.size)
                ring.forEach { coordinate ->
                    buffer.putDouble(coordinate.x)
                    buffer.putDouble(coordinate.y)
                }
            }
        }
        return buffer.array()
    }

    fun fromGeoPackage(blob: ByteArray): OfflineGeometry {
        require(blob.size >= 8 && blob[0] == 'G'.code.toByte() && blob[1] == 'P'.code.toByte()) {
            "Invalid GeoPackage geometry header"
        }
        val flags = blob[3].toInt() and 0xff
        val envelopeCode = (flags shr 1) and 0x07
        val envelopeDoubles = when (envelopeCode) {
            0 -> 0
            1 -> 4
            2, 3 -> 6
            4 -> 8
            else -> throw IllegalArgumentException("Unsupported GeoPackage envelope code $envelopeCode")
        }
        val byteOrder = if ((flags and 1) == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val wkbOffset = 8 + envelopeDoubles * 8
        require(blob.size > wkbOffset) { "GeoPackage geometry has no WKB payload" }
        val buffer = ByteBuffer.wrap(blob, wkbOffset, blob.size - wkbOffset).slice()
        return readGeometry(buffer)
    }

    fun fromWkb(blob: ByteArray): OfflineGeometry = readGeometry(ByteBuffer.wrap(blob))

    private fun readGeometry(buffer: ByteBuffer): OfflineGeometry {
        val orderFlag = buffer.get().toInt() and 0xff
        require(orderFlag == 0 || orderFlag == 1) { "Invalid WKB byte order" }
        buffer.order(if (orderFlag == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val rawType = buffer.int
        val baseType = when {
            rawType >= 3000 -> rawType - 3000
            rawType >= 2000 -> rawType - 2000
            rawType >= 1000 -> rawType - 1000
            else -> rawType and 0x0fffffff
        }
        val hasZ = rawType >= 1000 && rawType < 4000 || (rawType and 0x80000000.toInt()) != 0
        val hasM = rawType in 2000..3999 || (rawType and 0x40000000) != 0
        val hasSrid = (rawType and 0x20000000) != 0
        if (hasSrid) buffer.int
        val extraDimensions = (if (hasZ) 1 else 0) + (if (hasM) 1 else 0)

        return when (baseType) {
            3 -> OfflineGeometry(listOf(readPolygonBody(buffer, extraDimensions)))
            6 -> {
                val count = checkedCount(buffer.int)
                val polygons = ArrayList<OfflinePolygon>(count)
                repeat(count) {
                    val nestedOrder = buffer.get().toInt() and 0xff
                    require(nestedOrder == 0 || nestedOrder == 1) { "Invalid nested WKB byte order" }
                    buffer.order(if (nestedOrder == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                    val nestedType = buffer.int
                    require((nestedType and 0x0fffffff) == 3) { "MultiPolygon contains a non-Polygon geometry" }
                    polygons += readPolygonBody(buffer, extraDimensions)
                }
                OfflineGeometry(polygons)
            }
            else -> throw IllegalArgumentException("Unsupported WKB geometry type $baseType")
        }
    }

    private fun readPolygonBody(buffer: ByteBuffer, extraDimensions: Int): OfflinePolygon {
        val ringCount = checkedCount(buffer.int)
        val rings = ArrayList<List<OfflineCoordinate>>(ringCount)
        repeat(ringCount) {
            val pointCount = checkedCount(buffer.int)
            val ring = ArrayList<OfflineCoordinate>(pointCount)
            repeat(pointCount) {
                ring += OfflineCoordinate(buffer.double, buffer.double)
                repeat(extraDimensions) { buffer.double }
            }
            rings += ring
        }
        return OfflinePolygon(rings)
    }

    private fun checkedCount(value: Int): Int {
        require(value in 0..10_000_000) { "Invalid WKB element count $value" }
        return value
    }
}

internal object Twd97Projection {
    private const val A = 6378137.0
    private const val INV_F = 298.257222101
    private const val K0 = 0.9999
    private const val CENTRAL_MERIDIAN = 121.0
    private const val FALSE_EASTING = 250000.0
    private val flattening = 1.0 / INV_F
    private val eccentricitySquared = 2.0 * flattening - flattening * flattening
    private val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)

    fun longitudeLatitudeToTwd97(longitude: Double, latitude: Double): OfflineCoordinate {
        val lat = Math.toRadians(latitude)
        val lon = Math.toRadians(longitude)
        val centralMeridian = Math.toRadians(CENTRAL_MERIDIAN)
        val sinLat = sin(lat)
        val cosLat = cos(lat)
        val tanLat = tan(lat)
        val n = A / sqrt(1.0 - eccentricitySquared * sinLat * sinLat)
        val t = tanLat * tanLat
        val c = secondEccentricitySquared * cosLat * cosLat
        val a = cosLat * (lon - centralMeridian)
        val e4 = eccentricitySquared * eccentricitySquared
        val e6 = e4 * eccentricitySquared
        val meridianArc = A * ((1.0 - eccentricitySquared / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * lat -
            (3.0 * eccentricitySquared / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * sin(2.0 * lat) +
            (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * sin(4.0 * lat) -
            (35.0 * e6 / 3072.0) * sin(6.0 * lat))
        val a2 = a * a
        val easting = FALSE_EASTING + K0 * n * (a + (1.0 - t + c) * a2 * a / 6.0 +
            (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * secondEccentricitySquared) * a2 * a2 * a / 120.0)
        val northing = K0 * (meridianArc + n * tanLat * (a2 / 2.0 +
            (5.0 - t + 9.0 * c + 4.0 * c * c) * a2 * a2 / 24.0 +
            (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * secondEccentricitySquared) * a2 * a2 * a2 / 720.0))
        return OfflineCoordinate(easting, northing)
    }

    fun twd97ToLongitudeLatitude(easting: Double, northing: Double): Pair<Double, Double> {
        val e1 = (1.0 - sqrt(1.0 - eccentricitySquared)) / (1.0 + sqrt(1.0 - eccentricitySquared))
        val e2 = eccentricitySquared
        val e4 = e2 * e2
        val e6 = e4 * e2
        val m = northing / K0
        val mu = m / (A * (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0))
        val e1_2 = e1 * e1
        val e1_3 = e1_2 * e1
        val e1_4 = e1_3 * e1
        val phi1 = mu + (3.0 * e1 / 2.0 - 27.0 * e1_3 / 32.0) * sin(2.0 * mu) +
            (21.0 * e1_2 / 16.0 - 55.0 * e1_4 / 32.0) * sin(4.0 * mu) +
            (151.0 * e1_3 / 96.0) * sin(6.0 * mu) + (1097.0 * e1_4 / 512.0) * sin(8.0 * mu)
        val sinPhi = sin(phi1)
        val cosPhi = cos(phi1)
        val tanPhi = tan(phi1)
        val n1 = A / sqrt(1.0 - e2 * sinPhi * sinPhi)
        val r1 = A * (1.0 - e2) / (1.0 - e2 * sinPhi * sinPhi).let { it * sqrt(it) }
        val t1 = tanPhi * tanPhi
        val c1 = secondEccentricitySquared * cosPhi * cosPhi
        val d = (easting - FALSE_EASTING) / (n1 * K0)
        val d2 = d * d
        val d3 = d2 * d
        val d4 = d2 * d2
        val d5 = d4 * d
        val d6 = d3 * d3
        val latitude = phi1 - n1 * tanPhi / r1 * (d2 / 2.0 -
            (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * secondEccentricitySquared) * d4 / 24.0 +
            (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * secondEccentricitySquared - 3.0 * c1 * c1) * d6 / 720.0)
        val longitude = Math.toRadians(CENTRAL_MERIDIAN) + (d - (1.0 + 2.0 * t1 + c1) * d3 / 6.0 +
            (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * secondEccentricitySquared + 24.0 * t1 * t1) * d5 / 120.0) / cosPhi
        return Math.toDegrees(longitude) to Math.toDegrees(latitude)
    }
}

internal object WebMercatorTileMath {
    const val TILE_SIZE = 256
    private const val HALF_WORLD = 20037508.342789244
    private const val MAX_LATITUDE = 85.05112878

    fun bounds3826(x: Int, y: Int, zoom: Int): DoubleArray {
        require(zoom in 0..30)
        val tileCount = 1L shl zoom
        require(x.toLong() in 0 until tileCount && y.toLong() in 0 until tileCount)
        val corners = listOf(
            longitudeLatitudeAtTileCorner(x, y, tileCount),
            longitudeLatitudeAtTileCorner(x + 1, y, tileCount),
            longitudeLatitudeAtTileCorner(x, y + 1, tileCount),
            longitudeLatitudeAtTileCorner(x + 1, y + 1, tileCount)
        ).map { Twd97Projection.longitudeLatitudeToTwd97(it.first, it.second) }
        return doubleArrayOf(
            corners.minOf { it.x }, corners.minOf { it.y },
            corners.maxOf { it.x }, corners.maxOf { it.y }
        )
    }

    fun tilePixel(easting: Double, northing: Double, x: Int, y: Int, zoom: Int): OfflineCoordinate {
        val (longitude, latitude) = Twd97Projection.twd97ToLongitudeLatitude(easting, northing)
        val clampedLatitude = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val worldX = A_MERCATOR * Math.toRadians(longitude)
        val worldY = A_MERCATOR * kotlin.math.ln(kotlin.math.tan(PI / 4.0 + Math.toRadians(clampedLatitude) / 2.0))
        val scale = TILE_SIZE.toDouble() * (1L shl zoom).toDouble()
        return OfflineCoordinate(
            ((worldX + HALF_WORLD) / (2.0 * HALF_WORLD) * scale) - x * TILE_SIZE,
            ((HALF_WORLD - worldY) / (2.0 * HALF_WORLD) * scale) - y * TILE_SIZE
        )
    }

    private fun longitudeLatitudeAtTileCorner(x: Int, y: Int, tileCount: Long): Pair<Double, Double> {
        val longitude = x.toDouble() / tileCount * 360.0 - 180.0
        val mercatorY = PI * (1.0 - 2.0 * y.toDouble() / tileCount)
        val latitude = Math.toDegrees(atan(sinh(mercatorY))).coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        return longitude to latitude
    }

    private const val A_MERCATOR = 6378137.0
}
