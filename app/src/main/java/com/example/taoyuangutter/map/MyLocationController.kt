package com.example.taoyuangutter.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource

class MyLocationController(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val mapProvider: () -> GoogleMap?
) {
    fun requestLocationAndMove(
        requestPermission: () -> Unit,
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)? = null
    ) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndMove(onLocationUpdated, onCameraMoveFinished)
        } else {
            requestPermission()
        }
    }

    @SuppressLint("MissingPermission")
    fun enableMyLocationAndMove(
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)? = null
    ) {
        val map = mapProvider() ?: return
        map.isMyLocationEnabled = true
        map.uiSettings?.isMyLocationButtonEnabled = false

        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationUpdated(loc)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            18f
                        ),
                        object : GoogleMap.CancelableCallback {
                            override fun onFinish() {
                                onCameraMoveFinished?.invoke()
                            }

                            override fun onCancel() {
                                onCameraMoveFinished?.invoke()
                            }
                        }
                    )
                }
            }
    }
}
