package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.common.PhotoUploadSlotState

/** The single source of truth for photos that require a batch upload. */
internal object PendingPhotoUploadCandidatePlanner {
    data class Candidate(
        val waypointIndex: Int,
        val slot: Int,
        val photoPath: String
    )

    fun resolve(
        waypoints: List<Waypoint>,
        isUnchangedPhoto: (Waypoint, Int) -> Boolean,
        isUsableForUpload: (String?) -> Boolean
    ): List<Candidate> = buildList {
        waypoints.forEachIndexed { waypointIndex, waypoint ->
            if (waypoint.isVirtual) return@forEachIndexed
            val isCantOpen = waypoint.basicData["IS_CANTOPEN"].isTrueValue()
            (1..3).forEach { slot ->
                if (isCantOpen && slot in 2..3) return@forEach
                if (isUnchangedPhoto(waypoint, slot)) return@forEach
                if (PhotoUploadSlotState.isAlreadyUploaded(waypoint.basicData, slot)) return@forEach
                val photoPath = waypoint.basicData["photo$slot"]
                if (!isUsableForUpload(photoPath)) return@forEach
                add(Candidate(waypointIndex, slot, photoPath.orEmpty()))
            }
        }
    }

    private fun String?.isTrueValue(): Boolean = when (this?.trim()?.lowercase()) {
        "1", "true", "t", "y", "yes" -> true
        else -> false
    }
}
