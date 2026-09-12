package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.api.DitchNode

/** Applies server-assigned node and photo IDs returned by storeDitch. */
object StoreDitchResponseWaypointMapper {
    fun apply(waypoints: List<Waypoint>, nodes: List<DitchNode>): List<Waypoint> =
        waypoints.mapIndexed { index, waypoint ->
            val node = nodes.getOrNull(index) ?: return@mapIndexed waypoint
            val merged = HashMap(waypoint.basicData).apply {
                put("_nodeId", node.nodeId.toString())
                node.url.forEach { image ->
                    val slot = image.fileCategory?.toIntOrNull()
                    val imageId = image.id
                    if (slot in 1..3 && imageId != null) {
                        put("photo${slot}ImgId", imageId.toString())
                    }
                }
            }
            waypoint.copy(basicData = merged)
        }
}
