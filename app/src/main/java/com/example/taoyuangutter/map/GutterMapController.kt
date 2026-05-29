package com.example.taoyuangutter.map

import android.graphics.Color
import android.location.Location
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.ceil

/**
 * 管理地圖上「目前工作中的」側溝標記與線段。
 * 不處理 inspect/scope 線段，只聚焦新增/編輯流程中的暫存工作層。
 */
class GutterMapController(
    private val mapProvider: () -> GoogleMap?,
    private val markerIconProvider: (type: WaypointType, isPendingDeploy: Boolean, isVirtual: Boolean) -> BitmapDescriptor,
    private val polylineColorProvider: (isCurve: Boolean) -> Int = { Color.parseColor("#562ECB") },
    private val pendingFlagParser: (String?) -> Boolean,
    private val baselinePolylineColor: Int = Color.parseColor("#B4B4B4")
) {
    private val workingMarkers = mutableListOf<Marker>()
    private var workingPolyline: Polyline? = null
    private var baselinePolyline: Polyline? = null
    private var referencePolyline: Polyline? = null

    fun clearWorkingLayer() {
        clearWorkingMarkers()
        workingPolyline?.remove()
        workingPolyline = null
    }

    fun clearPreviewLayer() {
        clearWorkingLayer()
        clearBaselineRoute()
    }

    /**
     * 檢視/編輯流程中保留的「灰色參考線」。
     * - 不屬於 preview layer，避免被 clearPreviewLayer() 清掉。
     * - 由 MainActivity 控制何時顯示/清除。
     */
    fun showReferenceRoute(points: List<LatLng>) {
        val map = mapProvider() ?: return
        referencePolyline?.remove()
        referencePolyline = null
        if (points.size < 2) return
        referencePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(baselinePolylineColor)
                .width(8f)
                .geodesic(true)
                .clickable(false)
                .zIndex(0f)
        )
    }

    fun clearReferenceRoute() {
        referencePolyline?.remove()
        referencePolyline = null
    }

    fun clearWorkingMarkers() {
        workingMarkers.forEach { it.remove() }
        workingMarkers.clear()
    }

    fun showBaselineRoute(waypoints: List<Waypoint>, isCurve: Boolean = false) {
        val map = mapProvider() ?: return
        baselinePolyline?.remove()
        baselinePolyline = null

        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return
        val baselinePoints = if (isCurve && routePoints.size == 3) {
            val steps = resolveCurveSteps(routePoints[0], routePoints[1], routePoints[2])
            generateCurvePointsWithControl(
                start = routePoints[0],
                end = routePoints[2],
                control = routePoints[1],
                steps = steps
            )
        } else {
            routePoints
        }

        baselinePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(baselinePoints)
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
            val isVirtual = wp.isVirtual
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(markerIconProvider(wp.type, isPending, isVirtual))
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
            val isVirtual = wp.isVirtual
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(markerIconProvider(wp.type, isPending, isVirtual))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
    }

    fun findWorkingMarkerByTag(tag: Int): Marker? = workingMarkers.firstOrNull { it.tag == tag }

    private fun resolveCurveSteps(
        start: LatLng,
        control: LatLng,
        end: LatLng
    ): Int {
        val distance = distanceMeters(start, control) + distanceMeters(control, end)
        val stepsByDistance = ceil(distance / 5.0).toInt()
        return stepsByDistance.coerceAtLeast(100).coerceAtMost(1000)
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    private fun generateCurvePointsWithControl(
        start: LatLng,
        end: LatLng,
        control: LatLng,
        steps: Int
    ): List<LatLng> {
        val points = ArrayList<LatLng>(steps + 1)
        for (i in 0..steps) {
            val t = i.toDouble() / steps.toDouble()
            val inv = 1.0 - t
            val lat = inv * inv * start.latitude +
                2.0 * inv * t * control.latitude +
                t * t * end.latitude
            val lng = inv * inv * start.longitude +
                2.0 * inv * t * control.longitude +
                t * t * end.longitude
            points.add(LatLng(lat, lng))
        }
        return points
    }
}
