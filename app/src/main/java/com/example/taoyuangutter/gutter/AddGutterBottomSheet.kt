package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
        /** 請求 MainActivity 顯示地圖選點 overlay */
        fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 當 BottomSheet 點擊提交完成後的回呼 */
        fun onGutterSubmitted(waypoints: List<Waypoint>)
    }

    /**
     * 當 waypoints 發生任何異動（座標更新、節點排序改變）時通知 MainActivity。
     * 傳入 null 代表 sheet 已關閉，MainActivity 應清除地圖疊加層。
     */
    var onWaypointsChanged: ((List<Waypoint>?) -> Unit)? = null

    // ── ViewBinding ─────────────────────────────────────────────────────
    private var _binding: BottomSheetAddGutterBinding? = null
    private val binding get() = _binding!!

    // ── 資料 ─────────────────────────────────────────────────────────────
    private lateinit var adapter: WaypointAdapter
    private val waypoints = mutableListOf(
        Waypoint(WaypointType.START, "起點"),
        Waypoint(WaypointType.END, "終點")
    )

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 禁止 back 鍵與點擊背景關閉，只有按「新增側溝」才能離開
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddGutterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior()
        setupRecyclerView()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // 移除 BottomSheetDialog 預設的背景遮罩，確保地圖可見且顏色正常
        dialog?.window?.setDimAmount(0f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // 關閉時保留地圖上的標記與連線：以目前 waypoints 狀態重繪（不清除）
        onWaypointsChanged?.invoke(waypoints.toList())
    }

    // ── 設定 BottomSheet 行為 ────────────────────────────────────────────
    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener {
            getBehavior()?.apply {
                peekHeight    = (resources.displayMetrics.heightPixels * 0.52).toInt()
                state         = BottomSheetBehavior.STATE_COLLAPSED
                isHideable    = false   // 禁止向下滑關閉
                skipCollapsed = false
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

    /**
     * 選點時呼叫：sheet 滑出畫面底部，並將 dialog window 設為不攔截觸控，
     * 讓觸控事件穿透到後方的地圖與 overlay 按鈕。
     */
    fun hideSelf() {
        val sheetView = getSheetView() ?: return
        sheetView.animate()
            .translationY(sheetView.height.toFloat())
            .setDuration(250)
            .start()
        // 讓 dialog window 不再攔截觸控，地圖 / overlay 才能接收到點擊
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    /**
     * 選點完成/取消後呼叫：sheet 滑回原位，並還原 dialog window 觸控攔截。
     */
    fun showSelf() {
        val sheetView = getSheetView() ?: return
        sheetView.animate()
            .translationY(0f)
            .setDuration(250)
            .start()
        // 還原觸控攔截，sheet 上的按鈕/cell 才能再次被點選
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // ── RecyclerView + ItemTouchHelper（拖曳排序） ───────────────────────
    private fun setupRecyclerView() {
        adapter = WaypointAdapter(waypoints) { position ->
            // Cell 點選 → 請 MainActivity 顯示選點 overlay
            (requireActivity() as? LocationPickerHost)
                ?.startLocationPick(this, position)
        }

        // ItemTouchHelper：所有 cell 皆可拖曳，排序後依位置重新命名
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

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
                renumberAll()   // 依位置重新指派 type + label
            }
        })

        // 把 startDrag 回呼注入 Adapter（按住拖把手時啟動）
        adapter.startDragListener = { touchHelper.startDrag(it) }

        touchHelper.attachToRecyclerView(binding.rvWaypoints)

        binding.rvWaypoints.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AddGutterBottomSheet.adapter
            isNestedScrollingEnabled = true
        }
    }

    // ── 按鈕 ─────────────────────────────────────────────────────────────
    private fun setupButtons() {
        // X 按鈕：取消本次新增，地圖標記保留，sheet 關閉回地圖
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnAddNode.setOnClickListener {
            val nodeCount = waypoints.count { it.type == WaypointType.NODE }
            val insertIndex = waypoints.size - 1  // 終點之前
            waypoints.add(insertIndex, Waypoint(WaypointType.NODE, "節點${nodeCount + 1}"))
            adapter.notifyItemInserted(insertIndex)
            adapter.notifyItemChanged(insertIndex + 1) // 終點連接線更新
            binding.rvWaypoints.scrollToPosition(insertIndex)
        }

        binding.btnSubmitGutter.setOnClickListener {
            // 通知 Host (MainActivity) 提交完成，並傳遞 waypoints 資料
            (requireActivity() as? LocationPickerHost)?.onGutterSubmitted(waypoints.toList())
            dismiss()
        }
    }

    // ── 依位置重新命名全部 waypoints ──────────────────────────────────────
    /**
     * 拖曳排序結束後呼叫。
     * 位置 0      → WaypointType.START / "起點"
     * 位置 last   → WaypointType.END   / "終點"
     * 其餘位置    → WaypointType.NODE  / "節點N"（依序編號）
     */
    private fun renumberAll() {
        var nodeCount = 0
        waypoints.forEachIndexed { idx, wp ->
            when (idx) {
                0 -> {
                    wp.type  = WaypointType.START
                    wp.label = "起點"
                }
                waypoints.size - 1 -> {
                    wp.type  = WaypointType.END
                    wp.label = "終點"
                }
                else -> {
                    nodeCount++
                    wp.type  = WaypointType.NODE
                    wp.label = "節點$nodeCount"
                }
            }
        }
        adapter.notifyDataSetChanged()
        onWaypointsChanged?.invoke(waypoints.toList())
    }

    // ── 由 MainActivity 回呼：寫入選定座標 ──────────────────────────────
    fun updateWaypointLocation(index: Int, latLng: LatLng) {
        if (index in waypoints.indices) {
            waypoints[index].latLng = latLng
            adapter.notifyItemChanged(index)
            // 座標更新：通知地圖新增/移動標記與連線
            onWaypointsChanged?.invoke(waypoints.toList())
        }
    }

    // ── Companion ────────────────────────────────────────────────────────
    companion object {
        const val TAG = "AddGutterBottomSheet"
        fun newInstance() = AddGutterBottomSheet()
    }
}
