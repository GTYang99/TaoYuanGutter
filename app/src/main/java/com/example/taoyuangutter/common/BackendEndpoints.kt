package com.example.taoyuangutter.common

/**
 * Active formal endpoints and inactive legacy DEMO values.
 *
 * The legacy values are intentionally not selected by runtime code and must not
 * be used as an automatic fallback for reads or writes.
 */
object BackendEndpoints {
    const val ACTIVE_HOST = "taipei.srgeo.com.tw"
    //const val ACTIVE_API_BASE_URL = "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/"
    const val ACTIVE_API_TAPIEI_URL = "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/"
    const val LEGACY_DEMO_API_BASE_URL = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/"

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
