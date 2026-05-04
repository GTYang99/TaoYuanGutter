package com.example.taoyuangutter.gutter

import android.content.Context
import android.content.Intent
import android.location.Location
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson

class GutterFormNavigator(
    private val context: Context
) {
    data class AddFormLaunch(
        val intent: Intent,
        val draftId: Long
    )

    fun buildInspectIntent(
        waypointIndex: Int,
        waypoint: Waypoint,
        latLng: LatLng,
        wmtsLayer: String,
        hostLastLocation: Location?
    ): Intent {
        return GutterFormActivity.newViewIntent(
            context,
            waypoint.label,
            latLng.latitude,
            latLng.longitude,
            waypointIndex,
            waypoint.basicData,
            wmtsLayer
        ).also { attachHostLastLocation(it, hostLastLocation) }
    }

    fun buildAddIntent(
        currentWaypoints: List<Waypoint>,
        currentIndex: Int,
        waypoint: Waypoint,
        isEditMode: Boolean,
        currentSessionDraftId: Long?,
        wmtsLayer: String,
        sessionIsOffline: Boolean,
        hostLastLocation: Location?
    ): AddFormLaunch {
        val ensuredDraftId = currentSessionDraftId ?: System.currentTimeMillis()
        val labels = ArrayList(currentWaypoints.map { it.label })
        val lats = currentWaypoints.map { it.latLng?.latitude ?: 0.0 }.toDoubleArray()
        val lngs = currentWaypoints.map { it.latLng?.longitude ?: 0.0 }.toDoubleArray()
        val sessionWaypointsJson = Gson().toJson(
            currentWaypoints.map { currentWaypoint ->
                WaypointSnapshot(
                    type = currentWaypoint.type.name,
                    label = currentWaypoint.label,
                    latitude = currentWaypoint.latLng?.latitude,
                    longitude = currentWaypoint.latLng?.longitude,
                    basicData = HashMap(currentWaypoint.basicData)
                )
            }
        )
        val intent = GutterFormActivity.newIntent(
            context = context,
            labels = labels,
            lats = lats,
            lngs = lngs,
            index = currentIndex,
            basicData = waypoint.basicData,
            isEditMode = isEditMode,
            sessionDraftId = ensuredDraftId,
            sessionWaypointsJson = sessionWaypointsJson,
            wmtsLayer = wmtsLayer,
            sessionIsOffline = sessionIsOffline
        ).also { attachHostLastLocation(it, hostLastLocation) }
        return AddFormLaunch(intent = intent, draftId = ensuredDraftId)
    }

    private fun attachHostLastLocation(intent: Intent, hostLastLocation: Location?) {
        val loc = hostLastLocation ?: return
        intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_LAT, loc.latitude)
        intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_LNG, loc.longitude)
        intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_TIME, loc.time)
        intent.putExtra(
            GutterFormActivity.EXTRA_HOST_LAST_ACC,
            if (loc.hasAccuracy()) loc.accuracy else -1f
        )
    }
}
