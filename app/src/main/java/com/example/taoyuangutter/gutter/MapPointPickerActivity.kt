package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taoyuangutter.databinding.ActivityMapPointPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapPointPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapPointPickerBinding
    private var googleMap: GoogleMap? = null

    companion object {
        private const val EXTRA_INITIAL_LAT = "extra_initial_lat"
        private const val EXTRA_INITIAL_LNG = "extra_initial_lng"

        const val RESULT_LATITUDE = "result_latitude"
        const val RESULT_LONGITUDE = "result_longitude"

        fun newIntent(context: Context, initialLat: Double, initialLng: Double): Intent =
            Intent(context, MapPointPickerActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_LAT, initialLat)
                putExtra(EXTRA_INITIAL_LNG, initialLng)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPointPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(binding.mapPicker.id) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        binding.locationPickerOverlay.root.visibility = android.view.View.VISIBLE
        binding.locationPickerOverlay.btnCancelPick.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val map = googleMap ?: return@setOnClickListener
            val target = map.cameraPosition.target
            val data = Intent().apply {
                putExtra(RESULT_LATITUDE, target.latitude)
                putExtra(RESULT_LONGITUDE, target.longitude)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isMyLocationButtonEnabled = false

        val lat = intent.getDoubleExtra(EXTRA_INITIAL_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_INITIAL_LNG, 0.0)
        val start = if (lat != 0.0 || lng != 0.0) LatLng(lat, lng) else LatLng(25.0330, 121.5654)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 20f))
    }
}

