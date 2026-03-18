package com.example.taoyuangutter

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
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

    // 最新一版 waypoints（供點擊標記時重開表單用）
    private var currentWaypoints: List<Waypoint> = emptyList()

    /**
     * GutterFormActivity 的 ActivityResultLauncher。
     * 當使用者填完表單並返回時，呼叫 showSelf() 把 BottomSheet 帶回畫面。
     */
    private lateinit var gutterFormLauncher: ActivityResultLauncher<android.content.Intent>

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 必須在 STARTED 之前完成 launcher 的註冊
        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // GutterFormActivity 返回 → 顯示 btnAddGutter、讓 BottomSheet 滑回
            binding.btnAddGutter.visibility = View.VISIBLE
            activeSheet?.showSelf()
        }

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

        // ── 點擊大頭針 → 重開 GutterFormActivity 編輯該點位 ──────────
        map.setOnMarkerClickListener { marker ->
            val wpIndex = marker.tag as? Int ?: return@setOnMarkerClickListener false
            val wp      = currentWaypoints.getOrNull(wpIndex)
                ?: return@setOnMarkerClickListener false
            val latLng  = wp.latLng ?: return@setOnMarkerClickListener false

            val intent = GutterFormActivity.newIntent(
                this,
                arrayListOf(wp.label),
                doubleArrayOf(latLng.latitude),
                doubleArrayOf(latLng.longitude),
                0
            )
            gutterFormLauncher.launch(intent)
            true   // 消費事件，不顯示預設 info window
        }

        // info window 點擊同樣開啟表單（使用者若先點開了 info window 再點選它）
        map.setOnInfoWindowClickListener { marker ->
            val wpIndex = marker.tag as? Int ?: return@setOnInfoWindowClickListener
            val wp      = currentWaypoints.getOrNull(wpIndex) ?: return@setOnInfoWindowClickListener
            val latLng  = wp.latLng ?: return@setOnInfoWindowClickListener

            val intent = GutterFormActivity.newIntent(
                this,
                arrayListOf(wp.label),
                doubleArrayOf(latLng.latitude),
                doubleArrayOf(latLng.longitude),
                0
            )
            gutterFormLauncher.launch(intent)
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
            // 開啟新 sheet 前先清除上一次留下的地圖疊加層與快取 waypoints
            refreshMapOverlay(emptyList())
            currentWaypoints = emptyList()

            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet

            // 當 waypoints 有任何異動（座標確定、拖曳排序、關閉）時重繪地圖疊加層
            sheet.onWaypointsChanged = { waypoints ->
                currentWaypoints = waypoints ?: emptyList()
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

        for ((index, wp) in waypoints.withIndex()) {
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
            // tag = waypoint 的索引，供點擊標記時查詢表單資料
            marker?.tag = index
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
     * 當 AddGutterBottomSheet 點擊「新增側溝」後的回呼。
     * 每個點位在選點確認時已各自開啟表單，這裡只需完成整條路線的提交。
     * 地圖上的標記與連線保留（onDismiss 已保留疊加層）。
     */
    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        activeSheet = null
        binding.btnAddGutter.visibility = View.VISIBLE
        // TODO: 將整條路線的 waypoints 資料送至後端或本機資料庫
    }

    // ── 選點 Overlay 按鈕邏輯 ────────────────────────────────────────────
    private fun setupLocationPickerOverlay() {

        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            // 隱藏 overlay
            binding.locationPickerOverlay.root.visibility = View.GONE

            if (latLng != null && pickingIndex >= 0) {
                // 1. 將座標寫入 waypoint（同步更新地圖疊加層）
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)

                // 2. 取得此點位的標題，開啟單一點位的 GutterFormActivity
                val label = activeSheet?.getWaypointLabel(pickingIndex) ?: "點位"
                val intent = GutterFormActivity.newIntent(
                    this,
                    arrayListOf(label),
                    doubleArrayOf(latLng.latitude),
                    doubleArrayOf(latLng.longitude),
                    0
                )
                // 使用 launcher：表單返回後自動 showSelf()
                gutterFormLauncher.launch(intent)
            } else {
                // 沒有座標或 index 無效時直接回 BottomSheet
                binding.btnAddGutter.visibility = View.VISIBLE
                activeSheet?.showSelf()
            }

            pickingIndex = -1
        }

        // 取消：直接滑回 BottomSheet
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
