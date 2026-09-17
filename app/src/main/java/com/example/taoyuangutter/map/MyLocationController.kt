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
    private var currentLocationCancellation: CancellationTokenSource? = null
    private var requestGeneration = 0L

    fun requestLocationAndMove(
        requestPermission: () -> Unit,
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)? = null,
        onLocationUnavailable: (() -> Unit)? = null
    ) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndMove(onLocationUpdated, onCameraMoveFinished, onLocationUnavailable)
        } else {
            requestPermission()
        }
    }

    @SuppressLint("MissingPermission")
    fun enableMyLocationAndMove(
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)? = null,
        onLocationUnavailable: (() -> Unit)? = null
    ) {
        val map = mapProvider() ?: return
        map.isMyLocationEnabled = true
        map.uiSettings?.isMyLocationButtonEnabled = false

        cancelPendingLocationRequest()
        val generation = ++requestGeneration
        fusedLocationClient.lastLocation
            .addOnSuccessListener { cached ->
                if (generation != requestGeneration) return@addOnSuccessListener
                val accepted = cached.takeIf { LocationFixQualityPolicy.isUsableCached(it) }
                accepted?.let { moveMap(map, it, onLocationUpdated, onCameraMoveFinished) }
                requestHighAccuracyRefinement(generation, map, accepted, onLocationUpdated, onCameraMoveFinished, onLocationUnavailable)
            }
            .addOnFailureListener {
                if (generation == requestGeneration) {
                    requestHighAccuracyRefinement(generation, map, null, onLocationUpdated, onCameraMoveFinished, onLocationUnavailable)
                }
            }
    }

    fun cancelPendingLocationRequest() {
        requestGeneration++
        currentLocationCancellation?.cancel()
        currentLocationCancellation = null
    }

    @SuppressLint("MissingPermission")
    private fun requestHighAccuracyRefinement(
        generation: Long,
        map: GoogleMap,
        accepted: Location?,
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)?,
        onLocationUnavailable: (() -> Unit)?
    ) {
        val cancellation = CancellationTokenSource()
        currentLocationCancellation = cancellation
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { refinement ->
                if (generation != requestGeneration || currentLocationCancellation !== cancellation) return@addOnSuccessListener
                currentLocationCancellation = null
                if (LocationFixQualityPolicy.shouldUseRefinement(accepted, refinement)) {
                    moveMap(map, refinement!!, onLocationUpdated, onCameraMoveFinished)
                } else if (accepted == null) {
                    onLocationUnavailable?.invoke()
                }
            }
            .addOnFailureListener {
                if (generation != requestGeneration || currentLocationCancellation !== cancellation) return@addOnFailureListener
                currentLocationCancellation = null
                if (accepted == null) onLocationUnavailable?.invoke()
            }
    }

    private fun moveMap(
        map: GoogleMap,
        location: Location,
        onLocationUpdated: (Location) -> Unit,
        onCameraMoveFinished: (() -> Unit)?
    ) {
        onLocationUpdated(location)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 18f),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() = onCameraMoveFinished?.invoke() ?: Unit
                override fun onCancel() = onCameraMoveFinished?.invoke() ?: Unit
            }
        )
    }
}
