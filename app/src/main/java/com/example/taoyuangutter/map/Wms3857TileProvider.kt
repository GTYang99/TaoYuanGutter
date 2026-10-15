package com.example.taoyuangutter.map

import com.example.taoyuangutter.common.HttpTileProvider

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
) : HttpTileProvider(
    urlBuilder = { x, y, zoom ->
        Wms3857RequestBuilder.buildTileUrl(
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
    },
    tileSize = tileSize
)
