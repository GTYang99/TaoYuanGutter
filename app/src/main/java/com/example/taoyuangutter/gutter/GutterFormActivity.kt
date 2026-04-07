package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.databinding.ActivityGutterFormBinding
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.example.taoyuangutter.common.LocationPickEvents
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL

class  GutterFormActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGutterFormBinding
    private lateinit var pagerAdapter: GutterFormPagerAdapter

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
        const val EXTRA_DATA_DEPTH       = "ex_node_dep"
        const val EXTRA_DATA_TOP_WIDTH   = "ex_node_wid"
        const val EXTRA_DATA_IS_BROKEN   = "ex_is_broken"
        const val EXTRA_DATA_IS_HANGING  = "ex_is_hanging"
        const val EXTRA_DATA_IS_SILT     = "ex_is_silt"
        const val EXTRA_DATA_IS_CANTOPEN = "ex_is_cantopen"
        const val EXTRA_DATA_REMARKS     = "ex_node_note"
        const val EXTRA_DATA_PHOTO_1     = "ex_photo1"
        const val EXTRA_DATA_PHOTO_2     = "ex_photo2"
        const val EXTRA_DATA_PHOTO_3     = "ex_photo3"
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
        const val RESULT_DATA_DEPTH       = "r_node_dep"
        const val RESULT_DATA_TOP_WIDTH   = "r_node_wid"
        const val RESULT_DATA_IS_BROKEN   = "r_is_broken"
        const val RESULT_DATA_IS_HANGING  = "r_is_hanging"
        const val RESULT_DATA_IS_SILT     = "r_is_silt"
        const val RESULT_DATA_IS_CANTOPEN = "r_is_cantopen"
        const val RESULT_DATA_REMARKS     = "r_node_note"
        const val RESULT_DATA_PHOTO_1     = "r_photo1"
        const val RESULT_DATA_PHOTO_2     = "r_photo2"
        const val RESULT_DATA_PHOTO_3     = "r_photo3"

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
	            sessionIsOffline: Boolean = false
	        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
	            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, labels)
	            putExtra(EXTRA_LATITUDES, lats)
	            putExtra(EXTRA_LONGITUDES, lngs)
	            putExtra(EXTRA_CURRENT_INDEX, index)
	            putExtra(EXTRA_WAYPOINT_INDEX, index)   // 與 currentIndex 一致，確保 buildAndFinishWithResult 回傳正確索引
	            putExtra(EXTRA_VIEW_MODE, false)
	            putExtra(EXTRA_IS_EDIT_MODE, isEditMode) // 傳入編輯模式旗標
	            putExtra(EXTRA_SESSION_DRAFT_ID, sessionDraftId)
	            putExtra(EXTRA_SESSION_IS_OFFLINE, sessionIsOffline)
	            if (!sessionWaypointsJson.isNullOrEmpty()) {
	                putExtra(EXTRA_SESSION_WAYPOINTS_JSON, sessionWaypointsJson)
	            }
	            if (!wmtsLayer.isNullOrEmpty()) {
                putExtra(EXTRA_WMTS_LAYER, wmtsLayer)
            }
            basicData?.let { fillDataExtras(this, it) }
        }

        fun newViewIntent(
            context: Context,
            label: String,
            lat: Double,
            lng: Double,
            waypointIndex: Int,
            basicData: HashMap<String, String>,
            wmtsLayer: String? = null
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
            fillDataExtras(this, basicData)
        }

        /**
         * 離線模式：不需地圖點位，座標固定 (0.0, 0.0)。
         * 草稿直接儲存為 [GutterSessionDraft]（isOffline=true, isSinglePoint=true）。
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

        private fun fillDataExtras(intent: Intent, data: HashMap<String, String>) {
            intent.putExtra(EXTRA_DATA_GUTTER_ID,   data["SPI_NUM"]    ?: data["gutterId"] ?: "")
            intent.putExtra(EXTRA_DATA_GUTTER_TYPE, data["NODE_TYP"]   ?: data["gutterType"] ?: "")
            intent.putExtra(EXTRA_DATA_MAT_TYP,     data["MAT_TYP"]    ?: data["matTyp"] ?: "")
            intent.putExtra(EXTRA_DATA_COORD_X,     data["NODE_X"]     ?: data["coordX"] ?: "")
            intent.putExtra(EXTRA_DATA_COORD_Y,     data["NODE_Y"]     ?: data["coordY"] ?: "")
            intent.putExtra(EXTRA_DATA_COORD_Z,     data["NODE_LE"]    ?: data["coordZ"] ?: "")
            intent.putExtra(EXTRA_DATA_MEASURE_ID,  data["XY_NUM"]     ?: data["xyNum"] ?: "")
            intent.putExtra(EXTRA_DATA_DEPTH,       data["NODE_DEP"]   ?: data["depth"] ?: "")
            intent.putExtra(EXTRA_DATA_TOP_WIDTH,   data["NODE_WID"]   ?: data["topWidth"] ?: "")
            intent.putExtra(EXTRA_DATA_IS_BROKEN,   data["IS_BROKEN"]  ?: data["isBroken"] ?: "")
            intent.putExtra(EXTRA_DATA_IS_HANGING,  data["IS_HANGING"] ?: data["isHanging"] ?: "")
            intent.putExtra(EXTRA_DATA_IS_SILT,     data["IS_SILT"]    ?: data["isSilt"] ?: "")
            intent.putExtra(EXTRA_DATA_IS_CANTOPEN, data["IS_CANTOPEN"] ?: data["isCantOpen"] ?: "")
            intent.putExtra(EXTRA_DATA_REMARKS,     data["NODE_NOTE"]  ?: data["remarks"] ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_1,     data["photo1"]     ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_2,     data["photo2"]     ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_3,     data["photo3"]     ?: "")
            intent.putExtra(EXTRA_DATA_XY_NUM,      data["XY_NUM"]     ?: data["xyNum"] ?: "")
            intent.putExtra(EXTRA_DATA_NODE_ID,      data["_nodeId"]    ?: "") // 傳入 node_id（編輯模式）
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

    /** true → 離線模式，儲存至本機草稿，不向 MainActivity 回傳 result */
    private var isOfflineMode  = false
    /** 地圖流程中的 session 草稿 ID；0 表示尚未建立。 */
    private var sessionDraftId = 0L
    /** 整條側溝目前的 waypoint 快照，供表單編輯中即時覆寫草稿。 */
    private val sessionWaypoints = mutableListOf<WaypointSnapshot>()
    private var draftSyncJob: Job? = null
    private var originalSessionWaypoint: WaypointSnapshot? = null

    /** 編輯模式：API 的 node_id（有值時儲存才會上傳照片） */
    private var nodeId: Int? = null

    // ── 背景地圖 ──────────────────────────────────────────────────────────
    private var formMap: GoogleMap? = null
    private var formMapTileOverlay: TileOverlay? = null
    private val gutterRepository = GutterRepository()
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
        pagerAdapter.getBasicInfoFragment()?.updateCoordinates(longitude, latitude)

        // 表單仍開著時，立即通知 MainActivity 更新背景地圖的點位與線段
        val draftId = sessionDraftId.takeIf { it > 0L }
            ?: intent.getLongExtra(EXTRA_SESSION_DRAFT_ID, 0L)
        if (draftId > 0L) {
            sendBroadcast(
                Intent(LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED).apply {
                    setPackage(packageName)
                    putExtra(LocationPickEvents.EXTRA_SESSION_DRAFT_ID, draftId)
                    putExtra(LocationPickEvents.EXTRA_WAYPOINT_INDEX, currentIndex)
                    putExtra(LocationPickEvents.EXTRA_LATITUDE, latitude)
                    putExtra(LocationPickEvents.EXTRA_LONGITUDE, longitude)
                }
            )
        }
    }

    // 導入既有點位的 launcher
    private val importWaypointLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val json = data.getStringExtra(ImportExistingWaypointActivity.EXTRA_NODE_DETAILS_JSON)
        if (!json.isNullOrEmpty()) {
            try {
                val nodeDetails = Gson().fromJson(json, com.example.taoyuangutter.api.NodeDetails::class.java)
                pagerAdapter.getBasicInfoFragment()?.prefillDataFromImport(nodeDetails)

                // 匯入時同步下載照片到本機（依序 1→2→3），確保節點可直接滿足上傳條件
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

                        pagerAdapter.getPhotosFragment()?.prefillPhotos(p1, p2, p3)
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
                        Toast.makeText(this@GutterFormActivity, String.format(getString(R.string.msg_photo_download_failed), e.message), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.msg_data_parse_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 本點位的原始 GPS 座標（來自地圖選點），永遠保留以確保 result 能帶回正確定位 */
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGutterFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        waypointLabels = intent.getStringArrayListExtra(EXTRA_WAYPOINT_LABELS) ?: arrayListOf()
        latitudes  = intent.getDoubleArrayExtra(EXTRA_LATITUDES)  ?: doubleArrayOf()
        longitudes = intent.getDoubleArrayExtra(EXTRA_LONGITUDES) ?: doubleArrayOf()
        currentIndex  = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        waypointIndex = intent.getIntExtra(EXTRA_WAYPOINT_INDEX, 0)
        isViewMode    = intent.getBooleanExtra(EXTRA_VIEW_MODE, false)
        isEditMode    = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false) // 取得編輯模式旗標
        isOfflineMode = intent.getBooleanExtra(EXTRA_OFFLINE_MODE, false)
        nodeId        = intent.getStringExtra(EXTRA_DATA_NODE_ID)?.toIntOrNull()
        sessionDraftId = intent.getLongExtra(EXTRA_SESSION_DRAFT_ID, 0L)
        restoreSessionWaypoints()

        // 離線模式開啟既有草稿：將 repo 中儲存的 basicData 填入 sessionWaypoints[0]，
        // 確保任何時機觸發的 syncSessionDraftNow() 都有正確基底資料，不會以空值覆寫。
        if (isOfflineMode && sessionDraftId > 0L && sessionWaypoints.isNotEmpty()) {
            val savedWp = GutterSessionRepository(this).getById(sessionDraftId)?.waypoints?.firstOrNull()
            if (savedWp != null) {
                sessionWaypoints[0] = sessionWaypoints[0].copy(basicData = HashMap(savedWp.basicData))
            }
        }

        originalSessionWaypoint = sessionWaypoints.getOrNull(currentIndex)?.copy(
            basicData = HashMap(sessionWaypoints.getOrNull(currentIndex)?.basicData ?: hashMapOf())
        )

        val label = waypointLabels.getOrElse(currentIndex) { "點位" }
        val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
        val lng   = longitudes.getOrElse(currentIndex) { 0.0 }

        currentLat = lat
        currentLng = lng

        // 離線模式：若有既有草稿 ID，從 GutterSessionRepository 讀取 waypoint 資料
        val existingData: HashMap<String, String> = if (isOfflineMode && sessionDraftId > 0L) {
            val draft = GutterSessionRepository(this).getById(sessionDraftId)
            val wp = draft?.waypoints?.firstOrNull()
            if (wp != null) HashMap(wp.basicData) else buildEmptyData(lat, lng)
        } else if (isOfflineMode) {
            buildEmptyData(lat, lng)
        } else {
            hashMapOf(
                "SPI_NUM"    to (intent.getStringExtra(EXTRA_DATA_GUTTER_ID)   ?: ""),
                "NODE_TYP"   to (intent.getStringExtra(EXTRA_DATA_GUTTER_TYPE) ?: ""),
                "MAT_TYP"    to (intent.getStringExtra(EXTRA_DATA_MAT_TYP)     ?: ""),
                "NODE_X"     to (intent.getStringExtra(EXTRA_DATA_COORD_X)     ?: ""),
                "NODE_Y"     to (intent.getStringExtra(EXTRA_DATA_COORD_Y)     ?: ""),
                "NODE_LE"    to (intent.getStringExtra(EXTRA_DATA_COORD_Z)     ?: ""),
                "XY_NUM"     to (intent.getStringExtra(EXTRA_DATA_MEASURE_ID)  ?: ""),
                "NODE_DEP"   to (intent.getStringExtra(EXTRA_DATA_DEPTH)       ?: ""),
                "NODE_WID"   to (intent.getStringExtra(EXTRA_DATA_TOP_WIDTH)   ?: ""),
                "IS_BROKEN"  to (intent.getStringExtra(EXTRA_DATA_IS_BROKEN)   ?: ""),
                "IS_HANGING" to (intent.getStringExtra(EXTRA_DATA_IS_HANGING)  ?: ""),
                "IS_SILT"    to (intent.getStringExtra(EXTRA_DATA_IS_SILT)     ?: ""),
                "IS_CANTOPEN" to (intent.getStringExtra(EXTRA_DATA_IS_CANTOPEN) ?: ""),
                "NODE_NOTE"  to (intent.getStringExtra(EXTRA_DATA_REMARKS)     ?: ""),
                "photo1"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_1)     ?: ""),
                "photo2"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_2)     ?: ""),
                "photo3"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_3)     ?: ""),
                "XY_NUM"     to (intent.getStringExtra(EXTRA_DATA_XY_NUM)      ?: "")
            )
        }

        // 全螢幕地圖背景 + 表單面板（不論離線或一般模式皆使用新版佈局）
        setupFullScreenWithMap()
        initFormMap()

        // 檢視模式：標題改為「側溝編號 {gutterId}」；其他模式沿用點位 label（起點/節點/終點）
        val titleText = if (isViewMode) {
            val gutterId = existingData["SPI_NUM"]?.takeIf { it.isNotEmpty() } ?: "---"
            "側溝編號 $gutterId"
        } else {
            label
        }
        setupTitleBar(titleText)
        setupViewPager(lat, lng, existingData)
        setupTabButtons()
        setupImportWaypointButton()
        setupFab()
        binding.viewPager.post { attachDraftSyncCallbacks() }
        pagerAdapter.getBasicInfoFragment()?.onRequestLocationPick = { launchLocationPicker() }
    }

    override fun onPause() {
        if (!isFinishing) {
            draftSyncJob?.cancel()
            syncSessionDraftNow()
        }
        super.onPause()
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
        "NODE_DEP"  to "",
        "NODE_WID"  to "",
        "IS_BROKEN" to "",
        "IS_HANGING" to "",
        "IS_SILT"   to "",
        "NODE_NOTE" to "",
        "photo1"    to "", "photo2" to "", "photo3" to ""
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

        // 設定表單面板高度為螢幕 3/4
        val screenH = resources.displayMetrics.heightPixels
        val panelHeight = screenH * 3 / 4
        binding.formPanel.layoutParams = binding.formPanel.layoutParams.apply {
            height = panelHeight
        }

        // 套用 system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.formPanel) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, bars.top, 0, 0)
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

        val lat = if (currentLat != 0.0) currentLat else 25.0330
        val lng = if (currentLng != 0.0) currentLng else 121.5654
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f))
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
            // 離線模式：左上角改為 ×，離開表單一律存草稿（不打 API）
            binding.btnBack.setImageResource(com.example.taoyuangutter.R.drawable.ic_close)
            binding.btnBack.contentDescription = "取消"
            binding.btnBack.setOnClickListener { saveOfflineAndClose(silent = true) }
        } else {
            binding.btnBack.setOnClickListener {
                if (!isViewMode) confirmOrDiscardAndClose() else finish()
            }
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
        pagerAdapter.getPhotosFragment()?.setEditable(true)
        attachDraftSyncCallbacks()
    }

    private fun saveAndFinish() {
        val data = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) =
            pagerAdapter.getPhotosFragment()?.getPhotoPaths() ?: Triple(null, null, null)
        dispatchEditResult(data, photo1, photo2, photo3)
    }

    /** 組裝 inspect→edit 模式的 Result Intent 並 finish。 */
    private fun dispatchEditResult(
        data: Map<String, String>, photo1: String?, photo2: String?, photo3: String?
    ) {
        val resultIntent = Intent().apply {
            putExtra(RESULT_WAYPOINT_INDEX,   waypointIndex)
            if (!isEditMode) putExtra(RESULT_DATA_GUTTER_ID, data["SPI_NUM"] ?: "")
            putExtra(RESULT_DATA_GUTTER_TYPE, data["NODE_TYP"]   ?: "")
            putExtra(RESULT_DATA_MAT_TYP,     data["MAT_TYP"]    ?: "")
            putExtra(RESULT_DATA_COORD_X,     data["NODE_X"]     ?: "")
            putExtra(RESULT_DATA_COORD_Y,     data["NODE_Y"]     ?: "")
            putExtra(RESULT_DATA_COORD_Z,     data["NODE_LE"]    ?: "")
            putExtra(RESULT_DATA_MEASURE_ID,  data["XY_NUM"]     ?: "")
            putExtra(RESULT_DATA_DEPTH,       data["NODE_DEP"]   ?: "")
            putExtra(RESULT_DATA_TOP_WIDTH,   data["NODE_WID"]   ?: "")
            putExtra(RESULT_DATA_IS_BROKEN,   data["IS_BROKEN"]  ?: "")
            putExtra(RESULT_DATA_IS_HANGING,  data["IS_HANGING"] ?: "")
            putExtra(RESULT_DATA_IS_SILT,     data["IS_SILT"]    ?: "")
            putExtra(RESULT_DATA_IS_CANTOPEN, data["IS_CANTOPEN"] ?: "")
            putExtra(RESULT_DATA_REMARKS,     data["NODE_NOTE"]  ?: "")
            putExtra(RESULT_DATA_PHOTO_1,     photo1             ?: "")
            putExtra(RESULT_DATA_PHOTO_2,     photo2             ?: "")
            putExtra(RESULT_DATA_PHOTO_3,     photo3             ?: "")
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

    private fun attachDraftSyncCallbacks() {
        pagerAdapter.getBasicInfoFragment()?.onDraftChanged = { queueSessionDraftSync() }
        pagerAdapter.getPhotosFragment()?.onDraftChanged = { queueSessionDraftSync() }
    }

    private fun setupTabButtons() {
        // 使用 TabLayoutMediator 連接 TabLayout 和 ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "基本資料"
                1 -> "照片上傳"
                else -> ""
            }
        }.attach()
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        attachDraftSyncCallbacks()
        pagerAdapter.getBasicInfoFragment()?.onRequestLocationPick = { launchLocationPicker() }
    }

    private fun launchLocationPicker() {
        if (isViewMode) return
        val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val initialLat = basicData["NODE_Y"]?.toDoubleOrNull() ?: currentLat
        val initialLng = basicData["NODE_X"]?.toDoubleOrNull() ?: currentLng
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
            isEditMode = isEditMode
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
            val intent = ImportExistingWaypointActivity.newIntent(this)
            importWaypointLauncher.launch(intent)
        }
    }

    private fun setupFab() {
        // 編輯模式：FAB 文字改為「完成」
        if (isEditMode) {
            binding.fabSubmit.text = getString(R.string.form_finish_button)
        }
        binding.fabSubmit.setOnClickListener {
            when {
                isOfflineMode -> saveOfflineAndClose(silent = false)
                isEditMode && binding.viewPager.currentItem == 1 -> {
                    // 照片頁面編輯模式：直接上傳照片並完成，不驗證基本資料
                    buildAndFinishWithResult()
                }
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
        val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
        if (photoError != null) {
            binding.viewPager.currentItem = 1
            Toast.makeText(this, String.format(getString(R.string.msg_take_photo_required), photoError), Toast.LENGTH_SHORT).show()
            return
        }
        buildAndFinishWithResult()
    }

    private fun saveDraftAndClose() {
        if (isOfflineMode) saveOfflineAndClose(silent = true) else confirmOrDiscardAndClose()
    }

    /**
     * 按下返回時：
     * - 編輯模式（isEditMode）：資料不完整時詢問是否放棄修改，確認後回傳
     *   RESULT_CANCELED（不清除原有 API 點位資料）；資料完整則直接儲存。
     * - 新增模式：原邏輯不變，確認後回傳 RESULT_DELETE（清除該點位座標與資料）。
     */
    private fun confirmOrDiscardAndClose() {
        val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
        val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
        if (basicError != null || photoError != null) {
            if (isEditMode) {
                // 編輯模式：放棄修改 → RESULT_CANCELED，MainActivity 不清除既有點位資料
                AlertDialog.Builder(this)
                    .setTitle("放棄修改")
                    .setMessage("確定要放棄此次修改並返回嗎？")
                    .setPositiveButton("確定放棄") { _, _ ->
                        restoreCurrentWaypointDraft()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setNegativeButton("繼續填寫", null)
                    .show()
            } else {
                // 新增模式：放棄填寫 → RESULT_DELETE，清除點位座標與資料
                AlertDialog.Builder(this)
                    .setTitle("資料尚未完成")
                    .setMessage("此點位的資料尚未填寫完整，確定返回嗎？\n已輸入的資料將不會儲存。")
                    .setPositiveButton("確定返回") { _, _ ->
                        val deleteIntent = Intent().putExtra(RESULT_WAYPOINT_INDEX, waypointIndex)
                        setResult(RESULT_DELETE, deleteIntent)
                        finish()
                    }
                    .setNegativeButton("繼續填寫", null)
                    .show()
            }
        } else {
            buildAndFinishWithResult()
        }
    }

    private fun buildAndFinishWithResult() {
        val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) =
            pagerAdapter.getPhotosFragment()?.getPhotoPaths() ?: Triple(null, null, null)

        val formLat = basicData["NODE_Y"]?.toDoubleOrNull()
        val formLng = basicData["NODE_X"]?.toDoubleOrNull()
        val effectiveLat = if (formLat != null && formLat in -90.0..90.0)   formLat else currentLat
        val effectiveLng = if (formLng != null && formLng in -180.0..180.0) formLng else currentLng

        fun dispatchResult() {
            val resultIntent = Intent().apply {
                putExtra(RESULT_LATITUDE,         effectiveLat)
                putExtra(RESULT_LONGITUDE,        effectiveLng)
                putExtra(RESULT_WAYPOINT_INDEX,   waypointIndex)
                if (!isEditMode) putExtra(RESULT_DATA_GUTTER_ID, basicData["SPI_NUM"] ?: "")
                putExtra(RESULT_DATA_GUTTER_TYPE, basicData["NODE_TYP"]   ?: "")
                putExtra(RESULT_DATA_MAT_TYP,     basicData["MAT_TYP"]    ?: "")
                putExtra(RESULT_DATA_COORD_X,     basicData["NODE_X"]     ?: "")
                putExtra(RESULT_DATA_COORD_Y,     basicData["NODE_Y"]     ?: "")
                putExtra(RESULT_DATA_COORD_Z,     basicData["NODE_LE"]    ?: "")
                putExtra(RESULT_DATA_MEASURE_ID,  basicData["XY_NUM"]     ?: "")
                putExtra(RESULT_DATA_DEPTH,       basicData["NODE_DEP"]   ?: "")
                putExtra(RESULT_DATA_TOP_WIDTH,   basicData["NODE_WID"]   ?: "")
                putExtra(RESULT_DATA_IS_BROKEN,   basicData["IS_BROKEN"]  ?: "")
                putExtra(RESULT_DATA_IS_HANGING,  basicData["IS_HANGING"] ?: "")
                putExtra(RESULT_DATA_IS_SILT,     basicData["IS_SILT"]    ?: "")
                putExtra(RESULT_DATA_IS_CANTOPEN, basicData["IS_CANTOPEN"] ?: "")
                putExtra(RESULT_DATA_REMARKS,     basicData["NODE_NOTE"]  ?: "")
                putExtra(RESULT_DATA_PHOTO_1,     photo1                  ?: "")
                putExtra(RESULT_DATA_PHOTO_2,     photo2                  ?: "")
                putExtra(RESULT_DATA_PHOTO_3,     photo3                  ?: "")
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        dispatchResult()
    }

    private fun restoreSessionWaypoints() {
        val json = intent.getStringExtra(EXTRA_SESSION_WAYPOINTS_JSON)
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
        draftSyncJob?.cancel()
        draftSyncJob = lifecycleScope.launch {
            delay(400)
            syncSessionDraftNow()
        }
    }

	    private fun syncSessionDraftNow() {
	        if (isViewMode) return
	        if (currentIndex !in sessionWaypoints.indices) return

        val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) = pagerAdapter.getPhotosFragment()?.getPhotoPaths()
            ?: Triple(null, null, null)

        val formLat = basicData["NODE_Y"]?.toDoubleOrNull()
        val formLng = basicData["NODE_X"]?.toDoubleOrNull()
        val existing = sessionWaypoints[currentIndex]
        val mergedBasicData = HashMap(existing.basicData).apply {
            putAll(basicData)
            put("photo1", photo1 ?: "")
            put("photo2", photo2 ?: "")
            put("photo3", photo3 ?: "")
        }

        sessionWaypoints[currentIndex] = existing.copy(
            latitude = if (formLat != null && formLat in -90.0..90.0) formLat else existing.latitude,
            longitude = if (formLng != null && formLng in -180.0..180.0) formLng else existing.longitude,
            basicData = mergedBasicData
        )

        // 空草稿判斷：沒有任何座標，且所有 basicData 都是空值 → 不存草稿
        val hasAnyLatLng = sessionWaypoints.any { it.latitude != null && it.longitude != null }
        val hasAnyBasicData = sessionWaypoints.any { wp ->
            wp.basicData.any { (_, v) -> v.isNotBlank() }
        }
	        if (!hasAnyLatLng && !hasAnyBasicData) return

	        val resolvedDraftId = if (sessionDraftId > 0L) sessionDraftId else System.currentTimeMillis()
	        sessionDraftId = resolvedDraftId

	        val repo = GutterSessionRepository(this)
	        val existingDraft = repo.getById(resolvedDraftId)
	        val preservedIsOffline = existingDraft?.isOffline
	            ?: (isOfflineMode || intent.getBooleanExtra(EXTRA_SESSION_IS_OFFLINE, false))
	        val preservedIsSinglePoint = existingDraft?.isSinglePoint ?: isOfflineMode
	        repo.save(
	            GutterSessionDraft(
	                id = resolvedDraftId,
	                savedAt = System.currentTimeMillis(),
	                isOffline = isOfflineMode || preservedIsOffline,
	                isSinglePoint = preservedIsSinglePoint,
	                waypoints = sessionWaypoints.toList()
	            )
	        )
	    }

	    private fun restoreCurrentWaypointDraft() {
	        if (isOfflineMode) return
	        val original = originalSessionWaypoint ?: return
	        if (currentIndex !in sessionWaypoints.indices) return
        draftSyncJob?.cancel()
        sessionWaypoints[currentIndex] = original.copy(
            basicData = HashMap(original.basicData)
        )
	        val resolvedDraftId = if (sessionDraftId > 0L) sessionDraftId else System.currentTimeMillis()
	        sessionDraftId = resolvedDraftId

	        val repo = GutterSessionRepository(this)
	        val existingDraft = repo.getById(resolvedDraftId)
	        val preservedIsOffline = existingDraft?.isOffline
	            ?: (isOfflineMode || intent.getBooleanExtra(EXTRA_SESSION_IS_OFFLINE, false))
	        val preservedIsSinglePoint = existingDraft?.isSinglePoint ?: isOfflineMode
	        repo.save(
	            GutterSessionDraft(
	                id = resolvedDraftId,
	                savedAt = System.currentTimeMillis(),
	                isOffline = isOfflineMode || preservedIsOffline,
	                isSinglePoint = preservedIsSinglePoint,
	                waypoints = sessionWaypoints.toList()
	            )
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
     * 將尚未上傳的本機照片（content:// / file:// scheme）依照 fileCategory 上傳至 nodeImage API。
     * 已是 https:// 的照片（API 已存在）略過不重複上傳。
     * 失敗時僅寫入 log，不中止流程。
     *
     * @param nodeId       點位 ID
     * @param token        Bearer token
     * @param photo1-3     照片 URI 字串（null 或空字串表示未拍攝）
     */
    private suspend fun uploadLocalPhotos(
        nodeId: Int,
        token: String,
        photo1: String?,
        photo2: String?,
        photo3: String?
    ) {
        listOf(photo1 to 1, photo2 to 2, photo3 to 3).forEach { (path, category) ->
            if (path.isNullOrEmpty()) return@forEach
            val scheme = Uri.parse(path).scheme?.lowercase() ?: return@forEach
            if (scheme == "http" || scheme == "https") return@forEach   // 已在伺服器，略過
            when (val result = gutterRepository.uploadNodeImage(
                context      = this,
                nodeId       = nodeId,
                fileCategory = category,
                imageUri     = Uri.parse(path),
                token        = token
            )) {
                is ApiResult.Error ->
                    android.util.Log.w("PhotoUpload", "photo$category 上傳失敗: ${result.message}")
                is ApiResult.Success -> { /* 上傳成功，API 會覆蓋舊圖 */ }
            }
        }
    }

    // ── 離線模式（儲存至本機草稿）────────────────────────────────────────

    /**
     * 將目前填寫內容儲存為 GutterSessionDraft（isOffline=true, isSinglePoint=true）。
     * @param silent true → 不驗證、不顯示 Toast、直接 finish（返回鍵自動存草稿）
     *               false → 先驗證所有必填欄位與三張照片，通過才存檔並關閉
     */
    private fun saveOfflineAndClose(silent: Boolean = false) {
        // 先取消任何排隊中的延遲 sync，避免 finish() 後仍觸發造成非預期寫入
        draftSyncJob?.cancel()
        if (!silent) {
            val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
            if (basicError != null) {
                binding.viewPager.currentItem = 0
                Toast.makeText(this, String.format(getString(R.string.msg_fill_required), basicError), Toast.LENGTH_SHORT).show()
                return
            }
            val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
            if (photoError != null) {
                binding.viewPager.currentItem = 1
                Toast.makeText(this, String.format(getString(R.string.msg_take_photo_required), photoError), Toast.LENGTH_SHORT).show()
                return
            }
        }
        syncSessionDraftNow()
        if (!silent) {
            Toast.makeText(this, getString(R.string.msg_draft_saved), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            isOfflineMode -> saveOfflineAndClose(silent = true) // 離線：離開表單一律存草稿
            !isViewMode   -> confirmOrDiscardAndClose()  // 編輯模式：資料不完整則彈窗確認
            else          -> super.onBackPressed()
        }
    }
}

