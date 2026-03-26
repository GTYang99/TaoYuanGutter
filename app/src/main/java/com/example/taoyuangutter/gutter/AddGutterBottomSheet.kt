package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.BottomSheetAddGutterBinding
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.gson.reflect.TypeToken
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddGutterBottomSheet : BottomSheetDialogFragment() {

    // ── 與 MainActivity 通訊的介面 ──────────────────────────────────────
    interface LocationPickerHost {
        /** 請求 MainActivity 顯示地圖選點 overlay（新增模式，點位尚無座標） */
        fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 點位已有座標，直接開啟 GutterFormActivity 繼續編輯（新增模式或編輯模式） */
        fun openWaypointForEdit(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 當 BottomSheet 點擊「新增側溝」後的回呼（新增模式） */
        fun onGutterSubmitted(waypoints: List<Waypoint>)
        /** 取得目前要檢視的 waypoints（檢視模式） */
        fun getInspectWaypoints(): List<Waypoint>
        /** 使用者點選某個點位的 cell（檢視模式），開啟 GutterFormActivity 檢視/編輯 */
        fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 編輯模式：點擊「更新側溝」，提交修改後的 waypoints */
        fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String)
        /** 編輯模式：點擊「刪除側溝」，刪除指定側溝 */
        fun onDeleteGutter(spiNum: String)
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
    /** true = 從離線流程開啟，顯示「取消」而非返回箭頭 */
    private var isOfflineMode = false
    /** 待上傳草稿的 id；非零時表示此 sheet 從草稿恢復 */
    private var draftId: Long = 0L
    /** 編輯模式時帶入的 SPI_NUM，用於顯示標題；空字串代表新增模式 */
    private var editSpiNum: String = ""

    // ── 資料 ─────────────────────────────────────────────────────────────
    private lateinit var adapter: WaypointAdapter
    private val waypoints = mutableListOf(
        Waypoint(WaypointType.START, "起點"),
        Waypoint(WaypointType.END, "終點")
    )

    /**
     * 編輯模式初始快照：在 setupButtons() 時記錄 API 回填後的原始狀態，
     * 作為「是否有修改」的基準。
     */
    private var originalWaypointsSnapshot: List<WaypointSnapshot> = emptyList()

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isInspectMode = arguments?.getBoolean(ARG_INSPECT_MODE,  false) ?: false
        isOfflineMode = arguments?.getBoolean(ARG_OFFLINE_MODE, false) ?: false
        draftId       = arguments?.getLong(ARG_DRAFT_ID, 0L) ?: 0L
        editSpiNum    = arguments?.getString(ARG_SPI_NUM, "") ?: ""
        // 新增/檢視模式皆允許點選外部區域（dim 遮罩）關閉
        isCancelable = true

        if (isInspectMode) return   // 檢視模式不需恢復 waypoints

        // ── 優先順序（後者在 Bundle 存在時覆蓋前者）─────────────────────
        // 1. 從 API DitchDetails 預填（編輯模式，每次開啟都應以 API 資料為主）
        val editJson = arguments?.getString(ARG_EDIT_WAYPOINTS_JSON)
        if (editJson != null) {
            restoreWaypointsFromSnapshotJson(editJson)
            return
        }
        // 2. 從草稿恢復
        val draftJson = arguments?.getString(ARG_DRAFT_JSON)
        if (draftJson != null) {
            restoreWaypointsFromDraftJson(draftJson)
            return
        }
        // 3. 系統重建（Activity 被回收後恢復，無 editJson / draftJson）
        if (savedInstanceState != null) {
            restoreWaypointsState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isInspectMode) return
        // 儲存所有 waypoints（包含已填寫的 latLng 與 basicData），
        // 避免 Activity 在 GutterFormActivity 期間被系統回收後資料遺失。
        outState.putInt(KEY_WP_COUNT, waypoints.size)
        waypoints.forEachIndexed { i, wp ->
            outState.putString("wp_type_$i",  wp.type.name)
            outState.putString("wp_label_$i", wp.label)
            outState.putDouble("wp_lat_$i",   wp.latLng?.latitude  ?: Double.NaN)
            outState.putDouble("wp_lng_$i",   wp.latLng?.longitude ?: Double.NaN)
            val keys = wp.basicData.keys.toTypedArray()
            val vals = keys.map { wp.basicData[it] ?: "" }.toTypedArray()
            outState.putStringArray("wp_data_keys_$i", keys)
            outState.putStringArray("wp_data_vals_$i", vals)
        }
    }

    /** 從 savedInstanceState 恢復 waypoints（Activity 重建後呼叫）。 */
    private fun restoreWaypointsState(state: Bundle) {
        val count = state.getInt(KEY_WP_COUNT, -1)
        if (count <= 0) return
        waypoints.clear()
        for (i in 0 until count) {
            val typeName = state.getString("wp_type_$i") ?: WaypointType.START.name
            val type  = WaypointType.valueOf(typeName)
            val label = state.getString("wp_label_$i") ?: ""
            val lat   = state.getDouble("wp_lat_$i", Double.NaN)
            val lng   = state.getDouble("wp_lng_$i", Double.NaN)
            val latLng = if (!lat.isNaN() && !lng.isNaN()) LatLng(lat, lng) else null
            val keys  = state.getStringArray("wp_data_keys_$i") ?: emptyArray()
            val vals  = state.getStringArray("wp_data_vals_$i") ?: emptyArray()
            val data  = hashMapOf<String, String>().apply {
                keys.zip(vals.toList()).forEach { (k, v) -> put(k, v) }
            }
            waypoints.add(Waypoint(type, label, latLng, data))
        }
    }

    /**
     * 從 List<WaypointSnapshot> JSON 還原 waypoints（編輯模式，由 API DitchDetails 轉換而來）。
     * 解析成功時清除預設 [起點,終點] 並替換為 API 資料；失敗時保留預設並 Toast 提示。
     */
    private fun restoreWaypointsFromSnapshotJson(json: String) {
        try {
            val type = object : TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = Gson().fromJson(json, type) ?: run {
                android.util.Log.w("AddGutterSheet", "restoreFromSnapshot: fromJson returned null")
                return
            }
            if (snapshots.isEmpty()) {
                android.util.Log.w("AddGutterSheet", "restoreFromSnapshot: snapshots is empty")
                return
            }
            waypoints.clear()
            snapshots.forEach { snap ->
                val wpType = WaypointType.entries.firstOrNull { it.name == snap.type }
                    ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null)
                    LatLng(snap.latitude, snap.longitude) else null
                waypoints.add(Waypoint(wpType, snap.label, latLng, snap.basicData))
            }
            android.util.Log.d("AddGutterSheet", "restoreFromSnapshot: loaded ${waypoints.size} waypoints")
        } catch (e: Exception) {
            android.util.Log.e("AddGutterSheet", "restoreFromSnapshot failed: ${e.message}", e)
            // 解析失敗：保留預設 [起點, 終點]，並在 view 建立後提示
            arguments?.putString("_restore_error", e.message ?: "unknown")
        }
    }

    /** 從草稿 JSON 字串恢復 waypoints（首次從 PendingDraftsBottomSheet 恢復時呼叫）。 */
    private fun restoreWaypointsFromDraftJson(json: String) {
        try {
            val draft = Gson().fromJson(json, GutterSessionDraft::class.java) ?: return
            if (draft.waypoints.isEmpty()) return
            waypoints.clear()
            draft.waypoints.forEach { snap ->
                val type   = WaypointType.entries.firstOrNull { it.name == snap.type }
                    ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null)
                    LatLng(snap.latitude, snap.longitude) else null
                waypoints.add(Waypoint(type, snap.label, latLng, snap.basicData))
            }
        } catch (e: Exception) {
            // 解析失敗：保留預設 [起點, 終點]
        }
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
        setupTitle()
    }

    override fun onStart() {
        super.onStart()
        // 半透明遮罩，讓地圖背景稍微變暗，凸顯操作介面
        dialog?.window?.setDimAmount(0.5f)
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

    // ── 標題 ──────────────────────────────────────────────────────────────
    /**
     * 編輯模式（editSpiNum 非空）時，將標題改為兩行：
     *   「側溝編號」（18sp bold）
     *   「{SPI_NUM}」（14sp）
     * 新增模式則保留 XML 預設的「新增側溝」文字。
     */
    private fun setupTitle() {
        if (editSpiNum.isEmpty()) return
        val line1    = "側溝編號"
        val fullText = "$line1\n$editSpiNum"
        val spannable = SpannableStringBuilder(fullText)
        // line1 有 4 個字 + "\n" 共 5 個字元，spiNum 從 index 5 開始
        spannable.setSpan(
            AbsoluteSizeSpan(14, true),
            5,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvSheetTitle.text = spannable
    }

    // ── 設定 BottomSheet 行為 ────────────────────────────────────────────
    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener {
            val sheetView = getSheetView()
            val halfScreen = resources.displayMetrics.heightPixels / 2
            sheetView?.layoutParams?.height = halfScreen
            sheetView?.requestLayout()
            // 清除 design_bottom_sheet 容器的預設背景，
            // 讓 bottom_sheet_add_gutter.xml 的 bg_form_sheet 圓角可以正常顯示
            sheetView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            getBehavior()?.apply {
                peekHeight     = halfScreen
                expandedOffset = 0
                state          = BottomSheetBehavior.STATE_EXPANDED
                isHideable     = false
                skipCollapsed  = true
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

    /** MainActivity 取得目前 sheet 內的 waypoints（新增模式用） */
    fun getWaypoints(): List<Waypoint> = waypoints.toList()

    fun hideSelf() {
        val sheetView = getSheetView() ?: return
        // 先把遮罩清掉，動畫結束後將整個 dialog 視窗隱藏
        // 讓地圖的 pan/zoom gesture 可以完整穿透
        dialog?.window?.setDimAmount(0f)
        dialog?.setCanceledOnTouchOutside(false)
        sheetView.animate()
            .translationY(sheetView.height.toFloat())
            .setDuration(250)
            .withEndAction {
                dialog?.window?.decorView?.visibility = android.view.View.INVISIBLE
            }
            .start()
    }

    fun showSelf() {
        // 先把視窗恢復可見，再把 sheet 從底部滑回來
        dialog?.window?.decorView?.visibility = android.view.View.VISIBLE
        val sheetView = getSheetView() ?: return
        sheetView.translationY = sheetView.height.toFloat()
        sheetView.animate()
            .translationY(0f)
            .setDuration(250)
            .start()
        dialog?.window?.setDimAmount(0.5f)
        dialog?.setCanceledOnTouchOutside(true)
    }

    // ── RecyclerView + ItemTouchHelper ───────────────────────────────────
    private fun setupRecyclerView() {
        adapter = WaypointAdapter(waypoints) { position ->
            if (isInspectMode) {
                // 檢視模式：開啟表單檢視（唯讀）
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForInspect(this, position)
            } else if (editSpiNum.isNotEmpty()) {
                // 編輯模式（從 GutterInspectActivity 跳轉）：
                // 以 _nodeId 呼叫 nodeDetails API 取得完整資料後開表單
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForEdit(this, position)
            } else {
                // 新增模式：先到地圖上選取點位座標
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
        if (isOfflineMode) {
            // 離線模式：顯示「取消」文字按鈕，隱藏返回箭頭
            binding.btnClose.visibility  = View.GONE
            binding.tvCancel.visibility  = View.VISIBLE
            binding.tvCancel.setOnClickListener { dismiss() }
        } else {
            binding.btnClose.setOnClickListener { dismiss() }
        }

        if (isInspectMode) {
            // 檢視模式：隱藏新增節點、提交與刪除按鈕
            binding.btnAddNode.visibility       = View.GONE
            binding.btnSubmitGutter.visibility  = View.GONE
            binding.btnDeleteGutter.visibility  = View.GONE
        } else if (editSpiNum.isNotEmpty()) {
            // 編輯模式：隱藏「新增節點」、顯示「刪除側溝」（左）＋「更新側溝」（右）
            //binding.btnAddNode.visibility = View.GONE
            binding.btnDeleteGutter.visibility = View.VISIBLE
            binding.btnSubmitGutter.text = "更新側溝"

            // 以目前 API 回填的 waypoints 作為基準快照，初始化時按鈕為禁用狀態
            originalWaypointsSnapshot = takeWaypointSnapshot()
            updateSubmitButtonState()

            binding.btnDeleteGutter.setOnClickListener {
                (requireActivity() as? LocationPickerHost)?.onDeleteGutter(editSpiNum)
            }
            binding.btnSubmitGutter.setOnClickListener {
                (requireActivity() as? LocationPickerHost)?.onUpdateGutter(waypoints.toList(), editSpiNum)
                dismiss()
            }
            binding.btnAddNode.setOnClickListener {
                val nodeCount  = waypoints.count { it.type == WaypointType.NODE }
                val insertIdx  = waypoints.size - 1
                waypoints.add(insertIdx, Waypoint(WaypointType.NODE, "節點${nodeCount + 1}"))
                adapter.notifyItemInserted(insertIdx)
                binding.rvWaypoints.scrollToPosition(insertIdx)
                // 新節點插入後 waypoints index 改變，需通知 MainActivity 刷新大頭針 tag
                onWaypointsChanged?.invoke(waypoints.toList())
            }
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

    /** 若此 sheet 是從待上傳草稿恢復，回傳其 id；否則回傳 0。 */
    fun getRestoredDraftId(): Long = draftId

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
        updateSubmitButtonState()
    }

    // ── 編輯模式：變更偵測與按鈕狀態更新 ────────────────────────────────

    /** 將目前 waypoints 轉成 Snapshot 列表（深拷貝），作為比較基準。 */
    private fun takeWaypointSnapshot(): List<WaypointSnapshot> = waypoints.map { wp ->
        WaypointSnapshot(
            type      = wp.type.name,
            label     = wp.label,
            latitude  = wp.latLng?.latitude,
            longitude = wp.latLng?.longitude,
            basicData = HashMap(wp.basicData)
        )
    }

    /**
     * 比較目前 waypoints 與初始快照，判斷是否有任何變更：
     * - 點位數量增減
     * - 任意點位的座標變更
     * - 任意點位的 basicData 欄位變更
     */
    private fun hasEditChanges(): Boolean {
        if (editSpiNum.isEmpty()) return false
        if (waypoints.size != originalWaypointsSnapshot.size) return true
        waypoints.forEachIndexed { i, wp ->
            val orig = originalWaypointsSnapshot[i]
            if (wp.latLng?.latitude  != orig.latitude)  return true
            if (wp.latLng?.longitude != orig.longitude) return true
            if (wp.basicData != orig.basicData)         return true
        }
        return false
    }

    /**
     * 根據 [hasEditChanges] 啟用或禁用「更新側溝」按鈕：
     * - 有變更 → 啟用（colorPrimary 底白字）
     * - 無變更 → 禁用（灰階底白字）
     */
    private fun updateSubmitButtonState() {
        if (editSpiNum.isEmpty() || _binding == null) return
        val hasChanges = hasEditChanges()
        binding.btnSubmitGutter.isEnabled = hasChanges
        val tint = if (hasChanges)
            androidx.core.content.ContextCompat.getColor(requireContext(), com.example.taoyuangutter.R.color.colorPrimary)
        else
            android.graphics.Color.parseColor("#9E9E9E")
        binding.btnSubmitGutter.backgroundTintList =
            android.content.res.ColorStateList.valueOf(tint)
    }

    // ── 由 MainActivity 回呼：寫入選定座標 ──────────────────────────────
    fun getWaypointLabel(index: Int): String =
        waypoints.getOrNull(index)?.label ?: "點位"

    fun updateWaypointLocation(index: Int, latLng: LatLng) {
        if (index in waypoints.indices) {
            waypoints[index].latLng = latLng
            adapter.notifyItemChanged(index)
            onWaypointsChanged?.invoke(waypoints.toList())
            updateSubmitButtonState()
        }
    }

    /** 將表單填寫的基本資料存回對應的 waypoint（供新增流程返回後呼叫） */
    fun updateWaypointBasicData(index: Int, data: HashMap<String, String>) {
        if (index in waypoints.indices) {
            waypoints[index].basicData = data
            adapter.notifyItemChanged(index)
            updateSubmitButtonState()
        }
    }

    /** 清除指定點位的座標與基本資料（使用者放棄填寫時呼叫） */
    fun clearWaypointLocation(index: Int) {
        if (index in waypoints.indices) {
            waypoints[index].latLng    = null
            waypoints[index].basicData = hashMapOf()
            adapter.notifyItemChanged(index)
            onWaypointsChanged?.invoke(waypoints.toList())
        }
    }

    // ── Companion ────────────────────────────────────────────────────────
    companion object {
        const val TAG = "AddGutterBottomSheet"
        private const val ARG_INSPECT_MODE = "inspect_mode"
        private const val ARG_OFFLINE_MODE = "offline_mode"
        private const val ARG_DRAFT_ID     = "draft_id"
        private const val ARG_DRAFT_JSON   = "draft_json"
        private const val KEY_WP_COUNT     = "wp_count"

        private const val ARG_EDIT_WAYPOINTS_JSON = "edit_waypoints_json"
        private const val ARG_SPI_NUM             = "spi_num"

        /** 新增模式（一般地圖流程） */
        fun newInstance() = AddGutterBottomSheet()

        /** 新增模式（離線流程，顯示「取消」按鈕） */
        fun newOfflineInstance() = AddGutterBottomSheet().apply {
            arguments = Bundle().apply { putBoolean(ARG_OFFLINE_MODE, true) }
        }

        /** 檢視線段模式（點選 Polyline 後開啟） */
        fun newInstanceForInspect() = AddGutterBottomSheet().apply {
            arguments = Bundle().apply { putBoolean(ARG_INSPECT_MODE, true) }
        }

        /**
         * 從 API DitchDetails 轉換而來的 waypoints 預填編輯模式。
         * 以新增模式（非 inspectMode）開啟，點位資料從 API 資料帶入。
         *
         * @param waypoints 由 DitchDetails 轉換而來的點位列表
         * @param spiNum    DitchDetails.spiNum，用於顯示標題（空字串則顯示預設「新增側溝」）
         */
        fun newInstanceForEdit(waypoints: List<Waypoint>, spiNum: String = ""): AddGutterBottomSheet {
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
            return AddGutterBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_EDIT_WAYPOINTS_JSON, json)
                    if (spiNum.isNotEmpty()) putString(ARG_SPI_NUM, spiNum)
                }
            }
        }

        /**
         * 從待上傳草稿恢復（繼續編輯）。
         *
         * @param draft 要恢復的草稿，將以 Gson JSON 傳入 Bundle 以跨越 Fragment 邊界。
         */
        fun newInstanceFromDraft(draft: GutterSessionDraft): AddGutterBottomSheet =
            AddGutterBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DRAFT_ID,   draft.id)
                    putString(ARG_DRAFT_JSON, Gson().toJson(draft))
                }
            }
    }
}
