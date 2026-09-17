package com.example.taoyuangutter.map

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wms3857RequestBuilderTest {
    @Test
    fun emptyStyleIsKeptAsAnExplicitWmsParameter() {
        val url = Wms3857RequestBuilder.buildTileUrl(
            baseUrl = "https://example.test/geoserver/wms",
            layers = "roadServey",
            styles = "",
            x = 1,
            y = 2,
            zoom = 3
        )
        val query = url.queryParameters()

        assertTrue(url.contains("STYLES="))
        assertEquals("", query["STYLES"])
        assertEquals("roadServey", query["LAYERS"])
        assertEquals("WMS", query["SERVICE"])
        assertEquals("GetMap", query["REQUEST"])
        assertEquals("EPSG:3857", query["SRS"])
        assertEquals("256", query["WIDTH"])
        assertEquals("256", query["HEIGHT"])
        assertEquals("image/png", query["FORMAT"])
        assertEquals("true", query["TRANSPARENT"])
    }

    @Test
    fun namedStylesRemainEncodedForOtherLayers() {
        val url = Wms3857RequestBuilder.buildTileUrl(
            baseUrl = "https://example.test/geoserver/wms",
            layers = "legacyDitch",
            styles = "TY_RSGDBIP_水務局既有資料",
            format = "image/png8",
            x = 1,
            y = 2,
            zoom = 3
        )

        assertEquals("TY_RSGDBIP_水務局既有資料", url.queryParameters()["STYLES"])
        assertEquals("legacyDitch", url.queryParameters()["LAYERS"])
        assertEquals("image/png8", url.queryParameters()["FORMAT"])
    }

    private fun String.queryParameters(): Map<String, String> =
        substringAfter('?').split('&').associate { parameter ->
            val parts = parameter.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
        }
}
