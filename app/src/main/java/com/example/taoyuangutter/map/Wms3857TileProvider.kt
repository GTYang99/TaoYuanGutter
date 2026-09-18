package com.example.taoyuangutter.map

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL

/**
 * Google Maps tile(x,y,z) -> GeoServer WMS GetMap (EPSG:3857) adapter.
 *
 * WMS 1.1.0 + SRS=EPSG:3857:
 * - BBOX order: minX,minY,maxX,maxY (meters in WebMercator)
 * - Add TRANSPARENT=true so it can be overlaid on basemap.
 */
class Wms3857TileProvider(
    private val baseUrl: String,
    private val layers: String,
    private val styles: String,
    private val format: String = "image/png",
    private val version: String = "1.1.0",
    private val tileSize: Int = 256
) : UrlTileProvider(tileSize, tileSize) {

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        val url = Wms3857RequestBuilder.buildTileUrl(
            baseUrl = baseUrl,
            layers = layers,
            styles = styles,
            format = format,
            version = version,
            tileSize = tileSize,
            x = x,
            y = y,
            zoom = zoom
        )
        return runCatching { URL(url) }.getOrNull()
    }
}
