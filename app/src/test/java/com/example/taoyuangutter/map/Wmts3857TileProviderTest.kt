package com.example.taoyuangutter.map

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Wmts3857TileProviderTest {
    @Test
    fun supportedLayersBuildWebMercatorQuadRequestsWithBackendDefaultStyle() {
        val expectedFormats = mapOf(
            BackgroundWmtsLayer.ROAD_SURVEY to "image/png",
            BackgroundWmtsLayer.LEGACY_DITCH to "image/png8",
            BackgroundWmtsLayer.REGIONS to "image/png8"
        )

        expectedFormats.forEach { (layer, format) ->
            val query = queryOf(Wmts3857RequestBuilder.buildTileUrl(layer, x = 12, y = 7, zoom = 5))

            assertEquals("WMTS", query["SERVICE"])
            assertEquals("1.0.0", query["VERSION"])
            assertEquals("GetTile", query["REQUEST"])
            assertEquals(layer.layerName, query["LAYER"])
            assertEquals("", query["STYLE"])
            assertEquals(format, query["FORMAT"])
            assertEquals("WebMercatorQuad", query["TILEMATRIXSET"])
            assertEquals("5", query["TILEMATRIX"])
            assertEquals("7", query["TILEROW"])
            assertEquals("12", query["TILECOL"])
        }
    }

    @Test
    fun unsupportedZoomReturnsNoTile() {
        assertNull(Wmts3857TileProvider(BackgroundWmtsLayer.REGIONS).getTileUrl(0, 0, 25))
    }

    private fun queryOf(url: String): Map<String, String> = url.substringAfter('?').split('&').associate {
        val (key, value) = it.split('=', limit = 2)
        URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
