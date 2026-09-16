package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.PhotoUploadSlotState
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType

/** Builds one storeDitch node without changing the waypoint's in-memory photo state. */
object StoreDitchNodeRequestMapper {
    fun map(
        waypoint: Waypoint,
        requestNodeId: Int?,
        nodeSequence: Int
    ): StoreDitchNodeRequest {
        val isCantOpen = waypoint.basicData["IS_CANTOPEN"].toBooleanLoose()
        val isVirtual = waypoint.isVirtual
        val nodeAtt = when (waypoint.type) {
            WaypointType.START -> 1
            WaypointType.NODE -> 2
            WaypointType.END -> 3
        }
        // Replacement uploads return a new img_id before storeDitch is called.
        // Keep the current image association on both create and update;
        // omitting it during update leaves the old server-side image linked.
        val imgIds = (1..3).mapNotNull { slot ->
            if (isVirtual || isCantOpen && slot in 2..3) {
                null
            } else {
                PhotoUploadSlotState.readImgId(waypoint.basicData, slot)
            }
        }.takeIf { it.isNotEmpty() }
        // storeDitch does not accept photo timestamps.  Timestamps are sent
        // only by the node-image multipart upload API.
        val capturedAt: List<String>? = null

        return StoreDitchNodeRequest(
            nodeId = requestNodeId,
            nodeAtt = nodeAtt,
            nodeNum = if (nodeAtt == 2) nodeSequence else null,
            nodeTyp = waypoint.basicData["NODE_TYP"]?.toIntOrNull() ?: 1,
            latitude = waypoint.latLng?.latitude ?: 0.0,
            longitude = waypoint.latLng?.longitude ?: 0.0,
            nodeLe = if (isVirtual) null else waypoint.basicData["NODE_LE"]?.toDoubleOrNull(),
            xyNum = waypoint.basicData["XY_NUM"] ?: "",
            isPendingDeploy = if (waypoint.basicData["IS_PENDING_DEPLOY"].toBooleanLoose()) 1 else 0,
            isCantOpen = if (isVirtual) 0 else if (isCantOpen) 1 else 0,
            isVirtual = isVirtual,
            matTyp = if (isCantOpen || isVirtual) null else (waypoint.basicData["MAT_TYP"]?.toIntOrNull() ?: 1),
            nodeDep = if (isCantOpen || isVirtual) null else (waypoint.basicData["NODE_DEP"]?.toIntOrNull() ?: 0),
            nodeWid = if (isCantOpen || isVirtual) null else (waypoint.basicData["NODE_WID"]?.toIntOrNull() ?: 0),
            coverDep = if (isCantOpen || isVirtual) null else waypoint.basicData["COVER_DEP"]?.toIntOrNull(),
            isBroken = if (isCantOpen || isVirtual) null else (waypoint.basicData["IS_BROKEN"]?.toIntOrNull() ?: 0),
            isHanging = if (isCantOpen || isVirtual) null else (waypoint.basicData["IS_HANGING"]?.toIntOrNull() ?: 0),
            isSilt = if (isCantOpen || isVirtual) null else (waypoint.basicData["IS_SILT"]?.toIntOrNull() ?: 0),
            nodeNote = if (isVirtual) null else waypoint.basicData["NODE_NOTE"]?.takeIf { it.isNotEmpty() },
            capturedAt = capturedAt,
            imgIds = imgIds
        )
    }

    private fun String?.toBooleanLoose(): Boolean = when (this?.trim()?.lowercase()) {
        "1", "true", "yes", "y", "on" -> true
        else -> false
    }
}
