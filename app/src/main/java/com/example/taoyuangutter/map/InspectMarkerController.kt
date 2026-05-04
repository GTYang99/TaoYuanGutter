package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * 管理 inspect/檢視模式下的 marker 顯示與高亮，
 * 與編輯中的 working layer 拆開，避免共用狀態互相覆蓋。
 */
class InspectMarkerController(
    private val mapProvider: () -> GoogleMap?,
    private val markerIconProvider: (type: WaypointType, isPendingDeploy: Boolean) -> BitmapDescriptor,
    private val enlargedMarkerIconProvider: (type: WaypointType, isPendingDeploy: Boolean) -> BitmapDescriptor,
    private val pendingFlagParser: (String?) -> Boolean
) {
    private val markers = mutableListOf<Marker>()
    private var highlightedMarkerIndex: Int = -1

    fun clear() {
        markers.forEach { it.remove() }
        markers.clear()
        highlightedMarkerIndex = -1
    }

    fun showInspectMarkers(
        nodes: List<NodeDetails>,
        pendingByNodeId: Map<Int, Boolean> = emptyMap()
    ) {
        val map = mapProvider() ?: return
        clear()
        if (nodes.isEmpty()) return

        nodes.forEachIndexed { idx, node ->
            val lat = node.latitude?.toDoubleOrNull() ?: return@forEachIndexed
            val lng = node.longitude?.toDoubleOrNull() ?: return@forEachIndexed
            val latLng = LatLng(lat, lng)

            val wpType = when (node.nodeAttr) {
                "1" -> WaypointType.START
                "3" -> WaypointType.END
                else -> WaypointType.NODE
            }
            val nodeId = node.nodeId
            val isPending = when {
                nodeId != null && pendingByNodeId.containsKey(nodeId) -> pendingByNodeId[nodeId] == true
                else -> pendingFlagParser(node.isPendingDeploy)
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(markerIconProvider(wpType, isPending))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = idx
            marker?.let { markers.add(it) }
        }
    }

    fun highlightMarker(waypointIndex: Int, waypoints: List<Waypoint>) {
        resetHighlightedMarker(waypoints)
        val wp = waypoints.getOrNull(waypointIndex) ?: return
        if (wp.latLng == null) return
        highlightedMarkerIndex = waypointIndex
        markers.firstOrNull { it.tag == waypointIndex }?.let { marker ->
            val isPending = pendingFlagParser(wp.basicData["IS_PENDING_DEPLOY"])
            marker.setIcon(enlargedMarkerIconProvider(wp.type, isPending))
            marker.setAnchor(0.5f, 0.5f)
            marker.zIndex = 1f
        }
    }

    fun resetHighlightedMarker(waypoints: List<Waypoint>) {
        if (highlightedMarkerIndex < 0) return
        val wp = waypoints.getOrNull(highlightedMarkerIndex)
        markers.firstOrNull { it.tag == highlightedMarkerIndex }?.let { marker ->
            val type = wp?.type ?: WaypointType.NODE
            val isPending = pendingFlagParser(wp?.basicData?.get("IS_PENDING_DEPLOY"))
            marker.setIcon(markerIconProvider(type, isPending))
            marker.setAnchor(0.5f, 0.5f)
            marker.zIndex = 0f
        }
        highlightedMarkerIndex = -1
    }
}
