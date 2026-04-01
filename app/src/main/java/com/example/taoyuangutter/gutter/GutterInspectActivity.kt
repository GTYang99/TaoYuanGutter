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

import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.model.LatLng
import com.google.gson.reflect.TypeToken

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
    private var wgsLatitudes: DoubleArray = doubleArrayOf()
    private var wgsLongitudes: DoubleArray = doubleArrayOf()

    companion object {
        private const val EXTRA_DITCH_JSON        = "ditch_json"
        private const val EXTRA_CAN_EDIT          = "can_edit"
        private const val EXTRA_LATITUDES         = "latitudes"
        private const val EXTRA_LONGITUDES        = "longitudes"

        /** setResult code：使用者點擊編輯按鈕，要求 MainActivity 開啟 AddGutterBottomSheet */
        const val RESULT_EDIT_DITCH               = android.app.Activity.RESULT_FIRST_USER + 10
        /** result Intent 攜帶的 WaypointSnapshot 列表 JSON */
        const val EXTRA_RESULT_WAYPOINTS_JSON     = "result_waypoints_json"
        /** result Intent 攜帶的 SPI_NUM */
        const val EXTRA_RESULT_SPI_NUM            = "result_spi_num"

        /**
         * 建立開啟 GutterInspectActivity 的 Intent。
         * @param ditch   getDitchDetails API 回傳的線段詳細資料
         * @param canEdit 登入者 group_id 與線段 group_id 一致時為 true，顯示編輯按鈕
         * @param latitudes  線段所有點位的 WGS84 緯度（依起點→終點排序）
         * @param longitudes 線段所有點位的 WGS84 經度（依起點→終點排序）
         */
        fun newIntent(
            context: Context,
            ditch: DitchDetails,
            canEdit: Boolean = false,
            latitudes: DoubleArray = doubleArrayOf(),
            longitudes: DoubleArray = doubleArrayOf()
        ): Intent {
            val json = Gson().toJson(ditch)
            return Intent(context, GutterInspectActivity::class.java).apply {
                putExtra(EXTRA_DITCH_JSON, json)
                putExtra(EXTRA_CAN_EDIT,   canEdit)
                putExtra(EXTRA_LATITUDES,  latitudes)
                putExtra(EXTRA_LONGITUDES, longitudes)
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
        wgsLatitudes  = intent.getDoubleArrayExtra(EXTRA_LATITUDES)  ?: doubleArrayOf()
        wgsLongitudes = intent.getDoubleArrayExtra(EXTRA_LONGITUDES) ?: doubleArrayOf()

        setupTitleBar(ditch)
        setupViewPager(ditch)
        setupTabButtons()

        // group_id 一致才顯示編輯按鈕並掛載點擊事件
        val canEdit = intent.getBooleanExtra(EXTRA_CAN_EDIT, false)
        if (canEdit) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener { openEditForm() }
        } else {
            binding.btnEdit.visibility = View.GONE
        }
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

    // ── 編輯 ─────────────────────────────────────────────────────────────

    /**
     * 通知 MainActivity 開啟 AddGutterBottomSheet 進行編輯。
     * 將 DitchDetails 轉換為 Waypoint 列表快照後，以 setResult 傳回並關閉自身。
     */
    private fun openEditForm() {
        val d = ditch ?: return
        val waypoints = ditchToWaypoints(d)
        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type      = wp.type.name,
                label     = wp.label,
                latitude  = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = wp.basicData
            )
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_RESULT_WAYPOINTS_JSON, Gson().toJson(snapshots))
            putExtra(EXTRA_RESULT_SPI_NUM, d.spiNum)
        }
        setResult(RESULT_EDIT_DITCH, resultIntent)
        finish()
    }

    /**
     * 將 API [DitchDetails] 轉換為 [Waypoint] 列表。
     * 排序規則：NODE_ATT 1(起點) → 2(節點) → 3(終點)。
     * 並帶入從 Intent 傳入的 WGS84 座標（wgsLatitudes/wgsLongitudes）。
     */
    private fun ditchToWaypoints(d: DitchDetails): List<Waypoint> {
        val result = mutableListOf<Waypoint>()

        val sorted = d.nodes.sortedWith(
            compareBy(
                { when (it.nodeAtt) { "1" -> 0; "3" -> 2; else -> 1 } },
                { it.nodeNum?.toIntOrNull() ?: Int.MAX_VALUE }
            )
        )

        sorted.forEachIndexed { idx, node ->
            // 依序取得 WGS84 座標（Intent 傳入）
            val lat = wgsLatitudes.getOrNull(idx)
            val lng = wgsLongitudes.getOrNull(idx)
            val latLng = if (lat != null && lng != null) LatLng(lat, lng) else null

            when (node.nodeAtt) {
                "1" -> result.add(Waypoint(
                    WaypointType.START, "起點", latLng,
                    hashMapOf(
                        "_nodeId"    to node.nodeId.toString(),
                        "SPI_NUM"    to d.spiNum
                    )
                ))
                "3" -> result.add(Waypoint(
                    WaypointType.END, "終點", latLng,
                    hashMapOf(
                        "_nodeId"    to node.nodeId.toString(),
                        "SPI_NUM"    to d.spiNum
                    )
                ))
                else -> result.add(Waypoint(
                    WaypointType.NODE, "節點${node.nodeNum ?: "?"}", latLng,
                    hashMapOf(
                        "_nodeId"    to node.nodeId.toString(),
                        "SPI_NUM"    to d.spiNum
                    )
                ))
            }
        }
        if (result.isEmpty()) {
            result.add(Waypoint(WaypointType.START, "起點"))
            result.add(Waypoint(WaypointType.END,   "終點"))
        }
        return result
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
