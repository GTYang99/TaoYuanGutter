package com.example.taoyuangutter.map

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class BackgroundWmtsLayer(val layerName: String, val format: String) {
    ROAD_SURVEY("roadServey", "image/png"),
    LEGACY_DITCH("legacyDitch", "image/png8"),
    REGIONS("regions", "image/png8")
}

/** Builds WebMercatorQuad WMTS GetTile requests for the supported background layers. */
object Wmts3857RequestBuilder {
    const val BASE_URL = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts"
    private const val MAX_ZOOM = 24

    fun buildTileUrl(layer: BackgroundWmtsLayer, x: Int, y: Int, zoom: Int): String {
        require(zoom in 0..MAX_ZOOM) { "zoom must be between 0 and $MAX_ZOOM" }
        val tileCount = 1 shl zoom
        require(x in 0 until tileCount) { "x outside tile range" }
        require(y in 0 until tileCount) { "y outside tile range" }

        val params = linkedMapOf(
            "SERVICE" to "WMTS",
            "VERSION" to "1.0.0",
            "REQUEST" to "GetTile",
            "LAYER" to layer.layerName,
            "STYLE" to "",
            "FORMAT" to layer.format,
            "TILEMATRIXSET" to "WebMercatorQuad",
            "TILEMATRIX" to zoom.toString(),
            "TILEROW" to y.toString(),
            "TILECOL" to x.toString()
        )
        return "$BASE_URL?" + params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

class Wmts3857TileProvider(
    private val layer: BackgroundWmtsLayer
) : UrlTileProvider(TILE_SIZE, TILE_SIZE) {
    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = runCatching {
        URL(Wmts3857RequestBuilder.buildTileUrl(layer, x, y, zoom))
    }.getOrNull()

    private companion object {
        const val TILE_SIZE = 256
    }
}
