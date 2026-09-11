package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.WaypointSnapshot

/** Produces upload-only waypoint copies while preserving form and draft metadata. */
object PhotoUploadCandidateResolver {
    fun resolve(
        waypoints: List<Waypoint>,
        originalWaypoints: List<WaypointSnapshot>?,
        resumedFromDraft: Boolean
    ): List<Waypoint> {
        if (resumedFromDraft || originalWaypoints.isNullOrEmpty()) return waypoints

        val originalByUid = originalWaypoints
            .mapNotNull { snapshot ->
                snapshot.uid.takeIf { it.isNotBlank() }?.let { it to snapshot }
            }
            .toMap()

        return waypoints.map { waypoint ->
            val currentNodeId = waypoint.basicData["_nodeId"]?.trim()?.takeIf { it.isNotEmpty() }
            val original = waypoint.uid.takeIf { it.isNotBlank() }?.let { originalByUid[it] }
                ?: originalWaypoints.firstOrNull {
                    it.basicData["_nodeId"]?.trim()?.takeIf { id -> id.isNotEmpty() } == currentNodeId
                }
            if (original == null) return@map waypoint

            val merged = HashMap(waypoint.basicData)
            var changed = false
            for (slot in 1..3) {
                val photoKey = "photo$slot"
                val capturedAtKey = "photo${slot}CapturedAt"
                val currentPhoto = merged[photoKey]?.trim().orEmpty()
                val originalPhoto = original.basicData[photoKey]?.trim().orEmpty()
                val currentCapturedAt = merged[capturedAtKey]?.trim().orEmpty()
                val originalCapturedAt = original.basicData[capturedAtKey]?.trim().orEmpty()
                val sameByCapturedAt = currentCapturedAt.isNotEmpty() &&
                    currentCapturedAt == originalCapturedAt && originalCapturedAt.isNotEmpty()
                val sameByPhotoPath = currentPhoto.isNotEmpty() && currentPhoto == originalPhoto

                if (sameByCapturedAt || sameByPhotoPath) {
                    merged[photoKey] = ""
                    merged[capturedAtKey] = ""
                    changed = true
                }
            }
            if (changed) waypoint.copy(basicData = merged) else waypoint
        }
    }
}
