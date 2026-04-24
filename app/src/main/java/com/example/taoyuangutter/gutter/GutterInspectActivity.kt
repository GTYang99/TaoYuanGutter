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
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.databinding.ActivityGutterInspectBinding
import com.example.taoyuangutter.login.LoginActivity
import com.google.gson.Gson
import com.google.android.material.tabs.TabLayoutMediator

import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.model.LatLng
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    private var strPhoto1: String? = null
    private var strPhoto2: String? = null
    private var strPhoto3: String? = null
    private var endPhoto1: String? = null
    private var endPhoto2: String? = null
    private var endPhoto3: String? = null

    private val repository = GutterRepository()

    companion object {
        private const val EXTRA_DITCH_JSON        = "ditch_json"
        private const val EXTRA_CAN_EDIT          = "can_edit"
        private const val EXTRA_LATITUDES         = "latitudes"
        private const val EXTRA_LONGITUDES        = "longitudes"
        private const val EXTRA_STR_PHOTO_1       = "str_photo_1"
        private const val EXTRA_STR_PHOTO_2       = "str_photo_2"
        private const val EXTRA_STR_PHOTO_3       = "str_photo_3"
        private const val EXTRA_END_PHOTO_1       = "end_photo_1"
        private const val EXTRA_END_PHOTO_2       = "end_photo_2"
        private const val EXTRA_END_PHOTO_3       = "end_photo_3"

        /** setResult code：使用者點擊編輯按鈕，要求 MainActivity 開啟 AddGutterBottomSheet */
        const val RESULT_EDIT_DITCH               = android.app.Activity.RESULT_FIRST_USER + 10
        /** result Intent 攜帶的 WaypointSnapshot 列表 JSON */
        const val EXTRA_RESULT_WAYPOINTS_JSON     = "result_waypoints_json"
        /** result Intent 攜帶的 SPI_NUM */
        const val EXTRA_RESULT_SPI_NUM            = "result_spi_num"
        /** result Intent 攜帶的 is_curve（"0"/"1"） */
        const val EXTRA_RESULT_IS_CURVE           = "result_is_curve"

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
            longitudes: DoubleArray = doubleArrayOf(),
            strPhoto1: String? = null,
            strPhoto2: String? = null,
            strPhoto3: String? = null,
            endPhoto1: String? = null,
            endPhoto2: String? = null,
            endPhoto3: String? = null
        ): Intent {
            val json = Gson().toJson(ditch)
            return Intent(context, GutterInspectActivity::class.java).apply {
                putExtra(EXTRA_DITCH_JSON, json)
                putExtra(EXTRA_CAN_EDIT,   canEdit)
                putExtra(EXTRA_LATITUDES,  latitudes)
                putExtra(EXTRA_LONGITUDES, longitudes)
                putExtra(EXTRA_STR_PHOTO_1, strPhoto1)
                putExtra(EXTRA_STR_PHOTO_2, strPhoto2)
                putExtra(EXTRA_STR_PHOTO_3, strPhoto3)
                putExtra(EXTRA_END_PHOTO_1, endPhoto1)
                putExtra(EXTRA_END_PHOTO_2, endPhoto2)
                putExtra(EXTRA_END_PHOTO_3, endPhoto3)
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

        strPhoto1 = intent.getStringExtra(EXTRA_STR_PHOTO_1)
        strPhoto2 = intent.getStringExtra(EXTRA_STR_PHOTO_2)
        strPhoto3 = intent.getStringExtra(EXTRA_STR_PHOTO_3)
        endPhoto1 = intent.getStringExtra(EXTRA_END_PHOTO_1)
        endPhoto2 = intent.getStringExtra(EXTRA_END_PHOTO_2)
        endPhoto3 = intent.getStringExtra(EXTRA_END_PHOTO_3)

        setupTitleBar(ditch)
        setupViewPager(ditch)
        setupTabs()

        // group_id 一致才顯示編輯按鈕並掛載點擊事件
        val canEdit = intent.getBooleanExtra(EXTRA_CAN_EDIT, false)
        if (canEdit) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener { preloadAllNodeDetailsThenOpenEditForm() }
        } else {
            binding.btnEdit.visibility = View.GONE
        }
    }

    // Photo preloading is handled by MainActivity (download to contentUri) before launching this activity.

    // ── Window ──────────────────────────────────────────────────────────

    private fun applyBottomSheetWindow() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        // 確保視窗背景透明，以顯示佈局的圓角
        window.setBackgroundDrawableResource(android.R.color.transparent)

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
        val adapter = InspectPagerAdapter(
            activity = this,
            ditch = ditch,
            strPhoto1 = strPhoto1,
            strPhoto2 = strPhoto2,
            strPhoto3 = strPhoto3,
            endPhoto1 = endPhoto1,
            endPhoto2 = endPhoto2,
            endPhoto3 = endPhoto3
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1
    }

    private fun setupTabs() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(com.example.taoyuangutter.R.string.tab_basic_info)
                1 -> "照片"
                else -> ""
            }
        }.attach()
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
            putExtra(EXTRA_RESULT_IS_CURVE, d.isCurve ?: "0")
        }
        setResult(RESULT_EDIT_DITCH, resultIntent)
        finish()
    }

    /**
     * 編輯前預載（版本 1）：
     * - 先打所有點位的 v1/node/nodeDetails（預熱資料）
     * - 不提示、不阻擋進入編輯；缺漏在「送出/更新前」才統一檢查
     */
    private fun preloadAllNodeDetailsThenOpenEditForm() {
        val d = ditch ?: return
        val token = LoginActivity.getSavedToken(this) ?: run {
            android.app.AlertDialog.Builder(this)
                .setTitle("尚未登入")
                .setMessage("請先登入後再嘗試編輯。")
                .setPositiveButton("確定", null)
                .show()
            return
        }

        // 防止連點
        binding.btnEdit.isEnabled = false
        binding.btnEdit.alpha = 0.5f

        lifecycleScope.launch {
            try {
                d.nodes.forEach { node ->
                    val nodeId = node.nodeId
                    // 預熱：不管成功失敗都繼續，且不提示使用者
                    when (repository.getNodeDetails(nodeId, token)) {
                        is ApiResult.Success -> Unit
                        is ApiResult.Error -> Unit
                    }
                }

                openEditForm()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 預載失敗不阻擋編輯：直接進入編輯畫面，送出前會再檢查
                openEditForm()
            } finally {
                binding.btnEdit.isEnabled = true
                binding.btnEdit.alpha = 1.0f
            }
        }
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
                        "SPI_NUM"    to d.spiNum,
                        "IS_PENDING_DEPLOY" to (if (node.isPendingDeploy?.trim() == "1") "1" else "0")
                    )
                ))
                "3" -> result.add(Waypoint(
                    WaypointType.END, "終點", latLng,
                    hashMapOf(
                        "_nodeId"    to node.nodeId.toString(),
                        "SPI_NUM"    to d.spiNum,
                        "IS_PENDING_DEPLOY" to (if (node.isPendingDeploy?.trim() == "1") "1" else "0")
                    )
                ))
                else -> result.add(Waypoint(
                    WaypointType.NODE, "節點${node.nodeNum ?: "?"}", latLng,
                    hashMapOf(
                        "_nodeId"    to node.nodeId.toString(),
                        "SPI_NUM"    to d.spiNum,
                        "IS_PENDING_DEPLOY" to (if (node.isPendingDeploy?.trim() == "1") "1" else "0")
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
        private val ditch: DitchDetails?,
        private val strPhoto1: String?,
        private val strPhoto2: String?,
        private val strPhoto3: String?,
        private val endPhoto1: String?,
        private val endPhoto2: String?,
        private val endPhoto3: String?
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = when (position) {
            0    -> GutterInspectBasicFragment.newInstance(ditch)
            1    -> GutterInspectPhotosFragment.newInstance(
                nodes = ditch?.nodes ?: emptyList(),
                strPhoto1 = strPhoto1,
                strPhoto2 = strPhoto2,
                strPhoto3 = strPhoto3,
                endPhoto1 = endPhoto1,
                endPhoto2 = endPhoto2,
                endPhoto3 = endPhoto3
            )
            else -> throw IllegalArgumentException("Unknown page $position")
        }
    }
}
