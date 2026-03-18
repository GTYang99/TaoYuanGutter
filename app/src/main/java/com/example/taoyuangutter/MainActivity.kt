package com.example.taoyuangutter

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    // 目前存活的 BottomSheet 與正在選點的索引
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1

    // 地圖疊加層：路線標記與連線
    private val mapMarkers = mutableListOf<Marker>()
    private var mapPolyline: Polyline? = null

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
        setupLocationPickerOverlay()
    }

    // ── Google Map ────────────────────────────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val taoyuan = LatLng(24.9936, 121.3010)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taoyuan, 13f))
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = true
        }
    }

    // ── 地圖按鈕 ──────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener {
            // TODO: 請求權限並移至當前位置
        }
        binding.btnAddGutter.setOnClickListener {
            // 開啟新 sheet 前先清除上一次留下的地圖疊加層
            refreshMapOverlay(emptyList())

            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet

            // 當 waypoints 有任何異動（座標確定、拖曳排序、關閉）時重繪地圖疊加層
            sheet.onWaypointsChanged = { waypoints ->
                refreshMapOverlay(waypoints ?: emptyList())
                if (waypoints == null) activeSheet = null
            }

            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }
    }

    // ── 地圖疊加層：標記 + 連線 ──────────────────────────────────────────
    private fun refreshMapOverlay(waypoints: List<Waypoint>) {
        val map = googleMap ?: return

        // 清除舊疊加層
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()
        mapPolyline?.remove()
        mapPolyline = null

        if (waypoints.isEmpty()) return

        val routePoints = mutableListOf<LatLng>()

        for (wp in waypoints) {
            val latLng = wp.latLng ?: continue   // 尚未設定座標的點跳過

            routePoints.add(latLng)

            val hue = when (wp.type) {
                WaypointType.START -> BitmapDescriptorFactory.HUE_GREEN
                WaypointType.NODE  -> BitmapDescriptorFactory.HUE_AZURE
                WaypointType.END   -> BitmapDescriptorFactory.HUE_RED
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(wp.label)
                    .snippet("%.5f, %.5f".format(latLng.latitude, latLng.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
            marker?.let { mapMarkers.add(it) }
        }

        // 至少兩個已確定座標的點才畫連線
        if (routePoints.size >= 2) {
            mapPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.parseColor("#5C35CC"))   // brand_purple
                    .width(10f)
                    .geodesic(true)
            )
        }
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    override fun startLocationPick(
        sheet: AddGutterBottomSheet,
        waypointIndex: Int
    ) {
        activeSheet  = sheet
        pickingIndex = waypointIndex

        // 隱藏 BottomSheet，以地圖為主
        sheet.hideSelf()

        // 隱藏 + FAB，顯示 overlay
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
    }

    /**
     * 當 AddGutterBottomSheet 點擊「提交」後的回呼。
     * 這裡是流程結尾：回到地圖畫面（清除疊加層，或是依需求跳轉）。
     */
    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        // 1. 清除地圖上的規劃線（代表任務已結束）
        refreshMapOverlay(emptyList())

        // 2. 顯示成功提示或跳轉至表單畫面
        // 目前實作：回到地圖初始狀態
        activeSheet = null
        
        // 如果您有 fragment_form 且想跳過去：
        // findNavController(R.id.nav_host_fragment).navigate(R.id.action_map_to_form)
    }

    // ── 選點 Overlay 按鈕邏輯 ────────────────────────────────────────────
    private fun setupLocationPickerOverlay() {
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE

            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
            }

            activeSheet?.showSelf()
            pickingIndex = -1
        }

        binding.locationPickerOverlay.btnCancelPick.setOnClickListener {
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE
            activeSheet?.showSelf()
            pickingIndex = -1
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────
    private fun showMapTypeDialog() {
        val options = arrayOf(getString(R.string.map_normal), getString(R.string.map_satellite))
        AlertDialog.Builder(this)
            .setTitle(R.string.layers_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
            }
            .show()
    }

    private fun showLegendDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_legend, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
