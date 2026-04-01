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
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isMyLocationButtonEnabled = false

        // 跟主地圖一致：關閉 Google 預設底圖，改用 NLSC WMTS 圖層
        map.mapType = GoogleMap.MAP_TYPE_NONE
        setWmtsTiles(intent.getStringExtra(EXTRA_WMTS_LAYER) ?: "EMAP")

        drawExistingWaypoints(map)

        val lat = intent.getDoubleExtra(EXTRA_INITIAL_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_INITIAL_LNG, 0.0)
        val start = if (lat != 0.0 || lng != 0.0) LatLng(lat, lng) else LatLng(25.0330, 121.5654)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 20f))
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

            // 新增模式：只顯示「其他已選好的點位」
            if (!isEditMode && idx == currentIndex) {
                return@forEachIndexed
            }

            val pos = LatLng(lat, lng)
            val isCurrent = idx == currentIndex
            val type = WaypointType.entries.firstOrNull { it.name == wp.type } ?: WaypointType.NODE
            val icon = when {
                // 修改狀態下才需要 highlight 當前點位
                isEditMode && isCurrent -> MarkerIconFactory.enlarged(this, type)
                else -> MarkerIconFactory.normal(this, type)
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(wp.label)
                    .icon(icon)
            )
            marker?.let { markers.add(it) }

            if (isEditMode) {
                pointsForLine.add(pos)
            }
        }

        // 修改狀態：畫出原本側溝的 polyline（依 waypoint 列表順序連線）
        if (isEditMode && pointsForLine.size >= 2) {
            polyline = map.addPolyline(
                PolylineOptions()
                    .addAll(pointsForLine)
                    .width(10f)
                    .geodesic(true)
                    .color(android.graphics.Color.parseColor("#5C35CC"))
                    .clickable(false)
            )
        }
    }

    private fun setWmtsTiles(layer: String) {
        currentTileOverlay?.remove()
        val safeLayer = when (layer.uppercase()) {
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
