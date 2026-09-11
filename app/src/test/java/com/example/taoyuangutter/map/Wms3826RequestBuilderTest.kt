package com.example.taoyuangutter.map

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wms3826RequestBuilderTest {
    @Test
    fun fixedTileProducesExpectedEpsg3826Bounds() {
        val bbox = Wms3826RequestBuilder.tileBbox3826(54849, 28085, 16)

        assertEquals(279756.220, bbox[0], 0.1)
        assertEquals(2754335.172, bbox[1], 0.1)
        assertEquals(280312.364, bbox[2], 0.1)
        assertEquals(2754888.262, bbox[3], 0.1)
    }

    @Test
    fun tileUrlContainsCapabilitiesCompatibleWmsParameters() {
        val url = Wms3826RequestBuilder.buildTileUrl(54849, 28085, 16)
        val query = url.substringAfter('?').split('&').associate {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
        }

        assertEquals("WMS", query["SERVICE"])
        assertEquals("GetMap", query["REQUEST"])
        assertEquals("1.1.0", query["VERSION"])
        assertEquals("deleted_area", query["LAYERS"])
        assertEquals("TY_RSGDBIP_0910刪除資料", query["STYLES"])
        assertEquals("EPSG:3826", query["SRS"])
        assertEquals("image/png8", query["FORMAT"])
        assertEquals("256", query["WIDTH"])
        assertEquals("256", query["HEIGHT"])
        assertEquals("true", query["TRANSPARENT"])
        assertTrue(query["BBOX"].orEmpty().matches(Regex("\\d+\\.\\d{3},\\d+\\.\\d{3},\\d+\\.\\d{3},\\d+\\.\\d{3}")))
    }
}
