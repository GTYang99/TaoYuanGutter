package com.example.taoyuangutter.map

import android.net.Uri
import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL
import java.util.Locale

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
        val bbox = tileBbox3857(x, y, zoom, tileSize)
        val url = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("SERVICE", "WMS")
            .appendQueryParameter("REQUEST", "GetMap")
            .appendQueryParameter("VERSION", version)
            .appendQueryParameter("LAYERS", layers)
            .appendQueryParameter("STYLES", styles)
            .appendQueryParameter("SRS", "EPSG:3857")
            .appendQueryParameter("BBOX", bbox)
            .appendQueryParameter("WIDTH", tileSize.toString())
            .appendQueryParameter("HEIGHT", tileSize.toString())
            .appendQueryParameter("FORMAT", format)
            .appendQueryParameter("TRANSPARENT", "true")
            .build()
            .toString()

        return runCatching { URL(url) }.getOrNull()
    }

    private fun tileBbox3857(x: Int, y: Int, zoom: Int, tileSize: Int): String {
        // WebMercator extent in meters.
        val originShift = 20037508.342789244
        val res = (2.0 * originShift) / (tileSize.toDouble() * (1 shl zoom).toDouble())

        val minX = x * tileSize * res - originShift
        val maxX = (x + 1) * tileSize * res - originShift

        // y=0 at top in Google tile scheme.
        val maxY = originShift - y * tileSize * res
        val minY = originShift - (y + 1) * tileSize * res

        fun f(v: Double) = String.format(Locale.US, "%.8f", v)
        return "${f(minX)},${f(minY)},${f(maxX)},${f(maxY)}"
    }
}

