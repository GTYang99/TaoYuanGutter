package com.example.taoyuangutter.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendEndpointsTest {
    @Test
    fun formalEndpointsUseTaipeiAndLegacyValuesRemainSeparate() {
        assertEquals("taipei.srgeo.com.tw", BackendEndpoints.ACTIVE_HOST)
        assertEquals(
            "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/",
            BackendEndpoints.ACTIVE_API_BASE_URL
        )
        assertEquals(
            "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
            BackendEndpoints.ACTIVE_WMS_URL
        )
        assertEquals(
            "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver",
            BackendEndpoints.ACTIVE_GEOSERVER_BASE_URL
        )
        assertEquals(
            "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts",
            BackendEndpoints.ACTIVE_WMTS_URL
        )
        assertNotEquals(BackendEndpoints.ACTIVE_API_BASE_URL, BackendEndpoints.LEGACY_DEMO_API_BASE_URL)
        assertNotEquals(
            BackendEndpoints.ACTIVE_GEOSERVER_BASE_URL,
            BackendEndpoints.LEGACY_DEMO_GEOSERVER_BASE_URL
        )
        assertNotEquals(BackendEndpoints.ACTIVE_WMS_URL, BackendEndpoints.LEGACY_DEMO_WMS_URL)
        assertNotEquals(BackendEndpoints.ACTIVE_WMTS_URL, BackendEndpoints.LEGACY_DEMO_WMTS_URL)
        assertTrue(BackendHttpClient.TEMPORARY_ALLOW_TAIPEI_HOSTNAME_MISMATCH)
    }
}
