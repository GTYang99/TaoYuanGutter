package com.example.taoyuangutter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.example.taoyuangutter.common.LocationPickEvents
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.google.gson.Gson
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddCurveActivity
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.AddOptionBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.GutterInspectActivity
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.map.DistanceMeasureManager
import com.example.taoyuangutter.map.LayersBottomSheet
import com.example.taoyuangutter.map.LegendBottomSheet
import com.example.taoyuangutter.map.MeasureConfig
import com.example.taoyuangutter.map.Wms3857TileProvider
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.KIND_CURVE
import com.example.taoyuangutter.pending.PendingDraftsBottomSheet
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import kotlin.collections.map

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost,
    LayersBottomSheet.Host {

    private enum class MapMode { EMAP, EMAP01, PHOTO2 }

    companion object {
        private const val KEY_PENDING_WP_INDEX = "pending_wp_index"
        private const val GUTTER_LOAD_DEBOUNCE_MS = 500L
        const val EXTRA_OFFLINE_MAIN = "extra_offline_main"
    }

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private var isOfflineMainMode: Boolean = false

    private val waypointLocationChangedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED) return
            val draftId = intent.getLongExtra(LocationPickEvents.EXTRA_SESSION_DRAFT_ID, 0L)
            val index = intent.getIntExtra(LocationPickEvents.EXTRA_WAYPOINT_INDEX, -1)
            val lat = intent.getDoubleExtra(LocationPickEvents.EXTRA_LATITUDE, Double.NaN)
            val lng = intent.getDoubleExtra(LocationPickEvents.EXTRA_LONGITUDE, Double.NaN)
            if (draftId <= 0L) return
            if (draftId != currentSessionDraftId) return
            if (index < 0) return
            if (lat.isNaN() || lng.isNaN()) return
            val sheet = activeSheet ?: return
            sheet.updateWaypointLocation(index, LatLng(lat, lng))

            // 表單仍開著時，把背景地圖鏡頭 fit 到整條工作線段，
            // 底部預留空間避免被表單遮住（不要立刻重設 padding）。
            fitCameraToWaypoints(
                sheet.getWaypoints(),
                bottomOffsetRatio = 0.8,
                resetPaddingAfter = false,
                maxZoom = 19f,
                paddingDp = 24
            )
        }
    }

    // ── NLSC WMTS 底圖 ────────────────────────────────────────────────────
    private var currentTileOverlay: TileOverlay? = null
    private var currentMapMode = MapMode.EMAP

    // ── WMS overlays ─────────────────────────────────────────────────────
    private var showPlanOverlay = true
    private var showWaterOldOverlay = true
    private var showPossibleOverlay = true
    private var planWmsOverlay: TileOverlay? = null
    private var waterOldWmsOverlay: TileOverlay? = null

    // ── 定位 ─────────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    // ── Repository ───────────────────────────────────────────────────────
    private val gutterRepository = GutterRepository()
    private val sessionDraftRepository by lazy { GutterSessionRepository(this) }

    /**
     * 目前進行中的新增／編輯 session 所對應的 [GutterSessionDraft] ID。
     * - LocationPicker 確認座標或表單填寫返回時，由 [autoSaveSessionDraft] 建立並記住此 ID。
     * - 恢復草稿時設為已知草稿的 ID，讓後續自動更新覆蓋同一筆。
     * - 成功上傳（[onGutterSaved]）後重設為 null。
     */
    private var currentSessionDraftId: Long? = null
    /** true = 此次 session 為離線草稿（只存本機，不打 API） */
    private var currentSessionIsOffline: Boolean = false

    // ── 目前存活的 BottomSheet 與正在選點的索引 ───────────────────────────
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1

    /** 目前正在開啟 GutterFormActivity 的點位索引（新增模式）。 */
    private var pendingWaypointFormIndex: Int = -1

    // ── 檢視線段模式 ──────────────────────────────────────────────────────
    private var inspectSheet: AddGutterBottomSheet? = null
    private var inspectWaypoints: List<Waypoint> = emptyList()
    /** 防止連點側溝 Polyline 重複觸發 openInspectBottomSheet */
    private var isInspecting = false

    // ── 編輯/檢視/新增模式標誌（防止自動加載polylines） ────────────────────
    /** true = 正在編輯/檢視/新增模式，禁止 loadGuttersByViewport 自動加載 */
    private var isInEditingMode = false

    // ── 地圖疊加層 ────────────────────────────────────────────────────────
    private val workingMarkers = mutableListOf<Marker>()
    private var highlightedMarkerIndex: Int = -1
    private var workingPolyline: Polyline? = null
    private val submittedPolylines = mutableListOf<Polyline>()
    /** scopeSearch 從後端載入的 Polyline（依 SPI_NUM 索引，方便重繪時移除舊線段） */
    private val scopePolylines = mutableMapOf<String, Polyline>()
    private var currentWaypoints: List<Waypoint> = emptyList()

    // ── 防抖機制（避免重複調用 scopeSearch API） ────────────────────
    private var lastGutterLoadTime = 0L

    // ── 測距模式 ──────────────────────────────────────────────────────────────
    /**
     * 客製化測距圖示設定。
     * 若要替換起點大頭針或準星圖示，在此修改即可：
     *   measureConfig = MeasureConfig(
     *       startMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
     *       crosshairResId  = R.drawable.my_crosshair
     *   )
     */
    var measureConfig = MeasureConfig()
    private var measureManager: DistanceMeasureManager? = null

    private lateinit var gutterFormLauncher: ActivityResultLauncher<Intent>
    private lateinit var inspectLauncher: ActivityResultLauncher<Intent>
    private lateinit var addCurveLauncher: ActivityResultLauncher<Intent>

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isOfflineMainMode = intent.getBooleanExtra(EXTRA_OFFLINE_MAIN, false)

        ContextCompat.registerReceiver(
            this,
            waypointLocationChangedReceiver,
            android.content.IntentFilter(LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED),
            RECEIVER_NOT_EXPORTED
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) enableMyLocationAndMove()
        }

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE

            when {
                activeSheet != null -> {
                    when (result.resultCode) {
                        Activity.RESULT_OK -> if (pendingWaypointFormIndex >= 0) {
                            val data = result.data

                            // ── 更新地圖定位座標 ────────────────────────────────
                            // 優先使用 GutterFormActivity 回傳的 lat/lng（已保證永遠有值）；
                            // 若萬一仍為 NaN（例如舊版快取），退回使用 currentWaypoints 中
                            // 地圖選點時已儲存的座標，確保大頭針不會消失。
                            val resultLat = data?.getDoubleExtra(GutterFormActivity.RESULT_LATITUDE,  Double.NaN) ?: Double.NaN
                            val resultLng = data?.getDoubleExtra(GutterFormActivity.RESULT_LONGITUDE, Double.NaN) ?: Double.NaN
                            val fallbackLatLng = currentWaypoints.getOrNull(pendingWaypointFormIndex)?.latLng
                            val effectiveLat = if (!resultLat.isNaN()) resultLat else fallbackLatLng?.latitude  ?: Double.NaN
                            val effectiveLng = if (!resultLng.isNaN()) resultLng else fallbackLatLng?.longitude ?: Double.NaN
                            if (!effectiveLat.isNaN() && !effectiveLng.isNaN()) {
                                activeSheet?.updateWaypointLocation(pendingWaypointFormIndex, LatLng(effectiveLat, effectiveLng))
                            }

                            // ── 更新表單填寫的基本資料 ──────────────────────────
                            val newData = extractBasicData(result.data)
                            activeSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
                        }
                        GutterFormActivity.RESULT_DELETE -> if (pendingWaypointFormIndex >= 0) {
                            // 使用者放棄填寫 → 清除該點位的座標與資料（同時更新地圖大頭針）
                            activeSheet?.clearWaypointLocation(pendingWaypointFormIndex)
                        }
                    }
                    resetHighlightedMarker()
                    pendingWaypointFormIndex = -1
                    activeSheet?.showSelf()
                    if (currentWaypoints.isNotEmpty()) fitCameraToWaypoints(currentWaypoints)
                }
                inspectSheet != null -> {
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
                        if (idx >= 0) {
                            val newData = extractBasicData(data)
                            inspectWaypoints.getOrNull(idx)?.basicData = newData
                        }
                    }
                    inspectSheet?.showSelf()
                    fitCameraToWaypoints(inspectWaypoints)
                }
                else -> clearWorkingMarkers()
            }
        }

    // ── GutterInspectActivity 的 launcher ────────────────────────────
        inspectLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            isInspecting = false   // GutterInspectActivity 已返回，允許再次點擊側溝
            if (result.resultCode == GutterInspectActivity.RESULT_EDIT_DITCH) {
                val json   = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_WAYPOINTS_JSON) ?: return@registerForActivityResult
                val spiNum = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_NUM) ?: ""

                // ── 若 FM 內仍有同 TAG 的舊 sheet（例如先前新增流程的 activeSheet）
                //    必須先 dismiss 並等待事務完成，否則 show() 會因 TAG 衝突而失敗 ──
                (supportFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG)
                        as? AddGutterBottomSheet)
                    ?.dismissAllowingStateLoss()
                supportFragmentManager.executePendingTransactions()
                activeSheet = null

                // 直接以 WaypointSnapshot JSON 建立編輯模式的 BottomSheet
                val wpsType = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
                val snapshots: List<WaypointSnapshot> = try {
                    Gson().fromJson(json, wpsType)
                } catch (e: Exception) {
                    emptyList()
                }

                val wps = snapshots.map { snap ->
                    val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                    val latLng = if (snap.latitude != null && snap.longitude != null)
                        LatLng(snap.latitude, snap.longitude) else null
                    Waypoint(wpType, snap.label, latLng, snap.basicData)
                }

                currentSessionDraftId = null   // 編輯模式開始 → 以新 session ID 追蹤草稿

                // ── 進入編輯模式時：隱藏所有其他已存在的線段，只顯示正在編輯的側溝 ──
                isInEditingMode = true  // 禁止自動加載 polylines
                scopePolylines.values.forEach { it.remove() }
                scopePolylines.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()

                val sheet = AddGutterBottomSheet.newInstanceForEdit(wps, spiNum)
                var lastWaypointsSize = wps.size
                sheet.onWaypointsChanged = { updated ->
                    if (updated == null) {
                        // ── 編輯 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
                        isInEditingMode = false  // 允許自動加載 polylines
                        googleMap?.setPadding(0, 0, 0, 0)
                        clearWorkingMarkers()
                        activeSheet = null
                        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
                        loadGuttersByViewport()
                    } else {
                        val shouldRefit = updated.size > lastWaypointsSize
                        lastWaypointsSize = updated.size
                        currentWaypoints = updated.toMutableList()
                        refreshWorkingLayer(updated)
                        autoSaveSessionDraft(updated)
                        if (shouldRefit) {
                            fitCameraToWaypoints(updated, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
                        }
                    }
                }
                activeSheet = sheet
                sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)

                // 初始化地圖：繪製線段大頭針並將視角自動對齊至整條側溝
                currentWaypoints = wps.toMutableList()
                refreshWorkingLayer(wps)
                fitCameraToWaypoints(wps)
                // fitCameraToWaypoints 會觸發 setOnCameraIdleListener → loadGuttersByViewportDebounced()
            } else {
                // ── 從檢視模式返回（不編輯）時，清除起終點標記並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                clearWorkingMarkers()   // 移除檢視模式新增的起點／節點／終點標記
                loadGuttersByViewport()
            }
        }

        // ── AddCurveActivity 的 launcher（新增曲線側溝）────────────────────
        addCurveLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 新增成功 → 重新加載地圖線段
                loadGuttersByViewport()
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
        setupLocationPickerOverlay()

        // ── 系統重建後恢復狀態（相機 Activity 期間 MainActivity 被殺掉時觸發）──
        if (savedInstanceState != null) {
            restoreStateAfterRecreation(savedInstanceState)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(waypointLocationChangedReceiver) }
        super.onDestroy()
    }

    /**
     * 在系統重建（拍照流程期間記憶體不足）後恢復關鍵狀態：
     * - pendingWaypointFormIndex：讓 gutterFormLauncher 知道要更新哪個點位
     * - activeSheet / inspectSheet：重新綁定已被 FragmentManager 恢復的 BottomSheet
     */
    private fun restoreStateAfterRecreation(savedState: Bundle) {
        pendingWaypointFormIndex = savedState.getInt(KEY_PENDING_WP_INDEX, -1)

        val restoredSheet = supportFragmentManager
            .findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet
            ?: return

        if (restoredSheet.isAddMode()) {
            // 新增模式：重新綁定 activeSheet 與 onWaypointsChanged
            isInEditingMode = true  // 保持禁止自動加載 polylines
            activeSheet = restoredSheet
            restoredSheet.onWaypointsChanged = { wps ->
                currentWaypoints = wps ?: emptyList()
                refreshWorkingLayer(wps ?: emptyList())
                if (wps != null) {
                    autoSaveSessionDraft(wps)
                } else {
                    // ── 取消新增模式時，清除工作層並恢復其他線段顯示 ──
                    isInEditingMode = false  // 允許自動加載 polylines
                    googleMap?.setPadding(0, 0, 0, 0)
                    activeSheet = null
                    // 重新加載所有線段（scopePolylines 與 submittedPolylines）
                    loadGuttersByViewport()
                }
            }
        } else {
            // 檢視模式：重新綁定 inspectSheet
            isInEditingMode = true  // 保持禁止自動加載 polylines
            inspectSheet = restoredSheet
            restoredSheet.onWaypointsChanged = { if (it == null) {
                // ── 檢視 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                clearWorkingMarkers()
                inspectSheet = null
                // 重新加載所有線段（scopePolylines 與 submittedPolylines）
                loadGuttersByViewport()
            } }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 儲存正在開啟表單的點位索引，確保 MainActivity 重建後仍能正確回寫
        outState.putInt(KEY_PENDING_WP_INDEX, pendingWaypointFormIndex)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // 關閉 Google 預設底圖，改用 NLSC WMTS 圖層
        googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
        setMapTiles(MapMode.EMAP)
        applyWmsOverlays()

        // 避免地圖初始化時短暫跳到 (0,0) 或不合理位置：先以桃園作為初始鏡頭
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(24.9929, 121.3011), 16f))
        requestLocationAndMove()

        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = false
            isZoomControlsEnabled = false
        }

        map.setOnMarkerClickListener { marker ->
            // 測距模式：大頭針點擊視為設定測距起點，不開啟表單
            if (measureManager?.isMeasuring == true) {
                measureManager?.setStartPoint(marker.position)
                return@setOnMarkerClickListener true
            }
            val wpIndex = marker.tag as? Int ?: return@setOnMarkerClickListener false
            if (inspectSheet != null) {
                val wp = inspectWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                openInspectForm(wpIndex, wp, latLng)
            } else {
                val wp = currentWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                pendingWaypointFormIndex = wpIndex
                openAddForm(wpIndex, wp, latLng)
            }
            true
        }

        if (!isOfflineMainMode) {
            map.setOnPolylineClickListener { polyline ->
                // 測距模式：略過側溝線段點擊，避免開啟檢視表單
                if (measureManager?.isMeasuring == true) return@setOnPolylineClickListener
                openInspectBottomSheet(polyline)
            }
        }

        // 地圖停止移動後，依目前可視範圍向後端查詢側溝線段（使用防抖避免高頻調用）
        if (!isOfflineMainMode) {
            map.setOnCameraIdleListener { loadGuttersByViewportDebounced() }
        }

        // ── 測距管理器初始化（需在地圖就緒後才能建立） ────────────────────────
        // 起點大頭針使用 colorPrimary（紫色），與 App 配色一致
        measureConfig = MeasureConfig(
            startMarkerIcon = BitmapDescriptorFactory.defaultMarker(256f)
        )
        measureManager = DistanceMeasureManager(map, measureConfig) { meters ->
            updateMeasureDistanceDisplay(meters)
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
        googleMap?.setPadding(0, 0, 0, 0)
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        // 立即清除地圖暫存資料；API 結果由 onGutterSaved / onGutterSaveFailed 回報
        activeSheet?.onWaypointsChanged = null
        workingPolyline?.remove()
        workingPolyline = null
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        isInEditingMode = false  // 允許自動加載 polylines
        drawSubmittedGutter(waypoints)
        googleMap?.setPadding(0, 0, 0, 0)
    }

    /**
     * storeDitch 成功後，依照 API 回傳的 [nodes] 順序，
     * 找出對應 waypoint 的本機照片（content:// / file:// scheme）並上傳。
     * https:// 照片代表已在伺服器，略過。
     */
    private suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String
    ) {
        nodes.forEachIndexed { i, node ->
            val wp = waypoints.getOrNull(i) ?: return@forEachIndexed
            listOf(
                wp.basicData["photo1"] to 1,
                wp.basicData["photo2"] to 2,
                wp.basicData["photo3"] to 3
            ).forEach { (path, category) ->
                if (path.isNullOrEmpty()) return@forEach
                val scheme = Uri.parse(path).scheme?.lowercase() ?: return@forEach
                if (scheme == "http" || scheme == "https") return@forEach
                when (val r = gutterRepository.uploadNodeImage(
                    context      = this,
                    nodeId       = node.nodeId,
                    fileCategory = category,
                    imageUri     = Uri.parse(path),
                    token        = token
                )) {
                    is ApiResult.Error ->
                        android.util.Log.w("PhotoUpload", "node${node.nodeId} photo$category 失敗: ${r.message}")
                    is ApiResult.Success -> { /* OK */ }
                }
            }
        }
    }

    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
        // 斷開 onDismiss 回呼，避免 dismiss 後觸發重複清除；API 結果由 onGutterSaved 回報
        activeSheet?.onWaypointsChanged = null
        workingPolyline?.remove()
        workingPolyline = null
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        googleMap?.setPadding(0, 0, 0, 0)

        // ── 退出編輯模式時：重新加載所有線段 ──
        isInEditingMode = false  // 允許自動加載 polylines
        loadGuttersByViewport()
    }

    override fun onGutterSaved(spiNum: String?, waypoints: List<Waypoint>, nodes: List<DitchNode>) {
        // 上傳成功：刪除對應的待上傳草稿，並重置 session draft 追蹤 ID
        currentSessionDraftId?.let { sessionDraftRepository.delete(it) }
        currentSessionDraftId = null

        val token = LoginActivity.getSavedToken(this) ?: return
        if (spiNum != null) {
            // 更新模式：移除舊線段，重載可視範圍
            scopePolylines.remove(spiNum)?.remove()
            loadGuttersByViewport()
            Toast.makeText(this, getString(R.string.msg_gutter_updated), Toast.LENGTH_SHORT).show()
        } else {
            // 新增模式
            Toast.makeText(this, getString(R.string.msg_gutter_uploaded), Toast.LENGTH_SHORT).show()
            // 新增成功後立即重載，以後端正式線段為準（同時會清掉暫時提交線）
            loadGuttersByViewport()
        }
        // 依序上傳各點位的本機照片（已是 https:// 的舊照片會略過）
        lifecycleScope.launch {
            uploadWaypointPhotos(waypoints, nodes, token)
        }
    }

    override fun onGutterSaveFailed(waypoints: List<Waypoint>) {
        saveWaypointsAsPendingDraft(waypoints)
    }

    override fun onDeleteGutter(spiNum: String) {
        AlertDialog.Builder(this)
            .setTitle("刪除側溝")
            .setMessage("確定要刪除側溝「$spiNum」及其所有點位嗎？此操作無法復原。")
            .setPositiveButton("確定刪除") { _, _ ->
                val token = LoginActivity.getSavedToken(this)
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    when (val result = gutterRepository.deleteDitch(spiNum, token)) {
                        is ApiResult.Success -> {
                            sessionDraftRepository.getAll()
                                .filter { draft ->
                                    draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                                        ?.basicData?.get("SPI_NUM") == spiNum
                                }
                                .forEach { draft -> sessionDraftRepository.delete(draft.id) }
                            if (currentSessionDraftId != null) {
                                val currentDraft = sessionDraftRepository.getById(currentSessionDraftId!!)
                                if (currentDraft == null) currentSessionDraftId = null
                            }

                            // 從地圖移除該側溝的線段
                            scopePolylines.remove(spiNum)?.remove()
                            // 關閉 BottomSheet 並清除暫存標記
                            activeSheet?.onWaypointsChanged = null
                            workingPolyline?.remove()
                            workingPolyline = null
                            activeSheet?.dismiss()
                            activeSheet = null
                            clearWorkingMarkers()
                            binding.btnAddGutter.visibility = View.VISIBLE
                            googleMap?.setPadding(0, 0, 0, 0)
                            Toast.makeText(this@MainActivity, String.format(getString(R.string.msg_delete_success), spiNum), Toast.LENGTH_SHORT).show()
                            // ── 重新加載地圖可視範圍內的側溝數據 ──
                            loadGuttersByViewport()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this@MainActivity, String.format(getString(R.string.msg_delete_failed), result.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 上傳失敗時呼叫：委由 [autoSaveSessionDraft] 存入（或更新）待上傳草稿。 */
    private fun saveWaypointsAsPendingDraft(waypoints: List<Waypoint>) {
        autoSaveSessionDraft(waypoints)
    }

    /**
     * 自動將目前進行中的 session waypoints 儲存（或更新）成 [GutterSessionDraft]。
     *
     * - 若 [currentSessionDraftId] 已有值，直接以相同 id 覆寫（更新同一筆草稿）。
     * - 若 waypoints 中 START 點位帶有 SPI_NUM（gutterId），且 repository 裡已有同 SPI_NUM
     *   的不同草稿，則沿用其 id，確保每個 SPI_NUM 只會有一份草稿。
     * - 否則以目前時間戳記建立新草稿 id。
     *
     * 每次呼叫都會更新 [currentSessionDraftId] 以追蹤目前 session 所對應的草稿。
     */
    private fun autoSaveSessionDraft(waypoints: List<Waypoint>) {
        if (waypoints.isEmpty()) return
        // 空草稿判斷：沒有任何座標，且所有 basicData 都是空值 → 不存草稿
        val hasAnyLatLng = waypoints.any { it.latLng != null }
        val hasAnyBasicData = waypoints.any { wp ->
            wp.basicData.any { (_, v) -> v.isNotBlank() }
        }
        if (!hasAnyLatLng && !hasAnyBasicData) return
        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type      = wp.type.name,
                label     = wp.label,
                latitude  = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = wp.basicData
            )
        }
        // 從 START 點位取得 SPI_NUM（編輯模式下為伺服器側溝編號）
        val spiNum = waypoints.firstOrNull { it.type == WaypointType.START }
            ?.basicData?.get("SPI_NUM")?.takeIf { it.isNotEmpty() }

        // 記住本次呼叫前的 session 草稿 ID：後續若因為 SPI_NUM 去重而「切換」到另一筆既存草稿，
        // 需要考慮清掉先前產生的「未帶 SPI_NUM」占位草稿，避免清單多出一筆「側溝草稿」。
        val previousSessionId = currentSessionDraftId

        // 若已有相同 SPI_NUM 的草稿（且不是目前 session），沿用其 ID（覆蓋，確保唯一性）
        val existingId = if (!spiNum.isNullOrEmpty()) {
            sessionDraftRepository.getAll().firstOrNull { draft ->
                draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                    ?.basicData?.get("SPI_NUM") == spiNum &&
                draft.id != currentSessionDraftId
            }?.id
        } else null

        // 若找到既存 SPI_NUM 草稿，且本 session 先前已建立過草稿（通常是 SPI_NUM 還沒填時的占位草稿），
        // 則後續會「改用」既存草稿 ID；此時要避免占位草稿殘留在清單中。
        if (existingId != null && previousSessionId != null && previousSessionId != existingId) {
            val previousDraft = sessionDraftRepository.getById(previousSessionId)
            val previousSpiNum = previousDraft?.waypoints
                ?.firstOrNull { it.type == WaypointType.START.name }
                ?.basicData
                ?.get("SPI_NUM")
                ?.takeIf { it.isNotEmpty() }
            if (previousSpiNum.isNullOrEmpty()) {
                sessionDraftRepository.delete(previousSessionId)
            }
        }

        val draftId = existingId ?: currentSessionDraftId ?: System.currentTimeMillis()
        currentSessionDraftId = draftId

        if (!spiNum.isNullOrEmpty()) {
            sessionDraftRepository.getAll()
                .filter { draft ->
                    draft.id != draftId &&
                    draft.waypoints.firstOrNull { it.type == WaypointType.START.name }
                        ?.basicData?.get("SPI_NUM") == spiNum
                }
                .forEach { duplicate -> sessionDraftRepository.delete(duplicate.id) }
        }

        sessionDraftRepository.save(
            GutterSessionDraft(
                id = draftId,
                savedAt = System.currentTimeMillis(),
                isOffline = currentSessionIsOffline,
                waypoints = snapshots
            )
        )
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun openWaypointForEdit(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        currentWaypoints = sheet.getWaypoints()
        val wp = currentWaypoints.getOrNull(waypointIndex) ?: return

        // 直接開表單：若點位尚未選座標，先用目前地圖視角中心作為初始座標，
        // 讓使用者在表單內點擊 X/Y 欄位再進入選點頁面。
        val initialLatLng = wp.latLng ?: googleMap?.cameraPosition?.target ?: LatLng(0.0, 0.0)
        pendingWaypointFormIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openAddForm(waypointIndex, wp, initialLatLng, isEditMode = sheet.isEditMode())
    }

    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        val wp     = inspectWaypoints.getOrNull(waypointIndex) ?: return
        val latLng = wp.latLng ?: return
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openInspectForm(waypointIndex, wp, latLng)
    }

    private fun openInspectForm(waypointIndex: Int, wp: Waypoint, latLng: LatLng) {
        moveCameraToLatLngOffset(latLng, 0.75, zoomForGutterSize(wp.basicData))
        val layer = currentWmtsLayer()
        val intent = GutterFormActivity.newViewIntent(
            this,
            wp.label,
            latLng.latitude,
            latLng.longitude,
            waypointIndex,
            wp.basicData,
            layer
        )
        gutterFormLauncher.launch(intent)
    }

    private fun openAddForm(
        currentIndex: Int,
        wp: Waypoint,
        latLng: LatLng,
        isEditMode: Boolean = false
    ) {
        moveCameraToLatLngOffset(latLng, 0.75, zoomForGutterSize(wp.basicData))
        // 統一「表單內即時存草稿」與 MainActivity 的 session 草稿 ID：
        // 若尚未建立，先在這裡建立一次，確保同一條新增/編輯流程只會覆寫同一筆草稿。
        val ensuredDraftId = currentSessionDraftId ?: System.currentTimeMillis().also {
            currentSessionDraftId = it
        }
        val labels = ArrayList(currentWaypoints.map { it.label })
        val lats   = currentWaypoints.map { it.latLng?.latitude ?: 0.0 }.toDoubleArray()
        val lngs   = currentWaypoints.map { it.latLng?.longitude ?: 0.0 }.toDoubleArray()
        val sessionWaypointsJson = Gson().toJson(
            currentWaypoints.map { waypoint ->
                WaypointSnapshot(
                    type = waypoint.type.name,
                    label = waypoint.label,
                    latitude = waypoint.latLng?.latitude,
                    longitude = waypoint.latLng?.longitude,
                    basicData = HashMap(waypoint.basicData)
                )
            }
        )
        val layer = currentWmtsLayer()
	        val intent = GutterFormActivity.newIntent(
	            context = this,
	            labels = labels,
	            lats = lats,
	            lngs = lngs,
	            index = currentIndex,
	            basicData = wp.basicData,
	            isEditMode = isEditMode,
	            sessionDraftId = ensuredDraftId,
	            sessionWaypointsJson = sessionWaypointsJson,
	            wmtsLayer = layer,
	            sessionIsOffline = currentSessionIsOffline
	        )
	        gutterFormLauncher.launch(intent)
	    }

    private fun currentWmtsLayer(): String =
        when (currentMapMode) {
            MapMode.EMAP -> LayersBottomSheet.LAYER_EMAP
            MapMode.EMAP01 -> LayersBottomSheet.LAYER_EMAP01
            MapMode.PHOTO2 -> LayersBottomSheet.LAYER_PHOTO2
        }

    /**
     * 依側溝頂寬（NODE_WID，公分）動態計算縮放級數。
     *   NODE_WID ≤  50cm → zoom 20f（小溝，貼近查看）
     *   NODE_WID ≥ 200cm → zoom 18f（大溝，稍微拉遠）
     *   中間值線性插值；無資料或無法解析時預設 20f。
     */
    private fun zoomForGutterSize(basicData: Map<String, String>): Float {
        val wid = basicData["NODE_WID"]?.toFloatOrNull() ?: return 20f
        val minWid  = 50f;  val maxWid  = 200f
        val minZoom = 18f;  val maxZoom = 20f
        val clamped = wid.coerceIn(minWid, maxWid)
        return maxZoom - (clamped - minWid) / (maxWid - minWid) * (maxZoom - minZoom)
    }

    private fun moveCameraToLatLngOffset(latLng: LatLng, offsetRatio: Double, zoom: Float = 20f) {
        val map = googleMap ?: return
        val screenH = resources.displayMetrics.heightPixels
        // 暫時設定底部 padding，讓地圖移動時把點位顯示在視窗上半段；
        // 動畫結束（或被取消）後立即歸零，確保地圖始終佔滿全螢幕。
        map.setPadding(0, 0, 0, (screenH * offsetRatio).toInt())
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, zoom),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() { map.setPadding(0, 0, 0, 0) }
                override fun onCancel() { map.setPadding(0, 0, 0, 0) }
            }
        )
    }

    private fun fitCameraToWaypoints(
        waypoints: List<Waypoint>,
        bottomOffsetRatio: Double = 0.8,
        resetPaddingAfter: Boolean = true,
        maxZoom: Float? = null,
        paddingDp: Int = 64
    ) {
        val map = googleMap ?: return
        val points = waypoints.mapNotNull { it.latLng }
        if (points.isEmpty()) return
        val dm      = resources.displayMetrics
        val screenH = dm.heightPixels
        val padding = (paddingDp * dm.density).toInt()
        // 暫時設定 padding，讓相機計算範圍時排除底部 BottomSheet 區域；
        // 動畫結束（或被取消）後歸零，避免 Google Maps 縮放鈕 / 羅盤位置偏移。
        map.setPadding(padding, padding, padding, (screenH * bottomOffsetRatio).toInt())
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        try {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding),
                object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        // bounds 可能把縮放放得太近（點位距離很小時），這裡做一個上限夾住，讓視圖比例尺更適中
                        val z = map.cameraPosition.zoom
                        val cap = maxZoom
                        if (cap != null && z > cap) {
                            map.animateCamera(CameraUpdateFactory.zoomTo(cap))
                        }
                        if (resetPaddingAfter) map.setPadding(0, 0, 0, 0)
                    }
                    override fun onCancel() {
                        val z = map.cameraPosition.zoom
                        val cap = maxZoom
                        if (cap != null && z > cap) {
                            map.animateCamera(CameraUpdateFactory.zoomTo(cap))
                        }
                        if (resetPaddingAfter) map.setPadding(0, 0, 0, 0)
                    }
                }
            )
        } catch (e: Exception) {
            if (resetPaddingAfter) map.setPadding(0, 0, 0, 0)
            map.setOnMapLoadedCallback { fitCameraToWaypoints(waypoints, bottomOffsetRatio, resetPaddingAfter, maxZoom, paddingDp) }
        }
    }

    private fun fitCameraToAllGutters() {
        val allWaypoints = mutableListOf<Waypoint>()
        submittedPolylines.forEach { poly -> (poly.tag as? ArrayList<Waypoint>)?.let { allWaypoints.addAll(it) } }
        if (allWaypoints.isNotEmpty()) fitCameraToWaypoints(allWaypoints)
    }

    private fun requestLocationAndMove() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) enableMyLocationAndMove()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndMove() {
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> if (loc != null) googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 18f)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun openInspectBottomSheet(polyline: Polyline) {
        if (isOfflineMainMode) return
        // 防止連點：若已在查詢或已有 inspect 畫面，直接忽略
        if (isInspecting) return
        val tag      = polyline.tag as? Pair<*, *> ?: return
        val spiNum   = tag.first  as? String ?: return
        val groupId  = tag.second as? String ?: ""
        val token    = LoginActivity.getSavedToken(this)  ?: return

        isInspecting = true
        // ── 進入檢視流程立即禁止自動加載（避免 fitCamera 觸發 cameraIdle 後又把線段重畫回來）──
        isInEditingMode = true

        setInspectLoading(true, "載入側溝資料中…")

        // 登入的 group_id 與線段的 group_id 一致時才允許編輯
        val savedGroupId = LoginActivity.getSavedGroupId(this)
        val canEdit = savedGroupId != -1 &&
                groupId.isNotEmpty() &&
                groupId.toIntOrNull() == savedGroupId

        // 擷取線段點位的 WGS84 座標，供編輯模式預填大頭針位置
        val lats = polyline.points.map { it.latitude }.toDoubleArray()
        val lngs = polyline.points.map { it.longitude }.toDoubleArray()

        // 立即將鏡頭對齊點選的側溝（底部留 52% 給 BottomSheet），不需等 API 回傳
        val routeWaypoints = polyline.points.map { Waypoint(WaypointType.NODE, "", it, hashMapOf()) }
        fitCameraToWaypoints(routeWaypoints)

        lifecycleScope.launch {
            try {
                when (val result = gutterRepository.getDitchDetails(spiNum, token)) {
                    is ApiResult.Success -> {
                        val ditch = result.data.data
                        if (ditch != null) {
                        // ── 進入檢視模式時：隱藏所有其他已存在的線段，只顯示正在檢視的側溝 ──
                        // 保留使用者點選的 polyline（spiNum 對應那條），其餘全部移除。
                        scopePolylines.entries.toList().forEach { (key, p) ->
                            if (key != spiNum) {
                                p.remove()
                                scopePolylines.remove(key)
                            }
                        }
                        submittedPolylines.forEach { it.remove() }
                        submittedPolylines.clear()

                        // ── 點選的側溝高亮為「檢視與編輯中」配色 ──
                        scopePolylines[spiNum]?.color = android.graphics.Color.parseColor("#562ECB")

                        // ── 並行查詢各節點詳細座標，再繪製起點／節點／終點標記 ──
                        val nodeDetailsList = gutterRepository.getNodeDetailsForNodes(ditch.nodes, token)
                        showInspectMarkers(nodeDetailsList)

                        // ── 預先下載起點/終點 6 張照片到本機 contentUri ──
                        val startNode = ditch.nodes.firstOrNull { it.nodeAtt == "1" }
                        val endNode = ditch.nodes.firstOrNull { it.nodeAtt == "3" }
                        fun urlByCategory(node: DitchNode?, cat: String): String? =
                            node?.url?.firstOrNull { it.fileCategory == cat }?.url

                        val strUrls = listOf(
                            urlByCategory(startNode, "1"),
                            urlByCategory(startNode, "2"),
                            urlByCategory(startNode, "3")
                        )
                        val endUrls = listOf(
                            urlByCategory(endNode, "1"),
                            urlByCategory(endNode, "2"),
                            urlByCategory(endNode, "3")
                        )

                        suspend fun dl(url: String?, prefix: String): String? {
                            if (url.isNullOrBlank()) return null
                            return gutterRepository
                                .downloadImageToLocalContentUri(this@MainActivity, url, prefix = prefix)
                                ?.toString()
                        }

                        val downloaded: List<String?> = coroutineScope {
                            val a1 = async<String?> { dl(strUrls.getOrNull(0), "INSPECT_STR_1_") }
                            val a2 = async<String?> { dl(strUrls.getOrNull(1), "INSPECT_STR_2_") }
                            val a3 = async<String?> { dl(strUrls.getOrNull(2), "INSPECT_STR_3_") }
                            val b1 = async<String?> { dl(endUrls.getOrNull(0), "INSPECT_END_1_") }
                            val b2 = async<String?> { dl(endUrls.getOrNull(1), "INSPECT_END_2_") }
                            val b3 = async<String?> { dl(endUrls.getOrNull(2), "INSPECT_END_3_") }
                            awaitAll(a1, a2, a3, b1, b2, b3)
                        }
                        val str1 = downloaded.getOrNull(0)
                        val str2 = downloaded.getOrNull(1)
                        val str3 = downloaded.getOrNull(2)
                        val end1 = downloaded.getOrNull(3)
                        val end2 = downloaded.getOrNull(4)
                        val end3 = downloaded.getOrNull(5)

                        val intent = GutterInspectActivity.newIntent(
                            context    = this@MainActivity,
                            ditch      = ditch,
                            canEdit    = canEdit,
                            latitudes  = lats,
                            longitudes = lngs,
                            strPhoto1  = str1,
                            strPhoto2  = str2,
                            strPhoto3  = str3,
                            endPhoto1  = end1,
                            endPhoto2  = end2,
                            endPhoto3  = end3
                        )
                        // 若有進行中的新增流程 sheet，先隱藏它（不 dismiss，
                        // 讓使用者按返回時仍可繼續；若最終進入編輯模式則由 inspectLauncher 清除）
                        activeSheet?.hideSelf()
                        setInspectLoading(false)
                        inspectLauncher.launch(intent)
                        // isInspecting 在 inspectLauncher 結果回呼中重置
                        } else {
                            isInspecting = false
                            isInEditingMode = false  // 允許自動加載 polylines
                            android.widget.Toast.makeText(
                                this@MainActivity, getString(R.string.msg_no_line_data), android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is ApiResult.Error -> {
                        isInspecting = false
                        isInEditingMode = false  // 允許自動加載 polylines
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "查詢失敗(${result.code}): ${result.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                // 若已成功 launch，前面已 setInspectLoading(false)；這裡保險起見再關一次
                setInspectLoading(false)
            }
        }
    }

    private fun setInspectLoading(visible: Boolean, message: String? = null) {
        if (!::binding.isInitialized) return
        binding.inspectLoadingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        if (!message.isNullOrBlank()) {
            binding.tvInspectLoading.text = message
        }
    }

    // ── API code→顯示文字轉換（對應 GutterBasicInfoFragment 下拉選單選項）────
    private fun spiTypToText(code: String?): String = when (code) {
        "1" -> "U形溝（明溝）"; "2" -> "U形溝（加蓋）"
        "3" -> "L形溝與暗溝渠併用"; "4" -> "其他"
        else -> code ?: ""
    }
    private fun matTypToText(code: String?): String = when (code) {
        "1" -> "混凝土"; "2" -> "卵礫石"; "3" -> "紅磚"
        else -> code ?: ""
    }
    private fun isBrokenToText(code: String?): String = when (code) {
        "0" -> "否"; "1" -> "是"; else -> code ?: ""
    }
    private fun isHangingToText(code: String?): String = when (code) {
        "0" -> "無"; "1" -> "有"; else -> code ?: ""
    }
    private fun isSiltToText(code: String?): String = when (code) {
        "0" -> "無"; "1" -> "輕度"; "2" -> "中度"; "3" -> "嚴重"
        else -> code ?: ""
    }

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener {
            if (isOfflineMainMode) {
                // 離線填寫：直接回登入頁，不打 logout API
                clearAuthAndGoLogin()
                return@setOnClickListener
            }
            val token = LoginActivity.getSavedToken(this)
            if (token.isNullOrEmpty()) {
                // 本機無 token，直接跳登入頁
                goToLogin()
                return@setOnClickListener
            }
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                when (val result = gutterRepository.logout(token)) {
                    is ApiResult.Success -> {
                        clearAuthAndGoLogin()
                    }
                    is ApiResult.Error -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(
                            this@MainActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        binding.btnViewDrafts.setOnClickListener { showPendingDraftsSheet() }
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener { requestLocationAndMove() }
        binding.btnMeasureDistance.setOnClickListener { toggleMeasureMode() }
        binding.measurePanel.btnMeasureReset.setOnClickListener { measureManager?.reset() }
        binding.measurePanel.btnMeasureClose.setOnClickListener { exitMeasureMode() }
        binding.btnAddGutter.setOnClickListener {
            showAddOptionSheet()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  新增選項彈窗（新增側溝 / 新增曲線）
    // ═══════════════════════════════════════════════════════════════

    /** 顯示「新增選項」BottomSheet，讓使用者選擇「新增側溝」或「新增曲線」。 */
    private fun showAddOptionSheet() {
        val optionSheet = AddOptionBottomSheet()
        optionSheet.onAddGutterClicked = { openAddGutterFlow() }
        optionSheet.onAddCurveClicked = { openAddCurveFlow() }
        optionSheet.show(supportFragmentManager, AddOptionBottomSheet.TAG)
    }

    /** 原本的「新增側溝」流程，從 FAB 移入獨立方法。 */
    private fun openAddGutterFlow() {
        workingPolyline?.remove()
        clearWorkingMarkers()
        fitCameraToAllGutters()

        // ── 進入新增模式時：隱藏所有已存在的線段，只顯示新增中的側溝 ──
        isInEditingMode = true  // 禁止自動加載 polylines
        scopePolylines.values.forEach { it.remove() }
        scopePolylines.clear()
        submittedPolylines.forEach { it.remove() }
        submittedPolylines.clear()

        currentSessionIsOffline = isOfflineMainMode
        // 新增流程：離線模式下立即建立固定草稿 ID，避免每個動作產生多筆草稿
        currentSessionDraftId =
            if (isOfflineMainMode) System.currentTimeMillis() else null
        val sheet = if (isOfflineMainMode) {
            AddGutterBottomSheet.newOfflineInstance()
        } else {
            AddGutterBottomSheet.newInstance()
        }
        activeSheet = sheet
        var lastWaypointsSize = 0
        sheet.onWaypointsChanged = { wps ->
            currentWaypoints = wps ?: emptyList()
            refreshWorkingLayer(wps ?: emptyList())
            if (wps != null) {
                val shouldRefit = wps.size > lastWaypointsSize
                lastWaypointsSize = wps.size
                autoSaveSessionDraft(wps)
                if (shouldRefit) {
                    fitCameraToWaypoints(wps, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
                }
            } else {
                // ── 取消新增模式時，清除工作層並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                googleMap?.setPadding(0, 0, 0, 0)
                activeSheet = null
                // 重新加載所有線段（scopePolylines 與 submittedPolylines）
                loadGuttersByViewport()
            }
        }
        sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
    }

    /** 開啟「新增曲線」畫面。 */
    private fun openAddCurveFlow() {
        val intent = Intent(this, AddCurveActivity::class.java)
            .putExtra(AddCurveActivity.EXTRA_FORCE_OFFLINE_MODE, isOfflineMainMode)
        addCurveLauncher.launch(intent)
    }

    /** 顯示「待上傳草稿」BottomSheet，並處理「繼續編輯」回呼。 */
    private fun showPendingDraftsSheet() {
        val sheet = PendingDraftsBottomSheet.newInstance()
        sheet.onResumeDraft = { draft -> resumePendingDraft(draft) }
        sheet.show(supportFragmentManager, PendingDraftsBottomSheet.TAG)
    }

    /**
     * 從待上傳草稿恢復 AddGutterBottomSheet。
     *
     * 先 dismiss PendingDraftsBottomSheet（與任何現有 activeSheet），
     * 再以 [View.post] 延到下一幀才 show AddGutterBottomSheet，
     * 確保兩個 FragmentTransaction 不會同時競爭同一個 FragmentManager。
     */
    private fun resumePendingDraft(draft: GutterSessionDraft) {
        // ① 關閉 PendingDraftsBottomSheet
        (supportFragmentManager.findFragmentByTag(PendingDraftsBottomSheet.TAG)
                as? PendingDraftsBottomSheet)
            ?.dismissAllowingStateLoss()

        // ② 清除現有進行中的 sheet 與地圖疊加層
        activeSheet?.dismissAllowingStateLoss()
        activeSheet = null
        workingPolyline?.remove()
        workingPolyline = null
        clearWorkingMarkers()

        // ③ 弧線草稿：開啟 AddCurveActivity（預填 XY_NUM），不走 AddGutterBottomSheet 流程
        if (draft.kind == KIND_CURVE) {
            val startXy = draft.waypoints
                .firstOrNull { it.type == "START" }
                ?.basicData
                ?.get("XY_NUM")
                .orEmpty()
            val endXy = draft.waypoints
                .firstOrNull { it.type == "END" }
                ?.basicData
                ?.get("XY_NUM")
                .orEmpty()
            val offline = isOfflineMainMode || draft.isOffline
            Handler(Looper.getMainLooper()).postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                val intent = Intent(this, AddCurveActivity::class.java)
                    .putExtra(AddCurveActivity.EXTRA_FORCE_OFFLINE_MODE, offline)
                    .putExtra(AddCurveActivity.EXTRA_DRAFT_ID, draft.id)
                    .putExtra(AddCurveActivity.EXTRA_PREFILL_START_XY_NUM, startXy)
                    .putExtra(AddCurveActivity.EXTRA_PREFILL_END_XY_NUM, endXy)
                addCurveLauncher.launch(intent)
            }, 300L)
            return
        }

        // ③ 單點離線草稿（isSinglePoint=true）：直接開啟全螢幕離線表單，不走地圖流程
        if (draft.isSinglePoint) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                startActivity(GutterFormActivity.newOfflineIntent(this, draft.id))
            }, 300L)
            return
        }

        // ④ 地圖流程多點草稿：記住 ID，再展示 AddGutterBottomSheet
        currentSessionDraftId = draft.id
        // 回到線上模式後，離線草稿也應允許上傳；是否離線以「目前主模式」為準
        currentSessionIsOffline = isOfflineMainMode

        Handler(Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            // ── 進入草稿編輯模式時：隱藏所有已存在的線段，只顯示編輯中的側溝 ──
            isInEditingMode = true  // 禁止自動加載 polylines
            scopePolylines.values.forEach { it.remove() }
            scopePolylines.clear()
            submittedPolylines.forEach { it.remove() }
            submittedPolylines.clear()

            val sheet = AddGutterBottomSheet.newInstanceFromDraft(draft, forceOffline = isOfflineMainMode)
            activeSheet = sheet
            var lastWaypointsSize = 0
            sheet.onWaypointsChanged = { wps ->
                currentWaypoints = wps ?: emptyList()
                refreshWorkingLayer(wps ?: emptyList())
                if (wps != null) {
                    val shouldRefit = wps.size > lastWaypointsSize
                    lastWaypointsSize = wps.size
                    autoSaveSessionDraft(wps)
                    if (shouldRefit) {
                        fitCameraToWaypoints(wps, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
                    }
                } else {
                    // ── 取消草稿編輯模式時，清除工作層並恢復其他線段顯示 ──
                    isInEditingMode = false  // 允許自動加載 polylines
                    googleMap?.setPadding(0, 0, 0, 0)
                    activeSheet = null
                    // 重新加載所有線段（scopePolylines 與 submittedPolylines）
                    if (!isOfflineMainMode) loadGuttersByViewport()
                }
            }
            currentWaypoints = draft.waypoints.map { snap ->
                Waypoint(
                    type = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE,
                    label = snap.label,
                    latLng = if (snap.latitude != null && snap.longitude != null) {
                        LatLng(snap.latitude, snap.longitude)
                    } else null,
                    basicData = HashMap(snap.basicData)
                )
            }
            lastWaypointsSize = currentWaypoints.size
            refreshWorkingLayer(currentWaypoints)
            fitCameraToWaypoints(currentWaypoints, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
            // ── 編輯模式中已禁止自動加載，此呼叫會被忽略 ──
            // （保留此呼叫以便未來模式更改時使用，但因 isInEditingMode = true 會立即返回）
            if (!isOfflineMainMode) loadGuttersByViewport()
        }, 300L)
    }

    private fun refreshWorkingLayer(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        workingPolyline?.remove()
        if (waypoints.isEmpty()) return
        val routePoints = mutableListOf<LatLng>()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            routePoints.add(latLng)
            val marker = map.addMarker(MarkerOptions()
                .position(latLng)
                .icon(getMarkerIconFromXml(wp.type))
                .anchor(0.5f, 0.5f))
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
        if (routePoints.size >= 2) {
            workingPolyline = map.addPolyline(PolylineOptions().addAll(routePoints).color(Color.parseColor("#562ECB")).width(10f).geodesic(true).clickable(false))
        }
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            val marker = map.addMarker(MarkerOptions()
                .position(latLng)
                .icon(getMarkerIconFromXml(wp.type))
                .anchor(0.5f, 0.5f))
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
    }

    // ── 登出輔助 ──────────────────────────────────────────────────────────

    /** 清除本機 token 並跳回登入頁（完成登出後呼叫） */
    private fun clearAuthAndGoLogin() {
        getSharedPreferences("taoyuan_prefs", MODE_PRIVATE).edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_company")
            .apply()
        goToLogin()
    }

    /** 跳回登入頁並清除 Activity 堆疊 */
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearWorkingMarkers() {
        workingMarkers.forEach { it.remove() }
        workingMarkers.clear()
        highlightedMarkerIndex = -1
    }

    /**
     * 點選已提交側溝線段後，在地圖上顯示起點／節點／終點標記。
     *
     * 座標直接使用 [NodeDetails.latitude] / [NodeDetails.longitude]（WGS84），
     * 點位類型依 [NodeDetails.nodeAttr]（"1"=起點、"2"=節點、"3"=終點）決定圖示。
     * 解析失敗的節點（座標為 null 或非數值）靜默略過。
     *
     * 標記加入 [workingMarkers]，可透過 [clearWorkingMarkers] 統一清除。
     */
    private fun showInspectMarkers(nodes: List<com.example.taoyuangutter.api.NodeDetails>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        if (nodes.isEmpty()) return

        nodes.forEachIndexed { idx, node ->
            val lat = node.latitude?.toDoubleOrNull()  ?: return@forEachIndexed
            val lng = node.longitude?.toDoubleOrNull() ?: return@forEachIndexed
            val latLng = com.google.android.gms.maps.model.LatLng(lat, lng)

            val wpType = when (node.nodeAttr) {
                "1"  -> WaypointType.START
                "3"  -> WaypointType.END
                else -> WaypointType.NODE
            }

            val marker = map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(latLng)
                    .icon(getMarkerIconFromXml(wpType))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
    }

    private fun drawSubmittedGutter(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return
        val polyline = map.addPolyline(PolylineOptions().addAll(routePoints).color(Color.parseColor("#562ECB")).width(10f).geodesic(true).clickable(true))
        polyline.tag = ArrayList(waypoints)
        submittedPolylines.add(polyline)
    }

    private fun highlightMarker(waypointIndex: Int) {
        resetHighlightedMarker()
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(waypointIndex) ?: return
        if (wp.latLng == null) return
        highlightedMarkerIndex = waypointIndex
        workingMarkers.firstOrNull { it.tag == waypointIndex }?.let { marker ->
            marker.setIcon(createEnlargedMarkerIcon(wp.type))
            marker.setAnchor(0.5f, 0.5f)
            marker.zIndex = 1f
        }
    }

    private fun resetHighlightedMarker() {
        if (highlightedMarkerIndex < 0) return
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(highlightedMarkerIndex)
        workingMarkers.firstOrNull { it.tag == highlightedMarkerIndex }?.let { marker ->
            marker.setIcon(getMarkerIconFromXml(wp?.type ?: WaypointType.NODE))
            marker.setAnchor(0.5f, 0.5f)
            marker.zIndex = 0f
        }
        highlightedMarkerIndex = -1
    }

    // ── 側溝座標 API（scopeSearch）────────────────────────────────────────

    /**
     * 防抖版本的 loadGuttersByViewport
     * 避免短時間內多次調用 API（例如快速拖拽地圖時）
     */
    private fun loadGuttersByViewportDebounced() {
        if (isOfflineMainMode) return
        val now = System.currentTimeMillis()
        if (now - lastGutterLoadTime < GUTTER_LOAD_DEBOUNCE_MS) {
            return  // 距上次調用不足 500ms，跳過此次請求
        }
        lastGutterLoadTime = now
        loadGuttersByViewport()
    }

    /**
     * 取得目前地圖可視範圍（LatLngBounds）並呼叫 scopeSearch API，
     * 成功後呼叫 [drawScopePolylines] 更新地圖上的線段。
     */
    private fun loadGuttersByViewport() {
        if (isOfflineMainMode) return
        // ── 編輯/檢視/新增模式中禁止自動加載，避免重新顯示隱藏的線段 ──
        if (isInEditingMode) return
        val map = googleMap ?: return
        val token = LoginActivity.getSavedToken(this) ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        lifecycleScope.launch {
            when (val result = gutterRepository.getGuttersByScope(
                minLat = bounds.southwest.latitude,
                maxLat = bounds.northeast.latitude,
                minLng = bounds.southwest.longitude,
                maxLng = bounds.northeast.longitude,
                token  = token
            )) {
                is ApiResult.Success -> {
                    // scopeSearch 成功回來後，以後端正式線段為準：
                    // 清除暫時提交線（submittedPolylines），避免與正式線段重疊出現多色疊加。
                    submittedPolylines.forEach { it.remove() }
                    submittedPolylines.clear()
                    drawScopePolylines(result.data.data?.features ?: emptyList())
                }
                is ApiResult.Error -> {
                    // 靜默失敗：不打擾使用者，僅在 logcat 留紀錄
                    android.util.Log.w("ScopeSearch", "查詢失敗: ${result.message}")
                }
            }
        }
    }

    /**
     * 將 scopeSearch 回傳的 GeoJSON features 畫成地圖 Polyline。
     * 已存在的同 SPI_NUM 線段會先移除再重繪，避免重複疊加。
     */
    private fun drawScopePolylines(features: List<com.example.taoyuangutter.api.GeoFeature>) {
        val map = googleMap ?: return
        val savedGroupId = LoginActivity.getSavedGroupId(this)
        features.forEach { feature ->
            val spiNum  = feature.properties?.spiNum  ?: return@forEach
            val groupId = feature.properties?.groupId ?: ""
            val spiState = feature.properties?.spiState
            val coords  = feature.geometry?.coordinates ?: return@forEach
            if (coords.size < 2) return@forEach

            // 移除舊線段
            scopePolylines.remove(spiNum)?.remove()

            val points = coords.map { LatLng(it[1], it[0]) }   // GeoJSON: [lng, lat]
            val isSameGroup = savedGroupId != -1 &&
                groupId.isNotBlank() &&
                groupId.toIntOrNull() == savedGroupId
            val color = if (!isSameGroup) {
                android.graphics.Color.parseColor("#B4B4B4") // 非本公司管轄
            } else {
                when (spiState) {
                    1 -> android.graphics.Color.parseColor("#000000") // 已完成
                    2 -> android.graphics.Color.parseColor("#FF58E0") // 待修正
                    3 -> android.graphics.Color.parseColor("#FFC300") // 待匯入座標紀錄
                    else -> android.graphics.Color.parseColor("#562ECB") // 檢視與編輯中（預設）
                }
            }
            val polyline = map.addPolyline(
                com.google.android.gms.maps.model.PolylineOptions()
                    .addAll(points)
                    .color(color)
                    .width(6f)
                    .clickable(true)
            )
            // tag 同時儲存 spiNum 與 groupId，供點擊時比對編輯權限
            polyline.tag = Pair(spiNum, groupId)
            scopePolylines[spiNum] = polyline
        }
    }

    private fun createEnlargedMarkerIcon(type: WaypointType): com.google.android.gms.maps.model.BitmapDescriptor {
        /*
        /// 舊放大的圖標
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
        val tail = Path().apply { moveTo(r * 0.35f, r * 1.55f); lineTo(r, h.toFloat()); lineTo(r * 1.65f, r * 1.55f); close() }
        canvas.drawPath(tail, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(r, r, r * 0.38f, paint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
        */
        // 取得對應的 XML 資源 (建議您可以準備一組放大的版本，或是共用目前的)
        val resId = when (type) {
            WaypointType.START -> R.drawable.ic_legend_start // 或者是您的放大版 XML
            WaypointType.NODE  -> R.drawable.ic_legend_node
            WaypointType.END   -> R.drawable.ic_legend_end
        }

        val drawable = ContextCompat.getDrawable(this, resId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        // 設定放大的尺寸 (例如原始尺寸的 1.5 倍)
        val scale = 1.5f
        val width = (drawable.intrinsicWidth * scale).toInt()
        val height = (drawable.intrinsicHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getMarkerIconFromXml(type: WaypointType): BitmapDescriptor {
        val resId = when (type) {
            WaypointType.START -> R.drawable.ic_legend_start
            WaypointType.NODE  -> R.drawable.ic_legend_node
            WaypointType.END   -> R.drawable.ic_legend_end
        }
        val drawable = ContextCompat.getDrawable(this, resId) ?: return BitmapDescriptorFactory.defaultMarker()
        
        // 將 Drawable 轉換成 BitmapDescriptor
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun setupLocationPickerOverlay() {
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE
            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
                val wp = currentWaypoints.getOrNull(pickingIndex) ?: Waypoint(WaypointType.NODE, "點位")
                pendingWaypointFormIndex = pickingIndex
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

    /** 切換 NLSC WMTS 底圖圖層（電子地圖 / 正射影像）。 */
    private fun setMapTiles(mode: MapMode) {
        currentTileOverlay?.remove()
        currentMapMode = mode

        val layer = when (mode) {
            MapMode.EMAP -> LayersBottomSheet.LAYER_EMAP
            MapMode.EMAP01 -> LayersBottomSheet.LAYER_EMAP01
            MapMode.PHOTO2 -> LayersBottomSheet.LAYER_PHOTO2
        }
        val urlTemplate = "https://wmts.nlsc.gov.tw/wmts/$layer/default/GoogleMapsCompatible/%d/%d/%d"

        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = try {
                URL(String.format(urlTemplate, zoom, y, x))
            } catch (e: MalformedURLException) { null }
        }
        currentTileOverlay = googleMap?.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider).zIndex(-1f)
        )
    }

    private fun applyWmsOverlays() {
        val map = googleMap ?: return

        // 可能側溝位置（道路調查 WMS）
        // 注意：此疊圖要綁在「可能側溝位置」的複選框，而不是「本次計畫調查」。
        if (showPossibleOverlay) {
            if (planWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/roadServey/wms",
                    layers = "roadServey",
                    styles = "TY_RSGDBIP_道路調查",
                    // png8 is smaller but may not be available everywhere; png is safe.
                    format = "image/png"
                )
                planWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions()
                        .tileProvider(provider)
                        .zIndex(0f)
                        .transparency(0f)
                )
            }
        } else {
            planWmsOverlay?.remove()
            planWmsOverlay = null
        }

        // 水務局舊資料
        if (showWaterOldOverlay) {
            if (waterOldWmsOverlay == null) {
                val provider = Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "legacyDitch",
                    styles = "TY_RSGDBIP_水務局既有資料",
                    format = "image/png8"
                )
                waterOldWmsOverlay = map.addTileOverlay(
                    TileOverlayOptions()
                        .tileProvider(provider)
                        .zIndex(0.1f)
                        .transparency(0f)
                )
            }
        } else {
            waterOldWmsOverlay?.remove()
            waterOldWmsOverlay = null
        }

        // 本次計畫調查：目前先保留 UI，待提供對應圖層規格後再接 API/WMS
    }

    private fun showMapTypeDialog() {
        val selected = when (currentMapMode) {
            MapMode.EMAP -> LayersBottomSheet.LAYER_EMAP
            MapMode.EMAP01 -> LayersBottomSheet.LAYER_EMAP01
            MapMode.PHOTO2 -> LayersBottomSheet.LAYER_PHOTO2
        }
        LayersBottomSheet.newInstance(
            selectedLayer = selected,
            showPlan = showPlanOverlay,
            showWaterOld = showWaterOldOverlay,
            showPossible = showPossibleOverlay
        )
            .show(supportFragmentManager, "LayersBottomSheet")
    }

    private fun showLegendDialog() {
        LegendBottomSheet()
            .show(supportFragmentManager, "LegendBottomSheet")
    }

    override fun onLayerSelected(layer: String) {
        when (layer) {
            LayersBottomSheet.LAYER_EMAP -> setMapTiles(MapMode.EMAP)
            LayersBottomSheet.LAYER_EMAP01 -> setMapTiles(MapMode.EMAP01)
            LayersBottomSheet.LAYER_PHOTO2 -> setMapTiles(MapMode.PHOTO2)
        }
    }

    override fun onOverlayTogglesChanged(showPlan: Boolean, showWaterOld: Boolean, showPossible: Boolean) {
        showPlanOverlay = showPlan
        showWaterOldOverlay = showWaterOld
        showPossibleOverlay = showPossible
        applyWmsOverlays()
    }

    private fun extractBasicData(data: Intent?): HashMap<String, String> =
        hashMapOf(
            "SPI_NUM"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID)   ?: ""),
            "NODE_TYP"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE) ?: ""),
            "MAT_TYP"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MAT_TYP)     ?: ""),
            "NODE_X"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_X)     ?: ""),
            "NODE_Y"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Y)     ?: ""),
            "NODE_LE"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Z)     ?: ""),
            "XY_NUM"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID)  ?: ""),
            "NODE_DEP"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_DEPTH)       ?: ""),
            "NODE_WID"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH)   ?: ""),
            "IS_BROKEN"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_BROKEN)   ?: ""),
            "IS_HANGING" to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_HANGING)  ?: ""),
            "IS_SILT"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_SILT)     ?: ""),
            "IS_CANTOPEN" to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_CANTOPEN) ?: ""),
            "NODE_NOTE"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_REMARKS)     ?: ""),
            "photo1"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_1)     ?: ""),
            "photo2"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_2)     ?: ""),
            "photo3"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_3)     ?: "")
        )

    // ── 測距模式 ──────────────────────────────────────────────────────────────

    /**
     * 切換測距模式（進入 / 離開）。
     * 點擊主畫面測距 FAB 時呼叫。
     */
    private fun toggleMeasureMode() {
        val mgr = measureManager ?: return
        if (mgr.isMeasuring) exitMeasureMode() else enterMeasureMode()
    }

    /** 進入測距模式：顯示底部面板，準心暫時隱藏，等使用者點選起點後才顯示。 */
    private fun enterMeasureMode() {
        val mgr = measureManager ?: return
        mgr.enter()

        // 套用 crosshair drawable（支援客製化），但先隱藏——有起點後才由 updateMeasureDistanceDisplay 顯示
        binding.ivMeasureCrosshair.setImageResource(measureConfig.crosshairResId)
        binding.ivMeasureCrosshair.visibility = View.GONE
        binding.measurePanel.root.visibility = View.VISIBLE

        // FAB active 狀態：底色改為 colorPrimary，圖示改為白色
        binding.btnMeasureDistance.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
        binding.btnMeasureDistance.imageTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, android.R.color.white)
            )
    }

    /** 離開測距模式：隱藏準星與底部面板，還原 FAB 樣式。 */
    private fun exitMeasureMode() {
        val mgr = measureManager ?: return
        mgr.exit()

        binding.ivMeasureCrosshair.visibility = View.GONE
        binding.measurePanel.root.visibility = View.GONE
        binding.measurePanel.tvMeasureDistance.text = getString(R.string.measure_tap_hint)

        // FAB inactive 狀態：還原白底、colorPrimary 圖示
        binding.btnMeasureDistance.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)
            )
        binding.btnMeasureDistance.imageTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
    }

    /**
     * 更新底部面板距離文字，並同步控制準心顯示。
     * - [meters] 為 null → 無起點，隱藏準心，顯示提示文字
     * - [meters] 有值  → 有起點，顯示準心，顯示距離數值
     */
    private fun updateMeasureDistanceDisplay(meters: Double?) {
        if (meters == null) {
            binding.ivMeasureCrosshair.visibility = View.GONE
            binding.measurePanel.tvMeasureDistance.text = getString(R.string.measure_tap_hint)
        } else {
            binding.ivMeasureCrosshair.visibility = View.VISIBLE
            binding.measurePanel.tvMeasureDistance.text = DistanceMeasureManager.formatDistance(meters)
        }
    }
}
