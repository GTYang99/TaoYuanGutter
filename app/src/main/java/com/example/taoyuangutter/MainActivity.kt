package com.example.taoyuangutter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    // ── 定位 ─────────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    // ── Repository ───────────────────────────────────────────────────────
    private val gutterRepository = GutterRepository()

    // ── 目前存活的 BottomSheet 與正在選點的索引 ───────────────────────────
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1

    /** 目前正在開啟 GutterFormActivity 的點位索引（新增模式）。 */
    private var pendingWaypointFormIndex: Int = -1

    // ── 檢視線段模式 ──────────────────────────────────────────────────────
    private var inspectSheet: AddGutterBottomSheet? = null
    private var inspectWaypoints: List<Waypoint> = emptyList()

    // ── 地圖疊加層 ────────────────────────────────────────────────────────

    /**
     * 暫時性大頭針：新增流程選點中 或 檢視模式顯示。
     * 每次 refreshWorkingMarkers / clearWorkingMarkers 時更新。
     */
    private val workingMarkers = mutableListOf<Marker>()

    /** 目前被放大顯示的大頭針所對應的 waypoint index（-1 = 無）。 */
    private var highlightedMarkerIndex: Int = -1

    /**
     * 新增流程中目前繪製中的 Polyline（尚未提交的那條）。
     * 提交或放棄新增後設為 null。
     */
    private var workingPolyline: Polyline? = null

    /**
     * 已提交（暫存）的側溝 Polyline 列表。
     * 關閉 APP 才消失；呼叫 API 拉取後端資料後可一併替換。
     */
    private val submittedPolylines = mutableListOf<Polyline>()

    /** 目前正在操作的 waypoints（新增流程用，供大頭針點擊返回表單）。 */
    private var currentWaypoints: List<Waypoint> = emptyList()

    // ── ActivityResultLauncher ────────────────────────────────────────────
    private lateinit var gutterFormLauncher: ActivityResultLauncher<android.content.Intent>

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 定位權限請求結果
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                enableMyLocationAndMove()
            } else {
                Toast.makeText(this, "需要定位權限才能顯示目前位置", Toast.LENGTH_SHORT).show()
            }
        }

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE

            when {
                activeSheet != null -> {
                    if (result.resultCode == Activity.RESULT_OK && pendingWaypointFormIndex >= 0) {
                        val data = result.data

                        // 以表單填寫的座標更新大頭針位置
                        val newLat = data?.getDoubleExtra(GutterFormActivity.RESULT_LATITUDE,  Double.NaN) ?: Double.NaN
                        val newLng = data?.getDoubleExtra(GutterFormActivity.RESULT_LONGITUDE, Double.NaN) ?: Double.NaN
                        if (!newLat.isNaN() && !newLng.isNaN()) {
                            activeSheet?.updateWaypointLocation(
                                pendingWaypointFormIndex,
                                LatLng(newLat, newLng)
                            )
                        }

                        // 將表單基本資料存回 waypoint.basicData
                        val newData = extractBasicData(result.data)
                        activeSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
                    }
                    resetHighlightedMarker()
                    pendingWaypointFormIndex = -1
                    activeSheet?.showSelf()
                    
                    // 從表單回來後，若有更新座標，自動縮放以看清線段
                    if (currentWaypoints.isNotEmpty()) {
                        fitCameraToWaypoints(currentWaypoints)
                    }
                }
                inspectSheet != null -> {
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
                        if (idx >= 0) {
                            val newData = extractBasicData(data)
                            inspectWaypoints.getOrNull(idx)?.basicData = newData
                            @Suppress("UNCHECKED_CAST")
                            (findPolylineByWaypoints(inspectWaypoints) as? Polyline)
                                ?.let { poly ->
                                    (poly.tag as? ArrayList<Waypoint>)?.getOrNull(idx)?.basicData = newData
                                }
                        }
                    }
                    // 編輯完成後關閉 BottomSheet，回到只顯示線段的地圖
                    inspectSheet?.dismiss()
                }
                else -> clearWorkingMarkers()
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

        // 點擊大頭針
        map.setOnMarkerClickListener { marker ->
            val wpIndex = marker.tag as? Int ?: return@setOnMarkerClickListener false

            if (inspectSheet != null) {
                val wp     = inspectWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                openInspectForm(wpIndex, wp, latLng)
            } else {
                val wp     = currentWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                pendingWaypointFormIndex = wpIndex
                openAddForm(wpIndex, wp, latLng)
            }
            true
        }

        // 點擊 Polyline → 檢視模式
        map.setOnPolylineClickListener { polyline ->
            openInspectBottomSheet(polyline)
        }
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        activeSheet  = sheet
        pickingIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        // 先清空回呼，避免後續 onDismiss 觸發 refreshWorkingLayer 在 submitted polyline 上疊繪
        activeSheet?.onWaypointsChanged = null
        workingPolyline?.remove()
        workingPolyline = null
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE

        drawSubmittedGutter(waypoints)

        lifecycleScope.launch {
            when (val result = gutterRepository.submitGutter(waypoints)) {
                is ApiResult.Success -> {
                    val msg = result.data.message ?: "側溝資料上傳成功"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    val code = result.code?.let { "（$it）" } ?: ""
                    Toast.makeText(this@MainActivity, "上傳失敗$code：${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        val wp     = inspectWaypoints.getOrNull(waypointIndex) ?: return
        val latLng = wp.latLng ?: return
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openInspectForm(waypointIndex, wp, latLng)
    }

    /** 檢視現有側溝點位 */
    private fun openInspectForm(waypointIndex: Int, wp: Waypoint, latLng: LatLng) {
        moveCameraToLatLngOffset(latLng)
        val intent = GutterFormActivity.newViewIntent(
            this,
            label         = wp.label,
            lat           = latLng.latitude,
            lng           = latLng.longitude,
            waypointIndex = waypointIndex,
            basicData     = wp.basicData
        )
        gutterFormLauncher.launch(intent)
    }

    /** 新增模式：開啟表單（包含跳轉下一個點位的邏輯） */
    private fun openAddForm(currentIndex: Int, wp: Waypoint, latLng: LatLng) {
        moveCameraToLatLngOffset(latLng)
        
        val labels = ArrayList(currentWaypoints.map { it.label })
        val lats   = currentWaypoints.map { it.latLng?.latitude ?: 0.0 }.toDoubleArray()
        val lngs   = currentWaypoints.map { it.latLng?.longitude ?: 0.0 }.toDoubleArray()

        val intent = GutterFormActivity.newIntent(
            this,
            labels, lats, lngs, currentIndex, wp.basicData
        )
        gutterFormLauncher.launch(intent)
    }

    // ── 地圖鏡頭自動定位 ─────────────────────────────────────────────────

    private fun moveCameraToLatLngOffset(latLng: LatLng, zoom: Float = 20f) {
        val map = googleMap ?: return
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                val screenH = resources.displayMetrics.heightPixels
                val targetY = screenH / 8
                val newPoint = map.projection.toScreenLocation(latLng)
                val dy = newPoint.y - targetY
                map.animateCamera(CameraUpdateFactory.scrollBy(0f, dy.toFloat()))
            }
            override fun onCancel() {}
        })
    }

    private fun fitCameraToWaypoints(
        waypoints: List<Waypoint>,
        bottomOffsetRatio: Double = 0.52
    ) {
        val map    = googleMap ?: return
        val points = waypoints.mapNotNull { it.latLng }
        if (points.isEmpty()) return

        val dm      = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val mapVisibleH = ((1.0 - bottomOffsetRatio) * screenH).toInt()
        val padding = (64 * dm.density).toInt()

        if (points.size == 1) {
            moveCameraToLatLngOffset(points[0])
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, screenW, mapVisibleH, padding), object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    val offsetPx = (screenH * bottomOffsetRatio / 2).toInt()
                    map.animateCamera(CameraUpdateFactory.scrollBy(0f, offsetPx.toFloat()))
                }
                override fun onCancel() {}
            })
        } catch (e: Exception) {
            map.setOnMapLoadedCallback { fitCameraToWaypoints(waypoints, bottomOffsetRatio) }
        }
    }

    private fun fitCameraToAllGutters() {
        val allWaypoints = mutableListOf<Waypoint>()
        submittedPolylines.forEach { poly ->
            @Suppress("UNCHECKED_CAST")
            val wps = poly.tag as? ArrayList<Waypoint>
            if (wps != null) allWaypoints.addAll(wps)
        }
        if (allWaypoints.isNotEmpty()) {
            fitCameraToWaypoints(allWaypoints)
        }
    }

    // ── 定位功能 ──────────────────────────────────────────────────────────

    private fun requestLocationAndMove() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndMove()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndMove() {
        val map = googleMap ?: return
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 18f))
                }
            }
    }

    // ── 點擊 Polyline → 開啟檢視 BottomSheet ─────────────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun openInspectBottomSheet(polyline: Polyline) {
        val waypoints = (polyline.tag as? ArrayList<Waypoint>) ?: return
        if (waypoints.isEmpty()) return

        inspectWaypoints = waypoints.toList()
        refreshWorkingMarkers(inspectWaypoints)
        
        val sheet = AddGutterBottomSheet.newInstanceForInspect()
        inspectSheet = sheet

        sheet.onWaypointsChanged = { wps ->
            if (wps == null) {
                clearWorkingMarkers()
                inspectSheet     = null
                inspectWaypoints = emptyList()
            }
        }

        sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        binding.root.post { fitCameraToWaypoints(inspectWaypoints) }
    }

    // ── 地圖按鈕 ──────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener { requestLocationAndMove() }
        binding.btnAddGutter.setOnClickListener {
            workingPolyline?.remove()
            workingPolyline = null
            clearWorkingMarkers()
            currentWaypoints = emptyList()

            fitCameraToAllGutters()

            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet

            sheet.onWaypointsChanged = { waypoints ->
                currentWaypoints = waypoints ?: emptyList()
                refreshWorkingLayer(waypoints ?: emptyList())
                if (waypoints == null) activeSheet = null
            }

            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }
    }

    // ── Working 層 ──────────────────────────────────────────────────────

    private fun refreshWorkingLayer(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        workingPolyline?.remove()
        workingPolyline = null

        if (waypoints.isEmpty()) return

        val routePoints = mutableListOf<LatLng>()
        for ((index, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            routePoints.add(latLng)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type)))
            )
            marker?.tag = index
            marker?.let { workingMarkers.add(it) }
        }

        if (routePoints.size >= 2) {
            workingPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.parseColor("#5C35CC"))
                    .width(10f)
                    .geodesic(true)
                    .clickable(false)
            )
        }
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        for ((index, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type)))
            )
            marker?.tag = index
            marker?.let { workingMarkers.add(it) }
        }
    }

    private fun clearWorkingMarkers() {
        workingMarkers.forEach { it.remove() }
        workingMarkers.clear()
        highlightedMarkerIndex = -1
    }

    private fun drawSubmittedGutter(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return

        val polyline = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(Color.parseColor("#5C35CC"))
                .width(10f)
                .geodesic(true)
                .clickable(true)
        )
        polyline.tag = ArrayList(waypoints)
        submittedPolylines.add(polyline)
    }

    private fun findPolylineByWaypoints(waypoints: List<Waypoint>): Polyline? {
        return submittedPolylines.firstOrNull { poly ->
            val tagList = poly.tag as? ArrayList<*>
            tagList != null && tagList.size == waypoints.size
        }
    }

    private fun highlightMarker(waypointIndex: Int) {
        resetHighlightedMarker()
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(waypointIndex) ?: return
        if (wp.latLng == null) return
        highlightedMarkerIndex = waypointIndex
        workingMarkers.firstOrNull { it.tag == waypointIndex }?.let { marker ->
            marker.setIcon(createEnlargedMarkerIcon(wp.type))
            marker.zIndex = 1f
        }
    }

    private fun resetHighlightedMarker() {
        if (highlightedMarkerIndex < 0) return
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(highlightedMarkerIndex)
        workingMarkers.firstOrNull { it.tag == highlightedMarkerIndex }?.let { marker ->
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerHue(wp?.type ?: WaypointType.NODE)))
            marker.zIndex = 0f
        }
        highlightedMarkerIndex = -1
    }

    private fun createEnlargedMarkerIcon(type: WaypointType): com.google.android.gms.maps.model.BitmapDescriptor {
        val dp = resources.displayMetrics.density
        val w  = (48 * dp).toInt()
        val h  = (76 * dp).toInt()
        val r  = w / 2f
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillColor = when (type) {
            WaypointType.START -> Color.rgb(0x34, 0xA8, 0x53)
            WaypointType.NODE  -> Color.rgb(0x42, 0x85, 0xF4)
            WaypointType.END   -> Color.rgb(0xEA, 0x43, 0x35)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawCircle(r, r, r, paint)
        val tail = Path().apply {
            moveTo(r * 0.35f, r * 1.55f)
            lineTo(r, h.toFloat())
            lineTo(r * 1.65f, r * 1.55f)
            close()
        }
        canvas.drawPath(tail, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(r, r, r * 0.38f, paint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun markerHue(type: WaypointType): Float = when (type) {
        WaypointType.START -> BitmapDescriptorFactory.HUE_GREEN
        WaypointType.NODE  -> BitmapDescriptorFactory.HUE_AZURE
        WaypointType.END   -> BitmapDescriptorFactory.HUE_RED
    }

    private fun setupLocationPickerOverlay() {
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE
            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
                val label  = activeSheet?.getWaypointLabel(pickingIndex) ?: "點位"
                pendingWaypointFormIndex = pickingIndex
                
                // 找到對應的 waypoint 並預填資料（若有）
                val wp = currentWaypoints.getOrNull(pickingIndex) ?: Waypoint(WaypointType.NODE, label)
                openAddForm(pickingIndex, wp, latLng)
            } else {
                binding.btnAddGutter.visibility = View.VISIBLE
                activeSheet?.showSelf()
            }
            pickingIndex = -1
        }
        binding.locationPickerOverlay.btnCancelPick.setOnClickListener {
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE
            resetHighlightedMarker()
            activeSheet?.showSelf()
            pickingIndex = -1
        }
    }

    private fun showMapTypeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layers, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivRadioNormal    = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioNormal)
        val ivRadioSatellite = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioSatellite)

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
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun extractBasicData(data: android.content.Intent?): HashMap<String, String> =
        hashMapOf(
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
}
