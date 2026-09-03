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
import androidx.activity.viewModels
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
import com.example.taoyuangutter.common.PhotoImgIdTraceDebugger
import com.example.taoyuangutter.common.PhotoUriStore
import com.example.taoyuangutter.common.UploadFailureClassifier
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterApiClient
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NoDitchPoint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.PhotoUploadManager
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
import com.example.taoyuangutter.main.MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM
import com.example.taoyuangutter.main.MainMapLocationRecenterReloadTracker
import com.example.taoyuangutter.main.MainMapLoadIndicatorController
import com.example.taoyuangutter.main.MainViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost,
    LayersBottomSheet.Host {

    private fun logPhotoImgIdTrace(stage: String, data: Map<String, String>) {
        val summary = (1..3).joinToString(" | ") { slot ->
            "p$slot(photo=${!data["photo$slot"].isNullOrBlank()},imgId=${data["photo${slot}ImgId"] ?: "-"},state=${data["photo${slot}UploadState"] ?: "-"})"
        }
        PhotoImgIdTraceDebugger.record(
            owner = "MainActivity",
            stage = stage,
            imgIds = listOf(data["photo1ImgId"], data["photo2ImgId"], data["photo3ImgId"]),
            summary = summary
        )
    }

    companion object {
        private const val KEY_PENDING_WP_INDEX = "pending_wp_index"
        private const val GUTTER_LOAD_DEBOUNCE_MS = 500L
        const val EXTRA_OFFLINE_MAIN = "extra_offline_main"
    }

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
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
    private lateinit var mainMapLoadIndicatorController: MainMapLoadIndicatorController
    /** 主地圖最近一次取得的定位（供表單/匯入既有點位快速使用，避免再次等待 GPS fix）。 */
    private var lastKnownLocation: Location? = null

    // ── Repository ───────────────────────────────────────────────────────
    private val gutterRepository = GutterRepository()
    private val photoUploadManager by lazy { PhotoUploadManager(this, gutterRepository) }
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
            loadViewport = scopeViewportLoader::load,
            drawFeatures = { features, savedGroupId ->
                scopeGutterPolylineController.drawFeatures(features, savedGroupId)
            },
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
    /** 編輯更新時的照片原始快照，供上傳流程判斷哪些 slot 真正變更。 */
    private var pendingEditOriginalWaypoints: List<WaypointSnapshot>? = null
    /** true = 目前這次 session 是由待上傳草稿恢復而來，照片需全量上傳。 */
    private var currentSessionResumedFromDraft: Boolean = false
    /** true = 此次 session 為離線草稿（只存本機，不打 API） */
    private var currentSessionIsOffline: Boolean = false
    /** 編輯模式開始時，側溝的原始狀態（用於判斷是否觸發「恢復狀態」詢問）。 */
    private var initialSpiState: String? = null

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
    private data class PendingInspectPreviewReload(
        val spiNum: String,
        val waypoints: List<Waypoint>,
        val token: String
    )
    private var pendingInspectPreviewReload: PendingInspectPreviewReload? = null
    /** 防止連點側溝 Polyline 重複觸發 openInspectBottomSheet */
    private var isInspecting = false
    /** 檢視流程鎖：從開啟檢視到真正關閉前，主畫面按鈕都保持不可用。 */
    private var isInspectUiLocked = false

    // ── 編輯/檢視/新增模式標誌（防止自動加載polylines） ────────────────────
    /** true = 正在編輯/檢視/新增模式，禁止 loadGuttersByViewport 自動加載 */
    private var isInEditingMode = false
    private var isMainMapGestureActive = false
    private var pendingUserLocationRecenter = false
    private var pendingLocationRecenterReload = false
    private val locationRecenterReloadTracker = MainMapLocationRecenterReloadTracker()
    private data class PendingForceReload(
        val id: Long,
        val reason: String
    )
    private var pendingForceReload: PendingForceReload? = null
    private var forceReloadInFlightId: Long? = null
    private var nextForceReloadId = 0L

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
        draftCoordinator.cleanupEmptyDrafts()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()
        mainBlockingUiController = MainBlockingUiController(
            context = this,
            binding = binding,
            isMeasuring = { measureManager?.isMeasuring == true },
            isSheetActive = { activeSheet != null || inspectSheet != null || isInspecting || isInspectUiLocked }
        )
        mainMapLoadIndicatorController = MainMapLoadIndicatorController(
            context = this,
            binding = binding
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
        mainMapLoadIndicatorController.setBottomInset(currentSheetBottomInsetPx)

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
                    handleMainMapLocationUpdated(location)
                }
            } else {
                pendingUserLocationRecenter = false
                locationRecenterReloadTracker.cancelPendingLocationMove()
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
                            val returnedDraftId = data?.getLongExtra(GutterFormActivity.EXTRA_SESSION_DRAFT_ID, 0L) ?: 0L
                            if (returnedDraftId > 0L) {
                                currentSessionDraftId = returnedDraftId
                            }

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
                                logPhotoImgIdTrace("gutterFormLauncher.rawResult", rawData)
		                            lifecycleScope.launch {
                                val newData = PhotoUriStore.normalizeBasicDataPhotoUris(
                                    context = this@MainActivity,
                                    basicData = rawData,
                                    prefix = "GUTTER_EXT_"
                                )
                                logPhotoImgIdTrace("gutterFormLauncher.normalizedResult", newData)
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
                        val returnedDraftId = data?.getLongExtra(GutterFormActivity.EXTRA_SESSION_DRAFT_ID, 0L) ?: 0L
                        if (returnedDraftId > 0L) {
                            currentSessionDraftId = returnedDraftId
                        }
                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
		                        if (idx >= 0) {
		                            val rawData = GutterFormContract.readResultData(data)
                                logPhotoImgIdTrace("inspectBranch.rawResult", rawData)
                            lifecycleScope.launch {
                                val newData = PhotoUriStore.normalizeBasicDataPhotoUris(
                                    context = this@MainActivity,
                                    basicData = rawData,
                                    prefix = "GUTTER_EXT_"
                                )
                                logPhotoImgIdTrace("inspectBranch.normalizedResult", newData)
                                inspectWaypoints.getOrNull(idx)?.basicData = newData
                            }
                        }
		                    }
                    inspectSheet?.showSelf()
                    mapCameraController.fitCameraToWaypointsWithViewportFraction(
                        inspectWaypoints,
                        viewportHeightFraction = 1.0 / 3.0
                    )
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
                val spiTyp = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_TYP) ?: ""
                val spiState = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_STATE) ?: ""
                initialSpiState = spiState
                val isCurveRaw = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_IS_CURVE) ?: "0"
                val isCurve = isCurveRaw.trim() == "1" || isCurveRaw.trim().equals("true", true)

                // ── 若 FM 內仍有同 TAG 的舊 sheet（例如先前新增流程的 activeSheet）
                //    必須先 dismiss 並等待事務完成，否則 show() 會因 TAG 衝突而失敗 ──
                val oldSheet = (supportFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG)
                        as? AddGutterBottomSheet)
                oldSheet?.onWaypointsChanged = null
                oldSheet?.dismissAllowingStateLoss()
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
                    Waypoint(wpType, snap.label, latLng, snap.basicData, snap.uid.ifBlank { snap.basicData["_nodeId"] ?: "${wpType.name}_${snap.label}" })
                }

                currentSessionDraftId = null   // 編輯模式開始 → 以新 session ID 追蹤草稿
                currentSessionResumedFromDraft = false
                shouldReturnToInspectPreview = true

                // ── 進入編輯模式時：先顯示紫色主線，真的變動後再把原始路徑轉為灰色 ──
                isInEditingMode = true  // 禁止自動加載 polylines
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
                // 保留原始路徑資料，但先不顯示灰色參考線
                editLatLngSnapshot = buildLatLngSnapshot(wps)
                hasShownEditPolyline = false

                val sheet = AddGutterBottomSheet.newInstanceForEdit(wps, spiNum, isCurve, spiTyp)
                var lastWaypointsSize = wps.size
                sheet.onWaypointsChanged = { updated ->
                    if (updated == null) {
                        mapCameraController.setPersistentBottomInset(0)
                        activeSheet = null
                        val reopenInspectPreview =
                            shouldReturnToInspectPreview && inspectPreviewIntent != null
                        shouldReturnToInspectPreview = false
                        if (reopenInspectPreview) {
                            currentWaypoints = inspectWaypoints
                            // 返回檢視：維持灰色參考線，不顯示紫色工作線
                            refreshWorkingMarkers(inspectWaypoints)
                            lockInspectUi()
                            val reopened = inspectPreviewIntent?.let { launchInspectSafely(Intent(it)) } == true
                            if (!reopened) {
                                isInEditingMode = false
                                inspectPreviewIntent = null
                                shouldReturnToInspectPreview = false
                                clearReferenceRoute()
                                gutterMapController.clearPreviewLayer()
                                unlockInspectUiIfIdle()
                                loadGuttersByViewport(showFeedback = true)
                            }
                        } else {
                            // ── 編輯 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
                            isInEditingMode = false  // 允許自動加載 polylines
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            // 重新加載所有正式線段與暫時提交線
                            unlockInspectUiIfIdle()
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
                                if (!isReferenceRouteActive && referenceRoutePoints.size >= 2) {
                                    setReferenceRoute(referenceRoutePoints)
                                }
                                hasShownEditPolyline = true
                            }
                        }
                        editLatLngSnapshot = after
                        refreshWorkingLayer(updated)
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
                mapCameraController.fitCameraToWaypointsWithViewportFraction(
                    wps,
                    viewportHeightFraction = 1.0 / 3.0
                )
                // fitCameraToWaypoints 會觸發 setOnCameraIdleListener → loadGuttersByViewportDebounced()
            } else {
                // ── 從檢視模式返回（不編輯）時，清除起終點標記並恢復其他線段顯示 ──
                isInEditingMode = false  // 允許自動加載 polylines
                inspectPreviewIntent = null
                shouldReturnToInspectPreview = false
                unlockInspectUiIfIdle()
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
        currentSessionDraftId = if (savedState.containsKey("saved_session_draft_id")) savedState.getLong("saved_session_draft_id") else null
        currentSessionResumedFromDraft = savedState.getBoolean("saved_session_resumed_from_draft", false)
        currentSessionIsOffline = savedState.getBoolean("saved_session_is_offline", false)
        isInspecting = savedState.getBoolean("saved_is_inspecting", false)
        isInEditingMode = savedState.getBoolean("saved_is_in_editing_mode", false)
        initialSpiState = savedState.getString("saved_initial_spi_state")

        val currentWpsJson = savedState.getString("saved_current_waypoints_json")
        if (!currentWpsJson.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = try { Gson().fromJson(currentWpsJson, type) } catch (e: Exception) { emptyList() }
            currentWaypoints = snapshots.map { snap ->
                val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null) LatLng(snap.latitude, snap.longitude) else null
                Waypoint(wpType, snap.label, latLng, snap.basicData, snap.uid.ifBlank { snap.basicData["_nodeId"] ?: "${wpType.name}_${snap.label}" })
            }
        }

        val inspectWpsJson = savedState.getString("saved_inspect_waypoints_json")
        if (!inspectWpsJson.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = try { Gson().fromJson(inspectWpsJson, type) } catch (e: Exception) { emptyList() }
            inspectWaypoints = snapshots.map { snap ->
                val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null) LatLng(snap.latitude, snap.longitude) else null
                Waypoint(wpType, snap.label, latLng, snap.basicData, snap.uid.ifBlank { snap.basicData["_nodeId"] ?: "${wpType.name}_${snap.label}" })
            }
        }

        isReferenceRouteActive = savedState.getBoolean("saved_is_reference_route_active", false)
        val refLats = savedState.getDoubleArray("saved_reference_route_lats")
        val refLngs = savedState.getDoubleArray("saved_reference_route_lngs")
        if (refLats != null && refLngs != null && refLats.size == refLngs.size) {
            referenceRoutePoints = refLats.indices.map { LatLng(refLats[it], refLngs[it]) }
        }

        hasShownEditPolyline = savedState.getBoolean("saved_has_shown_edit_polyline", false)
        val editSnapshotJson = savedState.getString("saved_edit_lat_lng_snapshot_json")
        if (!editSnapshotJson.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<List<Pair<Long, Long>>>() {}.type
            editLatLngSnapshot = try { Gson().fromJson(editSnapshotJson, type) } catch (e: Exception) { null }
        }

        val restoredSheet = supportFragmentManager
            .findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet
            ?: return

        if (restoredSheet.isAddMode()) {
            // 新增模式：重新綁定 activeSheet 與 onWaypointsChanged
            activeSheet = restoredSheet
            bindAddGutterSheet(restoredSheet)
        } else {
            // 檢視模式：重新綁定 inspectSheet
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
        currentSessionDraftId?.let { outState.putLong("saved_session_draft_id", it) }
        outState.putBoolean("saved_session_resumed_from_draft", currentSessionResumedFromDraft)
        outState.putBoolean("saved_session_is_offline", currentSessionIsOffline)
        outState.putBoolean("saved_is_inspecting", isInspecting)
        outState.putBoolean("saved_is_in_editing_mode", isInEditingMode)
        outState.putString("saved_initial_spi_state", initialSpiState)

        val currentWpsSnapshots = currentWaypoints.map { wp ->
            WaypointSnapshot(type = wp.type.name, label = wp.label, latitude = wp.latLng?.latitude, longitude = wp.latLng?.longitude, basicData = wp.basicData, uid = wp.uid)
        }
        outState.putString("saved_current_waypoints_json", Gson().toJson(currentWpsSnapshots))

        val inspectWpsSnapshots = inspectWaypoints.map { wp ->
            WaypointSnapshot(type = wp.type.name, label = wp.label, latitude = wp.latLng?.latitude, longitude = wp.latLng?.longitude, basicData = wp.basicData, uid = wp.uid)
        }
        outState.putString("saved_inspect_waypoints_json", Gson().toJson(inspectWpsSnapshots))

        outState.putBoolean("saved_is_reference_route_active", isReferenceRouteActive)
        if (referenceRoutePoints.isNotEmpty()) {
            outState.putDoubleArray("saved_reference_route_lats", referenceRoutePoints.map { it.latitude }.toDoubleArray())
            outState.putDoubleArray("saved_reference_route_lngs", referenceRoutePoints.map { it.longitude }.toDoubleArray())
        }

        outState.putBoolean("saved_has_shown_edit_polyline", hasShownEditPolyline)
        if (editLatLngSnapshot != null) {
            outState.putString("saved_edit_lat_lng_snapshot_json", Gson().toJson(editLatLngSnapshot))
        }
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

        // Restore overlay state if it exists in ViewModel
        mainViewModel.overlayState?.let { savedState ->
            mapOverlayController.applyState(savedState)
            scopeGutterPolylineController.setVisible(savedState.showPlan)
        } ?: run {
            mapOverlayController.setBaseLayer(LayersBottomSheet.LAYER_EMAP)
            mapOverlayController.applyWmsOverlays()
            scopeGutterPolylineController.setVisible(mapOverlayController.currentState().showPlan)
        }
        mapOverlayController.ensureMeasureLabelsOverlay()

        // 避免地圖初始化時短暫跳到 (0,0) 或不合理位置：先以桃園作為初始鏡頭
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(24.9929, 121.3011), 16f))
        locationRecenterReloadTracker.expectReloadAfterLocationMove()
        myLocationController.requestLocationAndMove(
            requestPermission = {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            },
            onLocationUpdated = { location ->
                handleMainMapLocationUpdated(location)
            }
        )

        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = false
            isZoomControlsEnabled = false
        }

        map.setOnCameraMoveStartedListener { reason ->
            handleMainMapCameraMoveStarted(reason)
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
                handleMainMapCameraIdle()
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

        // 若在檢視/編輯流程中地圖被重建，確保灰色參考線與工作層能被重繪回來
        renderReferenceRouteIfActive()
        if (isInEditingMode) {
            if (activeSheet != null) {
                refreshWorkingLayer(currentWaypoints)
                mainBlockingUiController.setMainButtonsEnabled(false)
            } else if (inspectSheet != null) {
                refreshWorkingLayer(inspectWaypoints, inspectSheet?.isCurveMode() == true)
                mainBlockingUiController.setMainButtonsEnabled(false)
            }
        }

        if (!isOfflineMainMode) {
            requestBackgroundScopeRefresh()
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
        mapCameraController.setPersistentBottomInset(0)
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        // 隱藏 BottomSheet 並開始上傳動畫
        activeSheet?.hideSelf()
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onPendingPhotoUploadStarted(totalCount: Int) {
        if (totalCount <= 0) return
        activeSheet?.hideSelf()
        mainBlockingUiController.beginPhotoUpload(totalCount)
    }

    override fun onPendingPhotoUploadProgress(success: Boolean) {
        mainBlockingUiController.recordPhotoUploadResult(success)
    }

    override fun onPendingPhotoUploadFinished() {
        mainBlockingUiController.endPhotoUpload()
    }

    override fun onGutterRetry() {
        // 重傳時，確保 Activity 端的載入動畫重新顯示
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onGutterSubmitting() {
        activeSheet?.hideSelf()
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onStoreDitchNetworkClosed(spiNum: String?, waypoints: List<Waypoint>) {
        mainBlockingUiController.setInspectLoading(false)
        saveWaypointsAsPendingDraft(waypoints)
        activeSheet?.onWaypointsChanged = null
        activeSheet?.dismissAllowingStateLoss()
        activeSheet = null

        val resolvedSpiNum = spiNum?.takeIf { it.isNotBlank() }
            ?: waypoints.firstOrNull { it.type == WaypointType.START }
                ?.basicData?.get("SPI_NUM")
                ?.takeIf { it.isNotBlank() }

        if (resolvedSpiNum.isNullOrBlank()) {
            shouldReturnToInspectPreview = false
            inspectPreviewIntent = null
            pendingInspectPreviewReload = null
            inspectSheet?.onWaypointsChanged = null
            inspectSheet?.dismissAllowingStateLoss()
            inspectSheet = null
            isInEditingMode = false
            clearReferenceRoute()
            gutterMapController.clearPreviewLayer()
            clearWorkingMarkers()
            mapCameraController.setPersistentBottomInset(0)
            restoreMainUiAfterSheetClosed()
            loadGuttersByViewport(showFeedback = true)
            return
        }

        val token = LoginActivity.getSavedToken(this)
        pendingInspectPreviewReload = token?.let {
            PendingInspectPreviewReload(
                spiNum = resolvedSpiNum,
                waypoints = waypoints.toList(),
                token = it
            )
        }
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        isInEditingMode = false
        clearReferenceRoute()
        gutterMapController.clearPreviewLayer()
        clearWorkingMarkers()
        mapCameraController.setPersistentBottomInset(0)
        restoreMainUiAfterSheetClosed()
        loadGuttersByViewport(showFeedback = true)
    }

    /**
     * 上傳所有點位的本機照片，回傳批次結果。
     * 委派給 [PhotoUploadManager] 處理並行上傳、重試與暫存清理，
     * 透過 [PhotoUploadManager.UploadListener] 橋接 [MainBlockingUiController] 的 UI 進度顯示。
     */
    private suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String,
        originalWaypoints: List<WaypointSnapshot>? = null
    ): PhotoUploadManager.UploadBatchResult {
        // 先計算待上傳數量以決定是否需要顯示進度 UI
        val pendingCount = photoUploadManager.countPendingPhotos(
            waypoints = waypoints,
            nodes = nodes,
            originalWaypoints = originalWaypoints
        )
        if (pendingCount == 0) return PhotoUploadManager.UploadBatchResult.Completed(0)

        mainBlockingUiController.beginPhotoUpload(pendingCount)
        try {
            return photoUploadManager.uploadWaypointPhotos(
                waypoints = waypoints,
                nodes = nodes,
                token = token,
                originalWaypoints = originalWaypoints,
                listener = object : PhotoUploadManager.UploadListener {
                    override fun onProgressUpdate(completedCount: Int, totalCount: Int) {
                        // PhotoUploadManager 已在 Main thread 回呼
                    }
                    override fun onPhotoUploadResult(success: Boolean) {
                        mainBlockingUiController.recordPhotoUploadResult(success)
                    }
                }
            )
        } finally {
            mainBlockingUiController.endPhotoUpload()
        }
    }

    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        pendingEditOriginalWaypoints = activeSheet?.getOriginalWaypointSnapshots()
        // 斷開 onDismiss 回呼，避免 dismiss 後觸發重複清除；API 結果由 onGutterSaved 回報
        activeSheet?.onWaypointsChanged = null
        // 保留 baseline（灰色參考線）直到 onGutterSaved 確認 storeDitch 成功再清除
        gutterMapController.clearWorkingLayer()
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        mapCameraController.setPersistentBottomInset(0)
        setMainButtonsEnabledRespectingInspectLock(true)

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
            spiTyp = resolveCurrentSessionSpiTyp(updatedWaypoints),
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
        token: String,
        originalWaypoints: List<WaypointSnapshot>? = null
    ) {
        lifecycleScope.launch {
            try {
                val uploadWaypoints = if (currentSessionResumedFromDraft) {
                    persistedWaypoints
                } else {
                    buildEditPhotoUploadWaypoints(
                        waypoints = persistedWaypoints,
                        originalWaypoints = originalWaypoints
                    )
                }
                android.util.Log.d(
                    "PhotoUpload",
                    "photo upload mode=${if (currentSessionResumedFromDraft) "FULL_DRAFT" else "EDIT_DIFF_OR_DEFAULT"}, resumedFromDraft=$currentSessionResumedFromDraft, originalWaypoints=${originalWaypoints?.size ?: 0}"
                )
                when (val result = uploadWaypointPhotos(
                    waypoints = uploadWaypoints,
                    nodes = nodes,
                    token = token,
                    originalWaypoints = originalWaypoints
                )) {
                    is PhotoUploadManager.UploadBatchResult.Completed -> {
                        val failCount = result.failCount
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
                            currentSessionResumedFromDraft = false

                            if (initialSpiState == "2" && !spiNum.isNullOrBlank()) {
                                // 重設狀態，避免之後重複觸發
                                initialSpiState = null
                                showRestoreStateDialog(spiNum, persistedWaypoints, token)
                            } else if (!isFinishing && !isDestroyed && !spiNum.isNullOrBlank()) {
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
                            val errorUi = UploadFailureClassifier.forPhotoBatchFailures(result.failures)
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("上傳失敗")
                                .setMessage(errorUi.buildDialogMessage())
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
                                        token = token,
                                        originalWaypoints = originalWaypoints
                                    )
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                    is PhotoUploadManager.UploadBatchResult.TimedOut -> {
                        mainBlockingUiController.setInspectLoading(false)
                        if (!isFinishing && !isDestroyed) {
                            showPhotoUploadTimeoutAlert(
                                UploadFailureClassifier.forPhotoTimeout(
                                    failCount = result.failCount,
                                    completedCount = result.completedCount
                                ).buildDialogMessage()
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PhotoUpload", "uploadWaypointPhotos 例外: ${e.message}", e)
                mainBlockingUiController.setInspectLoading(false)
                if (!isFinishing && !isDestroyed) {
                    val errorUi = UploadFailureClassifier.forPhotoException(e.localizedMessage)
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("上傳失敗")
                        .setMessage(errorUi.buildDialogMessage())
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
                                token = token,
                                originalWaypoints = originalWaypoints
                            )
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    /**
     * 編輯更新時，若某個 node 的照片與原始版本完全相同，就把該 slot 清空，
     * 讓後續既有的照片上傳流程自然跳過。
     *
     * 這只影響 edit update 的「上傳用副本」，不改動 storeDitch、草稿或原始 waypoints。
     */
    private fun buildEditPhotoUploadWaypoints(
        waypoints: List<Waypoint>,
        originalWaypoints: List<WaypointSnapshot>?
    ): List<Waypoint> {
        if (originalWaypoints.isNullOrEmpty()) return waypoints

        val originalByUid = originalWaypoints
            .mapNotNull { snapshot ->
                val uid = snapshot.uid.takeIf { it.isNotBlank() }
                if (uid.isNullOrBlank()) null else uid to snapshot
            }
            .toMap()

        return waypoints.map { waypoint ->
            val currentNodeId = waypoint.basicData["_nodeId"]?.takeIf { it.isNotBlank() }
            val original = waypoint.uid.takeIf { it.isNotBlank() }?.let { originalByUid[it] }
                ?: originalWaypoints.firstOrNull {
                    it.basicData["_nodeId"]?.takeIf { id -> id.isNotBlank() } == currentNodeId
                }
            if (original == null) return@map waypoint

            val originalNodeId = original.basicData["_nodeId"]?.takeIf { it.isNotBlank() } ?: currentNodeId ?: "unknown"
            val merged = HashMap(waypoint.basicData)
            var changed = false

            for (slot in 1..3) {
                val photoKey = "photo$slot"
                val capturedAtKey = "photo${slot}CapturedAt"
                val currentPhoto = merged[photoKey]?.trim().orEmpty()
                val originalPhoto = original.basicData[photoKey]?.trim().orEmpty()
                val currentCapturedAt = merged[capturedAtKey]?.trim().orEmpty()
                val originalCapturedAt = original.basicData[capturedAtKey]?.trim().orEmpty()

                val sameByCapturedAt =
                    currentCapturedAt.isNotEmpty() &&
                        currentCapturedAt == originalCapturedAt &&
                        originalCapturedAt.isNotEmpty()
                val sameByPhotoPath =
                    currentPhoto.isNotEmpty() && currentPhoto == originalPhoto

                if (sameByCapturedAt || sameByPhotoPath) {
                    merged[photoKey] = ""
                    merged[capturedAtKey] = ""
                    changed = true
                    android.util.Log.d(
                        "PhotoUpload",
                        "skip unchanged photo upload: nodeId=$originalNodeId slot=$slot by=${if (sameByCapturedAt) "capturedAt" else "photoPath"}"
                    )
                }
            }

            if (!changed) waypoint else waypoint.copy(basicData = merged)
        }
    }

    private fun showPhotoUploadTimeoutAlert(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.msg_photo_upload_timeout_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                activeSheet?.dismissAllowingStateLoss()
                activeSheet = null
                restoreMainUiAfterSheetClosed()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRestoreStateDialog(
        spiNum: String,
        persistedWaypoints: List<Waypoint>,
        token: String
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle("確認是否變更狀態")
            .setMessage("目前側溝為「待修正」狀態，確認後將變更至「待修正」之前的狀態。")
            .setPositiveButton("是") { _, _ ->
                performRestoreState(spiNum, persistedWaypoints, token)
            }
            .setNegativeButton("否") { _, _ ->
                // 即使選「否」，仍要重新開啟預覽以反映資料更新
                reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
            }
            .setCancelable(false)
            .show()
    }

    private fun performRestoreState(
        spiNum: String,
        persistedWaypoints: List<Waypoint>,
        token: String
    ) {
        lifecycleScope.launch {
            mainBlockingUiController.setInspectLoading(true, "正在變更側溝狀態…")
            val result = gutterRepository.updateDitchState(spiNum = spiNum, token = token)
            mainBlockingUiController.setInspectLoading(false)

            when (result) {
                is ApiResult.Success -> {
                    // 成功後，重新開啟預覽
                    reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
                }
                is ApiResult.Error -> {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("狀態變更失敗")
                        .setMessage(result.message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // 失敗後仍應開啟預覽（資料已更新，只是狀態沒變回）
                            reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
                        }
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
                        val referencePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        referenceRoutePoints = referencePoints
                        isReferenceRouteActive = false
                        refreshWorkingLayer(inspectWaypoints, isCurve)
                        lockInspectUi()
                        val launched = launchInspectSafely(result.data.intent)
                        if (!launched) {
                            isInEditingMode = false
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            clearWorkingMarkers()
                            unlockInspectUiIfIdle()
                            loadGuttersByViewport(showFeedback = true)
                        }
                    }
                    is ApiResult.Error -> {
                        isInEditingMode = false
                        inspectPreviewIntent = null
                        shouldReturnToInspectPreview = false
                        gutterMapController.clearPreviewLayer()
                        clearWorkingMarkers()
                        unlockInspectUiIfIdle()
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
            // 更新模式：斷開監聽以避免 dismiss 觸發 redundant reload，移除舊線段，重載可視範圍
            activeSheet?.onWaypointsChanged = null
            scopeGutterPolylineController.remove(spiNum)
            loadGuttersByViewport()
        } else {
            // 新增成功後才清掉暫存預覽與工作圖層，失敗時保留原畫面供使用者重傳。
            activeSheet?.onWaypointsChanged = null
            gutterMapController.clearPreviewLayer()
            clearWorkingMarkers()
            binding.btnAddGutter.visibility = View.VISIBLE
            isInEditingMode = false
            setMainButtonsEnabledRespectingInspectLock(true)
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
            token = token,
            originalWaypoints = pendingEditOriginalWaypoints
        )
        pendingEditOriginalWaypoints = null
    }

    override fun onGutterSaveFailed(waypoints: List<Waypoint>) {
        mainBlockingUiController.setInspectLoading(false)
        saveWaypointsAsPendingDraft(waypoints)
    }

    override fun onGutterSubmitFailed() {
        mainBlockingUiController.setInspectLoading(false)
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
                                if (currentDraft == null) {
                                    currentSessionDraftId = null
                                    currentSessionResumedFromDraft = false
                                }
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
                            restoreMainUiAfterSheetClosed()
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

    private fun resolveCurrentSessionSpiTyp(waypoints: List<Waypoint>): String? {
        return activeSheet?.getSelectedSpiTypCode()
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("SPI_TYP")
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("NODE_TYP")
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
            spiTyp = resolveCurrentSessionSpiTyp(waypoints),
            isOffline = currentSessionIsOffline,
            isCurve = activeSheet?.isCurveMode() ?: false
        ) ?: return
        currentSessionDraftId = result.draftId
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun onSheetViewportInsetChanged(bottomInsetPx: Int) {
        currentSheetBottomInsetPx = bottomInsetPx.coerceAtLeast(0)
        mapCameraController.setPersistentBottomInset(currentSheetBottomInsetPx)
        mainMapLoadIndicatorController.setBottomInset(currentSheetBottomInsetPx)
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
        val sessionDraftId = ensureCurrentSessionDraftId()
        val launch = gutterFormNavigator.buildAddIntent(
            currentWaypoints = currentWaypoints,
            currentIndex = currentIndex,
            waypoint = wp,
            isEditMode = isEditMode,
            currentSessionDraftId = sessionDraftId,
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

    private fun ensureCurrentSessionDraftId(): Long {
        currentSessionDraftId?.let { return it }
        return System.currentTimeMillis().also { currentSessionDraftId = it }
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
        val token    = LoginActivity.getSavedToken(this)  ?: return
        val start = inspectFlowCoordinator.prepareStart(polyline) ?: return

        isInspecting = true
        // ── 進入檢視流程立即禁止自動加載，並虛化主畫面按鈕 ──
        isInEditingMode = true
        lockInspectUi()

        mainBlockingUiController.setInspectLoading(true, "載入側溝資料中…")
        mapCameraController.fitCameraToWaypointsWithViewportFraction(
            start.routeWaypoints,
            viewportHeightFraction = 1.0 / 3.0
        )

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
                        // 檢視模式：先顯示紫色主線；等使用者真的變更座標後，再把原始路徑轉為灰色參考線
                        val referencePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        referenceRoutePoints = referencePoints
                        isReferenceRouteActive = false
                        refreshWorkingLayer(inspectWaypoints, isCurve)

                        // 若有進行中的新增流程 sheet，先隱藏它（不 dismiss，
                        // 讓使用者按返回時仍可繼續；若最終進入編輯模式則由 inspectLauncher 清除）
                        activeSheet?.hideSelf()
                        mainBlockingUiController.setInspectLoading(false)
                        val launched = launchInspectSafely(result.data.intent)
                        if (!launched) {
                            isInspecting = false
                            isInEditingMode = false
                            inspectPreviewIntent = null
                            shouldReturnToInspectPreview = false
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            clearWorkingMarkers()
                            unlockInspectUiIfIdle()
                            loadGuttersByViewport(showFeedback = true)
                        }
                        // isInspecting 在 inspectLauncher 結果回呼中重置
                    }
                    is ApiResult.Error -> {
                        isInspecting = false
                        isInEditingMode = false  // 允許自動加載 polylines
                        inspectPreviewIntent = null
                        shouldReturnToInspectPreview = false
                        unlockInspectUiIfIdle()
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
            pendingUserLocationRecenter = true
            locationRecenterReloadTracker.expectReloadAfterLocationMove()
            myLocationController.requestLocationAndMove(
                requestPermission = {
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                },
                onLocationUpdated = { location ->
                    handleMainMapLocationUpdated(location)
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
        currentSessionResumedFromDraft = false
        gutterSessionUiCoordinator.startAddSession(
            isOfflineMainMode = isOfflineMainMode,
            hooks = buildSessionUiHooks()
        )
    }

    private fun lockInspectUi() {
        isInspectUiLocked = true
        mainBlockingUiController.setMainButtonsEnabled(false)
    }

    private fun unlockInspectUiIfIdle() {
        if (activeSheet != null || inspectSheet != null || isInspecting || measureManager?.isMeasuring == true) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        isInspectUiLocked = false
        mainBlockingUiController.setMainButtonsEnabled(true)
        consumePendingForceReloadIfPossible()
    }

    private fun setMainButtonsEnabledRespectingInspectLock(enabled: Boolean) {
        if (enabled && isInspectUiLocked) {
            mainBlockingUiController.setMainButtonsEnabled(false)
        } else {
            mainBlockingUiController.setMainButtonsEnabled(enabled)
        }
    }

    /**
     * 在 sheet / dialog 收尾後，嘗試把主畫面恢復到可操作狀態。
     * 只有在沒有任何 loading、沒有任何 sheet、也沒有測距時才會真的解除 inspect lock。
     */
    private fun restoreMainUiAfterSheetClosed() {
        if (mainBlockingUiController.isBusyBlocking()) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        if (activeSheet != null || inspectSheet != null || isInspecting || measureManager?.isMeasuring == true) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        isInspectUiLocked = false
        mainBlockingUiController.setMainButtonsEnabled(true)
        consumePendingForceReloadIfPossible()
    }


    /** 顯示「待上傳草稿」BottomSheet，並處理「繼續編輯」回呼。 */
    private fun showPendingDraftsSheet() {
        if (isInspectUiLocked || isInspecting || shouldReturnToInspectPreview) return
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
        currentSessionResumedFromDraft = true
        gutterSessionUiCoordinator.resumeDraft(
            draft = draft,
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
                activeSheet?.onWaypointsChanged = null
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
                draftCoordinator.ensureDraftExists(
                    draftId = draftId,
                    waypoints = sheet.getWaypoints(),
                    spiTyp = sheet.getSelectedSpiTypCode(),
                    isOffline = isOffline,
                    isCurve = sheet.isCurveMode()
                )
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
                    currentSessionDraftId?.let { draftCoordinator.deleteDraftIfEffectivelyEmpty(it) }
                    currentSessionDraftId = null
                    currentSessionResumedFromDraft = false
                    isInEditingMode = false
                    mapCameraController.setPersistentBottomInset(0)
                    gutterMapController.clearPreviewLayer()
                    activeSheet = null
                    restoreMainUiAfterSheetClosed() // 關閉表單後，若沒有其他阻擋才還原按鈕
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

    private fun refreshWorkingLayer(waypoints: List<Waypoint>, isCurve: Boolean? = null) {
        gutterMapController.refreshWorkingLayer(
            waypoints = waypoints,
            isCurve = isCurve ?: activeSheet?.isCurveMode() == true
        )
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        gutterMapController.refreshWorkingMarkers(waypoints)
    }

    /**
     * 編輯流程用刷新：僅在座標順序或內容真的改變後才開始顯示紫色線段。
     * - 在使用者還沒移動/更新座標前：只更新 markers（保留灰色參考線作對照）
     * - 一旦節點順序或座標改變：開始顯示/更新紫色線段
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
                if (!isReferenceRouteActive && referenceRoutePoints.size >= 2) {
                    setReferenceRoute(referenceRoutePoints)
                }
                hasShownEditPolyline = true
            }
        }
        editLatLngSnapshot = after
        refreshWorkingLayer(waypoints)
    }

    private fun buildLatLngSnapshot(waypoints: List<Waypoint>): List<Pair<Long, Long>> {
        fun quantize(v: Double): Long = kotlin.math.round(v * 1_000_000.0).toLong()
        return waypoints.mapNotNull { it.latLng }
            .map { quantize(it.latitude) to quantize(it.longitude) }
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
                ),
                uid = node.nodeId?.toString() ?: "${type.name}_${lat}_${lng}"
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

    private fun handleMainMapCameraMoveStarted(reason: Int) {
        when (reason) {
            GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                isMainMapGestureActive = true
                mainMapLoadIndicatorController.prepareForNewOperation(currentMainMapZoom())
            }
            GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION,
            GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> {
                isMainMapGestureActive = false
            }
            else -> {
                isMainMapGestureActive = false
            }
        }
    }

    private fun handleMainMapCameraIdle() {
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)

        if (pendingLocationRecenterReload) {
            pendingLocationRecenterReload = false
            requestForceScopeReload("location recenter")
            return
        }

        if (isMainMapGestureActive) {
            isMainMapGestureActive = false
            requestUserInteractionScopeSearch()
            return
        }

        consumePendingForceReloadIfPossible()
    }

    private fun currentMainMapZoom(): Float {
        return googleMap?.cameraPosition?.zoom ?: 0f
    }

    private fun handleMainMapLocationUpdated(location: Location) {
        lastKnownLocation = location
        if (locationRecenterReloadTracker.consumeReloadOnLocationUpdated()) {
            pendingLocationRecenterReload = true
            pendingUserLocationRecenter = false
        }
    }

    private fun requestUserInteractionScopeSearch() {
        if (isOfflineMainMode) return
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        if (!canRunMainMapScopeQuery()) return
        scopeMapCoordinator.loadDebounced(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = true),
            hooks = buildScopeLoadHooks(),
            debounceMs = GUTTER_LOAD_DEBOUNCE_MS
        )
    }

    private fun requestBackgroundScopeRefresh() {
        if (isOfflineMainMode) return
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        if (!canRunMainMapScopeQuery()) return
        scopeMapCoordinator.load(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = false),
            hooks = buildScopeLoadHooks()
        )
    }

    private fun requestForceScopeReload(reason: String) {
        if (isOfflineMainMode) return
        val token = LoginActivity.getSavedToken(this)
        if (token.isNullOrBlank()) return
        val request = PendingForceReload(
            id = ++nextForceReloadId,
            reason = reason
        )
        if (!canRunMainMapScopeQuery() || forceReloadInFlightId != null) {
            pendingForceReload = request
            return
        }
        pendingForceReload = null
        executeForceScopeReload(request)
    }

    private fun executeForceScopeReload(request: PendingForceReload) {
        forceReloadInFlightId = request.id
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.beginLoading(zoom)
        scopeMapCoordinator.load(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = true),
            hooks = buildScopeLoadHooks()
        )
    }

    private fun consumePendingForceReloadIfPossible() {
        if (forceReloadInFlightId != null) return
        val pending = pendingForceReload ?: return
        if (!canRunMainMapScopeQuery()) return
        pendingForceReload = null
        executeForceScopeReload(pending)
    }

    private fun canRunMainMapScopeQuery(): Boolean {
        val token = LoginActivity.getSavedToken(this)
        return lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) &&
            googleMap != null &&
            !token.isNullOrBlank() &&
            !mainBlockingUiController.isBusyBlocking() &&
            activeSheet == null &&
            !isInEditingMode &&
            inspectSheet == null &&
            !isInspecting &&
            !isInspectUiLocked &&
            !shouldReturnToInspectPreview
    }

    /**
     * 防抖版本的 loadGuttersByViewport
     * 避免短時間內多次調用 API（例如快速拖拽地圖時）
     */
    private fun loadGuttersByViewportDebounced() {
        requestUserInteractionScopeSearch()
    }

    /**
     * 取得目前地圖可視範圍（LatLngBounds）並呼叫 scopeSearch API，
     * 成功後呼叫 [drawScopePolylines] 更新地圖上的線段。
     */
    private fun loadGuttersByViewport(showFeedback: Boolean = true) {
        if (showFeedback) {
            requestForceScopeReload("legacy-visible-load")
        } else {
            requestBackgroundScopeRefresh()
        }
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
            onBeforeDraw = { _ ->
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            onLoadingStarted = { _ ->
                mainMapLoadIndicatorController.beginLoading(currentMainMapZoom())
            },
            onLoadingFinished = { _ ->
                mainMapLoadIndicatorController.finishLoading(currentMainMapZoom())
                forceReloadInFlightId = null
                pendingInspectPreviewReload?.let { pending ->
                    pendingInspectPreviewReload = null
                    reopenInspectPreviewAfterUpdate(
                        spiNum = pending.spiNum,
                        persistedWaypoints = pending.waypoints,
                        token = pending.token
                    )
                }
                consumePendingForceReloadIfPossible()
            },
            onLoadingFailed = { _, _ ->
                mainMapLoadIndicatorController.failLoading(currentMainMapZoom())
                forceReloadInFlightId = null
                consumePendingForceReloadIfPossible()
            },
            onSilentError = { _, message ->
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
        mainViewModel.overlayState = mapOverlayController.currentState()
    }

    override fun onOverlayTogglesChanged(
        showPlan: Boolean,
        showWaterOld: Boolean,
        showPossible: Boolean,
        showRegion: Boolean,
        showNoDitchPoints: Boolean
    ) {
        mapOverlayController.updateOverlayToggles(
            showPlan,
            showWaterOld,
            showPossible,
            showRegion,
            showNoDitchPoints
        )
        scopeGutterPolylineController.setVisible(showPlan)
        if (showNoDitchPoints) {
            loadNoDitchPointsForVisibleArea()
        }
        mainViewModel.overlayState = mapOverlayController.currentState()
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
