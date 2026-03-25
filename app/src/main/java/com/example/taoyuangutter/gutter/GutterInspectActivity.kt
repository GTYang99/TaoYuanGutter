package com.example.taoyuangutter.gutter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.taoyuangutter.databinding.ActivityGutterInspectBinding
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * GutterInspectActivity
 *
 * 唯讀檢視整條側溝（多點位）的彙整資料。
 * 以 FormSheet 半透明底部樣式浮現於地圖上（與 GutterFormActivity 一致）。
 *
 * 包含兩個分頁：
 *   0 → 基本資料（[GutterInspectBasicFragment]）
 *   1 → 照片（[GutterInspectPhotosFragment]）
 *
 * 標題格式：「側溝編號 \n {gutterId}」，後者字體較小。
 */
class GutterInspectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGutterInspectBinding
    private var waypoints: List<Waypoint> = emptyList()

    companion object {
        private const val EXTRA_WAYPOINTS_JSON = "waypoints_json"

        /**
         * 建立開啟 GutterInspectActivity 的 Intent。
         * @param waypoints 整條側溝的點位列表（起點 + 節點… + 終點）
         *
         * 使用 [WaypointSnapshot]（純 data class）進行 Gson 序列化，
         * 避免直接序列化 LatLng（Maps SDK class）造成問題。
         */
        fun newIntent(context: Context, waypoints: List<Waypoint>): Intent {
            val snapshots = waypoints.map { wp ->
                WaypointSnapshot(
                    type      = wp.type.name,
                    label     = wp.label,
                    latitude  = wp.latLng?.latitude,
                    longitude = wp.latLng?.longitude,
                    basicData = wp.basicData
                )
            }
            val json = Gson().toJson(snapshots)
            return Intent(context, GutterInspectActivity::class.java).apply {
                putExtra(EXTRA_WAYPOINTS_JSON, json)
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGutterInspectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 以 FormSheet 底部浮窗樣式呈現（佔下方 3/4 螢幕）
        applyBottomSheetWindow()

        waypoints = parseWaypoints()
        setupTitleBar(waypoints)
        setupViewPager(waypoints)
        setupTabButtons()
        setupEditButton()
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

    private fun parseWaypoints(): List<Waypoint> {
        val json = intent.getStringExtra(EXTRA_WAYPOINTS_JSON) ?: return emptyList()
        return try {
            val snapshotType = object : TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = Gson().fromJson(json, snapshotType) ?: emptyList()
            snapshots.map { snap ->
                val wpType = when (snap.type.uppercase()) {
                    "START" -> WaypointType.START
                    "END"   -> WaypointType.END
                    else    -> WaypointType.NODE
                }
                val latLng = if (snap.latitude != null && snap.longitude != null)
                    LatLng(snap.latitude, snap.longitude) else null
                Waypoint(
                    type      = wpType,
                    label     = snap.label,
                    latLng    = latLng,
                    basicData = snap.basicData
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 標題列 ───────────────────────────────────────────────────────────

    private fun setupTitleBar(waypoints: List<Waypoint>) {
        val start    = waypoints.firstOrNull { it.type == WaypointType.START }
        val gutterId = start?.basicData?.get("gutterId")?.takeIf { it.isNotEmpty() } ?: "---"
        
        val line1 = "側溝編號"
        val fullText = "$line1\n$gutterId"
        
        val spannable = SpannableStringBuilder(fullText)
        // "側溝編號" 長度為 4，\n 佔 1，所以 gutterId 從 5 開始
        spannable.setSpan(
            AbsoluteSizeSpan(16, true), // 18sp 減 2 = 16sp
            5,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.tvInspectTitle.text = spannable
        binding.btnBack.setOnClickListener { finish() }
    }

    // ── 編輯按鈕 ─────────────────────────────────────────────────────────

    /**
     * 點擊鉛筆 icon → 以 GutterFormActivity 的「檢視模式」開啟起點點位，
     * 使用者可在該畫面再按鉛筆進行逐點編輯。
     */
    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener { openEditForStart() }
    }

    private fun openEditForStart() {
        val start = waypoints.firstOrNull { it.type == WaypointType.START } ?: return
        val lat   = start.latLng?.latitude  ?: 0.0
        val lng   = start.latLng?.longitude ?: 0.0
        // waypointIndex：起點在整條 waypoints 列表中的索引（通常為 0）
        val idx   = waypoints.indexOfFirst { it.type == WaypointType.START }.coerceAtLeast(0)
        val intent = GutterFormActivity.newViewIntent(
            context       = this,
            label         = start.label,
            lat           = lat,
            lng           = lng,
            waypointIndex = idx,
            basicData     = start.basicData
        )
        startActivity(intent)
    }

    // ── ViewPager + 分頁 ─────────────────────────────────────────────────

    private fun setupViewPager(waypoints: List<Waypoint>) {
        val adapter = InspectPagerAdapter(this, waypoints)
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
        private val waypoints: List<Waypoint>
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> GutterInspectBasicFragment.newInstance(waypoints)
            1 -> GutterInspectPhotosFragment.newInstance(waypoints)
            else -> throw IllegalArgumentException("Unknown page $position")
        }
    }
}
