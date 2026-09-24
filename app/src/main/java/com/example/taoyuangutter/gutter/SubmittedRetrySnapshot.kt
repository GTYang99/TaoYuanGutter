package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.model.LatLng
import java.util.UUID

/**
 * Room-authoritative source for a submitted re-upload attempt.
 *
 * The stored snapshots are copied on construction. Each transport projection
 * receives a fresh mutable [Waypoint] copy, so upload metadata cannot leak
 * into the editable sheet state or back into the persisted draft.
 */
internal class SubmittedRetrySnapshot private constructor(
    private val snapshots: List<WaypointSnapshot>,
    val spiTyp: String?,
    val isCurve: Boolean
) {
    fun toTransportWaypoints(): List<Waypoint> = snapshots.map { snapshot ->
        val type = WaypointType.entries.firstOrNull { it.name == snapshot.type }
            ?: WaypointType.NODE
        val latLng = if (snapshot.latitude != null && snapshot.longitude != null) {
            LatLng(snapshot.latitude, snapshot.longitude)
        } else {
            null
        }
        Waypoint(
            type = type,
            label = snapshot.label,
            latLng = latLng,
            basicData = HashMap(snapshot.basicData),
            uid = snapshot.uid.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        )
    }

    companion object {
        fun fromDraft(draft: GutterSessionDraft): SubmittedRetrySnapshot =
            SubmittedRetrySnapshot(
                snapshots = draft.waypoints.map { it.copy(basicData = HashMap(it.basicData)) },
                spiTyp = draft.spiTyp,
                isCurve = draft.kind == com.example.taoyuangutter.pending.KIND_CURVE
            )
    }
}
