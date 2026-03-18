package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── 設定 BottomSheet 行為 ────────────────────────────────────────────
    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener {
            getBehavior()?.apply {
                peekHeight   = (resources.displayMetrics.heightPixels * 0.52).toInt()
                state        = BottomSheetBehavior.STATE_COLLAPSED
                isHideable   = true          // 允許程式碼隱藏
                skipCollapsed = false
            }
        }
    }

    private fun getBehavior(): BottomSheetBehavior<View>? {
        val sheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return null
        return BottomSheetBehavior.from(sheet)
    }

    /** 隱藏底部面板（選點時呼叫） */
    fun hideSelf() {
        getBehavior()?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /** 重新展開底部面板（選點完成 / 取消後呼叫） */
    fun showSelf() {
        getBehavior()?.apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    // ── RecyclerView + ItemTouchHelper（拖曳排序） ───────────────────────
    private fun setupRecyclerView() {
        adapter = WaypointAdapter(waypoints) { position ->
            // Cell 點選 → 請 MainActivity 顯示選點 overlay
            (requireActivity() as? LocationPickerHost)
                ?.startLocationPick(this, position)
        }

        // ItemTouchHelper：只允許節點（非起點/終點）拖曳
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val pos = viewHolder.adapterPosition
                return if (waypoints.getOrNull(pos)?.type == WaypointType.NODE)
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                else
                    makeMovementFlags(0, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to   = target.adapterPosition
                // 不允許移動到起點（0）或終點（最後一個）
                if (to == 0 || to == waypoints.size - 1) return false

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
                renumberNodes()
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
            // TODO: 將 waypoints 資料傳遞至後端或下一個畫面
            dismiss()
        }
    }

    // ── 節點重新編號 ──────────────────────────────────────────────────────
    private fun renumberNodes() {
        var count = 0
        waypoints.forEachIndexed { idx, wp ->
            if (wp.type == WaypointType.NODE) {
                count++
                waypoints[idx].label = "節點$count"
            }
        }
        adapter.notifyDataSetChanged()
    }

    // ── 由 MainActivity 回呼：寫入選定座標 ──────────────────────────────
    fun updateWaypointLocation(index: Int, latLng: LatLng) {
        if (index in waypoints.indices) {
            waypoints[index].latLng = latLng
            adapter.notifyItemChanged(index)
        }
    }

    // ── Companion ────────────────────────────────────────────────────────
    companion object {
        const val TAG = "AddGutterBottomSheet"
        fun newInstance() = AddGutterBottomSheet()
    }
}
