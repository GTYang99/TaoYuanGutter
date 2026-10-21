package com.example.taoyuangutter.common

/**
 * API endpoints selectable by the debug-only tester control, plus fixed Taipei map endpoints.
 * No target is an automatic fallback for another target.
 */
object BackendEndpoints {
    const val ACTIVE_HOST = "taipei.srgeo.com.tw"
    const val BASE_API_URL = "http://192.168.10.84/TY_RSGDBIP/"
    const val TAIPEI_API_URL = "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/"
    const val DEMO_API_URL = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/"
    //const val ACTIVE_API_BASE_URL = "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/"
    const val ACTIVE_API_TAPIEI_URL = TAIPEI_API_URL
    const val LEGACY_DEMO_API_BASE_URL = DEMO_API_URL

    fun apiBaseUrl(target: ApiBackendTarget): String = when (target) {
        ApiBackendTarget.BASE -> BASE_API_URL
        ApiBackendTarget.TAIPEI -> TAIPEI_API_URL
        ApiBackendTarget.DEMO -> DEMO_API_URL
    }

    const val ACTIVE_GEOSERVER_BASE_URL =
        "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver"
    const val LEGACY_DEMO_GEOSERVER_BASE_URL =
        "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver"

    const val ACTIVE_WMS_URL =
        "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms"
    const val LEGACY_DEMO_WMS_URL =
        "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms"

    const val ACTIVE_WMTS_URL =
        "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts"
    const val LEGACY_DEMO_WMTS_URL =
        "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts"
}

enum class ApiBackendTarget { BASE, TAIPEI, DEMO }
