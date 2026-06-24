package com.example.taoyuangutter.map

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL

class MapOverlayController(
    private val mapProvider: () -> GoogleMap?,
    private val onNoDitchPointsLayerChanged: ((Boolean) -> Unit)? = null
) {
    data class OverlayState(
        val selectedLayer: String,
        val showPlan: Boolean,
        val showWaterOld: Boolean,
        val showPossible: Boolean,
        val showRegion: Boolean,
        val showNoDitchPoints: Boolean
    )

    private var currentTileOverlay: TileOverlay? = null
    private var planWmsOverlay: TileOverlay? = null
    private var waterOldWmsOverlay: TileOverlay? = null
    private var regionWmsOverlay: TileOverlay? = null
    private var noDitchPointsWmsOverlay: TileOverlay? = null
    private var measureLabelsWmsOverlay: TileOverlay? = null
    private var noDitchPointsInteractionEnabled: Boolean = false

    private var currentLayer: String = LayersBottomSheet.LAYER_EMAP
    private var showPlanOverlay = true
    private var showWaterOldOverlay = true
    private var showPossibleOverlay = true
    private var showRegionOverlay = true
    private var showNoDitchPointsOverlay = false

    fun currentLayer(): String = currentLayer

    fun currentState(): OverlayState = OverlayState(
        selectedLayer = currentLayer,
        showPlan = showPlanOverlay,
        showWaterOld = showWaterOldOverlay,
        showPossible = showPossibleOverlay,
        showRegion = showRegionOverlay,
        showNoDitchPoints = showNoDitchPointsOverlay
    )

    fun applyState(state: OverlayState) {
        currentLayer = state.selectedLayer
        showPlanOverlay = state.showPlan
        showWaterOldOverlay = state.showWaterOld
        showPossibleOverlay = state.showPossible
        showRegionOverlay = state.showRegion
        showNoDitchPointsOverlay = state.showNoDitchPoints

        // Re-apply base layer and overlays
        setBaseLayer(currentLayer)
        applyWmsOverlays()
    }

    fun setBaseLayer(layer: String) {
        currentTileOverlay?.remove()
        currentLayer = layer
        val urlTemplate = "https://wmts.nlsc.gov.tw/wmts/$layer/default/GoogleMapsCompatible/%d/%d/%d"
        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = try {
                URL(String.format(urlTemplate, zoom, y, x))
            } catch (_: MalformedURLException) {
                null
            }
        }
        currentTileOverlay = mapProvider()?.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider).zIndex(-1f)
        )
    }

    fun updateOverlayToggles(
        showPlan: Boolean,
        showWaterOld: Boolean,
        showPossible: Boolean,
        showRegion: Boolean,
        showNoDitchPoints: Boolean
    ) {
        showPlanOverlay = showPlan
        showWaterOldOverlay = showWaterOld
        showPossibleOverlay = showPossible
        showRegionOverlay = showRegion
        showNoDitchPointsOverlay = showNoDitchPoints
        applyWmsOverlays()
    }

    fun ensureMeasureLabelsOverlay() {
        val map = mapProvider() ?: return
        if (measureLabelsWmsOverlay != null) return
        val provider = Wms3857TileProvider(
            baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
            layers = "map_ditch_nodes_labels",
            styles = "TY_RSGDBIP_測量座標編號",
            format = "image/png8"
        )
        measureLabelsWmsOverlay = map.addTileOverlay(
            TileOverlayOptions().tileProvider(provider).zIndex(0.3f).transparency(0f)
        )
    }

    fun applyWmsOverlays() {
        val map = mapProvider() ?: return

        if (showPossibleOverlay) {
            if (planWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/roadServey/wms",
                    layers = "roadServey",
                    styles = "TY_RSGDBIP_道路調查",
                    format = "image/png"
                )
                planWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(0f).transparency(0f)
                )
            }
        } else {
            planWmsOverlay?.remove()
            planWmsOverlay = null
        }

        if (showWaterOldOverlay) {
            if (waterOldWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "legacyDitch",
                    styles = "TY_RSGDBIP_水務局既有資料",
                    format = "image/png8"
                )
                waterOldWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(0.1f).transparency(0f)
                )
            }
        } else {
            waterOldWmsOverlay?.remove()
            waterOldWmsOverlay = null
        }

        if (showRegionOverlay) {
            if (regionWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "regions",
                    styles = "TY_RSGDBIP_桃園行政區",
                    format = "image/png8"
                )
                regionWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(-0.5f).transparency(0f)
                )
            }
        } else {
            regionWmsOverlay?.remove()
            regionWmsOverlay = null
        }

        if (showNoDitchPointsOverlay) {
            if (noDitchPointsWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "map_no_ditch_points",
                    styles = "",
                    format = "image/png8"
                )
                noDitchPointsWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(0.2f).transparency(0f)
                )
            }
            if (!noDitchPointsInteractionEnabled) {
                noDitchPointsInteractionEnabled = true
                onNoDitchPointsLayerChanged?.invoke(true)
            }
        } else {
            noDitchPointsWmsOverlay?.remove()
            noDitchPointsWmsOverlay = null
            if (noDitchPointsInteractionEnabled) {
                noDitchPointsInteractionEnabled = false
                onNoDitchPointsLayerChanged?.invoke(false)
            }
        }

    }
}
