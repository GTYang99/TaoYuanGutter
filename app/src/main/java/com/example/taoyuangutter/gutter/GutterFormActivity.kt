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
        }
    }

    private var waypointLabels = arrayListOf<String>()
    private var latitudes  = doubleArrayOf()
    private var longitudes = doubleArrayOf()
    private var currentIndex  = 0
    private var waypointIndex = 0
    private var isViewMode = false

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
        binding.btnBack.setOnClickListener { finish() }

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

    private fun setupViewPager(lat: Double, lng: Double, basicData: HashMap<String, String>) {
        pagerAdapter = GutterFormPagerAdapter(this, lat, lng, isViewMode, basicData)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false
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
        binding.fabSubmit.setOnClickListener {
            val basicData = pagerAdapter.getBasicInfoFragment()?.collectData() ?: emptyMap()
            val parsedLat = basicData["coordY"]?.toDoubleOrNull()
            val parsedLng = basicData["coordX"]?.toDoubleOrNull()
            
            val resultIntent = Intent().apply {
                if (parsedLat != null && parsedLng != null) {
                    putExtra(RESULT_LATITUDE,  parsedLat)
                    putExtra(RESULT_LONGITUDE, parsedLng)
                }
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
            // 儲存目前點位資料並回到地圖
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
