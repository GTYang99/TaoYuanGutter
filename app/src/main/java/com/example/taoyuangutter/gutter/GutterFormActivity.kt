package com.example.taoyuangutter.gutter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityGutterFormBinding

class GutterFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGutterFormBinding

    companion object {
        const val EXTRA_WAYPOINT_LABEL = "waypoint_label"
        const val EXTRA_LATITUDE       = "latitude"
        const val EXTRA_LONGITUDE      = "longitude"
        // 後續多點順序流程用
        const val EXTRA_WAYPOINT_LABELS = "waypoint_labels"
        const val EXTRA_LATITUDES       = "latitudes"
        const val EXTRA_LONGITUDES      = "longitudes"
        const val EXTRA_CURRENT_INDEX   = "current_index"

        /**
         * 開啟單一 waypoint 表單（起點 / 節點 / 終點）
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
        }
    }

    // ── 資料 ─────────────────────────────────────────────────────────────
    private var waypointLabels = arrayListOf<String>()
    private var latitudes  = doubleArrayOf()
    private var longitudes = doubleArrayOf()
    private var currentIndex = 0

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGutterFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        waypointLabels = intent.getStringArrayListExtra(EXTRA_WAYPOINT_LABELS) ?: arrayListOf()
        latitudes  = intent.getDoubleArrayExtra(EXTRA_LATITUDES)  ?: doubleArrayOf()
        longitudes = intent.getDoubleArrayExtra(EXTRA_LONGITUDES) ?: doubleArrayOf()
        currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)

        val label = waypointLabels.getOrElse(currentIndex) { "點位" }
        val lat   = latitudes.getOrElse(currentIndex)  { 0.0 }
        val lng   = longitudes.getOrElse(currentIndex) { 0.0 }

        // ── 將視窗定位至螢幕下方 3/4 ──────────────────────────────
        applyBottomSheetWindow()

        setupTitleBar(label)
        setupViewPager(lat, lng)
        setupTabButtons()
        setupFab()
    }

    // ── 視窗定位：下方 3/4 + 去除背景模糊 ────────────────────────────────
    private fun applyBottomSheetWindow() {
        // 不需要 dim（地圖需完整可見）
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        // 高度 = 螢幕高度 × 3/4，錨定螢幕底部
        val screenH = resources.displayMetrics.heightPixels
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, screenH * 3 / 4)
        val attrs = window.attributes
        attrs.gravity = Gravity.BOTTOM
        window.attributes = attrs
    }

    // ── 標題列 ────────────────────────────────────────────────────────────
    private fun setupTitleBar(label: String) {
        binding.tvFormTitle.text = label
        binding.btnBack.setOnClickListener { finish() }
    }

    // ── ViewPager2 ────────────────────────────────────────────────────────
    private fun setupViewPager(lat: Double, lng: Double) {
        binding.viewPager.adapter = GutterFormPagerAdapter(this, lat, lng)
        binding.viewPager.isUserInputEnabled = false   // 禁止左右滑動，只允許按鈕切換
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateTabUI(position)
        })
    }

    // ── 分頁按鈕 ──────────────────────────────────────────────────────────
    private fun setupTabButtons() {
        binding.btnTabBasicInfo.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnTabPhotos.setOnClickListener    { binding.viewPager.currentItem = 1 }
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        val primary = getColor(R.color.colorPrimary)
        val white   = getColor(R.color.white)
        val grey    = getColor(R.color.inputFieldHint)

        if (selected == 0) {
            binding.btnTabBasicInfo.setBackgroundColor(primary); binding.btnTabBasicInfo.setTextColor(white)
            binding.btnTabPhotos.setBackgroundColor(white);      binding.btnTabPhotos.setTextColor(grey)
            // 指示條移至左半
            binding.tabIndicator.layoutParams = (binding.tabIndicator.layoutParams).also {
                it.width = binding.tabBar.width / 2
            }
        } else {
            binding.btnTabBasicInfo.setBackgroundColor(white);   binding.btnTabBasicInfo.setTextColor(grey)
            binding.btnTabPhotos.setBackgroundColor(primary);    binding.btnTabPhotos.setTextColor(white)
        }
    }

    // ── 新增側溝 FAB ──────────────────────────────────────────────────────
    private fun setupFab() {
        binding.fabSubmit.setOnClickListener {
            // 取得基本資料
            val basicFrag = supportFragmentManager.fragments
                .filterIsInstance<GutterBasicInfoFragment>()
                .firstOrNull()
            val photoFrag = supportFragmentManager.fragments
                .filterIsInstance<GutterPhotosFragment>()
                .firstOrNull()

            val basicData = basicFrag?.collectData() ?: emptyMap()
            val photos    = photoFrag?.getPhotoUris()

            // TODO: 將 basicData + photos 送至後端或本機資料庫

            // 若還有下一個 waypoint，繼續開啟下一張表單
            val nextIndex = currentIndex + 1
            if (nextIndex < waypointLabels.size) {
                startActivity(
                    newIntent(this, waypointLabels, latitudes, longitudes, nextIndex)
                )
                // 不 finish()，讓使用者按返回可回到上一個點位的表單
            } else {
                // 全部點位填完，回到地圖
                finish()
            }
        }
    }
}
