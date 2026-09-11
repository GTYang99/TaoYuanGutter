package com.example.taoyuangutter.map

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL

class Wms3826TileProvider : UrlTileProvider(256, 256) {
    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = runCatching {
        URL(Wms3826RequestBuilder.buildTileUrl(x, y, zoom))
    }.getOrNull()
}
