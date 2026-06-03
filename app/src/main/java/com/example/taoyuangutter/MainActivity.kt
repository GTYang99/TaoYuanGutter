package com.example.taoyuangutter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.example.taoyuangutter.common.LocationPickEvents
import com.example.taoyuangutter.common.PhotoUriStore
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterApiClient
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NoDitchPoint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.GutterFormContract
import com.example.taoyuangutter.gutter.GutterFormNavigator
import com.example.taoyuangutter.gutter.GutterInspectActivity
import com.example.taoyuangutter.gutter.GutterSheetSessionBinder
import com.example.taoyuangutter.gutter.GutterSessionFlowCoordinator
import com.example.taoyuangutter.gutter.GutterSessionUiCoordinator
import com.example.taoyuangutter.gutter.InspectFlowCoordinator
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.example.taoyuangutter.login.AuthNavigator
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.main.MainBlockingUiController
import com.example.taoyuangutter.main.MeasureModeUiController
import com.example.taoyuangutter.main.NoDitchModeUiController
import com.example.taoyuangutter.map.DistanceMeasureManager
import com.example.taoyuangutter.map.GutterMapController
import com.example.taoyuangutter.map.InspectMarkerController
import com.example.taoyuangutter.map.LayersBottomSheet
import com.example.taoyuangutter.map.LegendBottomSheet
import com.example.taoyuangutter.map.MapCameraController
import com.example.taoyuangutter.map.MarkerIconFactory
import com.example.taoyuangutter.map.MapOverlayController
import com.example.taoyuangutter.map.MeasureConfig
import com.example.taoyuangutter.map.MyLocationController
import com.example.taoyuangutter.map.ScopeMapCoordinator
import com.example.taoyuangutter.map.ScopeGutterPolylineController
import com.example.taoyuangutter.map.ScopeViewportLoader
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterDraftCoordinator
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.DraftPhotoCleaner
import com.example.taoyuangutter.pending.PendingDraftSheetNavigator
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost,
    LayersBottomSheet.Host {

    companion object {
        private const val KEY_PENDING_WP_INDEX = "pending_wp_index"
        private const val GUTTER_LOAD_DEBOUNCE_MS = 500L
        const val EXTRA_OFFLINE_MAIN = "extra_offline_main"
    }

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null
    private var isOfflineMainMode: Boolean = false
    private var currentSheetBottomInsetPx: Int = 0
    private var addGutterBaseBottomMarginPx: Int? = null
    private var pickerBarBaseBottomMarginPx: Int? = null

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
            mapCameraController.fitCameraToWaypoints(
                sheet.getWaypoints(),
                bottomOffsetRatio = 0.8,
                resetPaddingAfter = false,
                maxZoom = 19f,
                paddingDp = 24
            )
        }
    }
    // ── 定位 ─────────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mainBlockingUiController: MainBlockingUiController
    /** 主地圖最近一次取得的定位（供表單/匯入既有點位快速使用，避免再次等待 GPS fix）。 */
    private var lastKnownLocation: Location? = null

    // ── Repository ───────────────────────────────────────────────────────
    private val gutterRepository = GutterRepository()
    private val sessionDraftRepository by lazy { GutterSessionRepository(this) }
    private val draftCoordinator by lazy { GutterDraftCoordinator(this, sessionDraftRepository) }
    private val pendingDraftSheetNavigator by lazy { PendingDraftSheetNavigator(supportFragmentManager) }
    private val gutterSheetSessionBinder = GutterSheetSessionBinder()
    private val gutterSessionFlowCoordinator = GutterSessionFlowCoordinator()
    private val gutterSessionUiCoordinator by lazy {
        GutterSessionUiCoordinator(
            flowCoordinator = gutterSessionFlowCoordinator,
            pendingDraftSheetNavigator = pendingDraftSheetNavigator
        )
    }
    private val authNavigator by lazy { AuthNavigator(this) }
    private val gutterFormNavigator by lazy { GutterFormNavigator(this) }
    private val markerIconFactory by lazy { MarkerIconFactory(this) }
    private val inspectFlowCoordinator by lazy {
        InspectFlowCoordinator(
            context = this,
            repository = gutterRepository
        )
    }
    private val gutterMapController by lazy {
        GutterMapController(
            mapProvider = { googleMap },
            markerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.icon(type, isPending, isVirtual)
            },
            polylineColorProvider = { isCurve -> resolveGutterPolylineColor() },
            pendingFlagParser = ::parseLooseBoolean
        )
    }
    private val mapCameraController by lazy {
        MapCameraController(
            context = this,
            mapProvider = { googleMap }
        )
    }
    private val myLocationController by lazy {
        MyLocationController(
            context = this,
            fusedLocationClient = fusedLocationClient,
            mapProvider = { googleMap }
        )
    }
    private val inspectMarkerController by lazy {
        InspectMarkerController(
            mapProvider = { googleMap },
            markerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.icon(type, isPending, isVirtual)
            },
            enlargedMarkerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.enlargedIcon(type, isPending, isVirtual)
            },
            pendingFlagParser = ::parseLooseBoolean,
            virtualFlagParser = ::parseLooseBoolean
        )
    }
    private val scopeGutterPolylineController by lazy {
        ScopeGutterPolylineController(
            mapProvider = { googleMap }
        )
    }
    private val scopeViewportLoader by lazy {
        ScopeViewportLoader(
            repository = gutterRepository,
            mapProvider = { googleMap }
        )
    }
    private val scopeMapCoordinator by lazy {
        ScopeMapCoordinator(
            viewportLoader = scopeViewportLoader,
            polylineController = scopeGutterPolylineController,
            savedGroupIdProvider = { LoginActivity.getSavedGroupId(this) }
        )
    }
    private val mapOverlayController by lazy {
        MapOverlayController(
            mapProvider = { googleMap },
            onNoDitchPointsLayerChanged = { enabled ->
                if (!enabled) clearNoDitchPointsMarkers()
            }
        )
    }

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
    private var inspectPreviewIntent: Intent? = null
    private var shouldReturnToInspectPreview = false
    /** 防止連點側溝 Polyline 重複觸發 openInspectBottomSheet */
    private var isInspecting = false

    // ── 編輯/檢視/新增模式標誌（防止自動加載polylines） ────────────────────
    /** true = 正在編輯/檢視/新增模式，禁止 loadGuttersByViewport 自動加載 */
    private var isInEditingMode = false

    // ── 檢視/編輯流程的灰色參考線（弧線/線段） ─────────────────────────────
    private var isReferenceRouteActive: Boolean = false
    private var referenceRoutePoints: List<LatLng> = emptyList()

    // ── 編輯模式：僅在 latLng 變更後顯示紫色線段 ─────────────────────────
    private var editLatLngSnapshot: List<Pair<Long, Long>>? = null
    private var hasShownEditPolyline: Boolean = false

    // ── 地圖疊加層 ────────────────────────────────────────────────────────
    private val submittedPolylines = mutableListOf<Polyline>()
    private var currentWaypoints: List<Waypoint> = emptyList()

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
    private lateinit var measureModeUiController: MeasureModeUiController
    private lateinit var noDitchModeUiController: NoDitchModeUiController

    // ── 回報無側溝 ──────────────────────────────────────────────────────────
    private var noDitchMarker: com.google.android.gms.maps.model.Marker? = null
    private var noDitchPickedLatLng: LatLng? = null
    private var isNoDitchPickMode: Boolean = false
    private var isNoDitchPickClickEnabled: Boolean = false

    // ── 無側溝點位互動 ──────────────────────────────────────────────────────
    private var noDitchPointsMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private var noDitchPoints = mutableListOf<NoDitchPoint>()

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()
        mainBlockingUiController = MainBlockingUiController(
            context = this,
            binding = binding,
            isMeasuring = { measureManager?.isMeasuring == true },
            isSheetActive = { activeSheet != null || inspectSheet != null || isInspecting }
        )
        measureModeUiController = MeasureModeUiController(
            context = this,
            binding = binding,
            measureConfig = measureConfig,
            onMainButtonsEnabledChanged = { enabled ->
                mainBlockingUiController.setMainButtonsEnabledDuringMeasureMode(enabled)
            }
        )

        measureModeUiController.setupPanelInsets()

        noDitchModeUiController = NoDitchModeUiController(
            context = this,
            binding = binding,
            onMainButtonsEnabledChanged = { enabled -> mainBlockingUiController.setMainButtonsEnabledDuringNoDitchMode(enabled) },
            onExitRequested = { exitNoDitchMode() },
            onResetRequested = { resetNoDitchPick() },
            onSubmitRequested = { note -> submitNoDitch(note) }
        )
        noDitchModeUiController.setupPanelInsets()
        noDitchModeUiController.bind()

        isOfflineMainMode = intent.getBooleanExtra(EXTRA_OFFLINE_MAIN, false)

        // 注册位置变更广播接收器（Android 12+需指定导出方式，此处为不导出）
        ContextCompat.registerReceiver(
            this,
            waypointLocationChangedReceiver,
            android.content.IntentFilter(LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED  // 使用完整限定名确保兼容性
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                myLocationController.enableMyLocationAndMove { location ->
                    lastKnownLocation = location
                }
            }
        }

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE
            // 從表單返回時，保險重繪灰色參考線（避免地圖被重建或被其他流程清掉）
            renderReferenceRouteIfActive()

            when {
                activeSheet != null -> {
                    val liveSheet = (supportFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet)
                        ?: activeSheet
                    activeSheet = liveSheet
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
                                liveSheet?.updateWaypointLocation(pendingWaypointFormIndex, LatLng(effectiveLat, effectiveLng))
                            }

	                            // ── 更新表單填寫的基本資料 ──────────────────────────
	                            // 先做照片 URI 正規化（避免多節點上傳照片卡住），再回寫 basicData，並立刻刷新工作層 marker
	                            val rawData = GutterFormContract.readResultData(result.data)
		                            lifecycleScope.launch {
		                                val newData = PhotoUriStore.normalizeBasicDataPhotoUris(
		                                    context = this@MainActivity,
		                                    basicData = rawData,
		                                    prefix = "GUTTER_EXT_"
		                                )
		                                liveSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
		                                // Ensure map markers reflect the latest flags (e.g., IS_PENDING_DEPLOY) immediately.
		                                currentWaypoints = liveSheet?.getWaypoints() ?: currentWaypoints
		                                refreshWorkingForEditFlow(currentWaypoints)

                                        // ── 資料更新後，才顯示 BottomSheet 並調整鏡頭 ──
                                        resetHighlightedMarker()
                                        pendingWaypointFormIndex = -1
                                        liveSheet?.showSelf()
                                        if (currentWaypoints.isNotEmpty()) {
                                            mapCameraController.fitCameraToWaypoints(
                                                currentWaypoints,
                                                bottomOffsetRatio = 0.52,
                                                resetPaddingAfter = false
                                            )
                                        }
		                            }
			                        }
                        GutterFormActivity.RESULT_DELETE -> if (pendingWaypointFormIndex >= 0) {
                            // 使用者放棄填寫 → 清除該點位的座標與資料（同時更新地圖大頭針）
                            liveSheet?.clearWaypointLocation(pendingWaypointFormIndex)
                            resetHighlightedMarker()
                            currentWaypoints = liveSheet?.getWaypoints() ?: currentWaypoints
                            refreshWorkingForEditFlow(currentWaypoints)
                            pendingWaypointFormIndex = -1
                            liveSheet?.showSelf()
                        }
                        Activity.RESULT_CANCELED -> {
                            // 使用者取消編輯點位：保留原資料，只恢復 AddGutterBottomSheet 可操作狀態
                            resetHighlightedMarker()
                            pendingWaypointFormIndex = -1
                            liveSheet?.showSelf()
                            currentWaypoints = liveSheet?.getWaypoints() ?: currentWaypoints
                            refreshWorkingForEditFlow(currentWaypoints)
                        }
                    }
	                }
	                inspectSheet != null -> {
	                    if (result.resultCode == Activity.RESULT_OK) {
	                        val data = result.data
	                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
		                        if (idx >= 0) {
		                            val rawData = GutterFormContract.readResultData(data)
		                            lifecycleScope.launch {
		                                val newData = PhotoUriStore.normalizeBasicDataPhotoUris(
		                                    context = this@MainActivity,
		                                    basicData = rawData,
		                                    prefix = "GUTTER_EXT_"
		                                )
		                                inspectWaypoints.getOrNull(idx)?.basicData = newData
		                            }
		                        }
		                    }
                    inspectSheet?.showSelf()
                    mapCameraController.fitCameraToWaypoints(inspectWaypoints)
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
                // 這裡會接著開啟 AddGutterBottomSheet，由其 binding 邏輯維持按鈕禁用狀態
                val json   = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_WAYPOINTS_JSON) ?: return@registerForActivityResult
                val spiNum = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_NUM) ?: ""
                val isCurveRaw = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_IS_CURVE) ?: "0"
                val isCurve = isCurveRaw.trim() == "1" || isCurveRaw.trim().equals("true", true)

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
                shouldReturnToInspectPreview = true

                // ── 進入編輯模式時：隱藏所有其他已存在的線段，只顯示正在編輯的側溝 ──
                isInEditingMode = true  // 禁止自動加載 polylines
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
                // 進入編輯時保留檢視期間的灰色參考線；紫色工作線延後到 latLng 真正變更才出現
                editLatLngSnapshot = buildLatLngSnapshot(wps)
                hasShownEditPolyline = false

                val sheet = AddGutterBottomSheet.newInstanceForEdit(wps, spiNum, isCurve)
                var lastWaypointsSize = wps.size
                sheet.onWaypointsChanged = { updated ->
                    if (updated == null) {
                        mapCameraController.setPersistentBottomInset(0)
                        mainBlockingUiController.setMainButtonsEnabled(true) // 關閉編輯表單，還原按鈕
                        activeSheet = null
                        val reopenInspectPreview =
                            shouldReturnToInspectPreview && inspectPreviewIntent != null
                        shouldReturnToInspectPreview = false
                        if (reopenInspectPreview) {
                            currentWaypoints = inspectWaypoints
                            // 返回檢視：維持灰色參考線，不顯示紫色工作線
                            refreshWorkingMarkers(inspectWaypoints)
                            mainBlockingUiController.setMainButtonsEnabled(false) // 重新進入檢視，保持虛化
                            val reopened = inspectPreviewIntent?.let { launchInspectSafely(Intent(it)) } == true
                            if (!reopened) {
                                isInEditingMode = false
                                clearReferenceRoute()
                                gutterMapController.clearPreviewLayer()
                                loadGuttersByViewport(showFeedback = true)
                            }
                        } else {
                            // ── 編輯 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
                            isInEditingMode = false  // 允許自動加載 polylines
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            // 重新加載所有正式線段與暫時提交線
                            loadGuttersByViewport(showFeedback = true)
                        }
                    } else {
                        val shouldRefit = updated.size > lastWaypointsSize
                        lastWaypointsSize = updated.size
                        currentWaypoints = updated.toMutableList()
                        val after = buildLatLngSnapshot(updated)
                        if (!hasShownEditPolyline) {
                            val before = editLatLngSnapshot
                            val latLngChanged = before == null || after != before
                            if (latLngChanged) {
                                hasShownEditPolyline = true
                                editLatLngSnapshot = after
                                refreshWorkingLayer(updated)
                            } else {
                                refreshWorkingMarkers(updated)
                            }
                        } else {
                            editLatLngSnapshot = after
                            refreshWorkingLayer(updated)
                        }
                        autoSaveSessionDraft(updated)
                        if (shouldRefit) {
                            mapCameraController.fitCameraToWaypoints(
                                updated,
                                bottomOffsetRatio = 0.5,
                                resetPaddingAfter = false
                            )
                        }
                    }
                }
                activeSheet = sheet
                sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)

                // 初始化地圖：繪製線段大頭針並將視角自動對齊至整條側溝
                currentWaypoints = wps.toMutableList()
                // 進入編輯時先不顯示紫色線段；等座標真的變更後才顯示
                refreshWorkingMarkers(wps)
                mapCameraController.fitCameraToWaypoints(wps)
                // fitCameraToWaypoints 會觸發 setOnCameraIdleListener → loadGuttersByViewportDebounced()
            } else {
                // ── 從檢視模式返回（不編輯）時，清除起終點標記並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                mainBlockingUiController.setMainButtonsEnabled(true) // 從檢視返回，還原按鈕
                clearReferenceRoute()
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()   // 移除檢視模式新增的起點／節點／終點標記
                loadGuttersByViewport(showFeedback = true)
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

    private fun applySystemBarInsets() {
        val addFab = binding.btnAddGutter
        if (addGutterBaseBottomMarginPx == null) {
            val lp = addFab.layoutParams as? ViewGroup.MarginLayoutParams
            addGutterBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }
        val bottomPickerBar = binding.root.findViewById<View>(R.id.bottomPickerBar)
        if (pickerBarBaseBottomMarginPx == null) {
            val lp = bottomPickerBar?.layoutParams as? ViewGroup.MarginLayoutParams
            pickerBarBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = max(systemBottom, imeBottom)

            val addBase = addGutterBaseBottomMarginPx ?: 0
            addFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = addBase + bottomInset
            }

            val pickerBase = pickerBarBaseBottomMarginPx ?: 0
            bottomPickerBar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = pickerBase + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
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
            bindAddGutterSheet(restoredSheet)
        } else {
            // 檢視模式：重新綁定 inspectSheet
            isInEditingMode = true  // 保持禁止自動加載 polylines
            inspectSheet = restoredSheet
            restoredSheet.onWaypointsChanged = { if (it == null) {
                // ── 檢視 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                clearWorkingMarkers()
                inspectSheet = null
                // 重新加載所有正式線段與暫時提交線
                loadGuttersByViewport(showFeedback = true)
            } }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 儲存正在開啟表單的點位索引，確保 MainActivity 重建後仍能正確回寫
        outState.putInt(KEY_PENDING_WP_INDEX, pendingWaypointFormIndex)
    }

    private fun launchInspectSafely(intent: Intent): Boolean {
        if (isFinishing || isDestroyed) return false
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return false
        return runCatching {
            inspectLauncher.launch(intent)
            true
        }.getOrElse {
            android.util.Log.w("MainActivity", "Skip inspect launch due to lifecycle state", it)
            false
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // 關閉 Google 預設底圖，改用 NLSC WMTS 圖層
        googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
        mapOverlayController.setBaseLayer(LayersBottomSheet.LAYER_EMAP)
        mapOverlayController.applyWmsOverlays()
        scopeGutterPolylineController.setVisible(mapOverlayController.currentState().showPlan)

        // 避免地圖初始化時短暫跳到 (0,0) 或不合理位置：先以桃園作為初始鏡頭
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(24.9929, 121.3011), 16f))
        myLocationController.requestLocationAndMove(
            requestPermission = {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            },
            onLocationUpdated = { location ->
                lastKnownLocation = location
            }
        )

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

            // 檢查是否為無側溝點位
            if (marker.tag is NoDitchPoint) {
                fetchAndShowNoDitchPointNote(marker)
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
        map.setOnMapClickListener { latLng -> handleMainMapTap(latLng) }

        // 地圖停止移動後，依目前可視範圍向後端查詢側溝線段（使用防抖避免高頻調用）
        if (!isOfflineMainMode) {
            map.setOnCameraIdleListener {
                loadGuttersByViewportDebounced()
                if (mapOverlayController.currentState().showNoDitchPoints) {
                    loadNoDitchPointsForVisibleArea()
                }
            }
        }

        // ── 測距管理器初始化（需在地圖就緒後才能建立） ────────────────────────
        // 起點大頭針使用 colorPrimary（紫色），與 App 配色一致
        measureConfig = MeasureConfig(
            startMarkerIcon = BitmapDescriptorFactory.defaultMarker(256f)
        )
        measureManager = DistanceMeasureManager(map, measureConfig) { meters ->
            updateMeasureDistanceDisplay(meters)
        }

        // 若在檢視/編輯流程中地圖被重建，確保灰色參考線能被重繪回來
        renderReferenceRouteIfActive()
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        activeSheet  = sheet
        pickingIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
        mapCameraController.setPersistentBottomInset(0)
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        // 隱藏 BottomSheet 並開始上傳動畫
        activeSheet?.hideSelf()
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onGutterRetry() {
        // 重傳時，確保 Activity 端的載入動畫重新顯示
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    /**
     * storeDitch 成功後，依照 API 回傳的 [nodes] 順序，
     * 找出對應 waypoint 的本機照片（content:// / file:// scheme）並上傳。
     * https:// 照片代表已在伺服器，略過。
     */
    /**
     * 上傳所有點位的本機照片，回傳失敗張數。
     * - 已是 https:// 的舊照片與空路徑會直接略過。
     * - 每張最多重試 3 次；仍失敗則即時 Toast 提示「第 x/total 張上傳失敗」。
     * - 全程顯示進度條「x / total 張照片上傳中」；無需上傳時不顯示，直接回傳 0。
     */
    private suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String
    ): Int {
        // 欄位只要有值就上傳：
        // - 本機 content:// / file:// 直接傳
        // - 遠端 http(s):// 先下載成本機，再以上傳流程重送
        // 使用 Triple<DitchNode, String, Int> 取代 local data class，避免 coroutine 編譯問題
        val pending = mutableListOf<Triple<DitchNode, String, Int>>()
        nodes.forEachIndexed { i, node ->
            val wp = waypoints.getOrNull(i) ?: return@forEachIndexed
            if (wp.isVirtual) {
                android.util.Log.d("PhotoUpload", "節點 ${node.nodeId} 為虛擬點，略過所有照片上傳")
                return@forEachIndexed
            }
            listOf(
                wp.basicData["photo1"] to 1,
                wp.basicData["photo2"] to 2,
                wp.basicData["photo3"] to 3
            ).forEach { (path, category) ->
                if (path.isNullOrEmpty()) return@forEach
                val scheme = Uri.parse(path).scheme?.lowercase()
                if (scheme != null) pending.add(Triple(node, path, category))
            }
        }

        val total = pending.size
        if (total == 0) return 0   // 無需上傳，直接結束（不顯示進度條）

        // 阻擋使用者操作（上傳期間不可操作 App；顯示等待動畫與進度說明）
        var failedCount = 0
        mainBlockingUiController.beginPhotoUpload(total)

        try {
            // 並行上傳（保守：最多同時 2 張）
            val semaphore = Semaphore(3)

            coroutineScope {
                pending.map { entry ->
                    launch {
                        semaphore.withPermit {
                            val node = entry.first
                            val path = entry.second
                            val category = entry.third

                            suspend fun downloadIfRemoteUrl(url: String): String? {
                                val scheme = Uri.parse(url).scheme?.lowercase()
                                if (scheme != "http" && scheme != "https") return url
                                val prefix = "REUPLOAD_${node.nodeId}_${category}_"
                                return gutterRepository
                                    .downloadImageToLocalContentUri(this@MainActivity, url, prefix = prefix)
                                    ?.toString()
                            }

                            // 最多重試 3 次
                            var success = false
                            var tempDownloadedUri: String? = null
                            for (attempt in 1..3) {
                                val uploadPath = downloadIfRemoteUrl(path) ?: break
                                if (uploadPath != path && Uri.parse(uploadPath).scheme?.lowercase() == "content") {
                                    tempDownloadedUri = uploadPath
                                }
                                when (val r = gutterRepository.uploadNodeImage(
                                    context = this@MainActivity,
                                    nodeId = node.nodeId,
                                    fileCategory = category,
                                    imageUri = Uri.parse(uploadPath),
                                    token = token
                                )) {
                                    is ApiResult.Success -> {
                                        success = true
                                    }
                                    is ApiResult.Error -> {
                                        android.util.Log.w(
                                            "PhotoUpload",
                                            "node${node.nodeId} photo$category attempt$attempt 失敗: ${r.message}"
                                        )
                                    }
                                }
                                if (success) break
                            }

                            // 立即清除「為了重傳而下載」的暫存照片檔，避免累積佔用空間。
                            // 注意：相機拍攝的草稿照片由 onGutterSaved 的 finally 統一清理。
                            tempDownloadedUri?.let { downloaded ->
                                DraftPhotoCleaner.deleteWaypointsLocalPhotos(
                                    context = this@MainActivity,
                                    waypoints = listOf(mapOf("photo1" to downloaded))
                                )
                            }

                            withContext(Dispatchers.Main) {
                                mainBlockingUiController.recordPhotoUploadResult(success)
                                if (!success) {
                                    failedCount += 1
                                    android.util.Log.w(
                                        "PhotoUpload",
                                        "node${node.nodeId} photo$category 上傳失敗（3次重試均失敗）"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return failedCount
        } finally {
            // 解除阻擋
            mainBlockingUiController.endPhotoUpload()
        }
    }

    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        // 斷開 onDismiss 回呼，避免 dismiss 後觸發重複清除；API 結果由 onGutterSaved 回報
        activeSheet?.onWaypointsChanged = null
        // 保留 baseline（灰色參考線）直到 onGutterSaved 確認 storeDitch 成功再清除
        gutterMapController.clearWorkingLayer()
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        mapCameraController.setPersistentBottomInset(0)
        mainBlockingUiController.setMainButtonsEnabled(true)

        // ── 退出編輯模式時：重新加載所有線段 ──
        isInEditingMode = false  // 允許自動加載 polylines
        loadGuttersByViewport(showFeedback = true)
    }

    private fun persistServerIdsIntoDraft(
        spiNum: String?,
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>
    ): Pair<List<Waypoint>, Long?> {
        val resolvedSpiNum = spiNum
            ?.takeIf { it.isNotBlank() }
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("SPI_NUM")

        val updatedWaypoints = waypoints.mapIndexed { index, waypoint ->
            val merged = HashMap(waypoint.basicData).apply {
                if (!resolvedSpiNum.isNullOrBlank()) {
                    put("SPI_NUM", resolvedSpiNum)
                }
                nodes.getOrNull(index)?.let { node ->
                    put("_nodeId", node.nodeId.toString())
                }
            }
            waypoint.copy(basicData = merged)
        }

        val saveResult = draftCoordinator.autoSaveSessionDraft(
            waypoints = updatedWaypoints,
            currentSessionDraftId = currentSessionDraftId,
            isOffline = currentSessionIsOffline,
            isCurve = activeSheet?.isCurveMode() ?: false
        )
        val savedDraftId = saveResult?.draftId ?: currentSessionDraftId
        currentSessionDraftId = savedDraftId
        return updatedWaypoints to savedDraftId
    }

    private fun finalizePhotoUploadFlow(
        spiNum: String?,
        persistedWaypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        pendingDraftId: Long?,
        token: String
    ) {
        lifecycleScope.launch {
            try {
                val failCount = uploadWaypointPhotos(
                    waypoints = persistedWaypoints,
                    nodes = nodes,
                    token = token
                )
                mainBlockingUiController.setInspectLoading(false)
                if (failCount == 0) {
                    activeSheet?.dismissAllowingStateLoss()
                    activeSheet = null
                    pendingDraftId?.let { draftId ->
                        draftCoordinator.deleteDraftAndLocalPhotos(
                            context = this@MainActivity,
                            draftId = draftId,
                            fallbackWaypoints = persistedWaypoints
                        )
                    }
                    currentSessionDraftId = null
                    if (!isFinishing && !isDestroyed && !spiNum.isNullOrBlank()) {
                        reopenInspectPreviewAfterUpdate(
                            spiNum = spiNum,
                            persistedWaypoints = persistedWaypoints,
                            token = token
                        )
                    } else if (!isFinishing && !isDestroyed) {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("上傳成功")
                            .setMessage(getString(R.string.msg_gutter_uploaded))
                            .setPositiveButton(getString(R.string.confirm), null)
                            .show()
                    }
                } else if (!isFinishing && !isDestroyed) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("上傳失敗")
                        .setMessage("線段資料與照片已完成本輪上傳，但仍有 $failCount 張照片失敗。")
                        .setNegativeButton("存入草稿") { _, _ ->
                            activeSheet?.dismissAllowingStateLoss()
                            activeSheet = null
                        }
                        .setPositiveButton("重傳") { _, _ ->
                            finalizePhotoUploadFlow(
                                spiNum = spiNum,
                                persistedWaypoints = persistedWaypoints,
                                nodes = nodes,
                                pendingDraftId = pendingDraftId,
                                token = token
                            )
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PhotoUpload", "uploadWaypointPhotos 例外: ${e.message}", e)
                mainBlockingUiController.setInspectLoading(false)
                if (!isFinishing && !isDestroyed) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("上傳失敗")
                        .setMessage("線段資料已送出，但照片上傳發生錯誤。")
                        .setNegativeButton("存入草稿") { _, _ ->
                            activeSheet?.dismissAllowingStateLoss()
                            activeSheet = null
                        }
                        .setPositiveButton("重傳") { _, _ ->
                            finalizePhotoUploadFlow(
                                spiNum = spiNum,
                                persistedWaypoints = persistedWaypoints,
                                nodes = nodes,
                                pendingDraftId = pendingDraftId,
                                token = token
                            )
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun reopenInspectPreviewAfterUpdate(
        spiNum: String,
        persistedWaypoints: List<Waypoint>,
        token: String
    ) {
        val latitudes = persistedWaypoints.mapNotNull { it.latLng?.latitude }.toDoubleArray()
        val longitudes = persistedWaypoints.mapNotNull { it.latLng?.longitude }.toDoubleArray()
        val routeWaypoints = persistedWaypoints.filter { it.latLng != null }
        val start = InspectFlowCoordinator.InspectStart(
            spiNum = spiNum,
            canEdit = true,
            latitudes = latitudes,
            longitudes = longitudes,
            routeWaypoints = routeWaypoints
        )

        lifecycleScope.launch {
            mainBlockingUiController.setInspectLoading(true, "載入更新後側溝資料中…")
            try {
                when (val result = inspectFlowCoordinator.load(start, token)) {
                    is ApiResult.Success -> {
                        scopeGutterPolylineController.clear()
                        submittedPolylines.forEach { it.remove() }
                        submittedPolylines.clear()
                        inspectPreviewIntent = Intent(result.data.intent)
                        shouldReturnToInspectPreview = false
                        inspectWaypoints = buildInspectWaypoints(result.data.nodeDetailsList)
                        currentWaypoints = inspectWaypoints
                        val isCurve = result.data.ditch.isCurve?.trim() == "1" ||
                            result.data.ditch.isCurve?.trim()?.equals("true", true) == true
                        // 檢視模式：顯示灰色參考線（使用 scopeSearch 線段點位），不畫紫色工作線
                        val referencePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        setReferenceRoute(referencePoints)
                        refreshWorkingMarkers(inspectWaypoints)
                        launchInspectSafely(result.data.intent)
                    }
                    is ApiResult.Error -> {
                        isInEditingMode = false
                        gutterMapController.clearPreviewLayer()
                        clearWorkingMarkers()
                        loadGuttersByViewport(showFeedback = true)
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("更新成功")
                            .setMessage("側溝已更新，但重新載入檢視資料失敗：${result.message}")
                            .setPositiveButton(getString(R.string.confirm), null)
                            .show()
                    }
                }
            } finally {
                mainBlockingUiController.setInspectLoading(false)
            }
        }
    }

    override fun onGutterSaved(spiNum: String?, waypoints: List<Waypoint>, nodes: List<DitchNode>) {
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        // 只有在 storeDitch 成功後才清除灰色參考線（reference）
        clearReferenceRoute()
        editLatLngSnapshot = null
        hasShownEditPolyline = false
        val (persistedWaypoints, pendingDraftId) = persistServerIdsIntoDraft(spiNum, waypoints, nodes)

        val token = LoginActivity.getSavedToken(this) ?: run {
            mainBlockingUiController.setInspectLoading(false)
            return
        }
        if (spiNum != null) {
            // 更新模式：移除舊線段，重載可視範圍
            scopeGutterPolylineController.remove(spiNum)
            loadGuttersByViewport()
        } else {
            // 新增成功後才清掉暫存預覽與工作圖層，失敗時保留原畫面供使用者重傳。
            activeSheet?.onWaypointsChanged = null
            gutterMapController.clearPreviewLayer()
            clearWorkingMarkers()
            binding.btnAddGutter.visibility = View.VISIBLE
            isInEditingMode = false
            mainBlockingUiController.setMainButtonsEnabled(true)
            drawSubmittedGutter(waypoints)
            mapCameraController.setPersistentBottomInset(0)
            // 新增成功後立即重載，以後端正式線段為準（同時會清掉暫時提交線）
            loadGuttersByViewport()
        }
        // storeDitch 成功後，等所有照片都跑完，再統一決定成功或失敗提示。
        finalizePhotoUploadFlow(
            spiNum = spiNum,
            persistedWaypoints = persistedWaypoints,
            nodes = nodes,
            pendingDraftId = pendingDraftId,
            token = token
        )
    }

    override fun onGutterSaveFailed(waypoints: List<Waypoint>) {
        mainBlockingUiController.setInspectLoading(false)
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
                            shouldReturnToInspectPreview = false
                            inspectPreviewIntent = null
                            draftCoordinator.deleteDraftsBySpiNum(this@MainActivity, spiNum)
                            if (currentSessionDraftId != null) {
                                val currentDraft = draftCoordinator.getDraft(currentSessionDraftId!!)
                                if (currentDraft == null) currentSessionDraftId = null
                            }

                            // 從地圖移除該側溝的線段
                            scopeGutterPolylineController.remove(spiNum)
                            // 關閉 BottomSheet 並清除暫存標記
                            activeSheet?.onWaypointsChanged = null
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            activeSheet?.dismiss()
                            activeSheet = null
                            clearWorkingMarkers()
                            isInEditingMode = false  // 刪除成功後退出編輯模式，允許重新加載 scope 線段
                            binding.btnAddGutter.visibility = View.VISIBLE
                            mapCameraController.setPersistentBottomInset(0)
                            mainBlockingUiController.setMainButtonsEnabled(true)
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
        val result = draftCoordinator.autoSaveSessionDraft(
            waypoints = waypoints,
            currentSessionDraftId = currentSessionDraftId,
            isOffline = currentSessionIsOffline,
            isCurve = activeSheet?.isCurveMode() ?: false
        ) ?: return
        currentSessionDraftId = result.draftId
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun onSheetViewportInsetChanged(bottomInsetPx: Int) {
        currentSheetBottomInsetPx = bottomInsetPx.coerceAtLeast(0)
        mapCameraController.setPersistentBottomInset(currentSheetBottomInsetPx)
    }

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
        mapCameraController.moveCameraToLatLngOffset(
            latLng,
            0.75,
            mapCameraController.zoomForGutterSize(wp.basicData)
        )
        val state = mapOverlayController.currentState()
        val intent = gutterFormNavigator.buildInspectIntent(
            waypointIndex = waypointIndex,
            waypoint = wp,
            latLng = latLng,
            wmtsLayer = currentWmtsLayer(),
            hostLastLocation = lastKnownLocation,
            referencePoints = if (isReferenceRouteActive) referenceRoutePoints else emptyList(),
            showPlan = state.showPlan,
            showWaterOld = state.showWaterOld,
            showPossible = state.showPossible,
            showRegion = state.showRegion
        )
        gutterFormLauncher.launch(intent)
    }

    private fun openAddForm(
        currentIndex: Int,
        wp: Waypoint,
        latLng: LatLng,
        isEditMode: Boolean = false
    ) {
        mapCameraController.moveCameraToLatLngOffset(
            latLng,
            0.75,
            mapCameraController.zoomForGutterSize(wp.basicData)
        )
        val state = mapOverlayController.currentState()
        val launch = gutterFormNavigator.buildAddIntent(
            currentWaypoints = currentWaypoints,
            currentIndex = currentIndex,
            waypoint = wp,
            isEditMode = isEditMode,
            currentSessionDraftId = currentSessionDraftId,
            wmtsLayer = currentWmtsLayer(),
            sessionIsOffline = currentSessionIsOffline,
            hostLastLocation = lastKnownLocation,
            referencePoints = if (isReferenceRouteActive) referenceRoutePoints else emptyList(),
            showPlan = state.showPlan,
            showWaterOld = state.showWaterOld,
            showPossible = state.showPossible,
            showRegion = state.showRegion
        )
        currentSessionDraftId = launch.draftId
        gutterFormLauncher.launch(launch.intent)
    }

    private fun currentWmtsLayer(): String = mapOverlayController.currentLayer()

    private fun fitCameraToAllGutters() {
        mapCameraController.fitCameraToTaggedPolylines(submittedPolylines)
    }

    @Suppress("UNCHECKED_CAST")
    private fun openInspectBottomSheet(polyline: Polyline) {
        if (isOfflineMainMode) return
        // 防止連點：若已在查詢或已有 inspect 畫面，直接忽略
        if (isInspecting) return
        disableNoDitchPointsOverlayIfNeeded()
        val token    = LoginActivity.getSavedToken(this)  ?: return
        val start = inspectFlowCoordinator.prepareStart(polyline) ?: return

        isInspecting = true
        // ── 進入檢視流程立即禁止自動加載，並虛化主畫面按鈕 ──
        isInEditingMode = true
        mainBlockingUiController.setMainButtonsEnabled(false)

        mainBlockingUiController.setInspectLoading(true, "載入側溝資料中…")
        mapCameraController.fitCameraToWaypoints(start.routeWaypoints)

        lifecycleScope.launch {
            try {
                when (val result = inspectFlowCoordinator.load(start, token)) {
                    is ApiResult.Success -> {
                        // 點擊地圖線段後一律進入「預覽模式」：
                        // 正式 scope 線段先全部清掉，改由 preview layer 畫紫色線與標點。
                        scopeGutterPolylineController.clear()
                        submittedPolylines.forEach { it.remove() }
                        submittedPolylines.clear()
                        inspectPreviewIntent = Intent(result.data.intent)
                        shouldReturnToInspectPreview = false
                        inspectWaypoints = buildInspectWaypoints(result.data.nodeDetailsList)
                        currentWaypoints = inspectWaypoints
                        val isCurve = result.data.ditch.isCurve?.trim() == "1" ||
                            result.data.ditch.isCurve?.trim()?.equals("true", true) == true
                        // 檢視模式：顯示灰色參考線（使用 scopeSearch 線段點位），不畫紫色工作線
                        val referencePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        setReferenceRoute(referencePoints)
                        refreshWorkingMarkers(inspectWaypoints)

                        // 若有進行中的新增流程 sheet，先隱藏它（不 dismiss，
                        // 讓使用者按返回時仍可繼續；若最終進入編輯模式則由 inspectLauncher 清除）
                        activeSheet?.hideSelf()
                        mainBlockingUiController.setInspectLoading(false)
                        launchInspectSafely(result.data.intent)
                        // isInspecting 在 inspectLauncher 結果回呼中重置
                    }
                    is ApiResult.Error -> {
                        isInspecting = false
                        isInEditingMode = false  // 允許自動加載 polylines
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            if (result.message == "查無側溝資料") getString(R.string.msg_no_line_data)
                            else "查詢失敗(${result.code}): ${result.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                // 若已成功 launch，前面已 setInspectLoading(false)；這裡保險起見再關一次
                mainBlockingUiController.setInspectLoading(false)
            }
        }
    }

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener {
            if (isOfflineMainMode) {
                // 離線填寫：直接回登入頁，不打 logout API
                authNavigator.clearAuthAndGoLogin()
                return@setOnClickListener
            }
            val token = LoginActivity.getSavedToken(this)
            if (token.isNullOrEmpty()) {
                // 本機無 token，直接跳登入頁
                authNavigator.goToLogin()
                return@setOnClickListener
            }
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                when (val result = gutterRepository.logout(token)) {
                    is ApiResult.Success -> {
                        authNavigator.clearAuthAndGoLogin()
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
        if (GutterApiClient.ENABLE_GROUP_SIMULATION) {
            binding.btnLogout.setOnLongClickListener {
                showGroupSimulationDialog()
                true
            }
        }
        binding.btnViewDrafts.setOnClickListener { showPendingDraftsSheet() }
        binding.btnLegend.setOnClickListener {
            LegendBottomSheet().show(supportFragmentManager, "LegendBottomSheet")
        }
        binding.btnLayers.setOnClickListener {
            val state = mapOverlayController.currentState()
            LayersBottomSheet.newInstance(
                selectedLayer = state.selectedLayer,
                showPlan = state.showPlan,
                showWaterOld = state.showWaterOld,
                showPossible = state.showPossible,
                showRegion = state.showRegion,
                showNoDitchPoints = state.showNoDitchPoints
            ).show(supportFragmentManager, "LayersBottomSheet")
        }
        binding.btnMyLocation.setOnClickListener {
            myLocationController.requestLocationAndMove(
                requestPermission = {
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                },
                onLocationUpdated = { location ->
                    lastKnownLocation = location
                }
            )
        }
        binding.btnReportNoDitch.setOnClickListener { openNoDitchReport() }
        binding.btnMeasureDistance.setOnClickListener { toggleMeasureMode() }
        binding.measurePanel.btnMeasureReset.setOnClickListener { measureManager?.reset() }
        binding.measurePanel.btnMeasureClose.setOnClickListener { exitMeasureMode() }
        binding.btnAddGutter.setOnClickListener {
            openAddGutterFlow()
        }
    }

    private fun openNoDitchReport() {
        // 避免測距模式與回報模式同時占用 map click listener
        if (measureManager?.isMeasuring == true) exitMeasureMode()
        if (isNoDitchPickMode) return
        enterNoDitchMode()
    }

    /** 原本的「新增側溝」流程，從 FAB 移入獨立方法。 */
    private fun openAddGutterFlow() {
        disableNoDitchPointsOverlayIfNeeded()
        gutterSessionUiCoordinator.startAddSession(
            isOfflineMainMode = isOfflineMainMode,
            hooks = buildSessionUiHooks()
        )
    }


    /** 顯示「待上傳草稿」BottomSheet，並處理「繼續編輯」回呼。 */
    private fun showPendingDraftsSheet() {
        gutterSessionUiCoordinator.showPendingDrafts { draft -> resumePendingDraft(draft) }
    }

    /**
     * 從待上傳草稿恢復 AddGutterBottomSheet。
     *
     * 先 dismiss PendingDraftsBottomSheet（與任何現有 activeSheet），
     * 再以 [View.post] 延到下一幀才 show AddGutterBottomSheet，
     * 確保兩個 FragmentTransaction 不會同時競爭同一個 FragmentManager。
     */
    private fun resumePendingDraft(draft: GutterSessionDraft) {
        gutterSessionUiCoordinator.resumeDraft(
            draft = draft,
            context = this,
            isOfflineMainMode = isOfflineMainMode,
            hooks = buildSessionUiHooks()
        )
    }

    private fun buildSessionUiHooks(): GutterSessionUiCoordinator.Hooks {
        return GutterSessionUiCoordinator.Hooks(
            isHostFinishing = { isFinishing || isDestroyed },
            prepareForNewMapSession = {
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                fitCameraToAllGutters()
                isInEditingMode = true
                mainBlockingUiController.setMainButtonsEnabled(false) // 立即虛化背景按鈕
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            prepareForResumedMapSession = {
                activeSheet?.dismissAllowingStateLoss()
                activeSheet = null
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                isInEditingMode = true
                mainBlockingUiController.setMainButtonsEnabled(false) // 立即虛化背景按鈕
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            bindSheet = { sheet, initialWaypointCount ->
                bindAddGutterSheet(sheet, initialWaypointCount)
            },
            showSheet = { sheet ->
                sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
            },
            onMapSessionReady = { draftId, isOffline, sheet ->
                currentSessionDraftId = draftId
                currentSessionIsOffline = isOffline
                activeSheet = sheet
            },
            onResumedWaypointsReady = { waypoints ->
                currentWaypoints = waypoints
                refreshWorkingLayer(waypoints)
            },
            onRefitRequested = { waypoints ->
                mapCameraController.fitCameraToWaypoints(
                    waypoints,
                    bottomOffsetRatio = 0.5,
                    resetPaddingAfter = false
                )
            },
            onReloadRequested = {
                loadGuttersByViewport()
            },
            launchOfflineForm = { intent ->
                startActivity(intent)
            }
        )
    }

    private fun bindAddGutterSheet(
        sheet: AddGutterBottomSheet,
        initialWaypointCount: Int = 0
    ) {
        gutterSheetSessionBinder.bind(
            sheet = sheet,
            config = GutterSheetSessionBinder.Config(
                initialWaypointCount = initialWaypointCount,
                refitOnGrowth = true
            ),
            hooks = GutterSheetSessionBinder.Hooks(
                onWaypointsUpdated = { waypoints ->
                    currentWaypoints = waypoints
                    refreshWorkingLayer(waypoints)
                    // 當 BottomSheet 有內容時，確保背景按鈕虛化
                    mainBlockingUiController.setMainButtonsEnabled(false)
                },
                onWaypointsCleared = {
                    isInEditingMode = false
                    mainBlockingUiController.setMainButtonsEnabled(true) // 關閉表單，還原按鈕
                    mapCameraController.setPersistentBottomInset(0)
                    gutterMapController.clearPreviewLayer()
                    activeSheet = null
                    if (!isOfflineMainMode) {
                        loadGuttersByViewport(showFeedback = true)
                    } else {
                        refreshWorkingLayer(emptyList())
                    }
                },
                onAutoSaveRequested = { waypoints ->
                    autoSaveSessionDraft(waypoints)
                },
                onRefitRequested = { waypoints ->
                    mapCameraController.fitCameraToWaypoints(
                        waypoints,
                        bottomOffsetRatio = 0.5,
                        resetPaddingAfter = false
                    )
                }
            )
        )
    }

    private fun refreshWorkingLayer(waypoints: List<Waypoint>) {
        gutterMapController.refreshWorkingLayer(
            waypoints = waypoints,
            isCurve = activeSheet?.isCurveMode() == true
        )
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        gutterMapController.refreshWorkingMarkers(waypoints)
    }

    /**
     * 編輯流程用刷新：僅在 latLng 真正改變後才開始顯示紫色線段。
     * - 在使用者還沒移動/更新座標前：只更新 markers（保留灰色參考線作對照）
     * - 一旦座標改變：開始顯示/更新紫色線段
     */
    private fun refreshWorkingForEditFlow(waypoints: List<Waypoint>) {
        // 非編輯流程（例如新增/檢視）沿用原本行為
        if (activeSheet == null || activeSheet?.isEditMode() != true) {
            refreshWorkingLayer(waypoints)
            return
        }

        val after = buildLatLngSnapshot(waypoints)
        if (!hasShownEditPolyline) {
            val before = editLatLngSnapshot
            val latLngChanged = before == null || after != before
            if (latLngChanged) {
                hasShownEditPolyline = true
                editLatLngSnapshot = after
                refreshWorkingLayer(waypoints)
            } else {
                refreshWorkingMarkers(waypoints)
            }
        } else {
            editLatLngSnapshot = after
            refreshWorkingLayer(waypoints)
        }
    }

    private fun buildLatLngSnapshot(waypoints: List<Waypoint>): List<Pair<Long, Long>> {
        fun quantize(v: Double): Long = kotlin.math.round(v * 1_000_000.0).toLong()
        return waypoints.mapNotNull { it.latLng }
            .map { quantize(it.latitude) to quantize(it.longitude) }
            .sortedWith(compareBy({ it.first }, { it.second }))
    }

    private fun setReferenceRoute(points: List<LatLng>) {
        isReferenceRouteActive = true
        referenceRoutePoints = points
        gutterMapController.showReferenceRoute(points)
    }

    private fun clearReferenceRoute() {
        isReferenceRouteActive = false
        referenceRoutePoints = emptyList()
        gutterMapController.clearReferenceRoute()
    }

    private fun renderReferenceRouteIfActive() {
        if (!isReferenceRouteActive) return
        gutterMapController.showReferenceRoute(referenceRoutePoints)
    }

    private fun clearWorkingMarkers() {
        gutterMapController.clearWorkingMarkers()
        inspectMarkerController.clear()
    }

    private fun parseLooseBoolean(raw: String?): Boolean {
        val v = raw?.trim()?.lowercase()
        return when (v) {
            "1", "true", "t", "y", "yes" -> true
            else -> false
        }
    }


    private fun drawSubmittedGutter(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return
        val polyline = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(resolveGutterPolylineColor())
                .width(10f)
                .geodesic(true)
                .clickable(true)
        )
        polyline.tag = ArrayList(waypoints)
        submittedPolylines.add(polyline)
    }

    private fun buildInspectWaypoints(
        nodes: List<com.example.taoyuangutter.api.NodeDetails>
    ): List<Waypoint> {
        return nodes.mapNotNull { node ->
            val lat = node.latitude?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = node.longitude?.toDoubleOrNull() ?: return@mapNotNull null
            val type = when (node.nodeAttr) {
                "1" -> WaypointType.START
                "3" -> WaypointType.END
                else -> WaypointType.NODE
            }
            val label = when (type) {
                WaypointType.START -> "起點"
                WaypointType.END -> "終點"
                WaypointType.NODE -> "節點"
            }
            Waypoint(
                type = type,
                label = label,
                latLng = LatLng(lat, lng),
                basicData = hashMapOf(
                    "IS_PENDING_DEPLOY" to (if (parseLooseBoolean(node.isPendingDeploy)) "1" else "0"),
                    "is_virtual" to (if (parseLooseBoolean(node.isVirtual)) "1" else "0")
                )
            )
        }
    }

    /**
     * 側溝線段顏色決策入口（弧線/非弧線）。
     * 目前先維持既有顏色；後續要改弧線配色只需改這裡。
     */
    private fun resolveGutterPolylineColor(): Int {
        return Color.parseColor("#562ECB")
    }

    private fun highlightMarker(waypointIndex: Int) {
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        inspectMarkerController.highlightMarker(waypointIndex, waypoints)
    }

    private fun resetHighlightedMarker() {
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        inspectMarkerController.resetHighlightedMarker(waypoints)
    }

    // ── 側溝座標 API（scopeSearch）────────────────────────────────────────

    /**
     * 防抖版本的 loadGuttersByViewport
     * 避免短時間內多次調用 API（例如快速拖拽地圖時）
     */
    private fun loadGuttersByViewportDebounced() {
        scopeMapCoordinator.loadDebounced(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = false),
            hooks = buildScopeLoadHooks(),
            debounceMs = GUTTER_LOAD_DEBOUNCE_MS
        )
    }

    /**
     * 取得目前地圖可視範圍（LatLngBounds）並呼叫 scopeSearch API，
     * 成功後呼叫 [drawScopePolylines] 更新地圖上的線段。
     */
    private fun loadGuttersByViewport(showFeedback: Boolean = false) {
        scopeMapCoordinator.load(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = showFeedback),
            hooks = buildScopeLoadHooks()
        )
    }

    private fun buildScopeLoadConfig(showFeedback: Boolean): ScopeMapCoordinator.Config {
        return ScopeMapCoordinator.Config(
            isOfflineMode = isOfflineMainMode,
            isBlocked = isInEditingMode,
            token = LoginActivity.getSavedToken(this),
            showFeedback = showFeedback
        )
    }

    private fun buildScopeLoadHooks(): ScopeMapCoordinator.Hooks {
        return ScopeMapCoordinator.Hooks(
            onBeforeDraw = {
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            onLoadingStarted = {
                Toast.makeText(this, getString(R.string.msg_loading_gutters), Toast.LENGTH_SHORT).show()
            },
            onLoadingFinished = {
                Toast.makeText(this, getString(R.string.msg_loading_gutters_done), Toast.LENGTH_SHORT).show()
            },
            onLoadingFailed = {
                Toast.makeText(this, getString(R.string.msg_loading_gutters_failed), Toast.LENGTH_SHORT).show()
            },
            onSilentError = { message ->
                android.util.Log.w("ScopeSearch", "查詢失敗: $message")
            }
        )
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

    override fun onLayerSelected(layer: String) {
        mapOverlayController.setBaseLayer(layer)
    }

    override fun onOverlayTogglesChanged(
        showPlan: Boolean,
        showWaterOld: Boolean,
        showPossible: Boolean,
        showRegion: Boolean,
        showNoDitchPoints: Boolean
    ) {
        mapOverlayController.updateOverlayToggles(showPlan, showWaterOld, showPossible, showRegion, showNoDitchPoints)
        scopeGutterPolylineController.setVisible(showPlan)
        if (showNoDitchPoints) {
            loadNoDitchPointsForVisibleArea()
        }
    }

    private fun disableNoDitchPointsOverlayIfNeeded() {
        val state = mapOverlayController.currentState()
        if (!state.showNoDitchPoints) return
        onOverlayTogglesChanged(
            showPlan = state.showPlan,
            showWaterOld = state.showWaterOld,
            showPossible = state.showPossible,
            showRegion = state.showRegion,
            showNoDitchPoints = false
        )
    }

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
        measureModeUiController.enter(measureManager)
    }

    /** 離開測距模式：隱藏準星與底部面板，還原 FAB 樣式。 */
    private fun exitMeasureMode() {
        measureModeUiController.exit(measureManager)
    }

    /**
     * 更新底部面板距離文字，並同步控制準心顯示。
     * - [meters] 為 null → 無起點，隱藏準心，顯示提示文字
     * - [meters] 有值  → 有起點，顯示準心，顯示距離數值
     */
    private fun updateMeasureDistanceDisplay(meters: Double?) {
        measureModeUiController.updateDistanceDisplay(meters)
    }

    // ── 回報無側溝（BottomSheet）──────────────────────────────────────────

    private fun enterNoDitchMode() {
        isNoDitchPickMode = true
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.enter()
        noDitchModeUiController.setPickedLatLng(null)
        setNoDitchMapClickListenerEnabled(true)
    }

    private fun exitNoDitchMode() {
        isNoDitchPickMode = false
        setNoDitchMapClickListenerEnabled(false)
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.exit()
    }

    private fun resetNoDitchPick() {
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.setPickedLatLng(null)
        if (isNoDitchPickMode) setNoDitchMapClickListenerEnabled(true)
    }

    private fun submitNoDitch(note: String) {
        val latLng = noDitchPickedLatLng ?: return
        val token = LoginActivity.getSavedToken(this)
        if (token.isNullOrEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.no_ditch_mode_title))
                .setMessage(getString(R.string.msg_not_logged_in))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        noDitchModeUiController.setSubmitting(true)
        lifecycleScope.launch {
            val result = gutterRepository.storeNoDitch(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                note = note,
                token = token
            )
            noDitchModeUiController.setSubmitting(false)

            when (result) {
                is ApiResult.Success -> {
                    val message = result.data.message ?: getString(R.string.msg_submit_success)
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(getString(R.string.no_ditch_mode_title))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            exitNoDitchMode()
                        }
                        .show()
                }
                is ApiResult.Error -> {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(getString(R.string.no_ditch_mode_title))
                        .setMessage(result.message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun setNoDitchMapClickListenerEnabled(enabled: Boolean) {
        isNoDitchPickClickEnabled = enabled
    }

    private fun handleMainMapTap(latLng: LatLng) {
        if (isNoDitchPickClickEnabled && isNoDitchPickMode && noDitchPickedLatLng == null) {
            onNoDitchLatLngPicked(latLng)
            return
        }
        if (!isNoDitchPickMode && mapOverlayController.currentState().showNoDitchPoints) {
            fetchAndShowNoDitchPointNoteAt(latLng, showNoNoteToast = false)
        }
    }

    private fun onNoDitchLatLngPicked(latLng: LatLng) {
        val map = googleMap ?: return
        noDitchPickedLatLng = latLng
        clearNoDitchMarker()
        noDitchMarker = map.addMarker(
            com.google.android.gms.maps.model.MarkerOptions()
                .position(latLng)
                .icon(buildNoDitchMarkerIcon())
                .anchor(0.5f, 1.0f)
        )
        // 選點後禁止再次點擊地圖，直到按下「重設點位」
        setNoDitchMapClickListenerEnabled(false)
        noDitchModeUiController.setPickedLatLng(latLng)
    }

    private fun showGroupSimulationDialog() {
        val options = arrayOf("管理員 (Group 1)", "廠商 A (Group 2)", "廠商 B (Group 3)", "廠商 C (Group 4)")
        val ids = intArrayOf(1, 2, 3, 4)
        val currentId = LoginActivity.getSavedGroupId(this)

        MaterialAlertDialogBuilder(this)
            .setTitle("模擬廠商切換 (目前: $currentId)")
            .setItems(options) { _, which ->
                val targetId = ids[which]
                getSharedPreferences("taoyuan_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("group_id", targetId)
                    .apply()
                
                Toast.makeText(this, "身分已切換為 Group $targetId", Toast.LENGTH_SHORT).show()
                loadGuttersByViewport(showFeedback = true)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearNoDitchMarker() {
        noDitchMarker?.remove()
        noDitchMarker = null
    }

    private fun buildNoDitchMarkerIcon(): com.google.android.gms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_noditch_pin)
            ?: return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
            )
        val sizePx = (60f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // ── 無側溝點位載入與互動 ──────────────────────────────────────────────

    private fun loadNoDitchPointsForVisibleArea() {
        val map = googleMap ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        val bbox = buildNoDitchPointsWfsBbox(bounds)

        lifecycleScope.launch {
            try {
                val response = GutterApiClient.instance.getNoDitchPointsByBbox(bbox = bbox)

                if (response.isSuccessful) {
                    val features = response.body()?.features ?: emptyList()
                    updateNoDitchPointsMarkers(features.map { feature ->
                        // 優先嘗試從 properties 取得座標，若無則從 geometry 取得
                        val lat = if (feature.properties.latitude != 0.0) feature.properties.latitude else feature.geometry.coordinates.getOrNull(1) ?: 0.0
                        val lng = if (feature.properties.longitude != 0.0) feature.properties.longitude else feature.geometry.coordinates.getOrNull(0) ?: 0.0
                        
                        NoDitchPoint(
                            id = feature.id,
                            latitude = lat,
                            longitude = lng,
                            note = feature.properties.note
                        )
                    })
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to load no ditch points", e)
            }
        }
    }

    private fun updateNoDitchPointsMarkers(points: List<NoDitchPoint>) {
        val map = googleMap ?: return

        // 清除舊的markers
        noDitchPointsMarkers.forEach { it.remove() }
        noDitchPointsMarkers.clear()

        // 添加新的markers
        points.forEach { point ->
            val marker = map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(point.latLng)
                    .icon(buildNoDitchPointIcon())
                    .anchor(0.5f, 1.0f)
                    .title(point.note ?: "無側溝點位")
            )
            marker?.tag = point
            marker?.let { noDitchPointsMarkers.add(it) }
        }

        noDitchPoints = points.toMutableList()
    }

    private fun clearNoDitchPointsMarkers() {
        noDitchPointsMarkers.forEach { it.remove() }
        noDitchPointsMarkers.clear()
        noDitchPoints.clear()
    }

    private fun buildNoDitchPointIcon(): com.google.android.gms.maps.model.BitmapDescriptor {
        // 無側溝點位互動 Marker 圖示
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_noditch_point)
            ?: return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
            )
        val sizePx = (20f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun buildNoDitchPointsWfsBbox(bounds: com.google.android.gms.maps.model.LatLngBounds): String {
        val sw = bounds.southwest
        val ne = bounds.northeast
        return "${sw.longitude},${sw.latitude},${ne.longitude},${ne.latitude},EPSG:4326"
    }

    private fun buildNoDitchPointsWmsBbox(bounds: com.google.android.gms.maps.model.LatLngBounds): String {
        val sw = bounds.southwest
        val ne = bounds.northeast
        // WMS 1.3.0 + EPSG:4326：常見軸序為 lat,lng
        return "${sw.latitude},${sw.longitude},${ne.latitude},${ne.longitude}"
    }

    private fun fetchAndShowNoDitchPointNote(marker: com.google.android.gms.maps.model.Marker) {
        fetchAndShowNoDitchPointNoteAt(marker.position, showNoNoteToast = true)
    }

    private fun fetchAndShowNoDitchPointNoteAt(
        targetLatLng: LatLng,
        showNoNoteToast: Boolean
    ) {
        val map = googleMap ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        val mapView = binding.map
        val width = mapView.width
        val height = mapView.height
        if (width <= 0 || height <= 0) return

        val bbox = buildNoDitchPointsWmsBbox(bounds)
        val screenPoint = map.projection.toScreenLocation(targetLatLng)

        lifecycleScope.launch {
            try {
                val response = GutterApiClient.instance.getNoDitchPointsFeatureInfo(
                    bbox = bbox,
                    width = width,
                    height = height,
                    i = screenPoint.x,
                    j = screenPoint.y,
                    featureCount = 10
                )
                if (!response.isSuccessful) return@launch

                val note = response.body()
                    ?.features
                    ?.asSequence()
                    ?.mapNotNull { it.properties.note }
                    ?.firstOrNull()

                if (note.isNullOrBlank()) {
                    if (!showNoNoteToast) return@launch
                    Toast.makeText(this@MainActivity, "此點位無備註", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("無側溝點位備註")
                        .setMessage(note)
                        .setPositiveButton("確定", null)
                        .show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to fetch no ditch point note", e)
            }
        }
    }
}
