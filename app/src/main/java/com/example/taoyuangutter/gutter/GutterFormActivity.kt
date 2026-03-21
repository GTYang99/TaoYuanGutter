package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
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
        const val EXTRA_VIEW_MODE        = "view_mode"
        const val EXTRA_WAYPOINT_INDEX   = "waypoint_index"

        // 資料 Key
        const val EXTRA_DATA_GUTTER_ID   = "ex_gutterId"
        const val EXTRA_DATA_GUTTER_TYPE = "ex_gutterType"
        const val EXTRA_DATA_COORD_X     = "ex_coordX"
        const val EXTRA_DATA_COORD_Y     = "ex_coordY"
        const val EXTRA_DATA_COORD_Z     = "ex_coordZ"
        const val EXTRA_DATA_MEASURE_ID  = "ex_measureId"
        const val EXTRA_DATA_DEPTH       = "ex_depth"
        const val EXTRA_DATA_TOP_WIDTH   = "ex_topWidth"
        const val EXTRA_DATA_REMARKS     = "ex_remarks"
        const val EXTRA_DATA_PHOTO_1     = "ex_photo1"
        const val EXTRA_DATA_PHOTO_2     = "ex_photo2"
        const val EXTRA_DATA_PHOTO_3     = "ex_photo3"

        const val RESULT_LATITUDE        = "result_lat"
        const val RESULT_LONGITUDE       = "result_lng"
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
        const val RESULT_DATA_PHOTO_1     = "r_photo1"
        const val RESULT_DATA_PHOTO_2     = "r_photo2"
        const val RESULT_DATA_PHOTO_3     = "r_photo3"

        fun newIntent(
            context: Context,
            labels: ArrayList<String>,
            lats: DoubleArray,
            lngs: DoubleArray,
            index: Int = 0,
            basicData: HashMap<String, String>? = null
        ): Intent = Intent(context, GutterFormActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_WAYPOINT_LABELS, labels)
            putExtra(EXTRA_LATITUDES, lats)
            putExtra(EXTRA_LONGITUDES, lngs)
            putExtra(EXTRA_CURRENT_INDEX, index)
            putExtra(EXTRA_VIEW_MODE, false)
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
            fillDataExtras(this, basicData)
        }

        private fun fillDataExtras(intent: Intent, data: HashMap<String, String>) {
            intent.putExtra(EXTRA_DATA_GUTTER_ID,   data["gutterId"]   ?: "")
            intent.putExtra(EXTRA_DATA_GUTTER_TYPE, data["gutterType"] ?: "")
            intent.putExtra(EXTRA_DATA_COORD_X,     data["coordX"]     ?: "")
            intent.putExtra(EXTRA_DATA_COORD_Y,     data["coordY"]     ?: "")
            intent.putExtra(EXTRA_DATA_COORD_Z,     data["coordZ"]     ?: "")
            intent.putExtra(EXTRA_DATA_MEASURE_ID,  data["measureId"]  ?: "")
            intent.putExtra(EXTRA_DATA_DEPTH,       data["depth"]      ?: "")
            intent.putExtra(EXTRA_DATA_TOP_WIDTH,   data["topWidth"]   ?: "")
            intent.putExtra(EXTRA_DATA_REMARKS,     data["remarks"]    ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_1,     data["photo1"]     ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_2,     data["photo2"]     ?: "")
            intent.putExtra(EXTRA_DATA_PHOTO_3,     data["photo3"]     ?: "")
        }
    }

    private var waypointLabels = arrayListOf<String>()
    private var latitudes  = doubleArrayOf()
    private var longitudes = doubleArrayOf()
    private var currentIndex  = 0
    private var waypointIndex = 0
    private var isViewMode = false

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

        val label = waypointLabels.getOrElse(currentIndex) { "點位" }
        val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
        val lng   = longitudes.getOrElse(currentIndex) { 0.0 }

        // 儲存原始 GPS 座標，用於 result 回傳（不依賴表單欄位是否有值）
        currentLat = lat
        currentLng = lng

        val existingData = hashMapOf(
            "gutterId"   to (intent.getStringExtra(EXTRA_DATA_GUTTER_ID)   ?: ""),
            "gutterType" to (intent.getStringExtra(EXTRA_DATA_GUTTER_TYPE) ?: ""),
            "coordX"     to (intent.getStringExtra(EXTRA_DATA_COORD_X)     ?: ""),
            "coordY"     to (intent.getStringExtra(EXTRA_DATA_COORD_Y)     ?: ""),
            "coordZ"     to (intent.getStringExtra(EXTRA_DATA_COORD_Z)     ?: ""),
            "measureId"  to (intent.getStringExtra(EXTRA_DATA_MEASURE_ID)  ?: ""),
            "depth"      to (intent.getStringExtra(EXTRA_DATA_DEPTH)       ?: ""),
            "topWidth"   to (intent.getStringExtra(EXTRA_DATA_TOP_WIDTH)   ?: ""),
            "remarks"    to (intent.getStringExtra(EXTRA_DATA_REMARKS)     ?: ""),
            "photo1"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_1)     ?: ""),
            "photo2"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_2)     ?: ""),
            "photo3"     to (intent.getStringExtra(EXTRA_DATA_PHOTO_3)     ?: "")
        )

        applyBottomSheetWindow()
        setupTitleBar(label)
        setupViewPager(lat, lng, existingData)
        setupTabButtons()
        setupFab()
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
        binding.btnBack.setOnClickListener {
            // 新增模式：返回鍵儲存草稿（不驗證），避免資料遺失
            if (!isViewMode) saveDraftAndClose() else finish()
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
        binding.btnEdit.visibility   = View.GONE
        binding.btnDone.visibility   = View.VISIBLE
        binding.fabSubmit.visibility = View.GONE
        binding.btnDone.setOnClickListener { saveAndFinish() }
        pagerAdapter.getBasicInfoFragment()?.setEditable(true)
        pagerAdapter.getPhotosFragment()?.setEditable(true)
    }

    private fun saveAndFinish() {
        val data = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) =
            pagerAdapter.getPhotosFragment()?.getPhotoPaths() ?: Triple(null, null, null)
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
            putExtra(RESULT_DATA_PHOTO_1,     photo1             ?: "")
            putExtra(RESULT_DATA_PHOTO_2,     photo2             ?: "")
            putExtra(RESULT_DATA_PHOTO_3,     photo3             ?: "")
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun setupViewPager(lat: Double, lng: Double, basicData: HashMap<String, String>) {
        pagerAdapter = GutterFormPagerAdapter(this, lat, lng, isViewMode, basicData)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false
        // 確保兩個 Fragment 同時存在，saveAndClose() 才能同時讀取兩頁的資料
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateTabUI(position)
        })
    }

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
        } else {
            binding.btnTabBasicInfo.setBackgroundColor(white);   binding.btnTabBasicInfo.setTextColor(grey)
            binding.btnTabPhotos.setBackgroundColor(primary);    binding.btnTabPhotos.setTextColor(white)
        }
    }

    private fun setupFab() {
        binding.fabSubmit.setOnClickListener { saveAndClose() }
    }

    /**
     * FAB 按下：驗證所有必填欄位與三張照片，通過後才收集資料並關閉。
     * 驗證失敗時切換至對應 tab 並顯示提示，不關閉表單。
     */
    private fun saveAndClose() {
        // ① 驗證基本資料必填欄位
        val basicError = pagerAdapter.getBasicInfoFragment()?.validateRequiredFields()
        if (basicError != null) {
            binding.viewPager.currentItem = 0
            Toast.makeText(this, "請填寫「$basicError」", Toast.LENGTH_SHORT).show()
            return
        }

        // ② 驗證三張照片全部拍攝（暫時關閉，測試用）
        val photoError = pagerAdapter.getPhotosFragment()?.validateAllPhotos()
        if (photoError != null) {
            binding.viewPager.currentItem = 1
            Toast.makeText(this, "請拍攝「$photoError」照片", Toast.LENGTH_SHORT).show()
            return
        }

        // ③ 驗證通過 → 儲存並關閉
        buildAndFinishWithResult()
    }

    /**
     * 返回鍵／草稿儲存：不驗證，直接將目前填寫狀態存回並關閉。
     * 避免使用者中途返回時資料遺失。
     */
    private fun saveDraftAndClose() {
        buildAndFinishWithResult()
    }

    /** 收集基本資料與照片路徑，組成 RESULT_OK Intent 並 finish。 */
    private fun buildAndFinishWithResult() {
        val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
        val (photo1, photo2, photo3) =
            pagerAdapter.getPhotosFragment()?.getPhotoPaths() ?: Triple(null, null, null)

        // ── 決定地圖定位用的 lat/lng ────────────────────────────────────────
        // 優先採用表單欄位裡使用者確認過的 WGS84 座標值；
        // 若欄位空白或無法解析（例如使用者填入的是 TWD97 測量座標），
        // 則退回使用地圖選點時記錄的原始 GPS 座標，確保大頭針位置永遠有值。
        val formLat = basicData["coordY"]?.toDoubleOrNull()
        val formLng = basicData["coordX"]?.toDoubleOrNull()
        val effectiveLat = if (formLat != null && formLat in -90.0..90.0)   formLat else currentLat
        val effectiveLng = if (formLng != null && formLng in -180.0..180.0) formLng else currentLng

        val resultIntent = Intent().apply {
            // RESULT_LATITUDE / RESULT_LONGITUDE 永遠帶值，讓 MainActivity 能更新大頭針
            putExtra(RESULT_LATITUDE,         effectiveLat)
            putExtra(RESULT_LONGITUDE,        effectiveLng)
            putExtra(RESULT_WAYPOINT_INDEX,   waypointIndex)
            putExtra(RESULT_DATA_GUTTER_ID,   basicData["gutterId"]   ?: "")
            putExtra(RESULT_DATA_GUTTER_TYPE, basicData["gutterType"] ?: "")
            putExtra(RESULT_DATA_COORD_X,     basicData["coordX"]     ?: "")
            putExtra(RESULT_DATA_COORD_Y,     basicData["coordY"]     ?: "")
            putExtra(RESULT_DATA_COORD_Z,     basicData["coordZ"]     ?: "")
            putExtra(RESULT_DATA_MEASURE_ID,  basicData["measureId"]  ?: "")
            putExtra(RESULT_DATA_DEPTH,       basicData["depth"]      ?: "")
            putExtra(RESULT_DATA_TOP_WIDTH,   basicData["topWidth"]   ?: "")
            putExtra(RESULT_DATA_REMARKS,     basicData["remarks"]    ?: "")
            putExtra(RESULT_DATA_PHOTO_1,     photo1                  ?: "")
            putExtra(RESULT_DATA_PHOTO_2,     photo2                  ?: "")
            putExtra(RESULT_DATA_PHOTO_3,     photo3                  ?: "")
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 新增模式：系統返回鍵儲存草稿（不驗證）
        if (!isViewMode) saveDraftAndClose() else super.onBackPressed()
    }
}
