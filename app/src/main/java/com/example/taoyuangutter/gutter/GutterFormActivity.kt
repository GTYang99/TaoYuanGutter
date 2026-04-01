package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.databinding.ActivityGutterFormBinding
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.offline.OfflineDraft
import com.example.taoyuangutter.offline.OfflineDraftRepository
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GutterFormActivity : AppCompatActivity() {

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

        /** 離線模式旗標：不向 MainActivity 回傳 result，改儲存至本機草稿。 */
        const val EXTRA_OFFLINE_MODE     = "offline_mode"
        /** 離線模式：若要開啟既有草稿，帶入此 ID（-1 表示新增）。 */
        const val EXTRA_OFFLINE_DRAFT_ID = "offline_draft_id"

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
            sessionWaypointsJson: String? = null
        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, labels)
            putExtra(EXTRA_LATITUDES, lats)
            putExtra(EXTRA_LONGITUDES, lngs)
            putExtra(EXTRA_CURRENT_INDEX, index)
            putExtra(EXTRA_WAYPOINT_INDEX, index)   // 與 currentIndex 一致，確保 buildAndFinishWithResult 回傳正確索引
            putExtra(EXTRA_VIEW_MODE, false)
            putExtra(EXTRA_IS_EDIT_MODE, isEditMode) // 傳入編輯模式旗標
            putExtra(EXTRA_SESSION_DRAFT_ID, sessionDraftId)
            if (!sessionWaypointsJson.isNullOrEmpty()) {
                putExtra(EXTRA_SESSION_WAYPOINTS_JSON, sessionWaypointsJson)
            }
            basicData?.let { fillDataExtras(this, it) }
        }

        fun newViewIntent(
            context: Context,
            label: String,
            lat: Double,
            lng: Double,
            waypointIndex: Int,
            basicData: HashMap<String, String>
        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, arrayListOf(label))
            putExtra(EXTRA_LATITUDES, doubleArrayOf(lat))
            putExtra(EXTRA_LONGITUDES, doubleArrayOf(lng))
            putExtra(EXTRA_CURRENT_INDEX, 0)
            putExtra(EXTRA_VIEW_MODE, true)
            putExtra(EXTRA_WAYPOINT_INDEX, waypointIndex)
            putExtra(EXTRA_IS_EDIT_MODE, true) // 編輯模式為 true
            fillDataExtras(this, basicData)
        }

        /**
         * 離線模式：不需地圖點位，座標固定 (0.0, 0.0)。
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
                putExtra(EXTRA_OFFLINE_DRAFT_ID, draftId)
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
    /** 正在編輯的草稿 ID（-1L 表示新增）*/
    private var offlineDraftId = -1L
    /** 地圖流程中的 session 草稿 ID；0 表示尚未建立。 */
    private var sessionDraftId = 0L
    /** 整條側溝目前的 waypoint 快照，供表單編輯中即時覆寫草稿。 */
    private val sessionWaypoints = mutableListOf<WaypointSnapshot>()
    private var draftSyncJob: Job? = null
    private var originalSessionWaypoint: WaypointSnapshot? = null

    /** 編輯模式：API 的 node_id（有值時儲存才會上傳照片） */
    private var nodeId: Int? = null
    private val gutterRepository = GutterRepository()

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
        offlineDraftId = intent.getLongExtra(EXTRA_OFFLINE_DRAFT_ID, -1L)
        sessionDraftId = intent.getLongExtra(EXTRA_SESSION_DRAFT_ID, 0L)
        restoreSessionWaypoints()
        originalSessionWaypoint = sessionWaypoints.getOrNull(currentIndex)?.copy(
            basicData = HashMap(sessionWaypoints.getOrNull(currentIndex)?.basicData ?: hashMapOf())
        )

        val label = waypointLabels.getOrElse(currentIndex) { "點位" }
        val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
        val lng   = longitudes.getOrElse(currentIndex) { 0.0 }

        currentLat = lat
        currentLng = lng

        // 離線模式：若有既有草稿 ID，從本機讀取資料
        val existingData: HashMap<String, String> = if (isOfflineMode && offlineDraftId >= 0) {
            val draft = OfflineDraftRepository(this).getById(offlineDraftId)
            draft?.toBasicData() ?: buildEmptyData(lat, lng)
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
                "NODE_NOTE"  to (intent.getStringExtra(EXTRA_DATA_REMARKS)     ?: ""),
                "photo1"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_1)     ?: ""),
                "photo2"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_2)     ?: ""),
                "photo3"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_3)     ?: ""),
                "XY_NUM"     to (intent.getStringExtra(EXTRA_DATA_XY_NUM)      ?: "")
            )
        }

        // 離線模式直接全螢幕；一般模式保留底部 sheet 3/4 視窗樣式
        if (!isOfflineMode) {
            applyBottomSheetWindow()
        } else {
            applyFullScreenInsets()
        }

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
        setupFab()
        binding.viewPager.post { attachDraftSyncCallbacks() }
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
     * 離線全螢幕模式：讓 Activity 畫到系統列後方，
     * 再透過 WindowInsets 給 AppBarLayout 加上 statusBar 高度的 paddingTop，
     * 並給 FAB 加上 navigationBar 高度的 bottomMargin，確保切頁按鈕與 FAB 都在 safe area 內。
     */
    private fun applyFullScreenInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 頂部 safe area：AppBarLayout（含標題列 + 切頁按鈕列）整體下移
            binding.appBarLayout.setPadding(0, bars.top, 0, 0)
            // 底部 safe area：FAB 離 nav bar 保持 24dp 間距
            val fabParams = binding.fabSubmit.layoutParams as CoordinatorLayout.LayoutParams
            fabParams.bottomMargin = (24 * resources.displayMetrics.density).toInt() + bars.bottom
            binding.fabSubmit.layoutParams = fabParams
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun applyBottomSheetWindow() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val screenH = resources.displayMetrics.heightPixels
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, screenH * 3 / 4)
        val attrs = window.attributes
        attrs.gravity = Gravity.BOTTOM
        window.attributes = attrs
    }

    private fun setupTitleBar(label: String) {
        binding.tvFormTitle.text = label

        if (isOfflineMode) {
            // 離線模式：左上角改為 × 取消按鈕，直接 finish 不存草稿
            binding.btnBack.setImageResource(com.example.taoyuangutter.R.drawable.ic_close)
            binding.btnBack.contentDescription = "取消"
            binding.btnBack.setOnClickListener { finish() }
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
        binding.btnTabBasicInfo.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnTabPhotos.setOnClickListener    { binding.viewPager.currentItem = 1 }
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        attachDraftSyncCallbacks()
        val primary   = getColor(com.example.taoyuangutter.R.color.colorPrimary)
        val secondary = getColor(com.example.taoyuangutter.R.color.textColorSecondary)
        val white     = getColor(com.example.taoyuangutter.R.color.white)
        if (selected == 0) {
            binding.btnTabBasicInfo.setBackgroundColor(white); binding.btnTabBasicInfo.setTextColor(primary)
            binding.btnTabPhotos.setBackgroundColor(white);    binding.btnTabPhotos.setTextColor(secondary)
        }
        if (selected == 1) {
            binding.btnTabBasicInfo.setBackgroundColor(white); binding.btnTabBasicInfo.setTextColor(secondary)
            binding.btnTabPhotos.setBackgroundColor(white);    binding.btnTabPhotos.setTextColor(primary)
        }
    }

    private fun setupFab() {
        // 編輯模式：FAB 文字改為「完成」
        if (isEditMode) {
            binding.fabSubmit.text = "完成"
        }
        binding.fabSubmit.setOnClickListener {
            when {
                isOfflineMode -> saveOfflineAndClose()
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
            Toast.makeText(this, "請填寫「$basicError」", Toast.LENGTH_SHORT).show()
            return
        }
        val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
        if (photoError != null) {
            binding.viewPager.currentItem = 1
            Toast.makeText(this, "請拍攝「$photoError」照片", Toast.LENGTH_SHORT).show()
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
        if (isOfflineMode) return

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
        if (isOfflineMode || isViewMode) return
        if (currentIndex !in sessionWaypoints.indices) return
        draftSyncJob?.cancel()
        draftSyncJob = lifecycleScope.launch {
            delay(400)
            syncSessionDraftNow()
        }
    }

    private fun syncSessionDraftNow() {
        if (isOfflineMode || isViewMode) return
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

        val resolvedDraftId = if (sessionDraftId > 0L) sessionDraftId else System.currentTimeMillis()
        sessionDraftId = resolvedDraftId
        GutterSessionRepository(this).save(
            GutterSessionDraft(
                id = resolvedDraftId,
                savedAt = System.currentTimeMillis(),
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
        GutterSessionRepository(this).save(
            GutterSessionDraft(
                id = resolvedDraftId,
                savedAt = System.currentTimeMillis(),
                waypoints = sessionWaypoints.toList()
            )
        )
    }

    // ── 上傳等待遮罩 ─────────────────────────────────────────────────────

    /**
     * 顯示或隱藏上傳照片的等待遮罩。
     * 遮罩期間阻擋所有使用者操作；上傳完成後隱藏並繼續 finish 流程。
     */
    private fun showUploadLoading(show: Boolean) {
        binding.uploadLoadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
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
     * 將目前填寫內容儲存為本機草稿。
     * @param silent true → 不驗證、不顯示 Toast、直接 finish（返回鍵自動存草稿）
     *               false → 先驗證所有必填欄位與三張照片，通過才存檔並關閉
     */
    private fun saveOfflineAndClose(silent: Boolean = false) {
        if (!silent) {
            // 離線提交：同正式流程，所有必填欄位與照片都需填寫才能儲存
            val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
            if (basicError != null) {
                binding.viewPager.currentItem = 0
                Toast.makeText(this, "請填寫「$basicError」", Toast.LENGTH_SHORT).show()
                return
            }
            val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
            if (photoError != null) {
                binding.viewPager.currentItem = 1
                Toast.makeText(this, "請拍攝「$photoError」照片", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) =
            pagerAdapter.getPhotosFragment()?.getPhotoPaths() ?: Triple(null, null, null)

        val draft = OfflineDraft(
            id          = if (offlineDraftId >= 0) offlineDraftId else System.currentTimeMillis(),
            savedAt     = System.currentTimeMillis(),
            gutterId    = basicData["SPI_NUM"]    ?: "",
            gutterType  = basicData["NODE_TYP"]   ?: "",
            matTyp      = basicData["MAT_TYP"]    ?: "",
            coordX      = basicData["NODE_X"]     ?: "",
            coordY      = basicData["NODE_Y"]     ?: "",
            coordZ      = basicData["NODE_LE"]    ?: "",
            xyNum       = basicData["XY_NUM"]     ?: "",
            depth       = basicData["NODE_DEP"]   ?: "",
            topWidth    = basicData["NODE_WID"]   ?: "",
            isBroken    = basicData["IS_BROKEN"]  ?: "",
            isHanging   = basicData["IS_HANGING"] ?: "",
            isSilt      = basicData["IS_SILT"]    ?: "",
            remarks     = basicData["NODE_NOTE"]  ?: "",
            photo1      = photo1                  ?: "",
            photo2      = photo2                  ?: "",
            photo3      = photo3                  ?: ""
        )

        OfflineDraftRepository(this).save(draft)

        if (!silent) {
            Toast.makeText(this, "草稿已儲存至本機", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            isOfflineMode -> finish()                    // 離線取消：不存草稿
            !isViewMode   -> confirmOrDiscardAndClose()  // 編輯模式：資料不完整則彈窗確認
            else          -> super.onBackPressed()
        }
    }
}

// ── OfflineDraft 擴充函式 ─────────────────────────────────────────────────

/** 將 OfflineDraft 轉為 GutterFormActivity 可直接使用的 basicData HashMap。 */
private fun OfflineDraft.toBasicData(): HashMap<String, String> = hashMapOf(
    "SPI_NUM"    to gutterId,
    "NODE_TYP"   to gutterType,
    "MAT_TYP"    to matTyp,
    "NODE_X"     to coordX,
    "NODE_Y"     to coordY,
    "NODE_LE"    to coordZ,
    "XY_NUM"     to xyNum,
    "NODE_DEP"   to depth,
    "NODE_WID"   to topWidth,
    "IS_BROKEN"  to isBroken,
    "IS_HANGING" to isHanging,
    "IS_SILT"    to isSilt,
    "NODE_NOTE"  to remarks,
    "photo1"     to photo1,
    "photo2"     to photo2,
    "photo3"     to photo3
)
