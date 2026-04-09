package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.databinding.ActivityMapPointPickerBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.taoyuangutter.map.MarkerIconFactory
import com.example.taoyuangutter.gutter.WaypointType
import java.net.MalformedURLException
import java.net.URL

class MapPointPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapPointPickerBinding
    private var googleMap: GoogleMap? = null
    private var currentTileOverlay: TileOverlay? = null

    // ── 現在位置 ──────────────────────────────────────────────────────────
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocationAndJump()
        } else {
            Toast.makeText(this, getString(R.string.msg_location_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_INITIAL_LAT = "extra_initial_lat"
        private const val EXTRA_INITIAL_LNG = "extra_initial_lng"
        private const val EXTRA_WMTS_LAYER = "extra_wmts_layer"
        private const val EXTRA_SESSION_WAYPOINTS_JSON = "extra_session_waypoints_json"
        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
        private const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"

        const val RESULT_LATITUDE = "result_latitude"
        const val RESULT_LONGITUDE = "result_longitude"

        fun newIntent(
            context: Context,
            initialLat: Double,
            initialLng: Double,
            wmtsLayer: String? = null,
            sessionWaypointsJson: String? = null,
            currentIndex: Int = 0,
            isEditMode: Boolean = false
        ): Intent =
            Intent(context, MapPointPickerActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_LAT, initialLat)
                putExtra(EXTRA_INITIAL_LNG, initialLng)
                if (!wmtsLayer.isNullOrEmpty()) putExtra(EXTRA_WMTS_LAYER, wmtsLayer)
                if (!sessionWaypointsJson.isNullOrEmpty()) putExtra(EXTRA_SESSION_WAYPOINTS_JSON, sessionWaypointsJson)
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
                putExtra(EXTRA_IS_EDIT_MODE, isEditMode)
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

        binding.locationPickerOverlay.fabMyLocation.setOnClickListener {
            onMyLocationButtonClicked()
        }
    }

    private fun onMyLocationButtonClicked() {
        val fineGranted  = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            enableMyLocationAndJump()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @Suppress("MissingPermission")
    private fun enableMyLocationAndJump() {
        val map = googleMap ?: return
        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) { /* permission revoked between check and call */ }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val here = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 18f))
            } else {
                Toast.makeText(this, getString(R.string.msg_location_not_available), Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, String.format(getString(R.string.msg_location_failed), it.message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false

        // 跟主地圖一致：關閉 Google 預設底圖，改用 NLSC WMTS 圖層
        map.mapType = GoogleMap.MAP_TYPE_NONE
        setWmtsTiles(intent.getStringExtra(EXTRA_WMTS_LAYER) ?: "EMAP")

        drawExistingWaypoints(map)

        val lat = intent.getDoubleExtra(EXTRA_INITIAL_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_INITIAL_LNG, 0.0)

        if (lat != 0.0 || lng != 0.0) {
            val start = LatLng(lat, lng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 20f))
        } else {
            // 如果沒有初始座標：先以桃園作為初始鏡頭，再嘗試定位到使用者現在位置
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(23.9929, 121.3011), 16f))
            onMyLocationButtonClicked()
        }
    }

    private val markers = mutableListOf<Marker>()
    private var polyline: Polyline? = null

    private fun drawExistingWaypoints(map: GoogleMap) {
        // 清除舊疊加層（若 activity 因重建而重跑）
        markers.forEach { it.remove() }
        markers.clear()
        polyline?.remove()
        polyline = null

        val json = intent.getStringExtra(EXTRA_SESSION_WAYPOINTS_JSON) ?: return
        val waypoints: List<WaypointSnapshot> = runCatching {
            val type = object : TypeToken<List<WaypointSnapshot>>() {}.type
            Gson().fromJson<List<WaypointSnapshot>>(json, type)
        }.getOrNull() ?: return

        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        val isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)

        val pointsForLine = mutableListOf<LatLng>()
        waypoints.forEachIndexed { idx, wp ->
            val lat = wp.latitude
            val lng = wp.longitude
            if (lat == null || lng == null) return@forEachIndexed

            val isCurrent = idx == currentIndex

            // 非 edit mode 下，當前點位「尚未選點（0.0/0.0 sentinel）」才跳過。
            // 若已有有效座標（回頭修改既有點位），仍顯示讓使用者參考。
            if (!isEditMode && isCurrent && lat == 0.0 && lng == 0.0) {
                return@forEachIndexed
            }

            val pos = LatLng(lat, lng)
            val type = WaypointType.entries.firstOrNull { it.name == wp.type } ?: WaypointType.NODE
            // 當前點位一律放大 highlight，無論是否 edit mode
            val icon = if (isCurrent) MarkerIconFactory.enlarged(this, type)
                       else           MarkerIconFactory.normal(this, type)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(wp.label)
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
            )
            marker?.let { markers.add(it) }

            // 有效座標的點位都加入 polyline（不限 edit mode）
            pointsForLine.add(pos)
        }

        // 有 2 個以上有效點位時，畫出行程線段（不限 edit mode）
        if (pointsForLine.size >= 2) {
            polyline = map.addPolyline(
                PolylineOptions()
                    .addAll(pointsForLine)
                    .width(10f)
                    .geodesic(true)
                    .color(android.graphics.Color.parseColor("#562ECB"))
                    .clickable(false)
            )
        }
    }

    private fun setWmtsTiles(layer: String) {
        currentTileOverlay?.remove()
        val safeLayer = when (layer.uppercase()) {
            "EMAP01" -> "EMAP01"
            "PHOTO2" -> "PHOTO2"
            else -> "EMAP"
        }
        val urlTemplate = "https://wmts.nlsc.gov.tw/wmts/$safeLayer/default/GoogleMapsCompatible/%d/%d/%d"
        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = try {
                URL(String.format(urlTemplate, zoom, y, x))
            } catch (e: MalformedURLException) { null }
        }
        currentTileOverlay = googleMap?.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider).zIndex(-1f)
        )
    }
}
