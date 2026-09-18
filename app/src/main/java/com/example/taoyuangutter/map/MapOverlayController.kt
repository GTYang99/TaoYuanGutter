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
        val showNoDitchPoints: Boolean,
        val showDeletedArea: Boolean = true
    )

    private var currentTileOverlay: TileOverlay? = null
    private var planWmsOverlay: TileOverlay? = null
    private var waterOldWmsOverlay: TileOverlay? = null
    private var regionWmsOverlay: TileOverlay? = null
    private var noDitchPointsWmsOverlay: TileOverlay? = null
    private var deletedAreaWmsOverlay: TileOverlay? = null
    private var measureLabelsWmsOverlay: TileOverlay? = null
    private var noDitchPointsInteractionEnabled: Boolean = false

    private var currentLayer: String = LayersBottomSheet.LAYER_EMAP
    private var showPlanOverlay = true
    private var showWaterOldOverlay = true
    private var showPossibleOverlay = true
    private var showRegionOverlay = true
    private var showNoDitchPointsOverlay = false
    private var showDeletedAreaOverlay = true

    fun currentLayer(): String = currentLayer

    fun currentState(): OverlayState = OverlayState(
        selectedLayer = currentLayer,
        showPlan = showPlanOverlay,
        showWaterOld = showWaterOldOverlay,
        showPossible = showPossibleOverlay,
        showRegion = showRegionOverlay,
        showNoDitchPoints = showNoDitchPointsOverlay,
        showDeletedArea = showDeletedAreaOverlay
    )

    fun applyState(state: OverlayState) {
        currentLayer = state.selectedLayer
        showPlanOverlay = state.showPlan
        showWaterOldOverlay = state.showWaterOld
        showPossibleOverlay = state.showPossible
        showRegionOverlay = state.showRegion
        showNoDitchPointsOverlay = state.showNoDitchPoints
        showDeletedAreaOverlay = state.showDeletedArea

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
        showNoDitchPoints: Boolean,
        showDeletedArea: Boolean
    ) {
        showPlanOverlay = showPlan
        showWaterOldOverlay = showWaterOld
        showPossibleOverlay = showPossible
        showRegionOverlay = showRegion
        showNoDitchPointsOverlay = showNoDitchPoints
        showDeletedAreaOverlay = showDeletedArea
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

        if (showDeletedAreaOverlay) {
            if (deletedAreaWmsOverlay == null) {
                deletedAreaWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(Wms3826TileProvider()).zIndex(0.15f).transparency(0f)
                )
            }
        } else {
            deletedAreaWmsOverlay?.remove()
            deletedAreaWmsOverlay = null
        }

        if (showPossibleOverlay) {
            if (planWmsOverlay == null) {
                val provider = Wmts3857TileProvider(BackgroundWmtsLayer.ROAD_SURVEY)
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
                val provider = Wmts3857TileProvider(BackgroundWmtsLayer.LEGACY_DITCH)
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
                val provider = Wmts3857TileProvider(BackgroundWmtsLayer.REGIONS)
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
