package com.example.taoyuangutter.map

import com.example.taoyuangutter.common.HttpTileProvider

class Wms3826TileProvider : HttpTileProvider({ x, y, zoom ->
    Wms3826RequestBuilder.buildTileUrl(x, y, zoom)
})
