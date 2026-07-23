package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.api.safeCapturedAt
import com.example.taoyuangutter.common.PendingPhotoDraftState
import com.example.taoyuangutter.common.PhotoCapturedAtResolver
import com.example.taoyuangutter.common.PhotoSlotUploadCoordinator
import com.example.taoyuangutter.common.PhotoUploadSlotState
import com.example.taoyuangutter.common.PhotoUriStore
import com.example.taoyuangutter.common.PhotoUploadValidator
import com.example.taoyuangutter.databinding.ActivityGutterFormBinding
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WaypointSnapshot
import android.content.pm.PackageManager
import com.example.taoyuangutter.common.LocationPickEvents
import com.example.taoyuangutter.map.MarkerIconFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.net.MalformedURLException
import java.net.URL

class  GutterFormActivity : AppCompatActivity(), OnMapReadyCallback, PhotoLoadingHost,
    GutterBasicInfoFragment.DraftChangeHost,
    GutterPhotosFragment.DraftChangeHost {

    private lateinit var binding: ActivityGutterFormBinding
    private lateinit var pagerAdapter: GutterFormPagerAdapter
    private var photoLoadingCount: Int = 0
    private var restoredCurrentFormData: HashMap<String, String>? = null
    private var restoredIsVirtual: Boolean? = null
    private var photoDraftBatchDepth: Int = 0
    private var pendingPhotoDraftSync: Boolean = false
    private val photoUploadListeners = mutableMapOf<Int, (PhotoSlotUploadCoordinator.Snapshot) -> Unit>()

    override fun setPhotoLoading(visible: Boolean) {
        if (!::binding.isInitialized) return
        if (visible) photoLoadingCount++ else photoLoadingCount = (photoLoadingCount - 1).coerceAtLeast(0)
        binding.photoLoadingOverlay.visibility = if (photoLoadingCount > 0) View.VISIBLE else View.GONE
    }

    override fun onBasicInfoDraftChanged(data: Map<String, String>) {
        mergeCurrentFormData(data)
        queueSessionDraftSync()
    }

    override fun onPhotosDraftChanged(photo1: String?, photo2: String?, photo3: String?) {
        updateCurrentFormPhotos(photo1, photo2, photo3)
        queuePhotoDraftSync()
    }

    override fun onPhotoCapturedAtDraftChanged(slot: Int, capturedAt: String?) {
        updateCurrentPhotoCapturedAt(slot, capturedAt)
        queuePhotoDraftSync()
    }

    override fun onPendingPhotoDraftChanged(slot: Int, pendingOutputPath: String?) {
        updateCurrentPendingPhoto(slot, pendingOutputPath)
        queuePhotoDraftSync()
    }

    override fun onPhotoSlotReadyForUpload(slot: Int, photoPath: String?) {
        if (slot !in 1..3) return
        if (photoPath.isNullOrBlank()) {
            clearPhotoUploadState(slot)
            queuePhotoDraftSync()
            return
        }
        val token = LoginActivity.getSavedToken(this)
        if (token.isNullOrBlank()) {
            updatePhotoUploadState(slot, PhotoUploadSlotState.STATE_FAILED, error = getString(R.string.msg_login_first))
            queuePhotoDraftSync()
            return
        }
        val resolvedDraftId = sessionDraftId.takeIf { it > 0L } ?: return
        val resolvedPhotoPath = (currentFormData["photo$slot"] as? String)?.takeIf { it.isNotBlank() } ?: photoPath
        updatePhotoUploadState(slot, PhotoUploadSlotState.STATE_UPLOADING)
        queuePhotoDraftSync()
        PhotoSlotUploadCoordinator.enqueueUpload(
            context = applicationContext,
            repository = gutterRepository,
            draftId = resolvedDraftId,
            waypointIndex = currentIndex,
            slot = slot,
            photoPath = resolvedPhotoPath,
            token = token
        )
    }

    fun beginPhotoDraftBatch() {
        photoDraftBatchDepth++
    }

    fun endPhotoDraftBatch() {
        photoDraftBatchDepth = (photoDraftBatchDepth - 1).coerceAtLeast(0)
        if (photoDraftBatchDepth == 0 && pendingPhotoDraftSync) {
            pendingPhotoDraftSync = false
            queueSessionDraftSync()
        }
    }

    fun showCameraOverlay(slot: Int, outputPath: String) {
        if (!::binding.isInitialized) return
        binding.cameraOverlayContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                binding.cameraOverlayContainer.id,
                CameraOverlayFragment.newInstance(slot = slot, outputPath = outputPath),
                CameraOverlayFragment::class.java.name
            )
            .commitAllowingStateLoss()
    }

    fun hideCameraOverlay() {
        if (!::binding.isInitialized) return
        val tag = CameraOverlayFragment::class.java.name
        supportFragmentManager.findFragmentByTag(tag)?.let { frag ->
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .remove(frag)
                .commitAllowingStateLoss()
        }
        binding.cameraOverlayContainer.visibility = View.GONE
    }

    private val TAG = "GutterFormActivity"

    companion object {
        const val EXTRA_WAYPOINT_LABELS  = "waypoint_labels"
        const val EXTRA_LATITUDES        = "latitudes"
        const val EXTRA_LONGITUDES       = "longitudes"
        const val EXTRA_CURRENT_INDEX    = "current_index"
        const val EXTRA_VIEW_MODE        = "view_mode"
        const val EXTRA_WAYPOINT_INDEX   = "waypoint_index"
        const val EXTRA_IS_EDIT_MODE     = "is_edit_mode" // 新增：是否為編輯模式
	        const val EXTRA_SESSION_DRAFT_ID = "session_draft_id"
	        const val EXTRA_SESSION_WAYPOINTS_JSON = "session_waypoints_json"
	        /** 表單內即時存草稿時，用來保留草稿的 isOffline 屬性（避免被覆蓋回 false）。 */
	        const val EXTRA_SESSION_IS_OFFLINE = "session_is_offline"
		        const val EXTRA_WMTS_LAYER = "wmts_layer"
        const val EXTRA_SHOW_PLAN = "show_plan"
        const val EXTRA_SHOW_WATER_OLD = "show_water_old"
        const val EXTRA_SHOW_POSSIBLE = "show_possible"
        const val EXTRA_SHOW_REGION = "show_region"

	        // 主地圖最近一次定位（由 MainActivity 帶入，供匯入既有點位快速查詢）
	        const val EXTRA_HOST_LAST_LAT  = "host_last_lat"
	        const val EXTRA_HOST_LAST_LNG  = "host_last_lng"
	        const val EXTRA_HOST_LAST_TIME = "host_last_time"
	        const val EXTRA_HOST_LAST_ACC  = "host_last_acc"

        /** 離線模式旗標：不向 MainActivity 回傳 result，改儲存至 GutterSessionDraft（isOffline=true）。 */
        const val EXTRA_OFFLINE_MODE     = "offline_mode"

        // 資料 Key（Intent Extra）
        const val EXTRA_DATA_GUTTER_ID   = "ex_spi_num"
        const val EXTRA_DATA_GUTTER_TYPE = "ex_node_typ"
        const val EXTRA_DATA_MAT_TYP     = "ex_mat_typ"
        const val EXTRA_DATA_COORD_X     = "ex_node_x"
        const val EXTRA_DATA_COORD_Y     = "ex_node_y"
        const val EXTRA_DATA_COORD_Z     = "ex_node_le"
        const val EXTRA_DATA_MEASURE_ID  = "ex_xy_num"
        // 主要：COVER_DEP
        const val EXTRA_DATA_COVER_DEP = "ex_cover_dep"
        const val EXTRA_DATA_DEPTH       = "ex_node_dep"
        const val EXTRA_DATA_TOP_WIDTH   = "ex_node_wid"
        const val EXTRA_DATA_IS_BROKEN   = "ex_is_broken"
        const val EXTRA_DATA_IS_HANGING  = "ex_is_hanging"
        const val EXTRA_DATA_IS_SILT     = "ex_is_silt"
        const val EXTRA_DATA_IS_CANTOPEN = "ex_is_cantopen"
        const val EXTRA_DATA_IS_PENDING_DEPLOY = "ex_is_pending_deploy"
        const val EXTRA_DATA_IS_VIRTUAL  = "ex_is_virtual" // 新增：是否為虛擬點
        const val EXTRA_DATA_IS_IMPORTED = "ex_is_imported" // 新增：是否為匯入點位
        const val EXTRA_DATA_REMARKS     = "ex_node_note"
        const val EXTRA_DATA_PHOTO_1     = "ex_photo1"
        const val EXTRA_DATA_PHOTO_2     = "ex_photo2"
        const val EXTRA_DATA_PHOTO_3     = "ex_photo3"
        const val EXTRA_DATA_PHOTO_1_CAPTURED_AT = "ex_photo1_captured_at"
        const val EXTRA_DATA_PHOTO_2_CAPTURED_AT = "ex_photo2_captured_at"
        const val EXTRA_DATA_PHOTO_3_CAPTURED_AT = "ex_photo3_captured_at"
        const val EXTRA_DATA_PHOTO_1_IMG_ID = "ex_photo1_img_id"
        const val EXTRA_DATA_PHOTO_2_IMG_ID = "ex_photo2_img_id"
        const val EXTRA_DATA_PHOTO_3_IMG_ID = "ex_photo3_img_id"
        const val EXTRA_DATA_PHOTO_1_UPLOAD_STATE = "ex_photo1_upload_state"
        const val EXTRA_DATA_PHOTO_2_UPLOAD_STATE = "ex_photo2_upload_state"
        const val EXTRA_DATA_PHOTO_3_UPLOAD_STATE = "ex_photo3_upload_state"
        const val EXTRA_DATA_PHOTO_1_UPLOAD_ERROR = "ex_photo1_upload_error"
        const val EXTRA_DATA_PHOTO_2_UPLOAD_ERROR = "ex_photo2_upload_error"
        const val EXTRA_DATA_PHOTO_3_UPLOAD_ERROR = "ex_photo3_upload_error"
        const val EXTRA_DATA_XY_NUM      = "ex_xy_num_value"
        const val EXTRA_DATA_NODE_ID     = "ex_nodeId" // 新增：傳入 API 的 node_id（編輯模式）

        // 自訂 Result Code：使用者放棄填寫，要求刪除點位座標與資料
        const val RESULT_DELETE = Activity.RESULT_FIRST_USER

        // 回傳 Key（Result Intent）
        const val RESULT_LATITUDE        = "result_lat"
        const val RESULT_LONGITUDE       = "result_lng"
        const val RESULT_WAYPOINT_INDEX  = "result_wp_index"
        const val RESULT_DATA_GUTTER_ID   = "r_spi_num"
        const val RESULT_DATA_GUTTER_TYPE = "r_node_typ"
        const val RESULT_DATA_MAT_TYP     = "r_mat_typ"
        const val RESULT_DATA_COORD_X     = "r_node_x"
        const val RESULT_DATA_COORD_Y     = "r_node_y"
        const val RESULT_DATA_COORD_Z     = "r_node_le"
        const val RESULT_DATA_MEASURE_ID  = "r_xy_num"
        // 主要：COVER_DEP
        const val RESULT_DATA_COVER_DEP = "r_cover_dep"
        const val RESULT_DATA_DEPTH       = "r_node_dep"
        const val RESULT_DATA_TOP_WIDTH   = "r_node_wid"
        const val RESULT_DATA_IS_BROKEN   = "r_is_broken"
        const val RESULT_DATA_IS_HANGING  = "r_is_hanging"
        const val RESULT_DATA_IS_SILT     = "r_is_silt"
        const val RESULT_DATA_IS_CANTOPEN = "r_is_cantopen"
        const val RESULT_DATA_IS_PENDING_DEPLOY = "r_is_pending_deploy"
        const val RESULT_DATA_IS_VIRTUAL  = "r_is_virtual" // 新增：是否為虛擬點
        const val RESULT_DATA_IS_IMPORTED = "r_is_imported" // 新增：是否為匯入點位
        const val RESULT_DATA_REMARKS     = "r_node_note"
        const val RESULT_DATA_PHOTO_1     = "r_photo1"
        const val RESULT_DATA_PHOTO_2     = "r_photo2"
        const val RESULT_DATA_PHOTO_3     = "r_photo3"
        const val RESULT_DATA_PHOTO_1_CAPTURED_AT = "r_photo1_captured_at"
        const val RESULT_DATA_PHOTO_2_CAPTURED_AT = "r_photo2_captured_at"
        const val RESULT_DATA_PHOTO_3_CAPTURED_AT = "r_photo3_captured_at"
        const val RESULT_DATA_PHOTO_1_IMG_ID = "r_photo1_img_id"
        const val RESULT_DATA_PHOTO_2_IMG_ID = "r_photo2_img_id"
        const val RESULT_DATA_PHOTO_3_IMG_ID = "r_photo3_img_id"
        const val RESULT_DATA_PHOTO_1_UPLOAD_STATE = "r_photo1_upload_state"
        const val RESULT_DATA_PHOTO_2_UPLOAD_STATE = "r_photo2_upload_state"
        const val RESULT_DATA_PHOTO_3_UPLOAD_STATE = "r_photo3_upload_state"
        const val RESULT_DATA_PHOTO_1_UPLOAD_ERROR = "r_photo1_upload_error"
        const val RESULT_DATA_PHOTO_2_UPLOAD_ERROR = "r_photo2_upload_error"
        const val RESULT_DATA_PHOTO_3_UPLOAD_ERROR = "r_photo3_upload_error"

        // ── Reference Route (Gray Curve) ─────────────────────────────────
        const val EXTRA_REF_LATITUDES  = "extra_ref_latitudes"
        const val EXTRA_REF_LONGITUDES = "extra_ref_longitudes"

        // ── Factory ───────────────────────────────────────────────────────

		        fun newIntent(
		            context: Context,
		            labels: ArrayList<String>,
		            lats: DoubleArray,
		            lngs: DoubleArray,
		            index: Int = 0,
		            basicData: HashMap<String, String>? = null,
		            isEditMode: Boolean = false,
		            sessionDraftId: Long = 0L,
		            sessionWaypointsJson: String? = null,
		            wmtsLayer: String? = null,
		            sessionIsOffline: Boolean = false,
                    referenceLats: DoubleArray = doubleArrayOf(),
                    referenceLngs: DoubleArray = doubleArrayOf(),
                    showPlan: Boolean = true,
                    showWaterOld: Boolean = true,
                    showPossible: Boolean = true,
                    showRegion: Boolean = true
		        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
		            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, labels)
		            putExtra(EXTRA_LATITUDES, lats)
		            putExtra(EXTRA_LONGITUDES, lngs)
	            putExtra(EXTRA_CURRENT_INDEX, index)
	            putExtra(EXTRA_WAYPOINT_INDEX, index)   // 與 currentIndex 一致，確保 buildAndFinishWithResult 回傳正確索引
		            putExtra(EXTRA_VIEW_MODE, false)
		            putExtra(EXTRA_IS_EDIT_MODE, isEditMode) // 傳入編輯模式旗標
		            putExtra(EXTRA_SESSION_DRAFT_ID, sessionDraftId)
		            putExtra(EXTRA_OFFLINE_MODE, sessionIsOffline)
		            putExtra(EXTRA_SESSION_IS_OFFLINE, sessionIsOffline)
		            if (!sessionWaypointsJson.isNullOrEmpty()) {
		                putExtra(EXTRA_SESSION_WAYPOINTS_JSON, sessionWaypointsJson)
		            }
		            if (!wmtsLayer.isNullOrEmpty()) {
	                putExtra(EXTRA_WMTS_LAYER, wmtsLayer)
	            }
                putExtra(EXTRA_SHOW_PLAN, showPlan)
                putExtra(EXTRA_SHOW_WATER_OLD, showWaterOld)
                putExtra(EXTRA_SHOW_POSSIBLE, showPossible)
                putExtra(EXTRA_SHOW_REGION, showRegion)
                if (referenceLats.isNotEmpty() && referenceLngs.isNotEmpty()) {
                    putExtra(EXTRA_REF_LATITUDES, referenceLats)
                    putExtra(EXTRA_REF_LONGITUDES, referenceLngs)
                }
	            basicData?.let { GutterFormContract.putFormDataExtras(this, it) }
	        }

	        fun newViewIntent(
	            context: Context,
	            label: String,
	            lat: Double,
	            lng: Double,
	            waypointIndex: Int,
	            basicData: HashMap<String, String>,
	            wmtsLayer: String? = null,
                referenceLats: DoubleArray = doubleArrayOf(),
                referenceLngs: DoubleArray = doubleArrayOf(),
                showPlan: Boolean = true,
                showWaterOld: Boolean = true,
                showPossible: Boolean = true,
                showRegion: Boolean = true
	        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
	            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, arrayListOf(label))
	            putExtra(EXTRA_LATITUDES, doubleArrayOf(lat))
	            putExtra(EXTRA_LONGITUDES, doubleArrayOf(lng))
            putExtra(EXTRA_CURRENT_INDEX, 0)
            putExtra(EXTRA_VIEW_MODE, true)
            putExtra(EXTRA_WAYPOINT_INDEX, waypointIndex)
            putExtra(EXTRA_IS_EDIT_MODE, true) // 編輯模式為 true
	            if (!wmtsLayer.isNullOrEmpty()) {
	                putExtra(EXTRA_WMTS_LAYER, wmtsLayer)
	            }
                putExtra(EXTRA_SHOW_PLAN, showPlan)
                putExtra(EXTRA_SHOW_WATER_OLD, showWaterOld)
                putExtra(EXTRA_SHOW_POSSIBLE, showPossible)
                putExtra(EXTRA_SHOW_REGION, showRegion)
                if (referenceLats.isNotEmpty() && referenceLngs.isNotEmpty()) {
                    putExtra(EXTRA_REF_LATITUDES, referenceLats)
                    putExtra(EXTRA_REF_LONGITUDES, referenceLngs)
                }
	            GutterFormContract.putFormDataExtras(this, basicData)
	        }

        /**
         * 離線模式：不需地圖點位，座標固定 (0.0, 0.0)。
         * 草稿仍會以一般 [GutterSessionDraft] 形式儲存，供後續恢復與上傳。
         * @param draftId 傳入已存草稿 ID 以開啟既有資料；-1L 表示新增。
         */
        fun newOfflineIntent(context: Context, draftId: Long = -1L): Intent =
            Intent(context, GutterFormActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, arrayListOf("離線側溝"))
                putExtra(EXTRA_LATITUDES,     doubleArrayOf(0.0))
                putExtra(EXTRA_LONGITUDES,    doubleArrayOf(0.0))
                putExtra(EXTRA_CURRENT_INDEX, 0)
                putExtra(EXTRA_VIEW_MODE,     false)
                putExtra(EXTRA_OFFLINE_MODE,  true)
                putExtra(EXTRA_SESSION_IS_OFFLINE, true)
                if (draftId > 0L) putExtra(EXTRA_SESSION_DRAFT_ID, draftId)
            }

    }

    // ── 狀態欄位 ──────────────────────────────────────────────────────────

    private var waypointLabels = arrayListOf<String>()
    private var latitudes  = doubleArrayOf()
    private var longitudes = doubleArrayOf()
    private var currentIndex  = 0
    private var waypointIndex = 0
    private var isViewMode    = false
    private var isEditMode    = false // 新增：是否為編輯模式
    private var launchedInViewMode = false

    /** true → 離線模式，儲存至本機草稿，不向 MainActivity 回傳 result */
    private var isOfflineMode  = false
    /** true → 由匯入既有點位帶入後的鎖定狀態。 */
    private var importedWaypointLocked = false
    /** 地圖流程中的 session 草稿 ID；0 表示尚未建立。 */
    private var sessionDraftId = 0L
    /** 整條側溝目前的 waypoint 快照，供表單編輯中即時覆寫草稿。 */
    private val sessionWaypoints = mutableListOf<WaypointSnapshot>()
    /** 表單唯一正式資料來源：畫面顯示、草稿保存、送出上傳都以這份資料為準。 */
    private val currentFormData = hashMapOf<String, String>()
    private var originalSessionWaypoint: WaypointSnapshot? = null

    /** 編輯模式：API 的 node_id（有值時儲存才會上傳照片） */
    private var nodeId: Int? = null

    // ── 背景地圖圖層開關 ──────────────────────────────────────────────────
    private var showPlanOverlay = true
    private var showWaterOldOverlay = true
    private var showPossibleOverlay = true
    private var showRegionOverlay = true

    // ── 背景地圖 ──────────────────────────────────────────────────────────
    private var formMap: GoogleMap? = null
    private var formMapTileOverlay: TileOverlay? = null
    private var planWmsOverlay: TileOverlay? = null
    private var waterOldWmsOverlay: TileOverlay? = null
    private var regionWmsOverlay: TileOverlay? = null

    private val sessionMarkers = mutableListOf<Marker>()
    private val importCandidateMarkers = mutableListOf<Marker>() // 候選點標記列表
    private var sessionPolyline: Polyline? = null
    private var referencePolyline: Polyline? = null

    private var importMapPaddingEnabled = false
    private val gutterRepository = GutterRepository()

    // 灰色參考線（由 MainActivity 傳入的弧線展開點列）
    private var referencePoints: List<LatLng> = emptyList()
    // 僅在座標真的變更後才顯示紫色線段（避免一進表單就畫出編輯線）
    private var initialLatLngSnapshot: List<Pair<Long, Long>> = emptyList()
    private var hasShownEditPolyline: Boolean = false
		    private val locationPickerLauncher = registerForActivityResult(
		        ActivityResultContracts.StartActivityForResult()
		    ) { result ->
	        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
	        val data = result.data ?: return@registerForActivityResult
	        val latitude = data.getDoubleExtra(MapPointPickerActivity.RESULT_LATITUDE, Double.NaN)
	        val longitude = data.getDoubleExtra(MapPointPickerActivity.RESULT_LONGITUDE, Double.NaN)
        if (latitude.isNaN() || longitude.isNaN()) return@registerForActivityResult
        currentLat = latitude
        currentLng = longitude
        if (currentIndex in sessionWaypoints.indices) {
            sessionWaypoints[currentIndex] = sessionWaypoints[currentIndex].copy(
                latitude = latitude,
                longitude = longitude
            )
        }
        mergeCurrentFormData(
            mapOf(
                "NODE_X" to "%.6f".format(longitude),
                "NODE_Y" to "%.6f".format(latitude)
            )
        )
        pagerAdapter.getBasicInfoFragment()?.updateCoordinates(longitude, latitude)
        applyVirtualMode(binding.cbIsVirtual.isChecked)
        formMap?.let { map ->
            renderSessionPreview(map)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 18f))
        }
		    }

		    // ── 匯入既有點位（半屏 BottomSheet；上半部沿用本頁背景地圖） ─────────────

		    private var importSheet: ImportExistingWaypointBottomSheet? = null
		    private var importMapPickEnabled: Boolean = false
		    private var importCenterMarker: Marker? = null
		    private var importMyLocationMarker: Marker? = null
		    private lateinit var fusedLocationClient: FusedLocationProviderClient
		    private var importLocationPermissionAttempts: Int = 0
		    private var pendingImportSheetForLocation: ImportExistingWaypointBottomSheet? = null
		    private var importLocationCallback: LocationCallback? = null
		    private var importLocationTimeoutJob: Job? = null
		    private var importBestLocation: Location? = null

		    private val importLocationStaleMs: Long = 30_000L
		    private val importLocationAccuracyM: Float = 30f
		    private val importLocationTimeoutMs: Long = 25_000L

		    // 由主地圖帶入的最後定位（避免再次等待 GPS fix）
		    private var hostLastLatLng: LatLng? = null
		    private var hostLastLocationTime: Long = 0L
		    private var hostLastLocationAccuracy: Float = -1f

		    private val importLocationPermissionLauncher =
		        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
		            val sheet = pendingImportSheetForLocation
		            if (sheet == null) return@registerForActivityResult

		            if (granted) {
		                requestCurrentGpsAndLoadNearby(sheet)
		                return@registerForActivityResult
		            }

		            val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
		                this,
		                Manifest.permission.ACCESS_FINE_LOCATION
		            )
		            if (!canAskAgain) {
		                showImportLocationGoSettingsDialog(sheet)
		                return@registerForActivityResult
		            }

		            if (importLocationPermissionAttempts < 2) {
		                showImportLocationRetryDialog(sheet)
		            } else {
		                sheet.showNearbyAutoError("尚未授權定位權限，無法查詢附近點位")
		            }
		        }

    private fun handleImportedNodeDetails(nodeDetails: NodeDetails) {
        setImportedWaypointLocked(true)
        pagerAdapter.getBasicInfoFragment()?.prefillDataFromImport(nodeDetails)
        syncImportedVirtualState(parseLooseBoolean(nodeDetails.isVirtual))

        // 匯入時同步下載照片到本機（依序 1→2→3）
        lifecycleScope.launch {
            try {
                val photo1Url = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "1" }?.url
                val photo2Url = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "2" }?.url
                val photo3Url = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "3" }?.url

                showUploadLoading(true, "正在下載照片（1/3）…")
                val p1 = photo1Url?.let {
                    gutterRepository.downloadImageToLocalContentUri(
                        context = this@GutterFormActivity,
                        url = it,
                        prefix = "IMPORT_1_"
                    )
                }?.toString()

                showUploadLoading(true, "正在下載照片（2/3）…")
                val p2 = photo2Url?.let {
                    gutterRepository.downloadImageToLocalContentUri(
                        context = this@GutterFormActivity,
                        url = it,
                        prefix = "IMPORT_2_"
                    )
                }?.toString()

                showUploadLoading(true, "正在下載照片（3/3）…")
                val p3 = photo3Url?.let {
                    gutterRepository.downloadImageToLocalContentUri(
                        context = this@GutterFormActivity,
                        url = it,
                        prefix = "IMPORT_3_"
                    )
                }?.toString()

                val capturedAt1 = nodeDetails.safeCapturedAt(0, "GutterFormActivity", "import existing waypoint")
                val capturedAt2 = nodeDetails.safeCapturedAt(1, "GutterFormActivity", "import existing waypoint")
                val capturedAt3 = nodeDetails.safeCapturedAt(2, "GutterFormActivity", "import existing waypoint")

                pagerAdapter.getBasicInfoFragment()?.prefillPhotos(
                    photo1 = p1,
                    photo2 = p2,
                    photo3 = p3,
                    capturedAt1 = capturedAt1,
                    capturedAt2 = capturedAt2,
                    capturedAt3 = capturedAt3
                )
                updateCurrentFormPhotos(p1, p2, p3)
                updateCurrentPhotoCapturedAt(1, capturedAt1)
                updateCurrentPhotoCapturedAt(2, capturedAt2)
                updateCurrentPhotoCapturedAt(3, capturedAt3)
                updatePhotoUploadState(
                    1,
                    state = if (nodeDetails.nodeImg.firstOrNull { it.fileCategory == "1" }?.id != null) PhotoUploadSlotState.STATE_SUCCESS else PhotoUploadSlotState.STATE_IDLE,
                    imgId = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "1" }?.id
                )
                updatePhotoUploadState(
                    2,
                    state = if (nodeDetails.nodeImg.firstOrNull { it.fileCategory == "2" }?.id != null) PhotoUploadSlotState.STATE_SUCCESS else PhotoUploadSlotState.STATE_IDLE,
                    imgId = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "2" }?.id
                )
                updatePhotoUploadState(
                    3,
                    state = if (nodeDetails.nodeImg.firstOrNull { it.fileCategory == "3" }?.id != null) PhotoUploadSlotState.STATE_SUCCESS else PhotoUploadSlotState.STATE_IDLE,
                    imgId = nodeDetails.nodeImg.firstOrNull { it.fileCategory == "3" }?.id
                )
                showUploadLoading(false)

                val missing = mutableListOf<String>()
                if (p1.isNullOrEmpty()) missing.add("第1張")
                if (p2.isNullOrEmpty()) missing.add("第2張")
                if (p3.isNullOrEmpty()) missing.add("第3張")
                if (missing.isNotEmpty()) {
                    Toast.makeText(
                        this@GutterFormActivity,
                        "匯入完成，但${missing.joinToString("、")}照片未取得，請至照片頁補拍",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: CancellationException) {
                showUploadLoading(false)
            } catch (e: Exception) {
                showUploadLoading(false)
                Toast.makeText(
                    this@GutterFormActivity,
                    String.format(getString(R.string.msg_photo_download_failed), e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun syncImportedVirtualState(isVirtual: Boolean) {
        if (binding.cbIsVirtual.isChecked != isVirtual) {
            binding.cbIsVirtual.isChecked = isVirtual
        } else {
            applyVirtualModeUi(isVirtual)
        }
    }

    private fun showImportExistingWaypointSheet() {
        if (isOfflineMode) return
        if (importSheet?.isAdded == true) return

	        // 讓上半部地圖可見（半屏 sheet 覆蓋下半部）
	        binding.formPanel.visibility = View.GONE
	        // 讓地圖可操作：移除遮罩層，避免吃掉觸控事件
	        binding.mapDimOverlay.visibility = View.GONE
	        applyImportMapPadding(true)

		        val sheet = ImportExistingWaypointBottomSheet().apply {
		            callbacks = object : ImportExistingWaypointBottomSheet.Callbacks {
	                override fun onMapPickModeChanged(enabled: Boolean) {
	                    // Spec update: use GPS current location; never enable map click picking.
	                    importMapPickEnabled = false
	                    updateImportMapClickListener()
	                }

		                override fun onNearbyCenterChanged(center: LatLng) {
		                    // Spec update: no map click picking in Nearby mode.
		                    return
		                }

	                override fun onCandidateWaypointsChanged(items: List<NodeDetails>) {
	                    showImportCandidateWaypoints(items)
	                }

	                override fun onWaypointSelected(item: NodeDetails?) {
	                    showImportSelectedWaypoint(item)
	                }

	                override fun onImport(item: NodeDetails) {
	                    handleImportedNodeDetails(item)
	                }

		                override fun onDismissed() {
		                    stopImportLocationUpdates()
		                    clearImportMarkers()
		                    pendingImportSheetForLocation = null
		                    binding.formPanel.visibility = View.VISIBLE
		                    binding.mapDimOverlay.visibility = View.GONE
		                    applyImportMapPadding(false)
		                }

		                override fun onPageSwitched() {
		                    stopImportLocationUpdates()
		                    // Switching tabs cancels selection and clears all map markers.
		                    clearImportMarkers()
		                }

		                override fun onRequestMyLocation() {
		                    // Always allow the user to tap "current location" even without permission.
		                    val s = this@apply
		                    pendingImportSheetForLocation = s
		                    importLocationPermissionAttempts = 0
		                    ensureImportLocationPermissionAndFetch(s)
		                }
		            }
		        }

		        importSheet = sheet
		        sheet.show(supportFragmentManager, "ImportExistingWaypointBottomSheet")
		        // Spec update: open Nearby tab and immediately query by GPS current location.
		        pendingImportSheetForLocation = sheet
		        importLocationPermissionAttempts = 0
		        ensureImportLocationPermissionAndFetch(sheet)
		    }

		    private fun updateImportMapClickListener() {
		        val map = formMap ?: return
		        if (importMapPickEnabled) {
		            map.setOnMapClickListener { latLng ->
		                importSheet?.onMapClicked(latLng)
		            }
		        } else {
		            map.setOnMapClickListener(null)
		        }
		    }

		    private fun ensureImportLocationPermissionAndFetch(sheet: ImportExistingWaypointBottomSheet) {
		        pendingImportSheetForLocation = sheet
		        // Prefer using host (MainActivity) last known location for instant lookup.
		        if (tryLoadNearbyFromHost(sheet)) return
		        val granted = ContextCompat.checkSelfPermission(
		            this,
		            Manifest.permission.ACCESS_FINE_LOCATION
		        ) == PackageManager.PERMISSION_GRANTED
		        if (granted) {
		            requestCurrentGpsAndLoadNearby(sheet)
		            return
		        }
		        if (importLocationPermissionAttempts >= 2) {
		            sheet.showNearbyAutoError("尚未授權定位權限，無法查詢附近點位")
		            return
		        }
		        importLocationPermissionAttempts++
		        importLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
		    }

		    private fun tryLoadNearbyFromHost(sheet: ImportExistingWaypointBottomSheet): Boolean {
		        val latLng = hostLastLatLng ?: return false
		        // Use host location immediately; do not require fine-location permission here.
		        stopImportLocationUpdates()
		        showImportMyLocationMarker(latLng)
		        sheet.startNearbyAutoSearch(latLng)
		        return true
		    }

		    @android.annotation.SuppressLint("MissingPermission")
		    private fun requestCurrentGpsAndLoadNearby(sheet: ImportExistingWaypointBottomSheet) {
		        // Guard: sheet may already be dismissing.
		        if (sheet.isRemoving) return

		        stopImportLocationUpdates()
		        importBestLocation = null
		        sheet.showLocating("定位中…")

		        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
		            .setMinUpdateIntervalMillis(500L)
		            .setWaitForAccurateLocation(true)
		            .build()

		        val callback = object : LocationCallback() {
		            override fun onLocationResult(result: LocationResult) {
		                val now = System.currentTimeMillis()
		                val freshLocations = result.locations
		                    .filter { loc -> (now - loc.time) <= importLocationStaleMs }
		                if (freshLocations.isEmpty()) return

		                // Track best (smallest accuracy) among fresh updates.
		                freshLocations.forEach { loc ->
		                    val best = importBestLocation
		                    val locAcc = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE
		                    val bestAcc = if (best?.hasAccuracy() == true) best.accuracy else Float.MAX_VALUE
		                    if (best == null || locAcc < bestAcc) importBestLocation = loc
		                }

		                // Accept only when accuracy is good enough (fresh + accurate).
		                val accurate = freshLocations.firstOrNull { loc ->
		                    loc.hasAccuracy() && loc.accuracy <= importLocationAccuracyM
		                } ?: return

		                stopImportLocationUpdates()
		                val latLng = LatLng(accurate.latitude, accurate.longitude)
		                showImportMyLocationMarker(latLng)
		                sheet.startNearbyAutoSearch(latLng)
		            }
		        }

		        importLocationCallback = callback
		        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

		        importLocationTimeoutJob = lifecycleScope.launch {
		            delay(importLocationTimeoutMs)
		            if (importLocationCallback != callback) return@launch
		            stopImportLocationUpdates()
		            sheet.hideLocating()
		            sheet.showNearbyAutoError("定位逾時，請到空曠處或開啟定位服務後再試一次")
		        }
		    }

		    private fun stopImportLocationUpdates() {
		        importLocationTimeoutJob?.cancel()
		        importLocationTimeoutJob = null
		        importLocationCallback?.let { cb ->
		            fusedLocationClient.removeLocationUpdates(cb)
		        }
		        importLocationCallback = null
		    }

		    private fun showImportMyLocationMarker(latLng: LatLng) {
		        val map = formMap ?: return
		        importMyLocationMarker?.remove()
		        importMyLocationMarker = map.addMarker(
		            MarkerOptions()
		                .position(latLng)
		                .title("目前位置")
		                .anchor(0.5f, 0.5f)
		                .icon(bitmapDescriptorFromVector(R.drawable.ic_my_location))
		        )
		        // Move/zoom to current location (will be centered in the upper-half due to map padding).
		        val zoom = (map.cameraPosition?.zoom ?: 18f).coerceAtLeast(16f)
		        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
		    }

		    private fun bitmapDescriptorFromVector(resId: Int): com.google.android.gms.maps.model.BitmapDescriptor {
		        val drawable = ContextCompat.getDrawable(this, resId)
		            ?: return BitmapDescriptorFactory.defaultMarker()
		        val bitmap = Bitmap.createBitmap(
		            drawable.intrinsicWidth.coerceAtLeast(1),
		            drawable.intrinsicHeight.coerceAtLeast(1),
		            Bitmap.Config.ARGB_8888
		        )
		        val canvas = Canvas(bitmap)
		        drawable.setBounds(0, 0, canvas.width, canvas.height)
		        drawable.draw(canvas)
		        return BitmapDescriptorFactory.fromBitmap(bitmap)
		    }

		    private fun showImportLocationRetryDialog(sheet: ImportExistingWaypointBottomSheet) {
		        AlertDialog.Builder(this)
		            .setTitle("需要定位權限")
		            .setMessage("匯入既有點位的「附近點位」需要使用目前位置。是否要再次詢問定位權限？")
		            .setNegativeButton("取消") { _, _ ->
		                sheet.showNearbyAutoError("尚未授權定位權限，無法查詢附近點位")
		            }
		            .setPositiveButton("再試一次") { _, _ ->
		                ensureImportLocationPermissionAndFetch(sheet)
		            }
		            .show()
		    }

		    private fun showImportLocationGoSettingsDialog(sheet: ImportExistingWaypointBottomSheet) {
		        AlertDialog.Builder(this)
		            .setTitle("定位權限已關閉")
		            .setMessage("請到系統設定開啟定位權限後，再回到此頁查詢附近點位。")
		            .setNegativeButton("取消") { _, _ ->
		                sheet.showNearbyAutoError("尚未授權定位權限，無法查詢附近點位")
		            }
		            .setPositiveButton("前往設定") { _, _ ->
		                openAppSettings()
		            }
		            .show()
		    }

		    private fun openAppSettings() {
		        runCatching {
		            startActivity(
		                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
		                    data = Uri.fromParts("package", packageName, null)
		                }
		            )
		        }
		    }

		    private fun showImportCenterMarker(center: LatLng) {
		        val map = formMap ?: return
		        importCenterMarker?.remove()
		        // Use the same marker style as distance-measure start point.
	        importCenterMarker = map.addMarker(
	            MarkerOptions()
	                .position(center)
	                .title("選取位置")
	                .icon(BitmapDescriptorFactory.defaultMarker(256f))
	        )
	        map.animateCamera(CameraUpdateFactory.newLatLng(center))
	    }

	    private fun showImportSelectedWaypoint(item: NodeDetails?) {
	        // 清掉舊的選取 marker，但不動鏡頭（需求：切頁清 marker、視圖不動）
	        importCenterMarker?.remove()
	        importCenterMarker = null

	        if (item == null) {
                // 若取消選取，恢復所有候選點的可見性
                importCandidateMarkers.forEach { it.isVisible = true }
                return
            }
	        val lat = item.latitude?.toDoubleOrNull()
	        val lng = item.longitude?.toDoubleOrNull()
	        if (lat == null || lng == null) return
	        val target = LatLng(lat, lng)
            val xyNum = item.xyNum ?: "---"

            // 附近模式：選取時「不隱藏」其他點，而是將選中的點位在高亮層級
            // 我們可以透過隱藏「候選標記中與選中項相同 node_id 的那一個」來避免重疊
            importCandidateMarkers.forEach { 
                val markerItem = it.tag as? NodeDetails
                it.isVisible = markerItem?.nodeId != item.nodeId
            }

	        // 建立選中樣式：醒目大圓點 + 單一標籤
	        importCenterMarker = formMap?.addMarker(
	            MarkerOptions()
	                .position(target)
	                .title(xyNum)
	                .icon(createSelectedLargeMarker(xyNum))
                    .anchor(0.5f, 1.0f)
                    .zIndex(1.0f) // 確保在最上層
	        )
	        // Move/zoom to the selected point (will be centered in the upper-half due to map padding).
	        formMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 18f))
	    }

        private fun createSelectedLargeMarker(label: String): com.google.android.gms.maps.model.BitmapDescriptor {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 60f // 選中文字稍微大一點
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val textBounds = android.graphics.Rect()
            paint.getTextBounds(label, 0, label.length, textBounds)

            val padding = 14
            val markerRadius = 22 // 放大圓點
            val width = maxOf(textBounds.width() + padding * 2, markerRadius * 2)
            val height = textBounds.height() + padding * 2 + markerRadius * 2 + 12

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. 畫放大圓點 (Marker dot) - 藍底白邊
            val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(width / 2f, height - markerRadius.toFloat(), markerRadius.toFloat(), dotPaint)
            
            dotPaint.color = android.graphics.Color.parseColor("#4285F4") // Google Blue
            canvas.drawCircle(width / 2f, height - markerRadius.toFloat(), markerRadius.toFloat() - 4, dotPaint)
            
            // 2. 畫文字背景 (Rounded Rect) - 選中時背景更實一點
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
                setShadowLayer(8f, 0f, 4f, android.graphics.Color.parseColor("#40000000"))
            }
            val bgRect = android.graphics.RectF((width - textBounds.width() - padding * 2) / 2f, 0f, (width + textBounds.width() + padding * 2) / 2f, (textBounds.height() + padding * 2).toFloat())
            canvas.drawRoundRect(bgRect, 12f, 12f, bgPaint)
            
            // 3. 畫文字
            //paint.color = android.graphics.Color.parseColor("#4285F4") // 文字也用藍色強調
			paint.color = android.graphics.Color.BLACK
            canvas.drawText(label, width / 2f, padding + textBounds.height().toFloat(), paint)

            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        private fun showImportCandidateWaypoints(items: List<NodeDetails>) {
            val map = formMap ?: return
            clearImportCandidateMarkers()

            if (items.isEmpty()) return

            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            var hasValidPoint = false

            items.forEach { item ->
                val lat = item.latitude?.toDoubleOrNull()
                val lng = item.longitude?.toDoubleOrNull()
                if (lat != null && lng != null) {
                    val pos = LatLng(lat, lng)
                    val xyNum = item.xyNum ?: "---"

                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title(xyNum)
                            .icon(createMarkerWithLabel(xyNum))
                            .anchor(0.5f, 1.0f)
                            .zIndex(0.5f)
                    )
                    marker?.let { 
                        it.tag = item // 存入資料供未來點擊互動
                        importCandidateMarkers.add(it) 
                    }
                    builder.include(pos)
                    hasValidPoint = true
                }
            }

            if (hasValidPoint) {
                val bounds = builder.build()
                // 縮放鏡頭以容納所有候選點，並考慮到 padding
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            }
        }

        private fun createMarkerWithLabel(label: String): com.google.android.gms.maps.model.BitmapDescriptor {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 48f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val textBounds = android.graphics.Rect()
            paint.getTextBounds(label, 0, label.length, textBounds)

            val padding = 12
            val markerRadius = 15
            val width = textBounds.width() + padding * 2
            val height = textBounds.height() + padding * 2 + markerRadius * 2 + 10

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. 畫圓點 (Marker dot)
            val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4285F4") // Google Blue
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(width / 2f, height - markerRadius.toFloat(), markerRadius.toFloat(), dotPaint)
            
            // 2. 畫文字背景 (Rounded Rect)
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#CCFFFFFF") // 半透明白
                style = android.graphics.Paint.Style.FILL
            }
            val bgRect = android.graphics.RectF(0f, 0f, width.toFloat(), (textBounds.height() + padding * 2).toFloat())
            canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
            
            // 3. 畫邊框
            val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#B7B7C2")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(bgRect, 10f, 10f, strokePaint)

            // 4. 畫文字
            //paint.color = android.graphics.Color.parseColor("#333333")
            paint.color = com.example.taoyuangutter.R.color.formSubmitButton
            canvas.drawText(label, width / 2f, padding + textBounds.height().toFloat(), paint)

            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        private fun clearImportCandidateMarkers() {
            importCandidateMarkers.forEach { it.remove() }
            importCandidateMarkers.clear()
        }

		    private fun clearImportMarkers() {
		        importCenterMarker?.remove()
		        importCenterMarker = null
		        importMyLocationMarker?.remove()
		        importMyLocationMarker = null
                clearImportCandidateMarkers() // 同步清除候選點
		        importMapPickEnabled = false
		        updateImportMapClickListener()
		    }

    private fun applyImportMapPadding(enabled: Boolean) {
        importMapPaddingEnabled = enabled
        updateFormMapViewportPadding()
    }

    /** 本點位的原始 GPS 座標（來自地圖選點），永遠保留以確保 result 能帶回正確定位 */
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	        binding = ActivityGutterFormBinding.inflate(layoutInflater)
	        setContentView(binding.root)
	        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

	        // Cache host (MainActivity) last known location for fast "nearby" lookup (no waiting for GPS fix here).
	        val hostLat = intent.getDoubleExtra(EXTRA_HOST_LAST_LAT, Double.NaN)
	        val hostLng = intent.getDoubleExtra(EXTRA_HOST_LAST_LNG, Double.NaN)
	        hostLastLocationTime = intent.getLongExtra(EXTRA_HOST_LAST_TIME, 0L)
	        hostLastLocationAccuracy = intent.getFloatExtra(EXTRA_HOST_LAST_ACC, -1f)
	        if (!hostLat.isNaN() && !hostLng.isNaN()) {
	            hostLastLatLng = LatLng(hostLat, hostLng)
	        }

        waypointLabels = intent.getStringArrayListExtra(EXTRA_WAYPOINT_LABELS) ?: arrayListOf()
        latitudes  = intent.getDoubleArrayExtra(EXTRA_LATITUDES)  ?: doubleArrayOf()
        longitudes = intent.getDoubleArrayExtra(EXTRA_LONGITUDES) ?: doubleArrayOf()
        currentIndex  = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        waypointIndex = intent.getIntExtra(EXTRA_WAYPOINT_INDEX, 0)

        if (savedInstanceState != null) {
            isViewMode = savedInstanceState.getBoolean("saved_is_view_mode")
            isEditMode = savedInstanceState.getBoolean("saved_is_edit_mode")
            importedWaypointLocked = savedInstanceState.getBoolean("saved_imported_waypoint_locked")
            restoredIsVirtual = savedInstanceState.getBoolean("saved_is_virtual")
            currentLat = savedInstanceState.getDouble("saved_current_lat")
            currentLng = savedInstanceState.getDouble("saved_current_lng")
            hasShownEditPolyline = savedInstanceState.getBoolean("saved_has_shown_edit_polyline")
            sessionDraftId = savedInstanceState.getLong("saved_session_draft_id")
            restoredCurrentFormData = savedInstanceState.getString("saved_current_form_data_json")?.let { json ->
                try {
                    val type = object : TypeToken<HashMap<String, String>>() {}.type
                    Gson().fromJson<HashMap<String, String>>(json, type)
                } catch (_: Exception) {
                    null
                }
            }
            val origJson = savedInstanceState.getString("saved_original_waypoint_json")
            if (!origJson.isNullOrEmpty()) {
                originalSessionWaypoint = try { Gson().fromJson(origJson, WaypointSnapshot::class.java) } catch (e: Exception) { null }
            }
        } else {
            isViewMode    = intent.getBooleanExtra(EXTRA_VIEW_MODE, false)
            isEditMode    = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false) // 取得編輯模式旗標
            sessionDraftId = intent.getLongExtra(EXTRA_SESSION_DRAFT_ID, 0L)

            val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
            val lng   = longitudes.getOrElse(currentIndex) { 0.0 }
            currentLat = lat
            currentLng = lng
        }

        launchedInViewMode = intent.getBooleanExtra(EXTRA_VIEW_MODE, false)
        isOfflineMode = intent.getBooleanExtra(EXTRA_OFFLINE_MODE, false)
        showPlanOverlay = intent.getBooleanExtra(EXTRA_SHOW_PLAN, true)
        showWaterOldOverlay = intent.getBooleanExtra(EXTRA_SHOW_WATER_OLD, true)
        showPossibleOverlay = intent.getBooleanExtra(EXTRA_SHOW_POSSIBLE, true)
        showRegionOverlay = intent.getBooleanExtra(EXTRA_SHOW_REGION, true)

        nodeId        = intent.getStringExtra(EXTRA_DATA_NODE_ID)?.toIntOrNull()
        restoreSessionWaypoints(savedInstanceState)

        // 灰色參考線（弧線展開點列）：由 MainActivity 傳入，供表單期間對照
        val refLats = intent.getDoubleArrayExtra(EXTRA_REF_LATITUDES) ?: doubleArrayOf()
        val refLngs = intent.getDoubleArrayExtra(EXTRA_REF_LONGITUDES) ?: doubleArrayOf()
        referencePoints = buildReferencePoints(refLats, refLngs)

        // 離線模式開啟既有草稿：將 repo 中儲存的 basicData 填入 sessionWaypoints[0]，
        // 確保任何時機觸發的 syncSessionDraftNow() 都有正確基底資料，不會以空值覆寫。
        if (isOfflineMode && sessionDraftId > 0L && sessionWaypoints.isNotEmpty()) {
            val savedWp = GutterSessionRepository(this).getById(sessionDraftId)?.waypoints?.firstOrNull()
            if (savedWp != null) {
                val normalizedBasicData = runBlocking(Dispatchers.IO) {
                    PhotoUriStore.normalizeBasicDataPhotoUris(
                        context = this@GutterFormActivity,
                        basicData = HashMap(savedWp.basicData),
                        prefix = "GUTTER_EXT_"
                    )
                }
                sessionWaypoints[0] = sessionWaypoints[0].copy(basicData = normalizedBasicData)
            }
        }

        if (savedInstanceState == null) {
            originalSessionWaypoint = sessionWaypoints.getOrNull(currentIndex)?.copy(
                basicData = HashMap(sessionWaypoints.getOrNull(currentIndex)?.basicData ?: hashMapOf())
            )
        }

        initialLatLngSnapshot = buildLatLngSnapshot(sessionWaypoints)
        hasShownEditPolyline = false

        // 修正：如果是由系統重建，優先使用恢復後的 sessionWaypoints 作為目前點位的資料基底
        val existingData: HashMap<String, String> = if (savedInstanceState != null && currentIndex in sessionWaypoints.indices) {
            HashMap(sessionWaypoints[currentIndex].basicData).apply {
                restoredCurrentFormData?.let { putAll(it) }
            }
        } else if (isOfflineMode && sessionDraftId > 0L) {
            val draft = GutterSessionRepository(this).getById(sessionDraftId)
            val wp = draft?.waypoints?.firstOrNull()
            if (wp != null) HashMap(wp.basicData) else buildEmptyData(currentLat, currentLng)
        } else if (isOfflineMode) {
            buildEmptyData(currentLat, currentLng)
        } else {
            hashMapOf(
                "SPI_NUM"    to (intent.getStringExtra(EXTRA_DATA_GUTTER_ID)   ?: ""),
                "NODE_TYP"   to (intent.getStringExtra(EXTRA_DATA_GUTTER_TYPE) ?: ""),
                "MAT_TYP"    to (intent.getStringExtra(EXTRA_DATA_MAT_TYP)     ?: ""),
                "NODE_X"     to (intent.getStringExtra(EXTRA_DATA_COORD_X)     ?: ""),
                "NODE_Y"     to (intent.getStringExtra(EXTRA_DATA_COORD_Y)     ?: ""),
                "NODE_LE"    to (intent.getStringExtra(EXTRA_DATA_COORD_Z)     ?: ""),
                "XY_NUM"     to (intent.getStringExtra(EXTRA_DATA_MEASURE_ID)  ?: "")
            ).apply {
                putAll(GutterFormContract.readFormData(intent))
            }
        }

        if (savedInstanceState == null) {
            importedWaypointLocked = parseLooseBoolean(existingData["_isImported"])
        }
        Log.w(
            TAG,
            "form init index=$currentIndex label=${waypointLabels.getOrNull(currentIndex)} " +
                "isEditMode=$isEditMode isViewMode=$isViewMode " +
                "currentLat=$currentLat currentLng=$currentLng " +
                "dataNODE_X=${existingData["NODE_X"]} dataNODE_Y=${existingData["NODE_Y"]} " +
                "dataRawNodeCoordX=${existingData["_nodeCoordX"]} dataRawNodeCoordY=${existingData["_nodeCoordY"]} " +
                "xyNum=${existingData["XY_NUM"]} nodeTyp=${existingData["NODE_TYP"]} matTyp=${existingData["MAT_TYP"]} " +
                "photo1=${!existingData["photo1"].isNullOrBlank()} " +
                "photo2=${!existingData["photo2"].isNullOrBlank()} " +
                "photo3=${!existingData["photo3"].isNullOrBlank()}"
        )
        initializeCurrentFormData(existingData)
        registerPhotoUploadListeners()

        // 全螢幕地圖背景 + 表單面板（不論離線或一般模式皆使用新版佈局）
        setupFullScreenWithMap()
        initFormMap()

        // 檢視模式：標題改為「側溝編號 {gutterId}」；其他模式沿用點位 label（起點/節點/終點）
        val titleText = if (isViewMode) {
            val gutterId = existingData["SPI_NUM"]?.takeIf { it.isNotEmpty() } ?: "---"
            "側溝編號 $gutterId"
        } else {
            waypointLabels.getOrElse(currentIndex) { "點位" }
        }
        setupTitleBar(titleText)
        setupViewPager(currentLat, currentLng, existingData)
        setupTabButtons()
        
        // 取得初始虛擬狀態
        val isVirtualInitial = restoredIsVirtual ?: parseLooseBoolean(existingData["is_virtual"])
        setupVirtualPointToggle(isVirtualInitial)
        
        setupImportWaypointButton()
        setupFab()
        binding.viewPager.post {
            applyVirtualMode(binding.cbIsVirtual.isChecked)
            syncNormalizedPhotosBackToUiIfNeeded(
                photo1 = currentFormData["photo1"]?.takeIf { it.isNotBlank() },
                photo2 = currentFormData["photo2"]?.takeIf { it.isNotBlank() },
                photo3 = currentFormData["photo3"]?.takeIf { it.isNotBlank() }
            )
            refreshCurrentFormDataFromFragments()
        }
        pagerAdapter.getBasicInfoFragment()?.onRequestLocationPick = { launchLocationPicker() }
        binding.viewPager.post { applyImportedWaypointLock() }
    }

    private fun registerPhotoUploadListeners() {
        val resolvedDraftId = sessionDraftId.takeIf { it > 0L } ?: return
        unregisterPhotoUploadListeners()
        (1..3).forEach { slot ->
            val listener: (PhotoSlotUploadCoordinator.Snapshot) -> Unit = { snapshot ->
                runOnUiThread {
                    updatePhotoUploadState(
                        slot = snapshot.slot,
                        state = snapshot.state,
                        imgId = snapshot.imgId,
                        error = snapshot.error
                    )
                    queuePhotoDraftSync()
                }
            }
            photoUploadListeners[slot] = listener
            PhotoSlotUploadCoordinator.registerListener(
                draftId = resolvedDraftId,
                waypointIndex = currentIndex,
                slot = slot,
                listener = listener
            )
        }
    }

    private fun unregisterPhotoUploadListeners() {
        val resolvedDraftId = sessionDraftId.takeIf { it > 0L } ?: return
        photoUploadListeners.forEach { (slot, listener) ->
            PhotoSlotUploadCoordinator.unregisterListener(
                draftId = resolvedDraftId,
                waypointIndex = currentIndex,
                slot = slot,
                listener = listener
            )
        }
        photoUploadListeners.clear()
    }

    override fun onPause() {
        // 編輯中只要不是已經準備結束，就先把最新狀態直接寫回草稿，
        // 讓背景切走、系統回收、短暫閃退時都能盡量保住內容。
        if (!isFinishing && !isViewMode) {
            syncSessionDraftNowBlocking()
        }
        super.onPause()
    }

    override fun onDestroy() {
        unregisterPhotoUploadListeners()
        super.onDestroy()
    }

    private fun buildEmptyData(lat: Double, lng: Double) = hashMapOf(
        // 編輯模式下，側溝編號預設為空字串，以符合「不用顯示側溝編號欄位」的需求
        "SPI_NUM"   to "",
        "NODE_TYP"  to "",
        "MAT_TYP"   to "",
        "NODE_X"    to if (lng != 0.0) "%.6f".format(lng) else "",
        "NODE_Y"    to if (lat != 0.0) "%.6f".format(lat) else "",
        "NODE_LE"   to "",
        "XY_NUM"    to "",
        "COVER_DEP" to "",
        "NODE_DEP"  to "",
        "NODE_WID"  to "",
        "IS_BROKEN" to "",
        "IS_HANGING" to "",
        "IS_SILT"   to "",
        "IS_CANTOPEN" to "",
        "IS_PENDING_DEPLOY" to "",
        "NODE_NOTE" to "",
        "photo1"    to "", "photo2" to "", "photo3" to "",
        "photo1CapturedAt" to "",
        "photo2CapturedAt" to "",
        "photo3CapturedAt" to "",
        "photo1UploadState" to PhotoUploadSlotState.STATE_IDLE,
        "photo2UploadState" to PhotoUploadSlotState.STATE_IDLE,
        "photo3UploadState" to PhotoUploadSlotState.STATE_IDLE,
        "is_virtual" to "0"
    )

    /**
     * 新版全螢幕地圖 + 表單面板設定：
     * - Activity 全螢幕（畫到系統列後方）
     * - formPanel 高度設為螢幕 3/4
     * - system bar insets 套用至 AppBarLayout（top）與 FAB（bottom）
     * - 鍵盤出現時 formPanel 向上平移（動畫同步），隱藏時回位
     */
    private fun setupFullScreenWithMap() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        // 確保視窗背景透明，否則佈局的圓角會被系統預設背景色遮擋
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 設定表單面板高度為螢幕 3/4
        val screenH = resources.displayMetrics.heightPixels
        val panelHeight = screenH * 3 / 4
        binding.formPanel.layoutParams = binding.formPanel.layoutParams.apply {
            height = panelHeight
        }
        binding.formPanel.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateFormMapViewportPadding()
        }

        // 套用 system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.formPanel) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val compactTopSpacing = (8 * resources.displayMetrics.density).toInt()
            binding.appBarLayout.setPadding(0, compactTopSpacing, 0, 0)
            val fabParams = binding.fabSubmit.layoutParams as CoordinatorLayout.LayoutParams
            fabParams.bottomMargin = (24 * resources.displayMetrics.density).toInt() + bars.bottom
            binding.fabSubmit.layoutParams = fabParams
            WindowInsetsCompat.CONSUMED
        }

        // 鍵盤動畫：鍵盤升起時 formPanel 向上平移，鍵盤下收時還原
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.formPanel,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                    val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    binding.formPanel.translationY =
                        -maxOf(0, imeInsets.bottom - navInsets.bottom).toFloat()
                    updateFormMapViewportPadding()
                    return insets
                }
            }
        )
    }

    // ── 背景地圖初始化 ────────────────────────────────────────────────────

    private fun initFormMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(binding.formMapContainer.id) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        formMap = map
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.mapType = GoogleMap.MAP_TYPE_NONE
        val wmtsLayer = intent.getStringExtra(EXTRA_WMTS_LAYER) ?: "EMAP"
        setWmtsTiles(wmtsLayer)

        applyBackgroundWmsOverlays()

        renderSessionPreview(map)
        updateFormMapViewportPadding()

        val currentWaypoint = sessionWaypoints.getOrNull(currentIndex)
        val currentTarget = currentWaypoint?.latitude?.let { wpLat ->
            currentWaypoint.longitude?.let { wpLng -> LatLng(wpLat, wpLng) }
        }
        if (currentTarget != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentTarget, 18f))
        } else {
            // 若沒有既有座標，先以桃園作為初始鏡頭（表單內地圖不做定位跳轉）
            val lat = if (currentLat != 0.0) currentLat else 24.9929
            val lng = if (currentLng != 0.0) currentLng else 121.3011
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f))
        }
        updateImportMapClickListener()
    }

    private fun applyBackgroundWmsOverlays() {
        val map = formMap ?: return

        // 本次計畫調查 (roadServey) - 這裡由 showPossibleOverlay 控制，與 MainActivity 邏輯一致
        if (showPossibleOverlay) {
            if (planWmsOverlay == null) {
                val provider = com.example.taoyuangutter.map.Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/roadServey/wms",
                    layers = "roadServey",
                    styles = "TY_RSGDBIP_道路調查",
                    format = "image/png"
                )
                planWmsOverlay = map.addTileOverlay(
                    com.google.android.gms.maps.model.TileOverlayOptions().tileProvider(provider).zIndex(0f)
                )
            }
        }

        // 水務局舊資料 (legacyDitch)
        if (showWaterOldOverlay) {
            if (waterOldWmsOverlay == null) {
                val provider = com.example.taoyuangutter.map.Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "legacyDitch",
                    styles = "TY_RSGDBIP_水務局既有資料",
                    format = "image/png8"
                )
                waterOldWmsOverlay = map.addTileOverlay(
                    com.google.android.gms.maps.model.TileOverlayOptions().tileProvider(provider).zIndex(0.1f)
                )
            }
        }

        // 桃園行政區 (regions)
        if (showRegionOverlay) {
            if (regionWmsOverlay == null) {
                val provider = com.example.taoyuangutter.map.Wms3857TileProvider(
                    baseUrl = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms",
                    layers = "regions",
                    styles = "TY_RSGDBIP_桃園行政區",
                    format = "image/png8"
                )
                regionWmsOverlay = map.addTileOverlay(
                    com.google.android.gms.maps.model.TileOverlayOptions().tileProvider(provider).zIndex(-0.5f)
                )
            }
        }
    }

    private fun updateFormMapViewportPadding() {
        val map = formMap ?: return
        val bottomInset = if (importMapPaddingEnabled) {
            resources.displayMetrics.heightPixels / 2
        } else if (binding.formPanel.visibility == View.VISIBLE) {
            (binding.formPanel.height + binding.formPanel.translationY).toInt().coerceAtLeast(0)
        } else {
            0
        }
        map.setPadding(0, 0, 0, bottomInset)
    }

    private fun renderSessionPreview(map: GoogleMap) {
        renderReferencePolyline(map)
        sessionMarkers.forEach { it.remove() }
        sessionMarkers.clear()
        sessionPolyline?.remove()
        sessionPolyline = null

        val pointsForLine = mutableListOf<LatLng>()
        sessionWaypoints.forEachIndexed { idx, wp ->
            val lat = wp.latitude ?: return@forEachIndexed
            val lng = wp.longitude ?: return@forEachIndexed
            val pos = LatLng(lat, lng)
            val type = WaypointType.entries.firstOrNull { it.name == wp.type } ?: WaypointType.NODE
            val isPendingDeploy = when (wp.basicData["IS_PENDING_DEPLOY"]?.trim()?.lowercase()) {
                "1", "true", "y", "yes" -> true
                else -> false
            }
            val isVirtual = wp.isVirtual
            val icon = if (idx == currentIndex) {
                MarkerIconFactory.enlarged(this, type, isPendingDeploy, isVirtual)
            } else {
                MarkerIconFactory.normal(this, type, isPendingDeploy, isVirtual)
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(wp.label)
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
            )
            marker?.let { sessionMarkers.add(it) }
            pointsForLine.add(pos)
        }

        val shouldShowPurple = shouldShowEditPolyline()
        if (pointsForLine.size >= 2 && shouldShowPurple) {
            sessionPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(pointsForLine)
                    .width(10f)
                    .geodesic(true)
                    .color(android.graphics.Color.parseColor("#562ECB"))
                    .clickable(false)
            )
        }
    }

    private fun renderReferencePolyline(map: GoogleMap) {
        referencePolyline?.remove()
        referencePolyline = null
        if (referencePoints.size < 2) return
        referencePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(referencePoints)
                .width(8f)
                .geodesic(true)
                .color(android.graphics.Color.parseColor("#B4B4B4"))
                .clickable(false)
        )
    }

    private fun buildReferencePoints(lats: DoubleArray, lngs: DoubleArray): List<LatLng> {
        if (lats.isEmpty() || lngs.isEmpty()) return emptyList()
        val n = minOf(lats.size, lngs.size)
        val out = ArrayList<LatLng>(n)
        for (i in 0 until n) {
            val lat = lats[i]
            val lng = lngs[i]
            if (lat !in -90.0..90.0) continue
            if (lng !in -180.0..180.0) continue
            out.add(LatLng(lat, lng))
        }
        return out
    }

    private fun buildLatLngSnapshot(waypoints: List<WaypointSnapshot>): List<Pair<Long, Long>> {
        fun quantize(v: Double): Long = kotlin.math.round(v * 1_000_000.0).toLong()
        return waypoints.mapNotNull { wp ->
            val lat = wp.latitude ?: return@mapNotNull null
            val lng = wp.longitude ?: return@mapNotNull null
            quantize(lat) to quantize(lng)
        }.sortedWith(compareBy({ it.first }, { it.second }))
    }

    private fun shouldShowEditPolyline(): Boolean {
        // 若沒有參考線資料（一般線段/非弧線流程），沿用既有行為：永遠顯示紫色線段
        if (referencePoints.size < 2) return true
        // 檢視模式不允許改座標，不顯示紫色線段
        if (isViewMode) return false
        if (hasShownEditPolyline) return true
        val current = buildLatLngSnapshot(sessionWaypoints)
        val changed = current != initialLatLngSnapshot
        if (changed) hasShownEditPolyline = true
        return hasShownEditPolyline
    }

    private fun setWmtsTiles(layer: String) {
        formMapTileOverlay?.remove()
        val safeLayer = when (layer.uppercase()) {
            "EMAP01" -> "EMAP01"
            "PHOTO2" -> "PHOTO2"
            else -> "EMAP"
        }
        val urlTemplate =
            "https://wmts.nlsc.gov.tw/wmts/$safeLayer/default/GoogleMapsCompatible/%d/%d/%d"
        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = try {
                URL(String.format(urlTemplate, zoom, y, x))
            } catch (e: MalformedURLException) { null }
        }
        formMapTileOverlay = formMap?.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider).zIndex(-1f)
        )
    }

	    private fun setupTitleBar(label: String) {
	        binding.tvFormTitle.text = label

	        if (isOfflineMode) {
	            // 離線模式：左上角改為 ×，返回先同步草稿再關閉
	            binding.btnBack.setImageResource(com.example.taoyuangutter.R.drawable.ic_close)
	            binding.btnBack.contentDescription = "取消"
	            binding.btnBack.setOnClickListener { handleNavigateBack() }
	        } else {
	            binding.btnBack.setOnClickListener { handleNavigateBack() }
	        }

        if (isViewMode) {
            binding.btnEdit.visibility   = View.VISIBLE
            binding.btnDone.visibility   = View.GONE
            binding.fabSubmit.visibility = View.GONE
            binding.btnEdit.setOnClickListener { enterEditMode() }
        } else {
            binding.btnEdit.visibility   = View.GONE
            binding.btnDone.visibility   = View.GONE
            binding.fabSubmit.visibility = View.VISIBLE
        }
    }

    private fun enterEditMode() {
        isViewMode = false
        isEditMode = true // 進入編輯模式
        binding.btnEdit.visibility   = View.GONE
        binding.btnDone.visibility   = View.VISIBLE
        binding.fabSubmit.visibility = View.GONE
        binding.btnDone.setOnClickListener { saveAndFinish() }
        pagerAdapter.getBasicInfoFragment()?.setEditable(true)
        binding.cbIsVirtual.isEnabled = true
    }

    private fun setImportedWaypointLocked(locked: Boolean) {
        importedWaypointLocked = locked
        applyImportedWaypointLock()
    }

    private fun applyImportedWaypointLock() {
        if (!::pagerAdapter.isInitialized) return
        binding.viewPager.post {
            pagerAdapter.getBasicInfoFragment()?.setImportLocked(importedWaypointLocked)
        }
    }

    private fun returnToPreviewMode() {
        isViewMode = true
        binding.btnEdit.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE
        binding.fabSubmit.visibility = View.GONE
        binding.btnEdit.setOnClickListener { enterEditMode() }
        pagerAdapter.getBasicInfoFragment()?.setEditable(false)
        binding.cbIsVirtual.isEnabled = false
    }

    private fun initializeCurrentFormData(initialData: Map<String, String>) {
        currentFormData.clear()
        currentFormData.putAll(PendingPhotoDraftState.promotePendingFilesToPhotos(this, initialData))
        enrichMissingPhotoCapturedAt(currentFormData)
        ensureCurrentFormCoordinates()
        syncCurrentWaypointFromCurrentFormData()
    }

    private fun refreshCurrentFormDataFromFragments() {
        pagerAdapter.getBasicInfoFragment()?.let { mergeCurrentFormData(it.collectData()) }
        pagerAdapter.getBasicInfoFragment()?.let { photosFragment ->
            val (photo1, photo2, photo3) = photosFragment.getPhotoPaths()
            updateCurrentFormPhotos(photo1, photo2, photo3)
        }
    }

    private fun mergeCurrentFormData(data: Map<String, String>) {
        currentFormData.putAll(data)
        ensureCurrentFormCoordinates()
        syncCurrentWaypointFromCurrentFormData()
    }

    private fun updateCurrentFormPhotos(photo1: String?, photo2: String?, photo3: String?) {
        currentFormData["photo1"] = photo1 ?: ""
        currentFormData["photo2"] = photo2 ?: ""
        currentFormData["photo3"] = photo3 ?: ""
        if (photo1.isNullOrBlank()) {
            currentFormData["photo1CapturedAt"] = ""
            clearPhotoUploadState(1)
        }
        if (photo2.isNullOrBlank()) {
            currentFormData["photo2CapturedAt"] = ""
            clearPhotoUploadState(2)
        }
        if (photo3.isNullOrBlank()) {
            currentFormData["photo3CapturedAt"] = ""
            clearPhotoUploadState(3)
        }
        enrichMissingPhotoCapturedAt(currentFormData)
        syncCurrentWaypointFromCurrentFormData()
    }

    private fun updateCurrentPhotoCapturedAt(slot: Int, capturedAt: String?) {
        if (slot !in 1..3) return
        PhotoCapturedAtResolver.writeBasicData(currentFormData, slot, capturedAt)
        syncCurrentWaypointFromCurrentFormData()
    }

    private fun updateCurrentPendingPhoto(slot: Int, pendingOutputPath: String?) {
        if (slot !in 1..3) return
        PendingPhotoDraftState.writePath(currentFormData, slot, pendingOutputPath)
        syncCurrentWaypointFromCurrentFormData()
    }

    private fun updatePhotoUploadState(slot: Int, state: String, imgId: Int? = null, error: String? = null) {
        if (slot !in 1..3) return
        PhotoUploadSlotState.writeState(currentFormData, slot, state = state, imgId = imgId, error = error)
        syncCurrentWaypointFromCurrentFormData()
        pagerAdapter.getBasicInfoFragment()?.updatePhotoUploadStatus(slot, state, imgId, error)
    }

    private fun clearPhotoUploadState(slot: Int) {
        if (slot !in 1..3) return
        PhotoUploadSlotState.clear(currentFormData, slot)
        syncCurrentWaypointFromCurrentFormData()
        pagerAdapter.getBasicInfoFragment()?.updatePhotoUploadStatus(
            slot = slot,
            state = PhotoUploadSlotState.STATE_IDLE,
            imgId = null,
            error = null
        )
    }

    private fun currentFormPhotos(): Triple<String?, String?, String?> = Triple(
        currentFormData["photo1"]?.takeIf { it.isNotBlank() },
        currentFormData["photo2"]?.takeIf {
            it.isNotBlank() && !parseLooseBoolean(currentFormData["IS_CANTOPEN"])
        },
        currentFormData["photo3"]?.takeIf {
            it.isNotBlank() && !parseLooseBoolean(currentFormData["IS_CANTOPEN"])
        }
    )

    private fun currentFormSnapshot(): HashMap<String, String> = HashMap(currentFormData).apply {
        preserveEditSpiNum(this)
        putIfAbsent("photo1", "")
        putIfAbsent("photo2", "")
        putIfAbsent("photo3", "")
        putIfAbsent("photo1CapturedAt", "")
        putIfAbsent("photo2CapturedAt", "")
        putIfAbsent("photo3CapturedAt", "")
        if (parseLooseBoolean(this["IS_CANTOPEN"])) {
            this["photo2"] = ""
            this["photo3"] = ""
            this["photo2CapturedAt"] = ""
            this["photo3CapturedAt"] = ""
            PhotoUploadSlotState.clear(this, 2)
            PhotoUploadSlotState.clear(this, 3)
        }
    }

    private fun preserveEditSpiNum(target: MutableMap<String, String>) {
        if (!isEditMode) return
        val originalSpiNum = originalSessionWaypoint?.basicData?.get("SPI_NUM")
            ?.takeIf { it.isNotBlank() }
            ?: sessionWaypoints.getOrNull(currentIndex)?.basicData?.get("SPI_NUM")
                ?.takeIf { it.isNotBlank() }
        if (originalSpiNum.isNullOrBlank()) return
        if (target["SPI_NUM"].isNullOrBlank()) {
            target["SPI_NUM"] = originalSpiNum
        }
    }

    private fun currentFormPhotoCapturedAt(slot: Int): String? =
        PhotoCapturedAtResolver.readBasicData(currentFormData, slot)

    private fun currentFormPhotoUploadState(slot: Int): String =
        PhotoUploadSlotState.readState(currentFormData, slot)

    private fun currentFormPhotoImgId(slot: Int): Int? =
        PhotoUploadSlotState.readImgId(currentFormData, slot)

    private fun currentFormPhotoUploadError(slot: Int): String? =
        PhotoUploadSlotState.readError(currentFormData, slot)

    private fun enrichMissingPhotoCapturedAt(target: HashMap<String, String>) {
        (1..3).forEach { slot ->
            val photoPath = target["photo$slot"]
            if (photoPath.isNullOrBlank()) {
                PhotoCapturedAtResolver.writeBasicData(target, slot, null)
                return@forEach
            }
            val existing = PhotoCapturedAtResolver.readBasicData(target, slot)
            if (!existing.isNullOrBlank()) return@forEach
            val resolved = PhotoCapturedAtResolver.resolveBestEffort(this, photoPath)
            PhotoCapturedAtResolver.writeBasicData(target, slot, resolved)
        }
    }

    private fun ensureCurrentFormCoordinates() {
        if (currentLat != 0.0) {
            currentFormData["NODE_Y"] = currentFormData["NODE_Y"]?.takeIf { it.isNotBlank() }
                ?: "%.6f".format(currentLat)
        }
        if (currentLng != 0.0) {
            currentFormData["NODE_X"] = currentFormData["NODE_X"]?.takeIf { it.isNotBlank() }
                ?: "%.6f".format(currentLng)
        }
    }

    private fun syncCurrentWaypointFromCurrentFormData() {
        if (currentIndex !in sessionWaypoints.indices) return
        val existing = sessionWaypoints[currentIndex]
        val latitude = currentFormData["NODE_Y"]?.toDoubleOrNull()
            ?: existing.latitude
            ?: currentLat.takeIf { it != 0.0 }
        val longitude = currentFormData["NODE_X"]?.toDoubleOrNull()
            ?: existing.longitude
            ?: currentLng.takeIf { it != 0.0 }
        sessionWaypoints[currentIndex] = existing.copy(
            latitude = latitude,
            longitude = longitude,
            basicData = HashMap(existing.basicData).apply { putAll(currentFormData) }
        )
    }

    private fun resolveEffectiveCoordinates(data: Map<String, String>): Pair<Double?, Double?> {
        val formLat = data["NODE_Y"]?.toDoubleOrNull()
        val formLng = data["NODE_X"]?.toDoubleOrNull()
        val effectiveLat = if (formLat != null && formLat in -90.0..90.0) formLat
        else if (currentLat != 0.0) currentLat
        else null
        val effectiveLng = if (formLng != null && formLng in -180.0..180.0) formLng
        else if (currentLng != 0.0) currentLng
        else null
        return effectiveLat to effectiveLng
    }

    private fun syncNormalizedPhotosBackToUiIfNeeded(photo1: String?, photo2: String?, photo3: String?) {
        updateCurrentFormPhotos(photo1, photo2, photo3)
        if (!photo1.isNullOrBlank()) updateCurrentPendingPhoto(1, null)
        if (!photo2.isNullOrBlank()) updateCurrentPendingPhoto(2, null)
        if (!photo3.isNullOrBlank()) updateCurrentPendingPhoto(3, null)
        pagerAdapter.getBasicInfoFragment()?.syncPersistedPhotoState(
            photo1 = photo1,
            photo2 = photo2,
            photo3 = photo3,
            capturedAt1 = currentFormPhotoCapturedAt(1),
            capturedAt2 = currentFormPhotoCapturedAt(2),
            capturedAt3 = currentFormPhotoCapturedAt(3),
            uploadState1 = currentFormPhotoUploadState(1),
            uploadState2 = currentFormPhotoUploadState(2),
            uploadState3 = currentFormPhotoUploadState(3),
            imgId1 = currentFormPhotoImgId(1),
            imgId2 = currentFormPhotoImgId(2),
            imgId3 = currentFormPhotoImgId(3),
            uploadError1 = currentFormPhotoUploadError(1),
            uploadError2 = currentFormPhotoUploadError(2),
            uploadError3 = currentFormPhotoUploadError(3)
        )
    }

    private fun saveAndFinish() {
        syncSessionDraftNowBlocking()
        val data = currentFormSnapshot()
        val (photo1, photo2, photo3) = currentFormPhotos()
        dispatchEditResult(data, photo1, photo2, photo3)
    }

    /** 組裝 inspect→edit 模式的 Result Intent 並 finish。 */
    private fun dispatchEditResult(
        data: Map<String, String>, photo1: String?, photo2: String?, photo3: String?
    ) {
            val resultIntent = Intent().apply {
                GutterFormContract.putResultData(
                    intent = this,
                    waypointIndex = waypointIndex,
                    basicData = data,
                    photo1 = photo1,
                    photo2 = photo2,
                    photo3 = photo3,
                    includeSpiNum = true
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun setupViewPager(lat: Double, lng: Double, basicData: HashMap<String, String>) {
        pagerAdapter = GutterFormPagerAdapter(this, lat, lng, isViewMode, basicData, isOfflineMode, isEditMode)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateTabUI(position)
        })
    }

    private fun setupTabButtons() {
        // 使用 TabLayoutMediator 連接 TabLayout 和 ViewPager2
        binding.switchPageBar.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        pagerAdapter.getBasicInfoFragment()?.onRequestLocationPick = { launchLocationPicker() }
    }

    private fun launchLocationPicker() {
        if (isViewMode) return
        val initialLat = currentFormData["NODE_Y"]?.toDoubleOrNull() ?: currentLat
        val initialLng = currentFormData["NODE_X"]?.toDoubleOrNull() ?: currentLng
        val wmtsLayer = intent.getStringExtra(EXTRA_WMTS_LAYER)
        // 使用即時的 sessionWaypoints（而非啟動時的舊 JSON），
        // 讓地圖選點頁面能顯示最新的行程線段。單點離線模式只有 1 個 waypoint，不需傳線段。
        val waypointsJson = if (sessionWaypoints.size > 1) Gson().toJson(sessionWaypoints) else null
        val intent = MapPointPickerActivity.newIntent(
            context = this,
            initialLat = initialLat,
            initialLng = initialLng,
            wmtsLayer = wmtsLayer,
            sessionWaypointsJson = waypointsJson,
            currentIndex = currentIndex,
            isEditMode = isEditMode,
            showPlan = showPlanOverlay,
            showWaterOld = showWaterOldOverlay,
            showPossible = showPossibleOverlay,
            showRegion = showRegionOverlay
        )
        locationPickerLauncher.launch(intent)
    }

    private fun setupImportWaypointButton() {
        if (isOfflineMode) {
            // 離線模式不打 API，隱藏匯入功能
            binding.importWaypointBar.visibility = View.GONE
            return
        }
        binding.btnSelectWaypoint.setOnClickListener {
            showImportExistingWaypointSheet()
        }
    }

    private fun setupFab() {
        // 編輯模式：FAB 文字改為「完成」
        if (isEditMode) {
            binding.fabSubmit.text = getString(R.string.form_finish_button)
        }
        binding.fabSubmit.setOnClickListener {
            if (binding.cbIsVirtual.isChecked) {
                // 虛擬點模式：直接上傳/完成，跳過照片驗證
                if (isOfflineMode) saveOfflineAndClose(silent = false) else buildAndFinishWithResult()
                return@setOnClickListener
            }

            when {
                isOfflineMode -> saveOfflineAndClose(silent = false)
                else -> saveAndClose()
            }
        }
    }

    // ── 正式流程（有地圖點位）────────────────────────────────────────────

    private fun saveAndClose() {
        val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
        if (basicError != null) {
            binding.viewPager.currentItem = 0
            Toast.makeText(this, String.format(getString(R.string.msg_fill_required), basicError), Toast.LENGTH_SHORT).show()
            return
        }

        // 虛擬點不需驗證照片
        if (binding.cbIsVirtual.isChecked) {
            buildAndFinishWithResult()
            return
        }

        val photoError = pagerAdapter.getBasicInfoFragment()?.validateAllPhotos()
        if (photoError != null) {
            Toast.makeText(this, String.format(getString(R.string.msg_take_photo_required), photoError), Toast.LENGTH_SHORT).show()
            return
        }
        buildAndFinishWithResult()
    }

    private fun handleNavigateBack() {
        if (launchedInViewMode && isEditMode && !isViewMode) {
            // 檢視→編輯→返回：先把目前編輯結果寫回草稿，再回到預覽
            syncSessionDraftNowBlocking()
            formMap?.let { renderSessionPreview(it) }
            returnToPreviewMode()
            return
        }
        buildAndFinishWithResult()
    }

    private fun buildAndFinishWithResult() {
        syncSessionDraftNowBlocking()
        val basicData = currentFormSnapshot()
        val (photo1, photo2, photo3) = currentFormPhotos()
        val (effectiveLat, effectiveLng) = resolveEffectiveCoordinates(basicData)

        fun dispatchResult() {
            val resultIntent = Intent().apply {
                GutterFormContract.putResultData(
                    intent = this,
                    waypointIndex = waypointIndex,
                    latitude = effectiveLat,
                    longitude = effectiveLng,
                    basicData = basicData,
                    photo1 = photo1,
                    photo2 = photo2,
                    photo3 = photo3,
                    includeSpiNum = true
                )
                if (sessionDraftId > 0L) {
                    putExtra(EXTRA_SESSION_DRAFT_ID, sessionDraftId)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        dispatchResult()
    }

    private fun restoreSessionWaypoints(savedInstanceState: Bundle?) {
        val json = savedInstanceState?.getString("saved_waypoints_json")
            ?: intent.getStringExtra(EXTRA_SESSION_WAYPOINTS_JSON)
        if (!json.isNullOrEmpty()) {
            val parsed = runCatching {
                val type = object : TypeToken<List<WaypointSnapshot>>() {}.type
                Gson().fromJson<List<WaypointSnapshot>>(json, type)
            }.getOrNull()
            if (parsed != null) {
                sessionWaypoints.clear()
                sessionWaypoints.addAll(parsed)
            }
        }

        if (sessionWaypoints.isEmpty()) {
            waypointLabels.forEachIndexed { index, label ->
                sessionWaypoints.add(
                    WaypointSnapshot(
                        type = when {
                            index == 0 -> WaypointType.START.name
                            index == waypointLabels.lastIndex -> WaypointType.END.name
                            else -> WaypointType.NODE.name
                        },
                        label = label,
                        latitude = latitudes.getOrNull(index),
                        longitude = longitudes.getOrNull(index),
                        basicData = hashMapOf()
                    )
                )
            }
        }
    }

    private fun queueSessionDraftSync() {
        if (isViewMode) return
        if (currentIndex !in sessionWaypoints.indices) return
        syncSessionDraftNowBlocking()
    }

    private fun queuePhotoDraftSync() {
        if (photoDraftBatchDepth > 0) {
            pendingPhotoDraftSync = true
            return
        }
        queueSessionDraftSync()
    }

    /** 立即將目前表單狀態同步寫入草稿。 */
    private fun syncSessionDraftNowBlocking() {
        if (isViewMode) return
        runBlocking {
            syncSessionDraftNow()
        }
    }

    private suspend fun syncSessionDraftNow() {
        if (isViewMode) return
        if (currentIndex !in sessionWaypoints.indices) return

        ensureCurrentFormCoordinates()
        val existing = sessionWaypoints[currentIndex]
        val mergedBasicData = HashMap(existing.basicData).apply { putAll(currentFormSnapshot()) }
        enrichMissingPhotoCapturedAt(mergedBasicData)
        val normalizedBasicData = PhotoUriStore.normalizeBasicDataPhotoUris(
            context = this,
            basicData = mergedBasicData,
            prefix = "GUTTER_EXT_"
        )
        enrichMissingPhotoCapturedAt(normalizedBasicData)

        syncNormalizedPhotosBackToUiIfNeeded(
            photo1 = normalizedBasicData["photo1"]?.takeIf { it.isNotBlank() },
            photo2 = normalizedBasicData["photo2"]?.takeIf { it.isNotBlank() },
            photo3 = normalizedBasicData["photo3"]?.takeIf { it.isNotBlank() }
        )

        val formLat = normalizedBasicData["NODE_Y"]?.toDoubleOrNull()
        val formLng = normalizedBasicData["NODE_X"]?.toDoubleOrNull()

        sessionWaypoints[currentIndex] = existing.copy(
            latitude = if (formLat != null && formLat in -90.0..90.0) formLat else existing.latitude,
            longitude = if (formLng != null && formLng in -180.0..180.0) formLng else existing.longitude,
            basicData = normalizedBasicData
        )
        val normalizedWaypoints = PhotoUriStore.normalizeSnapshotPhotoUris(
            context = this,
            waypoints = sessionWaypoints.toList(),
            prefix = "GUTTER_EXT_"
        )
        sessionWaypoints.clear()
        sessionWaypoints.addAll(normalizedWaypoints)
        currentFormData.putAll(sessionWaypoints[currentIndex].basicData)

        val resolvedDraftId = sessionDraftId.takeIf { it > 0L } ?: run {
            android.util.Log.w(
                "GutterFormActivity",
                "skip draft sync because sessionDraftId is missing"
            )
            return
        }
        sessionDraftId = resolvedDraftId

        val repo = GutterSessionRepository(this)
        val existingDraft = repo.getById(resolvedDraftId)
        val preservedIsOffline = existingDraft?.isOffline
            ?: (isOfflineMode || intent.getBooleanExtra(EXTRA_SESSION_IS_OFFLINE, false))
        repo.save(
            GutterSessionDraft(
                id = resolvedDraftId,
                savedAt = System.currentTimeMillis(),
                isOffline = isOfflineMode || preservedIsOffline,
                isSinglePoint = false,
                waypoints = sessionWaypoints.toList()
            )
        )
    }

    private fun restoreCurrentWaypointState() {
        if (isOfflineMode) return
        val original = originalSessionWaypoint ?: return
        if (currentIndex !in sessionWaypoints.indices) return
        sessionWaypoints[currentIndex] = original.copy(basicData = HashMap(original.basicData))
        initializeCurrentFormData(original.basicData)
        val lat = original.latitude
        val lng = original.longitude
        if (lat != null && lng != null) {
            currentLat = lat
            currentLng = lng
            pagerAdapter.getBasicInfoFragment()?.updateCoordinates(lng, lat)
        }
        syncNormalizedPhotosBackToUiIfNeeded(
            photo1 = original.basicData["photo1"]?.takeIf { it.isNotBlank() },
            photo2 = original.basicData["photo2"]?.takeIf { it.isNotBlank() },
            photo3 = original.basicData["photo3"]?.takeIf { it.isNotBlank() }
        )
    }

    // ── 上傳等待遮罩 ─────────────────────────────────────────────────────

    /**
     * 顯示或隱藏上傳照片的等待遮罩。
     * 遮罩期間阻擋所有使用者操作；上傳完成後隱藏並繼續 finish 流程。
     */
    private fun showUploadLoading(show: Boolean, message: String? = null) {
        binding.uploadLoadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.tvUploadLoadingMessage.text = message ?: getString(R.string.msg_uploading_photos)
        }
    }

    // ── 照片上傳 ────────────────────────────────────────────────────────

    /**
     * 將可用的照片來源依 fileCategory 上傳至 nodeImage API。
     * 會先透過共用判定確認來源是否可讀，遠端 URL 會先下載成本機檔再上傳。
     * 使用 Coroutines 並行上傳，顯著加速多張照片時的處理時間。
     */
    private suspend fun uploadLocalPhotos(
        nodeId: Int,
        token: String,
        photo1: String?,
        photo2: String?,
        photo3: String?
    ) = coroutineScope {
        if (binding.cbIsVirtual.isChecked) {
            android.util.Log.d("PhotoUpload", "虛擬點不進行照片上傳，nodeId=$nodeId")
            return@coroutineScope
        }
        val isCantOpen = parseLooseBoolean(currentFormData["IS_CANTOPEN"])
        listOf(photo1 to 1, photo2 to 2, photo3 to 3)
            .filterNot { (_, category) -> isCantOpen && category in 2..3 }
            .filter { (path, _) -> PhotoUploadValidator.isUsableForUpload(this@GutterFormActivity, path) }
            .map { (path, category) ->
                async {
                    val usablePath = path ?: return@async
                    android.util.Log.d("PhotoUpload", "開始並行上傳 photo$category: $usablePath")
                    val result = gutterRepository.uploadNodeImage(
                        context      = this@GutterFormActivity,
                        nodeId       = nodeId,
                        fileCategory = category,
                        imageUri     = Uri.parse(usablePath),
                        token        = token
                    )
                    if (result is ApiResult.Error) {
                        android.util.Log.w("PhotoUpload", "photo$category 上傳失敗: ${result.message}")
                    } else {
                        android.util.Log.d("PhotoUpload", "photo$category 上傳成功")
                    }
                }
            }
            .awaitAll()
    }

    // ── 離線模式（儲存至本機草稿）────────────────────────────────────────

    /**
     * 將目前填寫內容儲存為一般 GutterSessionDraft。
     * @param silent true → 不驗證、不顯示 Toast、直接 finish（返回鍵自動存草稿）
     *               false → 先驗證所有必填欄位與三張照片，通過才存檔並關閉
     */
    private fun saveOfflineAndClose(silent: Boolean = false) {
        if (!silent) {
            val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
            if (basicError != null) {
                binding.viewPager.currentItem = 0
                Toast.makeText(this, String.format(getString(R.string.msg_fill_required), basicError), Toast.LENGTH_SHORT).show()
                return
            }

            // 虛擬點不需驗證照片
            if (!binding.cbIsVirtual.isChecked) {
                val photoError = pagerAdapter.getBasicInfoFragment()?.validateAllPhotos()
                if (photoError != null) {
                    Toast.makeText(this, String.format(getString(R.string.msg_take_photo_required), photoError), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        syncSessionDraftNowBlocking()
        if (!silent) {
            Toast.makeText(this@GutterFormActivity, getString(R.string.msg_draft_saved), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleNavigateBack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("saved_waypoints_json", Gson().toJson(sessionWaypoints))
        outState.putString("saved_current_form_data_json", Gson().toJson(currentFormData))
        outState.putBoolean("saved_is_view_mode", isViewMode)
        outState.putBoolean("saved_is_edit_mode", isEditMode)
        outState.putBoolean("saved_imported_waypoint_locked", importedWaypointLocked)
        outState.putBoolean("saved_is_virtual", binding.cbIsVirtual.isChecked)
        outState.putDouble("saved_current_lat", currentLat)
        outState.putDouble("saved_current_lng", currentLng)
        outState.putBoolean("saved_has_shown_edit_polyline", hasShownEditPolyline)
        outState.putLong("saved_session_draft_id", sessionDraftId)
        if (originalSessionWaypoint != null) {
            outState.putString("saved_original_waypoint_json", Gson().toJson(originalSessionWaypoint))
        }
    }

    private fun setupVirtualPointToggle(isVirtualInitial: Boolean) {
        binding.cbIsVirtual.isChecked = isVirtualInitial
        // 先對 Activity 自有的 UI 進行立即反應
        applyVirtualModeUi(isVirtualInitial)

        binding.cbIsVirtual.isEnabled = !isViewMode
        binding.cbIsVirtual.setOnCheckedChangeListener { _, isChecked ->
            // 通知 Fragment
            pagerAdapter.getBasicInfoFragment()?.setVirtualMode(isChecked)
            // 更新 Activity UI
            applyVirtualModeUi(isChecked)
            queueSessionDraftSync()
        }
    }

    /** 僅更新 Activity 層級的 UI（Tab, ViewPager 等）*/
    private fun applyVirtualModeUi(isVirtual: Boolean) {
        binding.switchPageBar.visibility = View.GONE
        
        if (isOfflineMode) {
            binding.importWaypointBar.visibility = View.GONE
        } else {
            binding.importWaypointBar.visibility = if (isVirtual) View.GONE else View.VISIBLE
        }

        binding.viewPager.isUserInputEnabled = false
    }

    private fun applyVirtualMode(isVirtual: Boolean) {
        pagerAdapter.getBasicInfoFragment()?.setVirtualMode(isVirtual)
        applyVirtualModeUi(isVirtual)
    }

    private fun parseLooseBoolean(raw: String?): Boolean {
        val v = raw?.trim()?.lowercase()
        return when (v) {
            "1", "true", "t", "y", "yes" -> true
            "0", "false", "f", "n", "no", "", null -> false
            else -> false
        }
    }
}
