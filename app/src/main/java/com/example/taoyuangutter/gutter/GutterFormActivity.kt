package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.taoyuangutter.databinding.ActivityGutterFormBinding

class GutterFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGutterFormBinding
    private lateinit var pagerAdapter: GutterFormPagerAdapter

    companion object {
        const val EXTRA_WAYPOINT_LABELS  = "waypoint_labels"
        const val EXTRA_LATITUDES        = "latitudes"
        const val EXTRA_LONGITUDES       = "longitudes"
        const val EXTRA_CURRENT_INDEX    = "current_index"

        /** 是否以「檢視模式」開啟（true = 唯讀，可切換編輯） */
        const val EXTRA_VIEW_MODE        = "view_mode"
        /** 當前 waypoint 在線段 waypoints 陣列中的索引（供回傳更新用） */
        const val EXTRA_WAYPOINT_INDEX   = "waypoint_index"
        /** 既有 basicData（逐一 key 傳入，與 GutterBasicInfoFragment.ARG_DATA_* 一致） */
        const val EXTRA_DATA_GUTTER_ID   = "ex_gutterId"
        const val EXTRA_DATA_GUTTER_TYPE = "ex_gutterType"
        const val EXTRA_DATA_COORD_X     = "ex_coordX"
        const val EXTRA_DATA_COORD_Y     = "ex_coordY"
        const val EXTRA_DATA_COORD_Z     = "ex_coordZ"
        const val EXTRA_DATA_MEASURE_ID  = "ex_measureId"
        const val EXTRA_DATA_DEPTH       = "ex_depth"
        const val EXTRA_DATA_TOP_WIDTH   = "ex_topWidth"
        const val EXTRA_DATA_REMARKS     = "ex_remarks"

        /** 回傳 result intent 的 key：更新後的座標（供 MainActivity 移動大頭針） */
        const val RESULT_LATITUDE        = "result_lat"
        const val RESULT_LONGITUDE       = "result_lng"

        /** 回傳 result intent 的 key：儲存的 basicData */
        const val RESULT_WAYPOINT_INDEX  = "result_wp_index"
        const val RESULT_DATA_GUTTER_ID   = "r_gutterId"
        const val RESULT_DATA_GUTTER_TYPE = "r_gutterType"
        const val RESULT_DATA_COORD_X     = "r_coordX"
        const val RESULT_DATA_COORD_Y     = "r_coordY"
        const val RESULT_DATA_COORD_Z     = "r_coordZ"
        const val RESULT_DATA_MEASURE_ID  = "r_measureId"
        const val RESULT_DATA_DEPTH       = "r_depth"
        const val RESULT_DATA_TOP_WIDTH   = "r_topWidth"
        const val RESULT_DATA_REMARKS     = "r_remarks"

        /**
         * 一般新增模式（從 BottomSheet 選點流程進入）
         */
        fun newIntent(
            context: Context,
            labels: ArrayList<String>,
            lats: DoubleArray,
            lngs: DoubleArray,
            index: Int = 0
        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, labels)
            putExtra(EXTRA_LATITUDES, lats)
            putExtra(EXTRA_LONGITUDES, lngs)
            putExtra(EXTRA_CURRENT_INDEX, index)
            putExtra(EXTRA_VIEW_MODE, false)
        }

        /**
         * 檢視/編輯模式（從 Polyline 點擊→BottomSheet 點選點位進入）
         */
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
            // basicData 逐一傳入
            putExtra(EXTRA_DATA_GUTTER_ID,   basicData["gutterId"]   ?: "")
            putExtra(EXTRA_DATA_GUTTER_TYPE, basicData["gutterType"] ?: "")
            putExtra(EXTRA_DATA_COORD_X,     basicData["coordX"]     ?: "")
            putExtra(EXTRA_DATA_COORD_Y,     basicData["coordY"]     ?: "")
            putExtra(EXTRA_DATA_COORD_Z,     basicData["coordZ"]     ?: "")
            putExtra(EXTRA_DATA_MEASURE_ID,  basicData["measureId"]  ?: "")
            putExtra(EXTRA_DATA_DEPTH,       basicData["depth"]      ?: "")
            putExtra(EXTRA_DATA_TOP_WIDTH,   basicData["topWidth"]   ?: "")
            putExtra(EXTRA_DATA_REMARKS,     basicData["remarks"]    ?: "")
        }
    }

    // ── 資料 ─────────────────────────────────────────────────────────────
    private var waypointLabels = arrayListOf<String>()
    private var latitudes  = doubleArrayOf()
    private var longitudes = doubleArrayOf()
    private var currentIndex  = 0
    private var waypointIndex = 0   // 在 polyline waypoints 中的索引

    /** true = 目前為唯讀檢視模式 */
    private var isViewMode = false

    // ── Lifecycle ────────────────────────────────────────────────────────
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

        val label = waypointLabels.getOrElse(currentIndex) { "點位" }
        val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
        val lng   = longitudes.getOrElse(currentIndex) { 0.0 }

        // 從 intent 讀取既有 basicData
        val existingData = hashMapOf(
            "gutterId"   to (intent.getStringExtra(EXTRA_DATA_GUTTER_ID)   ?: ""),
            "gutterType" to (intent.getStringExtra(EXTRA_DATA_GUTTER_TYPE) ?: ""),
            "coordX"     to (intent.getStringExtra(EXTRA_DATA_COORD_X)     ?: ""),
            "coordY"     to (intent.getStringExtra(EXTRA_DATA_COORD_Y)     ?: ""),
            "coordZ"     to (intent.getStringExtra(EXTRA_DATA_COORD_Z)     ?: ""),
            "measureId"  to (intent.getStringExtra(EXTRA_DATA_MEASURE_ID)  ?: ""),
            "depth"      to (intent.getStringExtra(EXTRA_DATA_DEPTH)       ?: ""),
            "topWidth"   to (intent.getStringExtra(EXTRA_DATA_TOP_WIDTH)   ?: ""),
            "remarks"    to (intent.getStringExtra(EXTRA_DATA_REMARKS)     ?: "")
        )

        applyBottomSheetWindow()
        setupTitleBar(label)
        setupViewPager(lat, lng, existingData)
        setupTabButtons()
        setupFab()
    }

    // ── 視窗定位：下方 3/4 + 去除背景模糊 ────────────────────────────────
    private fun applyBottomSheetWindow() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val screenH = resources.displayMetrics.heightPixels
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, screenH * 3 / 4)
        val attrs = window.attributes
        attrs.gravity = Gravity.BOTTOM
        window.attributes = attrs
    }

    // ── 標題列 ────────────────────────────────────────────────────────────
    private fun setupTitleBar(label: String) {
        binding.tvFormTitle.text = label
        applyTitleBarMode()
    }

    /**
     * 初始化標題列按鈕（onCreate 時依模式設定）。
     * - 返回箭頭：永遠在左側，點選回上一頁
     * - 右側：檢視模式顯示鉛筆，編輯模式顯示「完成」，新增模式右側空白
     */
    private fun applyTitleBarMode() {
        // 返回箭頭永遠可見
        binding.btnBack.setOnClickListener { finish() }

        if (isViewMode) {
            // 右側顯示鉛筆 icon，FAB 隱藏
            binding.btnEdit.visibility   = View.VISIBLE
            binding.btnDone.visibility   = View.GONE
            binding.fabSubmit.visibility = View.GONE
            binding.btnEdit.setOnClickListener { enterEditMode() }
        } else {
            // 一般新增模式：右側空白，FAB 顯示
            binding.btnEdit.visibility   = View.GONE
            binding.btnDone.visibility   = View.GONE
            binding.fabSubmit.visibility = View.VISIBLE
        }
    }

    /** 切換至編輯模式（右側換成「完成」按鈕） */
    private fun enterEditMode() {
        isViewMode = false

        // 右側換成「完成」
        binding.btnEdit.visibility   = View.GONE
        binding.btnDone.visibility   = View.VISIBLE
        binding.fabSubmit.visibility = View.GONE
        binding.btnDone.setOnClickListener { saveAndFinish() }

        // 通知兩個 fragment 切換可編輯
        pagerAdapter.getBasicInfoFragment()?.setEditable(true)
        pagerAdapter.getPhotosFragment()?.setEditable(true)
    }

    /** 完成編輯：收集資料，回傳結果並關閉 */
    private fun saveAndFinish() {
        val basicFrag = pagerAdapter.getBasicInfoFragment()
        val data = basicFrag?.collectData() ?: emptyMap()

        val resultIntent = Intent().apply {
            putExtra(RESULT_WAYPOINT_INDEX,   waypointIndex)
            putExtra(RESULT_DATA_GUTTER_ID,   data["gutterId"]   ?: "")
            putExtra(RESULT_DATA_GUTTER_TYPE, data["gutterType"] ?: "")
            putExtra(RESULT_DATA_COORD_X,     data["coordX"]     ?: "")
            putExtra(RESULT_DATA_COORD_Y,     data["coordY"]     ?: "")
            putExtra(RESULT_DATA_COORD_Z,     data["coordZ"]     ?: "")
            putExtra(RESULT_DATA_MEASURE_ID,  data["measureId"]  ?: "")
            putExtra(RESULT_DATA_DEPTH,       data["depth"]      ?: "")
            putExtra(RESULT_DATA_TOP_WIDTH,   data["topWidth"]   ?: "")
            putExtra(RESULT_DATA_REMARKS,     data["remarks"]    ?: "")
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    // ── ViewPager2 ────────────────────────────────────────────────────────
    private fun setupViewPager(lat: Double, lng: Double, basicData: HashMap<String, String>) {
        pagerAdapter = GutterFormPagerAdapter(this, lat, lng, isViewMode, basicData)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateTabUI(position)
            }
        )
    }

    // ── 分頁按鈕 ──────────────────────────────────────────────────────────
    private fun setupTabButtons() {
        binding.btnTabBasicInfo.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnTabPhotos.setOnClickListener    { binding.viewPager.currentItem = 1 }
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        val primary = getColor(com.example.taoyuangutter.R.color.colorPrimary)
        val white   = getColor(com.example.taoyuangutter.R.color.white)
        val grey    = getColor(com.example.taoyuangutter.R.color.inputFieldHint)

        if (selected == 0) {
            binding.btnTabBasicInfo.setBackgroundColor(primary); binding.btnTabBasicInfo.setTextColor(white)
            binding.btnTabPhotos.setBackgroundColor(white);      binding.btnTabPhotos.setTextColor(grey)
            binding.tabIndicator.layoutParams = (binding.tabIndicator.layoutParams).also {
                it.width = binding.tabBar.width / 2
            }
        } else {
            binding.btnTabBasicInfo.setBackgroundColor(white);   binding.btnTabBasicInfo.setTextColor(grey)
            binding.btnTabPhotos.setBackgroundColor(primary);    binding.btnTabPhotos.setTextColor(white)
        }
    }

    // ── 新增側溝 FAB（一般新增模式用） ───────────────────────────────────
    private fun setupFab() {
        binding.fabSubmit.setOnClickListener {
            val basicFrag = pagerAdapter.getBasicInfoFragment()
            val photoFrag = pagerAdapter.getPhotosFragment()
            val basicData = basicFrag?.collectData() ?: emptyMap()
            val photos    = photoFrag?.getPhotoUris()
            // TODO: 將 basicData + photos 送至後端或本機資料庫

            // 從表單的 X(經度)/Y(緯度) 欄位解析最新座標，回傳給 MainActivity 更新大頭針
            // coordX = 經度(lng)，coordY = 緯度(lat)（見 prefillCoordinates 的命名慣例）
            val parsedLat = basicData["coordY"]?.toDoubleOrNull()
            val parsedLng = basicData["coordX"]?.toDoubleOrNull()
            val resultIntent = android.content.Intent().apply {
                if (parsedLat != null && parsedLng != null) {
                    putExtra(RESULT_LATITUDE,  parsedLat)
                    putExtra(RESULT_LONGITUDE, parsedLng)
                }
                // 同時回傳所有表單資料，供 MainActivity 存回 waypoint.basicData
                putExtra(RESULT_DATA_GUTTER_ID,   basicData["gutterId"]   ?: "")
                putExtra(RESULT_DATA_GUTTER_TYPE, basicData["gutterType"] ?: "")
                putExtra(RESULT_DATA_COORD_X,     basicData["coordX"]     ?: "")
                putExtra(RESULT_DATA_COORD_Y,     basicData["coordY"]     ?: "")
                putExtra(RESULT_DATA_COORD_Z,     basicData["coordZ"]     ?: "")
                putExtra(RESULT_DATA_MEASURE_ID,  basicData["measureId"]  ?: "")
                putExtra(RESULT_DATA_DEPTH,       basicData["depth"]      ?: "")
                putExtra(RESULT_DATA_TOP_WIDTH,   basicData["topWidth"]   ?: "")
                putExtra(RESULT_DATA_REMARKS,     basicData["remarks"]    ?: "")
            }
            setResult(Activity.RESULT_OK, resultIntent)

            val nextIndex = currentIndex + 1
            if (nextIndex < waypointLabels.size) {
                startActivity(newIntent(this, waypointLabels, latitudes, longitudes, nextIndex))
            } else {
                finish()
            }
        }
    }
}
