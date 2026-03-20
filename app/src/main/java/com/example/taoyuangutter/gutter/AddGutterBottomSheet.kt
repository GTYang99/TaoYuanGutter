package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.BottomSheetAddGutterBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddGutterBottomSheet : BottomSheetDialogFragment() {

    // ── 與 MainActivity 通訊的介面 ──────────────────────────────────────
    interface LocationPickerHost {
        /** 請求 MainActivity 顯示地圖選點 overlay（新增模式） */
        fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 當 BottomSheet 點擊「新增側溝」後的回呼（新增模式） */
        fun onGutterSubmitted(waypoints: List<Waypoint>)
        /** 取得目前要檢視的 waypoints（檢視模式） */
        fun getInspectWaypoints(): List<Waypoint>
        /** 使用者點選某個點位的 cell（檢視模式），開啟 GutterFormActivity 檢視/編輯 */
        fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int)
    }

    /**
     * 當 waypoints 發生任何異動時通知 MainActivity。
     * 傳入 null 代表 sheet 已關閉。
     */
    var onWaypointsChanged: ((List<Waypoint>?) -> Unit)? = null

    // ── ViewBinding ─────────────────────────────────────────────────────
    private var _binding: BottomSheetAddGutterBinding? = null
    private val binding get() = _binding!!

    // ── 模式 ─────────────────────────────────────────────────────────────
    /** true = 檢視線段模式（點選 cell → 開啟表單檢視），false = 新增模式 */
    private var isInspectMode = false

    // ── 資料 ─────────────────────────────────────────────────────────────
    private lateinit var adapter: WaypointAdapter
    private val waypoints = mutableListOf(
        Waypoint(WaypointType.START, "起點"),
        Waypoint(WaypointType.END, "終點")
    )

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isInspectMode = arguments?.getBoolean(ARG_INSPECT_MODE, false) ?: false
        // 新增模式：禁止 back 鍵/點背景關閉；檢視模式：允許
        isCancelable = isInspectMode
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddGutterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isInspectMode) {
            // 從 Host 取得要檢視的 waypoints
            val inspect = (activity as? LocationPickerHost)?.getInspectWaypoints() ?: emptyList()
            waypoints.clear()
            waypoints.addAll(inspect)
        }

        setupBottomSheetBehavior()
        setupRecyclerView()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setDimAmount(0f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (isInspectMode) {
            // 檢視模式：null 通知 MainActivity 清除暫時大頭針
            onWaypointsChanged?.invoke(null)
        } else {
            // 新增模式：以目前 waypoints 狀態通知（保留地圖疊加層）
            onWaypointsChanged?.invoke(waypoints.toList())
        }
    }

    // ── 設定 BottomSheet 行為 ────────────────────────────────────────────
    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener {
            getBehavior()?.apply {
                val screenH    = resources.displayMetrics.heightPixels
                peekHeight     = (screenH * 0.52).toInt()
                // 完全展開時頂部仍保留 30%，讓地圖（含大頭針/線段）始終可見
                expandedOffset = (screenH * 0.30).toInt()
                state          = BottomSheetBehavior.STATE_COLLAPSED
                isHideable     = false
                skipCollapsed  = false
            }
        }
    }

    private fun getBehavior(): BottomSheetBehavior<View>? {
        val sheetView = getSheetView() ?: return null
        return BottomSheetBehavior.from(sheetView)
    }

    private fun getSheetView(): View? =
        (dialog as? BottomSheetDialog)
            ?.findViewById(com.google.android.material.R.id.design_bottom_sheet)

    fun hideSelf() {
        val sheetView = getSheetView() ?: return
        sheetView.animate()
            .translationY(sheetView.height.toFloat())
            .setDuration(250)
            .start()
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun showSelf() {
        val sheetView = getSheetView() ?: return
        sheetView.animate()
            .translationY(0f)
            .setDuration(250)
            .start()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // ── RecyclerView + ItemTouchHelper ───────────────────────────────────
    private fun setupRecyclerView() {
        adapter = WaypointAdapter(waypoints) { position ->
            if (isInspectMode) {
                // 檢視模式：開啟表單檢視
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForInspect(this, position)
            } else {
                // 新增模式：選點
                (requireActivity() as? LocationPickerHost)
                    ?.startLocationPick(this, position)
            }
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 檢視模式禁止拖曳
                if (isInspectMode) return makeMovementFlags(0, 0)
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to   = target.adapterPosition
                val moved = waypoints.removeAt(from)
                waypoints.add(to, moved)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                if (!isInspectMode) renumberAll()
            }
        })

        adapter.startDragListener = { if (!isInspectMode) touchHelper.startDrag(it) }
        // 在檢視模式隱藏拖曳把手
        adapter.showDragHandle = !isInspectMode

        touchHelper.attachToRecyclerView(binding.rvWaypoints)
        binding.rvWaypoints.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AddGutterBottomSheet.adapter
            isNestedScrollingEnabled = true
        }
    }

    // ── 按鈕 ─────────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }

        if (isInspectMode) {
            // 檢視模式：隱藏新增節點與提交按鈕
            binding.btnAddNode.visibility       = View.GONE
            binding.btnSubmitGutter.visibility  = View.GONE
        } else {
            binding.btnAddNode.setOnClickListener {
                val nodeCount  = waypoints.count { it.type == WaypointType.NODE }
                val insertIdx  = waypoints.size - 1
                waypoints.add(insertIdx, Waypoint(WaypointType.NODE, "節點${nodeCount + 1}"))
                adapter.notifyItemInserted(insertIdx)
                binding.rvWaypoints.scrollToPosition(insertIdx)
                // 新節點插入後 waypoints index 改變，需通知 MainActivity 刷新大頭針 tag
                onWaypointsChanged?.invoke(waypoints.toList())
            }

            binding.btnSubmitGutter.setOnClickListener {
                // ① 起點與終點必須已設定座標
                val start = waypoints.firstOrNull { it.type == WaypointType.START }
                val end   = waypoints.firstOrNull { it.type == WaypointType.END }
                if (start?.latLng == null) {
                    Toast.makeText(requireContext(), "請先設定起點座標", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (end?.latLng == null) {
                    Toast.makeText(requireContext(), "請先設定終點座標", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ② 自動移除「未選座標」或「資料不完整」的節點
                // 必填欄位：gutterId、gutterType、coordX、coordY、coordZ、measureId、depth、topWidth
                // 照片：三張都需拍攝（photo1/2/3 均非空）
                val requiredBasicKeys = listOf(
                    "gutterId", "gutterType", "coordX", "coordY", "coordZ",
                    "measureId", "depth", "topWidth"
                )
                val requiredPhotoKeys = listOf("photo1", "photo2", "photo3")
                val validWaypoints = waypoints.filter { wp ->
                    if (wp.type != WaypointType.NODE) return@filter true
                    val hasLocation   = wp.latLng != null
                    val hasBasicData  = requiredBasicKeys.all { wp.basicData[it]?.isNotEmpty() == true }
                    val hasAllPhotos  = requiredPhotoKeys.all { wp.basicData[it]?.isNotEmpty() == true }
                    hasLocation && hasBasicData && hasAllPhotos
                }
                val removedCount = waypoints.size - validWaypoints.size
                if (removedCount > 0) {
                    Toast.makeText(
                        requireContext(),
                        "已自動移除 $removedCount 個未完成設定的節點",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                (requireActivity() as? LocationPickerHost)?.onGutterSubmitted(validWaypoints)
                dismiss()
            }
        }
    }

    /**
     * 供 MainActivity 在系統重建後辨識模式用：
     * true = 新增模式（activeSheet），false = 檢視模式（inspectSheet）。
     */
    fun isAddMode(): Boolean = !isInspectMode

    // ── 依位置重新命名全部 waypoints ──────────────────────────────────────
    private fun renumberAll() {
        var nodeCount = 0
        waypoints.forEachIndexed { idx, wp ->
            when (idx) {
                0 -> { wp.type = WaypointType.START; wp.label = "起點" }
                waypoints.size - 1 -> { wp.type = WaypointType.END; wp.label = "終點" }
                else -> { nodeCount++; wp.type = WaypointType.NODE; wp.label = "節點$nodeCount" }
            }
        }
        adapter.notifyDataSetChanged()
        onWaypointsChanged?.invoke(waypoints.toList())
    }

    // ── 由 MainActivity 回呼：寫入選定座標 ──────────────────────────────
    fun getWaypointLabel(index: Int): String =
        waypoints.getOrNull(index)?.label ?: "點位"

    fun updateWaypointLocation(index: Int, latLng: LatLng) {
        if (index in waypoints.indices) {
            waypoints[index].latLng = latLng
            adapter.notifyItemChanged(index)
            onWaypointsChanged?.invoke(waypoints.toList())
        }
    }

    /** 將表單填寫的基本資料存回對應的 waypoint（供新增流程返回後呼叫） */
    fun updateWaypointBasicData(index: Int, data: HashMap<String, String>) {
        if (index in waypoints.indices) {
            waypoints[index].basicData = data
            adapter.notifyItemChanged(index)
        }
    }

    // ── Companion ────────────────────────────────────────────────────────
    companion object {
        const val TAG = "AddGutterBottomSheet"
        private const val ARG_INSPECT_MODE = "inspect_mode"

        /** 新增模式 */
        fun newInstance() = AddGutterBottomSheet()

        /** 檢視線段模式（點選 Polyline 後開啟） */
        fun newInstanceForInspect() = AddGutterBottomSheet().apply {
            arguments = Bundle().apply { putBoolean(ARG_INSPECT_MODE, true) }
        }
    }
}
