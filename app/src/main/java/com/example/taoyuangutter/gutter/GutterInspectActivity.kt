package com.example.taoyuangutter.gutter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.databinding.ActivityGutterInspectBinding
import com.google.gson.Gson

/**
 * GutterInspectActivity
 *
 * 唯讀檢視整條側溝（由 API getDitchDetails 取得）的彙整資料。
 * 以 FormSheet 半透明底部樣式浮現於地圖上。
 *
 * 包含兩個分頁：
 *   0 → 基本資料（[GutterInspectBasicFragment]）
 *   1 → 照片（[GutterInspectPhotosFragment]）
 *
 * 標題格式：「側溝編號 \n {SPI_NUM}」，後者字體較小。
 */
class GutterInspectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGutterInspectBinding
    private var ditch: DitchDetails? = null

    companion object {
        private const val EXTRA_DITCH_JSON = "ditch_json"

        /**
         * 建立開啟 GutterInspectActivity 的 Intent。
         * @param ditch getDitchDetails API 回傳的線段詳細資料
         */
        fun newIntent(context: Context, ditch: DitchDetails): Intent {
            val json = Gson().toJson(ditch)
            return Intent(context, GutterInspectActivity::class.java).apply {
                putExtra(EXTRA_DITCH_JSON, json)
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGutterInspectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyBottomSheetWindow()

        ditch = parseDitch()
        setupTitleBar(ditch)
        setupViewPager(ditch)
        setupTabButtons()

        // API 唯讀模式不開放編輯
        binding.btnEdit.visibility = View.GONE
    }

    // ── Window ──────────────────────────────────────────────────────────

    private fun applyBottomSheetWindow() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val screenH = resources.displayMetrics.heightPixels
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, screenH * 3 / 4)
        val attrs = window.attributes
        attrs.gravity = Gravity.BOTTOM
        window.attributes = attrs
    }

    // ── 資料解析 ─────────────────────────────────────────────────────────

    private fun parseDitch(): DitchDetails? {
        val json = intent.getStringExtra(EXTRA_DITCH_JSON) ?: return null
        return try {
            Gson().fromJson(json, DitchDetails::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ── 標題列 ───────────────────────────────────────────────────────────

    private fun setupTitleBar(ditch: DitchDetails?) {
        val spiNum   = ditch?.spiNum?.takeIf { it.isNotEmpty() } ?: "---"
        val line1    = "側溝編號"
        val fullText = "$line1\n$spiNum"

        val spannable = SpannableStringBuilder(fullText)
        // "側溝編號" 4字 + "\n" 1字 = 5，spiNum 從 index 5 開始
        spannable.setSpan(
            AbsoluteSizeSpan(16, true),
            5,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvInspectTitle.text = spannable
        binding.btnBack.setOnClickListener { finish() }
    }

    // ── ViewPager + 分頁 ─────────────────────────────────────────────────

    private fun setupViewPager(ditch: DitchDetails?) {
        val adapter = InspectPagerAdapter(this, ditch)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateTabUI(position)
            }
        )
    }

    private fun setupTabButtons() {
        binding.btnTabBasicInfo.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.btnTabPhotos.setOnClickListener    { binding.viewPager.currentItem = 1 }
        updateTabUI(0)
    }

    private fun updateTabUI(selected: Int) {
        val primary  = getColor(com.example.taoyuangutter.R.color.colorPrimary)
        val textGrey = getColor(com.example.taoyuangutter.R.color.text_grey)
        if (selected == 0) {
            binding.btnTabBasicInfo.setTextColor(primary)
            binding.btnTabPhotos.setTextColor(textGrey)
        } else {
            binding.btnTabBasicInfo.setTextColor(textGrey)
            binding.btnTabPhotos.setTextColor(primary)
        }
    }

    // ── 內部 PagerAdapter ────────────────────────────────────────────────

    private class InspectPagerAdapter(
        activity: FragmentActivity,
        private val ditch: DitchDetails?
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0    -> GutterInspectBasicFragment.newInstance(ditch)
            1    -> GutterInspectPhotosFragment.newInstance(ditch?.nodes ?: emptyList())
            else -> throw IllegalArgumentException("Unknown page $position")
        }
    }
}
