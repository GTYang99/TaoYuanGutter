package com.example.taoyuangutter.map

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Builds GeoServer WMS GetMap requests for Google Maps Web Mercator tiles. */
object Wms3857RequestBuilder {
    fun buildTileUrl(
        baseUrl: String,
        layers: String,
        styles: String,
        format: String = "image/png",
        version: String = "1.1.0",
        tileSize: Int = 256,
        x: Int,
        y: Int,
        zoom: Int
    ): String {
        val params = linkedMapOf(
            "SERVICE" to "WMS",
            "REQUEST" to "GetMap",
            "VERSION" to version,
            "LAYERS" to layers,
            "STYLES" to styles,
            "SRS" to "EPSG:3857",
            "BBOX" to tileBbox3857(x, y, zoom, tileSize),
            "WIDTH" to tileSize.toString(),
            "HEIGHT" to tileSize.toString(),
            "FORMAT" to format,
            "TRANSPARENT" to "true"
        )
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "${baseUrl.trimEnd('?', '&')}?$query"
    }

    fun tileBbox3857(x: Int, y: Int, zoom: Int, tileSize: Int): String {
        val originShift = 20037508.342789244
        val resolution = (2.0 * originShift) / (tileSize.toDouble() * (1 shl zoom).toDouble())
        val minX = x * tileSize * resolution - originShift
        val maxX = (x + 1) * tileSize * resolution - originShift
        val maxY = originShift - y * tileSize * resolution
        val minY = originShift - (y + 1) * tileSize * resolution

        fun format(value: Double) = String.format(Locale.US, "%.8f", value)
        return "${format(minX)},${format(minY)},${format(maxX)},${format(maxY)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
