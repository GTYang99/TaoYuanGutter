package com.example.taoyuangutter.map

import android.content.Context
import com.example.taoyuangutter.gutter.Waypoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline

class MapCameraController(
    private val context: Context,
    private val mapProvider: () -> GoogleMap?
) {
    private var persistentBottomInsetPx: Int = 0

    fun setPersistentBottomInset(bottomInsetPx: Int) {
        persistentBottomInsetPx = bottomInsetPx.coerceAtLeast(0)
        mapProvider()?.setPadding(0, 0, 0, persistentBottomInsetPx)
    }

    fun zoomForGutterSize(basicData: Map<String, String>): Float {
        val wid = basicData["NODE_WID"]?.toFloatOrNull() ?: return 20f
        val minWid = 50f
        val maxWid = 200f
        val minZoom = 18f
        val maxZoom = 20f
        val clamped = wid.coerceIn(minWid, maxWid)
        return maxZoom - (clamped - minWid) / (maxWid - minWid) * (maxZoom - minZoom)
    }

    fun moveCameraToLatLngOffset(latLng: LatLng, offsetRatio: Double, zoom: Float = 20f) {
        val map = mapProvider() ?: return
        val screenH = context.resources.displayMetrics.heightPixels
        map.setPadding(0, 0, 0, persistentBottomInsetPx + (screenH * offsetRatio).toInt())
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, zoom),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    map.setPadding(0, 0, 0, persistentBottomInsetPx)
                }

                override fun onCancel() {
                    map.setPadding(0, 0, 0, persistentBottomInsetPx)
                }
            }
        )
    }

    fun fitCameraToWaypoints(
        waypoints: List<Waypoint>,
        bottomOffsetRatio: Double = 0.8,
        resetPaddingAfter: Boolean = true,
        maxZoom: Float? = null,
        paddingDp: Int = 64
    ) {
        val map = mapProvider() ?: return
        val points = waypoints.mapNotNull { it.latLng }
        if (points.isEmpty()) return

        val dm = context.resources.displayMetrics
        val screenH = dm.heightPixels
        val padding = (paddingDp * dm.density).toInt()
        map.setPadding(
            padding,
            padding,
            padding,
            persistentBottomInsetPx + (screenH * bottomOffsetRatio).toInt()
        )

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        try {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding),
                object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        applyZoomCap(map, maxZoom)
                        if (resetPaddingAfter) map.setPadding(0, 0, 0, persistentBottomInsetPx)
                    }

                    override fun onCancel() {
                        applyZoomCap(map, maxZoom)
                        if (resetPaddingAfter) map.setPadding(0, 0, 0, persistentBottomInsetPx)
                    }
                }
            )
        } catch (_: Exception) {
            if (resetPaddingAfter) map.setPadding(0, 0, 0, persistentBottomInsetPx)
            map.setOnMapLoadedCallback {
                fitCameraToWaypoints(waypoints, bottomOffsetRatio, resetPaddingAfter, maxZoom, paddingDp)
            }
        }
    }

    /**
     * 檢視/編輯專用：以實際可視高度比例來 fit。
     * 例如可視區只剩畫面高度 1/3 時，就傳入 1/3。
     */
    fun fitCameraToWaypointsWithViewportFraction(
        waypoints: List<Waypoint>,
        viewportHeightFraction: Double,
        resetPaddingAfter: Boolean = true,
        maxZoom: Float? = null,
        paddingDp: Int = 64
    ) {
        val clampedFraction = viewportHeightFraction.coerceIn(0.1, 1.0)
        fitCameraToWaypoints(
            waypoints = waypoints,
            bottomOffsetRatio = 1.0 - clampedFraction,
            resetPaddingAfter = resetPaddingAfter,
            maxZoom = maxZoom,
            paddingDp = paddingDp
        )
    }

    fun fitCameraToTaggedPolylines(polylines: Iterable<Polyline>) {
        val allWaypoints = mutableListOf<Waypoint>()
        polylines.forEach { poly ->
            @Suppress("UNCHECKED_CAST")
            (poly.tag as? ArrayList<Waypoint>)?.let { allWaypoints.addAll(it) }
        }
        if (allWaypoints.isNotEmpty()) {
            fitCameraToWaypoints(allWaypoints)
        }
    }

    private fun applyZoomCap(map: GoogleMap, maxZoom: Float?) {
        val cap = maxZoom ?: return
        if (map.cameraPosition.zoom > cap) {
            map.animateCamera(CameraUpdateFactory.zoomTo(cap))
        }
    }
}
