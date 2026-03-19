package com.example.taoyuangutter

import android.app.Activity
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

    /**
     * 目前正在開啟 GutterFormActivity 的點位索引（新增模式）。
     * 用於表單返回後，以表單內座標欄位更新大頭針位置。
     */
    private var pendingWaypointFormIndex: Int = -1

    // 檢視線段模式的 BottomSheet
    private var inspectSheet: AddGutterBottomSheet? = null

    // 地圖疊加層
    private val mapMarkers = mutableListOf<Marker>()
    private var mapPolyline: Polyline? = null

    // 最新一版 waypoints（供點擊標記時重開表單用）
    private var currentWaypoints: List<Waypoint> = emptyList()

    // 目前正在檢視的 waypoints（polyline 點擊後從 polyline.tag 讀取）
    private var inspectWaypoints: List<Waypoint> = emptyList()

    /**
     * GutterFormActivity 的 ActivityResultLauncher。
     * - activeSheet != null：新增流程，返回後 showSelf()
     * - activeSheet == null, inspectSheet != null：檢視/編輯流程，返回後更新 waypoint 資料
     */
    private lateinit var gutterFormLauncher: ActivityResultLauncher<android.content.Intent>

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE

            when {
                activeSheet != null -> {
                    // 新增流程：若表單回傳了資料，同步更新 waypoint
                    if (result.resultCode == Activity.RESULT_OK && pendingWaypointFormIndex >= 0) {
                        val data = result.data

                        // ① 以表單填寫的座標更新大頭針位置
                        val newLat = data?.getDoubleExtra(GutterFormActivity.RESULT_LATITUDE,  Double.NaN) ?: Double.NaN
                        val newLng = data?.getDoubleExtra(GutterFormActivity.RESULT_LONGITUDE, Double.NaN) ?: Double.NaN
                        if (!newLat.isNaN() && !newLng.isNaN()) {
                            activeSheet?.updateWaypointLocation(
                                pendingWaypointFormIndex,
                                LatLng(newLat, newLng)
                            )
                        }

                        // ② 將表單基本資料存回 waypoint.basicData（供新增側溝時驗證是否已填寫）
                        val newData = hashMapOf(
                            "gutterId"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID)   ?: ""),
                            "gutterType" to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE) ?: ""),
                            "coordX"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_X)     ?: ""),
                            "coordY"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Y)     ?: ""),
                            "coordZ"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Z)     ?: ""),
                            "measureId"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID)  ?: ""),
                            "depth"      to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_DEPTH)       ?: ""),
                            "topWidth"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH)   ?: ""),
                            "remarks"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_REMARKS)     ?: "")
                        )
                        activeSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
                    }
                    pendingWaypointFormIndex = -1
                    activeSheet?.showSelf()
                }
                inspectSheet != null -> {
                    // 檢視/編輯流程：更新 waypoint basicData，讓 BottomSheet 滑回
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
                        if (idx >= 0) {
                            val newData = hashMapOf(
                                "gutterId"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID)   ?: ""),
                                "gutterType" to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE) ?: ""),
                                "coordX"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_X)     ?: ""),
                                "coordY"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Y)     ?: ""),
                                "coordZ"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Z)     ?: ""),
                                "measureId"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID)  ?: ""),
                                "depth"      to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_DEPTH)       ?: ""),
                                "topWidth"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH)   ?: ""),
                                "remarks"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_REMARKS)     ?: "")
                            )
                            // 同步更新 inspectWaypoints 與 polyline.tag
                            inspectWaypoints.getOrNull(idx)?.basicData = newData
                            @Suppress("UNCHECKED_CAST")
                            (mapPolyline?.tag as? ArrayList<Waypoint>)?.getOrNull(idx)?.basicData = newData
                        }
                    }
                    // 讓 BottomSheet 滑回（保留大頭針顯示）
                    inspectSheet?.showSelf()
                }
                else -> {
                    // 其他情況（不應發生）：清除暫時大頭針
                    clearMapMarkersOnly()
                }
            }
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
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taoyuan, 20f))
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = true
        }

        // 點擊大頭針 → 重開 GutterFormActivity（依目前情境選擇模式）
        map.setOnMarkerClickListener { marker ->
            val wpIndex = marker.tag as? Int ?: return@setOnMarkerClickListener false

            if (inspectSheet != null) {
                // 檢視模式：從 inspectWaypoints 開啟檢視表單
                val wp     = inspectWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                openInspectForm(wpIndex, wp, latLng)
            } else {
                // 新增模式：重開編輯表單
                val wp     = currentWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                pendingWaypointFormIndex = wpIndex  // 記住索引供表單返回時更新座標
                val intent = GutterFormActivity.newIntent(
                    this, arrayListOf(wp.label), doubleArrayOf(latLng.latitude),
                    doubleArrayOf(latLng.longitude), 0
                )
                gutterFormLauncher.launch(intent)
            }
            true
        }

        // 點擊 Polyline → 開啟檢視線段 BottomSheet
        map.setOnPolylineClickListener { polyline ->
            openInspectBottomSheet(polyline)
        }

        map.setOnInfoWindowClickListener { marker ->
            val wpIndex = marker.tag as? Int ?: return@setOnInfoWindowClickListener
            val wp      = currentWaypoints.getOrNull(wpIndex) ?: return@setOnInfoWindowClickListener
            val latLng  = wp.latLng ?: return@setOnInfoWindowClickListener
            pendingWaypointFormIndex = wpIndex
            val intent  = GutterFormActivity.newIntent(
                this, arrayListOf(wp.label), doubleArrayOf(latLng.latitude),
                doubleArrayOf(latLng.longitude), 0
            )
            gutterFormLauncher.launch(intent)
        }
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        activeSheet  = sheet
        pickingIndex = waypointIndex
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        activeSheet = null
        binding.btnAddGutter.visibility = View.VISIBLE
        binding.root.post { clearMapMarkersOnly() }
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        val wp     = inspectWaypoints.getOrNull(waypointIndex) ?: return
        val latLng = wp.latLng ?: return
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openInspectForm(waypointIndex, wp, latLng)
    }

    /** 開啟單一 waypoint 的 GutterFormActivity（檢視模式） */
    private fun openInspectForm(waypointIndex: Int, wp: Waypoint, latLng: LatLng) {
        val intent = GutterFormActivity.newViewIntent(
            this,
            label        = wp.label,
            lat          = latLng.latitude,
            lng          = latLng.longitude,
            waypointIndex = waypointIndex,
            basicData    = wp.basicData
        )
        gutterFormLauncher.launch(intent)
    }

    // ── 點擊 Polyline → 開啟檢視 BottomSheet ────────────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun openInspectBottomSheet(polyline: Polyline) {
        val waypoints = (polyline.tag as? ArrayList<Waypoint>) ?: return
        if (waypoints.isEmpty()) return

        inspectWaypoints = waypoints.toList()

        // 暫時顯示大頭針，方便使用者確認點位位置
        refreshMapMarkersOnly(inspectWaypoints)

        val sheet = AddGutterBottomSheet.newInstanceForInspect()
        inspectSheet = sheet

        sheet.onWaypointsChanged = { wps ->
            if (wps == null) {
                // BottomSheet 關閉 → 清除大頭針
                clearMapMarkersOnly()
                inspectSheet    = null
                inspectWaypoints = emptyList()
            }
            // wps != null（inspectMode onDismiss 不會傳非 null，忽略）
        }

        sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
    }

    // ── 地圖按鈕 ──────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener {
            // TODO: 請求權限並移至當前位置
        }
        binding.btnAddGutter.setOnClickListener {
            refreshMapOverlay(emptyList())
            currentWaypoints = emptyList()

            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet

            sheet.onWaypointsChanged = { waypoints ->
                currentWaypoints = waypoints ?: emptyList()
                refreshMapOverlay(waypoints ?: emptyList())
                if (waypoints == null) activeSheet = null
            }

            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }
    }

    // ── 地圖疊加層：只清除大頭針，保留 polyline ──────────────────────────
    private fun clearMapMarkersOnly() {
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()
    }

    // ── 地圖疊加層：標記 + 連線 ──────────────────────────────────────────
    private fun refreshMapOverlay(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()
        mapPolyline?.remove()
        mapPolyline = null

        if (waypoints.isEmpty()) return

        val routePoints = mutableListOf<LatLng>()

        for ((index, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            routePoints.add(latLng)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(wp.label)
                    .snippet("%.5f, %.5f".format(latLng.latitude, latLng.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type)))
            )
            marker?.tag = index
            marker?.let { mapMarkers.add(it) }
        }

        if (routePoints.size >= 2) {
            mapPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.parseColor("#5C35CC"))
                    .width(10f)
                    .geodesic(true)
                    .clickable(true)
            )
            mapPolyline?.tag = ArrayList(waypoints)
        }
    }

    // ── 重新顯示大頭針（不動 polyline，供檢視模式暫時顯示）──────────────
    private fun refreshMapMarkersOnly(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()

        for ((index, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(wp.label)
                    .snippet("%.5f, %.5f".format(latLng.latitude, latLng.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type)))
            )
            marker?.tag = index
            marker?.let { mapMarkers.add(it) }
        }
    }

    private fun markerHue(type: WaypointType): Float = when (type) {
        WaypointType.START -> BitmapDescriptorFactory.HUE_GREEN
        WaypointType.NODE  -> BitmapDescriptorFactory.HUE_AZURE
        WaypointType.END   -> BitmapDescriptorFactory.HUE_RED
    }

    // ── 選點 Overlay 按鈕邏輯 ────────────────────────────────────────────
    private fun setupLocationPickerOverlay() {
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE

            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
                val label  = activeSheet?.getWaypointLabel(pickingIndex) ?: "點位"
                pendingWaypointFormIndex = pickingIndex   // 記住索引供表單返回時更新座標
                val intent = GutterFormActivity.newIntent(
                    this, arrayListOf(label),
                    doubleArrayOf(latLng.latitude), doubleArrayOf(latLng.longitude), 0
                )
                gutterFormLauncher.launch(intent)
            } else {
                binding.btnAddGutter.visibility = View.VISIBLE
                activeSheet?.showSelf()
            }
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layers, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivRadioNormal    = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioNormal)
        val ivRadioSatellite = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioSatellite)

        // 根據目前地圖模式設定單選框狀態
        fun updateRadio(selectedType: Int) {
            ivRadioNormal.setImageResource(
                if (selectedType == GoogleMap.MAP_TYPE_NORMAL) R.drawable.ic_radio_checked
                else R.drawable.ic_radio_unchecked
            )
            ivRadioSatellite.setImageResource(
                if (selectedType == GoogleMap.MAP_TYPE_SATELLITE) R.drawable.ic_radio_checked
                else R.drawable.ic_radio_unchecked
            )
        }

        updateRadio(googleMap?.mapType ?: GoogleMap.MAP_TYPE_NORMAL)

        dialogView.findViewById<android.view.View>(R.id.rowNormalMap).setOnClickListener {
            googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            dialog.dismiss()
        }
        dialogView.findViewById<android.view.View>(R.id.rowSatelliteMap).setOnClickListener {
            googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            dialog.dismiss()
        }

        dialog.show()
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
