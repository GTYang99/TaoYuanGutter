package com.example.taoyuangutter.map

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

/** Builds GeoServer WMS requests for Google Maps Web Mercator tiles in EPSG:3826. */
object Wms3826RequestBuilder {
    const val DEFAULT_BASE_URL = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms"
    private const val TILE_SIZE = 256
    private const val HALF_WORLD_METERS = 20037508.342789244
    private const val A = 6378137.0
    private const val INV_F = 298.257222101
    private const val K0 = 0.9999
    private const val CENTRAL_MERIDIAN_DEGREES = 121.0
    private const val FALSE_EASTING = 250000.0

    fun buildTileUrl(
        x: Int,
        y: Int,
        zoom: Int,
        baseUrl: String = DEFAULT_BASE_URL
    ): String {
        require(zoom in 0..30) { "zoom must be between 0 and 30" }
        val bounds = tileBbox3826(x, y, zoom)
        val params = linkedMapOf(
            "SERVICE" to "WMS",
            "REQUEST" to "GetMap",
            "VERSION" to "1.1.0",
            "LAYERS" to "deleted_area",
            "STYLES" to "TY_RSGDBIP_0910刪除資料",
            "SRS" to "EPSG:3826",
            "BBOX" to bounds.joinToString(",") { format(it) },
            "FORMAT" to "image/png8",
            "WIDTH" to TILE_SIZE.toString(),
            "HEIGHT" to TILE_SIZE.toString(),
            "TRANSPARENT" to "true"
        )
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "${baseUrl.trimEnd('?','&')}?$query"
    }

    fun tileBbox3826(x: Int, y: Int, zoom: Int): DoubleArray {
        require(x >= 0 && x < (1 shl zoom)) { "x outside tile range" }
        require(y >= 0 && y < (1 shl zoom)) { "y outside tile range" }
        val scale = 1 shl zoom
        val corners = listOf(
            webMercatorTileCorner(x, y, scale),
            webMercatorTileCorner(x + 1, y, scale),
            webMercatorTileCorner(x, y + 1, scale),
            webMercatorTileCorner(x + 1, y + 1, scale)
        )
        val projected = corners.map { twd97Tm2Zone121(it.first, it.second) }
        return doubleArrayOf(
            projected.minOf { it.first },
            projected.minOf { it.second },
            projected.maxOf { it.first },
            projected.maxOf { it.second }
        )
    }

    private fun webMercatorTileCorner(tileX: Int, tileY: Int, scale: Int): Pair<Double, Double> {
        val lon = tileX.toDouble() / scale * 360.0 - 180.0
        val normalizedY = tileY.toDouble() / scale
        val mercatorY = Math.PI * (1.0 - 2.0 * normalizedY)
        val lat = Math.toDegrees(atan(sinh(mercatorY))).coerceIn(-85.05112878, 85.05112878)
        return lon to lat
    }

    private fun twd97Tm2Zone121(longitudeDegrees: Double, latitudeDegrees: Double): Pair<Double, Double> {
        val flattening = 1.0 / INV_F
        val eccentricitySquared = 2.0 * flattening - flattening * flattening
        val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)
        val latitude = Math.toRadians(latitudeDegrees)
        val longitude = Math.toRadians(longitudeDegrees)
        val centralMeridian = Math.toRadians(CENTRAL_MERIDIAN_DEGREES)
        val sinLat = sin(latitude)
        val cosLat = cos(latitude)
        val tanLat = tan(latitude)
        val n = A / sqrt(1.0 - eccentricitySquared * sinLat * sinLat)
        val t = tanLat * tanLat
        val c = secondEccentricitySquared * cosLat * cosLat
        val a = cosLat * (longitude - centralMeridian)
        val e4 = eccentricitySquared * eccentricitySquared
        val e6 = e4 * eccentricitySquared
        val meridianArc = A * ((1.0 - eccentricitySquared / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * latitude -
            (3.0 * eccentricitySquared / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * sin(2.0 * latitude) +
            (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * sin(4.0 * latitude) -
            (35.0 * e6 / 3072.0) * sin(6.0 * latitude))
        val a2 = a * a
        val easting = FALSE_EASTING + K0 * n * (a + (1.0 - t + c) * a2 * a / 6.0 +
            (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * secondEccentricitySquared) * a2 * a2 * a / 120.0)
        val northing = K0 * (meridianArc + n * tanLat * (a2 / 2.0 +
            (5.0 - t + 9.0 * c + 4.0 * c * c) * a2 * a2 / 24.0 +
            (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * secondEccentricitySquared) * a2 * a2 * a2 / 720.0))
        return easting to northing
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun format(value: Double): String = String.format(Locale.US, "%.3f", value)
}
