package com.example.taoyuangutter.map

import android.graphics.Color
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

/**
 * 管理地圖上「目前工作中的」側溝標記與線段。
 * 不處理 inspect/scope 線段，只聚焦新增/編輯流程中的暫存工作層。
 */
class GutterMapController(
    private val mapProvider: () -> GoogleMap?,
    private val markerIconProvider: (type: WaypointType, isPendingDeploy: Boolean) -> BitmapDescriptor,
    private val polylineColorProvider: (isCurve: Boolean) -> Int = { Color.parseColor("#562ECB") },
    private val pendingFlagParser: (String?) -> Boolean,
    private val baselinePolylineColor: Int = Color.parseColor("#B4B4B4")
) {
    private val workingMarkers = mutableListOf<Marker>()
    private var workingPolyline: Polyline? = null
    private var baselinePolyline: Polyline? = null

    fun clearWorkingLayer() {
        clearWorkingMarkers()
        workingPolyline?.remove()
        workingPolyline = null
    }

    fun clearPreviewLayer() {
        clearWorkingLayer()
        clearBaselineRoute()
    }

    fun clearWorkingMarkers() {
        workingMarkers.forEach { it.remove() }
        workingMarkers.clear()
    }

    fun showBaselineRoute(waypoints: List<Waypoint>) {
        val map = mapProvider() ?: return
        baselinePolyline?.remove()
        baselinePolyline = null

        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return

        baselinePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(baselinePolylineColor)
                .width(8f)
                .geodesic(true)
                .clickable(false)
                .zIndex(0f)
        )
    }

    fun clearBaselineRoute() {
        baselinePolyline?.remove()
        baselinePolyline = null
    }

    fun refreshWorkingLayer(waypoints: List<Waypoint>, isCurve: Boolean) {
        val map = mapProvider() ?: return
        clearWorkingLayer()
        if (waypoints.isEmpty()) return

        val routePoints = mutableListOf<LatLng>()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            routePoints.add(latLng)
            val isPending = pendingFlagParser(wp.basicData["IS_PENDING_DEPLOY"])
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(markerIconProvider(wp.type, isPending))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }

        if (routePoints.size >= 2) {
            workingPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(polylineColorProvider(isCurve))
                    .width(10f)
                    .geodesic(true)
                    .clickable(false)
                    .zIndex(1f)
            )
        }
    }

    fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        val map = mapProvider() ?: return
        clearWorkingMarkers()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            val isPending = pendingFlagParser(wp.basicData["IS_PENDING_DEPLOY"])
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(markerIconProvider(wp.type, isPending))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
    }

    fun findWorkingMarkerByTag(tag: Int): Marker? = workingMarkers.firstOrNull { it.tag == tag }
}
