package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.StoreDitchNodeRequest
import com.example.taoyuangutter.api.StoreDitchRequest
import com.example.taoyuangutter.databinding.BottomSheetAddGutterBinding
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.gson.reflect.TypeToken
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class AddGutterBottomSheet : BottomSheetDialogFragment() {

    // ── 與 MainActivity 通訊的介面 ──────────────────────────────────────
    interface LocationPickerHost {
        /** 請求 MainActivity 顯示地圖選點 overlay（新增模式，點位尚無座標） */
        fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /** 點位已有座標，直接開啟 GutterFormActivity 繼續編輯（新增模式或編輯模式） */
        fun openWaypointForEdit(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /**
         * 新增模式：storeDitch 呼叫前立即執行（清除地圖暫存資料）。
         * API 結果由 [onGutterSaved] / [onGutterSaveFailed] 回報。
         */
        fun onGutterSubmitted(waypoints: List<Waypoint>)
        /** 取得目前要檢視的 waypoints（檢視模式） */
        fun getInspectWaypoints(): List<Waypoint>
        /** 使用者點選某個點位的 cell（檢視模式），開啟 GutterFormActivity 檢視/編輯 */
        fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int)
        /**
         * 編輯模式：storeDitch 成功後立即執行（清除地圖暫存資料）。
         * API 結果由 [onGutterSaved] 回報。
         */
        fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String)
        /** 編輯模式：點擊「刪除側溝」，刪除指定側溝 */
        fun onDeleteGutter(spiNum: String)
        /**
         * storeDitch 成功後回呼。
         * @param spiNum null = 新增模式；非空 = 更新模式（帶 SPI_NUM）
         * @param nodes  後端回傳的 nodes 列表（含 node_id，供上傳照片用）
         */
        fun onGutterSaved(spiNum: String?, waypoints: List<Waypoint>, nodes: List<DitchNode>)
        /**
         * 新增模式下 storeDitch 失敗時回呼（供 MainActivity 存為待上傳草稿）。
         */
        fun onGutterSaveFailed(waypoints: List<Waypoint>)
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

    /** Window.Callback touch routing：ACTION_DOWN 落在 sheet 外時設為 true，後續事件轉發給 Activity */
    private var routeToActivity = false

    // ── Repository（storeDitch API） ──────────────────────────────────────
    private val repository by lazy { GutterRepository() }

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
    /** 編輯模式預載 node details 中，阻擋再次抓取與提交流程。 */
    private var isPreloadingEditDetails: Boolean = false

    /** iOS 風格左滑：目前已展開「刪除」按鈕的 item position，-1 代表無 */
    private var openedSwipePosition: Int = -1

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

        if (editSpiNum.isNotEmpty()) {
            preloadEditWaypointDetails()
        }
    }

    override fun onStart() {
        super.onStart()
        // 降低遮罩不透明度，讓背景地圖更容易看清楚
        dialog?.window?.setDimAmount(0.15f)
        // 觸碰 sheet 外部不 dismiss（地圖滑動由 Window.Callback 路由處理）
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        val isFromDraft = draftId > 0L || arguments?.containsKey(ARG_DRAFT_JSON) == true
        if (isInspectMode || editSpiNum.isNotEmpty() || isFromDraft) {
            // 檢視模式 / 編輯模式 / 從草稿恢復：null 通知 MainActivity 清除暫時大頭針
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
        if (isOfflineMode) {
            binding.tvSheetTitle.text = getString(R.string.msg_offline_draft_title)
            return
        }
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

            // ── 地圖觸碰穿透：Window.Callback 路由 ───────────────────────
            // ACTION_DOWN 落在 sheet 上方（地圖區）→ 轉發給 MainActivity 讓地圖處理
            // ACTION_DOWN 落在 sheet 內              → 正常 dispatch，sheet 行為不受影響
            // 後續 MOVE / UP 跟隨 DOWN 的判斷，確保手勢完整性
            val originalCb = dialog?.window?.callback ?: return@setOnShowListener
            dialog?.window?.callback = object : Window.Callback by originalCb {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        val loc = IntArray(2)
                        getSheetView()?.getLocationOnScreen(loc)
                        val sheetTopOnScreen = loc[1]
                        routeToActivity = event.rawY < sheetTopOnScreen
                    }
                    return if (routeToActivity) {
                        requireActivity().dispatchTouchEvent(event)
                    } else {
                        originalCb.dispatchTouchEvent(event)
                    }
                }
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
        dialog?.window?.setDimAmount(0.15f)
        dialog?.setCanceledOnTouchOutside(false)
    }

    // ── RecyclerView + ItemTouchHelper ───────────────────────────────────

    /**
     * 關閉目前已展開刪除按鈕的 item，動畫收回前景到 0，
     * 若 ViewHolder 不可見則直接通知 Adapter 重繪。
     */
    private fun closeOpenedSwipeItem() {
        val pos = openedSwipePosition
        if (pos < 0 || _binding == null) return
        openedSwipePosition = -1
        val vh = binding.rvWaypoints.findViewHolderForAdapterPosition(pos) as? WaypointAdapter.ViewHolder
        if (vh != null) {
            vh.foreground.animate().translationX(0f).setDuration(150).start()
        } else {
            adapter.notifyItemChanged(pos)
        }
    }

    private fun setupRecyclerView() {
        adapter = WaypointAdapter(
            items = waypoints,
            alwaysShowXyNumIfPresent = isInspectMode
        ) { position ->
            closeOpenedSwipeItem()
            if (isInspectMode) {
                // 檢視模式：開啟表單檢視（唯讀）
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForInspect(this, position)
            } else if (editSpiNum.isNotEmpty()) {
                // 編輯模式：直接編輯進場時已預載完成的 waypoint 資料
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForEdit(this, position)
            } else {
                // 新增模式：直接開表單，座標由表單內點擊 X/Y 欄位再選取
                (requireActivity() as? LocationPickerHost)
                    ?.openWaypointForEdit(this, position)
            }
        }

        val swipeBtnWidthPx by lazy {
            (80 * resources.displayMetrics.density).toInt()
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 檢視模式：完全禁止
                if (isInspectMode) return makeMovementFlags(0, 0)
                val pos = viewHolder.adapterPosition
                val wp  = waypoints.getOrNull(pos)
                // 起點 / 終點：只能拖曳，不能左滑刪除
                return if (wp?.type == WaypointType.NODE) {
                    makeMovementFlags(
                        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT
                    )
                } else {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                closeOpenedSwipeItem()
                val from = viewHolder.adapterPosition
                val to   = target.adapterPosition
                val moved = waypoints.removeAt(from)
                waypoints.add(to, moved)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos < 0 || pos >= waypoints.size) return
                // 安全檢查：只允許節點展開刪除按鈕
                if (waypoints[pos].type != WaypointType.NODE) {
                    adapter.notifyItemChanged(pos)
                    return
                }
                // 關閉上一個已展開的 item
                val prevPos = openedSwipePosition
                if (prevPos >= 0 && prevPos != pos) {
                    val prevVh = binding.rvWaypoints
                        .findViewHolderForAdapterPosition(prevPos) as? WaypointAdapter.ViewHolder
                    if (prevVh != null) {
                        prevVh.foreground.animate().translationX(0f).setDuration(150).start()
                    } else {
                        adapter.notifyItemChanged(prevPos)
                    }
                }
                openedSwipePosition = pos
                // clearView 會負責把前景固定在 -swipeBtnWidthPx
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val holder = viewHolder as? WaypointAdapter.ViewHolder ?: return
                    val pos = viewHolder.adapterPosition

                    // 若使用者開始滑動另一個 item，先關閉已展開的那個
                    if (isCurrentlyActive) {
                        val prev = openedSwipePosition
                        if (prev >= 0 && prev != pos) {
                            closeOpenedSwipeItem()
                        }
                    }

                    // 僅平移前景層，並根據目前是否已展開來計算位移
                    // 避免重新觸摸已展開項時 dX 從 0 開始導致畫面閃跳回 0
                    val translationX = if (pos == openedSwipePosition) {
                        // 已經是展開狀態，位移從 -swipeBtnWidthPx 開始
                        (dX - swipeBtnWidthPx).coerceIn(-swipeBtnWidthPx.toFloat(), 0f)
                    } else {
                        // 尚未展開
                        dX.coerceIn(-swipeBtnWidthPx.toFloat(), 0f)
                    }
                    holder.foreground.translationX = translationX
                    // 不呼叫 super，避免整個 itemView 被平移
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                val pos    = viewHolder.adapterPosition
                val holder = viewHolder as? WaypointAdapter.ViewHolder ?: return
                
                // 若位移已經回到 0，代表使用者主動關閉或取消滑動
                if (holder.foreground.translationX == 0f && pos == openedSwipePosition) {
                    openedSwipePosition = -1
                }

                if (pos == openedSwipePosition) {
                    // 展開狀態：前景固定在 -swipeBtnWidthPx，露出刪除按鈕
                    holder.foreground.translationX = -swipeBtnWidthPx.toFloat()
                } else {
                    holder.foreground.translationX = 0f
                    if (!isInspectMode) renumberAll()
                }
            }
        })

        // 點擊「刪除」按鈕：移除節點
        adapter.onSwipeDeleteClick = { pos ->
            if (pos in waypoints.indices && waypoints[pos].type == WaypointType.NODE) {
                openedSwipePosition = -1
                waypoints.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                renumberAll()
                updateSubmitButtonState()
            }
        }

        adapter.startDragListener = { if (!isInspectMode) touchHelper.startDrag(it) }
        // 在檢視模式隱藏拖曳把手
        adapter.showDragHandle = !isInspectMode

        touchHelper.attachToRecyclerView(binding.rvWaypoints)
        binding.rvWaypoints.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AddGutterBottomSheet.adapter
            isNestedScrollingEnabled = true
            // 滾動時關閉已展開的刪除按鈕
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    // 增加閾值，避免點擊刪除按鈕時的微小垂直手震導致按鈕自動收合
                    if (Math.abs(dy) > 10) closeOpenedSwipeItem()
                }
            })
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
            // 檢視模式：隱藏新增節點、調轉、提交與刪除按鈕
            binding.btnAddNode.visibility       = View.GONE
            binding.btnReverse.visibility       = View.GONE
            binding.btnSubmitGutter.visibility  = View.GONE
            binding.btnDeleteGutter.visibility  = View.GONE
        } else if (editSpiNum.isNotEmpty()) {
            // 編輯模式：隱藏「新增節點」、顯示「刪除側溝」（左）＋「更新側溝」（右）
            //binding.btnAddNode.visibility = View.GONE
            binding.btnDeleteGutter.visibility = View.VISIBLE
            binding.btnSubmitGutter.text = getString(R.string.btn_update_gutter)
            updateSubmitButtonState()

            binding.btnReverse.visibility = View.VISIBLE
            binding.btnReverse.setOnClickListener { reverseWaypoints() }

            binding.btnDeleteGutter.setOnClickListener {
                (requireActivity() as? LocationPickerHost)?.onDeleteGutter(editSpiNum)
            }
            binding.btnSubmitGutter.setOnClickListener {
                performEditSubmit()
            }
            binding.btnAddNode.setOnClickListener {
                val nodeCount  = waypoints.count { it.type == WaypointType.NODE }
                val insertIdx  = waypoints.size - 1
                waypoints.add(insertIdx, Waypoint(WaypointType.NODE, "節點${nodeCount + 1}"))
                adapter.notifyItemInserted(insertIdx)
                binding.rvWaypoints.scrollToPosition(insertIdx)
                onWaypointsChanged?.invoke(waypoints.toList())
                updateSubmitButtonState()
            }
        } else {
            if (isOfflineMode) {
                // 離線新增：提交按鈕只作為「完成」關閉，不打 API
                binding.btnDeleteGutter.visibility = View.GONE
                binding.btnSubmitGutter.text = getString(R.string.form_finish_button)
            }
            binding.btnReverse.visibility = View.VISIBLE
            binding.btnReverse.setOnClickListener { reverseWaypoints() }

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
                if (isOfflineMode) {
                    // 離線填寫：不打 API，不做必填驗證；直接確保草稿更新後關閉。
                    onWaypointsChanged?.invoke(waypoints.toList())
                    Toast.makeText(requireContext(), getString(R.string.msg_draft_saved), Toast.LENGTH_SHORT).show()
                    dismiss()
                    return@setOnClickListener
                }
                // ① 起點與終點必須已設定座標
                val start = waypoints.firstOrNull { it.type == WaypointType.START }
                val end   = waypoints.firstOrNull { it.type == WaypointType.END }
                if (start?.latLng == null) {
                    Toast.makeText(requireContext(), getString(R.string.msg_start_point_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (end?.latLng == null) {
                    Toast.makeText(requireContext(), getString(R.string.msg_end_point_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ② 自動移除「未選座標」或「資料不完整」的節點
                // 必填欄位：NODE_TYP、MAT_TYP、NODE_X、NODE_Y、XY_NUM、NODE_DEP、NODE_WID
                // 照片：三張都需拍攝（photo1/2/3 均非空）
                val requiredBasicKeys = listOf(
                    "NODE_TYP", "MAT_TYP", "NODE_X", "NODE_Y", "XY_NUM", "NODE_DEP", "NODE_WID"
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

                val token = LoginActivity.getSavedToken(requireContext()) ?: run {
                    Toast.makeText(requireContext(), getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 立即清除地圖暫存資料（新增模式不可回頭）
                val host = requireActivity() as? LocationPickerHost
                host?.onGutterSubmitted(validWaypoints)
                val activity = requireActivity()
                dismiss()

                // 呼叫 storeDitch（不帶 SPI_NUM，由後端分配）
                activity.lifecycleScope.launch {
                    try {
                        val request = buildStoreDitchRequest(validWaypoints, null)
                        android.util.Log.d("StoreDitch", "add request=$request")
                        when (val result = repository.storeDitch(request, token)) {
                            is ApiResult.Success -> {
                                val nodes = result.data.data?.nodes ?: emptyList()
                                (activity as? LocationPickerHost)
                                    ?.onGutterSaved(null, validWaypoints, nodes)
                            }
                            is ApiResult.Error -> {
                                android.util.Log.e(
                                    "StoreDitch",
                                    "add failed: message=${result.message}, code=${result.code}"
                                )
                                Toast.makeText(activity, getString(R.string.msg_upload_failed_saved_draft), Toast.LENGTH_LONG).show()
                                (activity as? LocationPickerHost)
                                    ?.onGutterSaveFailed(validWaypoints)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("StoreDitch", "add exception: ${e.message}", e)
                        Toast.makeText(activity, getString(R.string.msg_upload_failed_saved_draft), Toast.LENGTH_LONG).show()
                        (activity as? LocationPickerHost)
                            ?.onGutterSaveFailed(validWaypoints)
                    }
                }
            }
        }
    }

    /**
     * 供 MainActivity 在系統重建後辨識模式用：
     * true = 新增模式（activeSheet），false = 檢視模式（inspectSheet）。
     */
    fun isAddMode(): Boolean = !isInspectMode

    /** true = 正在編輯既有側溝（editSpiNum 非空）。 */
    fun isEditMode(): Boolean = editSpiNum.isNotEmpty()

    /** 若此 sheet 是從待上傳草稿恢復，回傳其 id；否則回傳 0。 */
    fun getRestoredDraftId(): Long = draftId

    /**
     * 執行編輯模式的更新流程（storeDitch with SPI_NUM）。
     * 可由 btnSubmitGutter 點擊觸發，也可由 MainActivity 在節點表單完成後自動觸發。
     * 呼叫時 sheet 可以是隱藏狀態，此方法會先 showSelf() 顯示進度，
     * 成功後 dismiss()、失敗後恢復按鈕狀態讓使用者重試。
     */
    fun performEditSubmit() {
        if (_binding == null) return
        val token = LoginActivity.getSavedToken(requireContext()) ?: run {
            showSelf()
            Toast.makeText(requireContext(), getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
            return
        }
        // 顯示 sheet 並鎖定按鈕，讓使用者看到「更新中」進度
        showSelf()
        binding.btnSubmitGutter.isEnabled = false
        binding.btnSubmitGutter.text = "更新中…"

        lifecycleScope.launch {
            try {
                if (isPreloadingEditDetails) {
                    Toast.makeText(requireContext(), getString(R.string.msg_waypoint_loading), Toast.LENGTH_SHORT).show()
                    updateSubmitButtonState()
                    return@launch
                }

                // 建立請求並呼叫 storeDitch（帶 SPI_NUM）
                val request = buildStoreDitchRequest(waypoints.toList(), editSpiNum)
                android.util.Log.d("StoreDitch", "edit request=$request")
                when (val result = repository.storeDitch(request, token)) {
                    is ApiResult.Success -> {
                        val nodes = result.data.data?.nodes ?: emptyList()
                        (requireActivity() as? LocationPickerHost)
                            ?.onUpdateGutter(waypoints.toList(), editSpiNum)
                        (requireActivity() as? LocationPickerHost)
                            ?.onGutterSaved(editSpiNum, waypoints.toList(), nodes)
                        dismiss()
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e(
                            "StoreDitch",
                            "edit failed: message=${result.message}, code=${result.code}"
                        )
                        Toast.makeText(
                            requireContext(),
                            "更新失敗：${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        // 更新失敗 → 存入待上傳草稿，讓使用者可以稍後重試
                        (requireActivity() as? LocationPickerHost)
                            ?.onGutterSaveFailed(waypoints.toList())
                        // 恢復按鈕狀態，讓使用者可以重試
                        updateSubmitButtonState()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("StoreDitch", "edit exception: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.msg_upload_failed_saved_draft), Toast.LENGTH_LONG).show()
                (requireActivity() as? LocationPickerHost)
                    ?.onGutterSaveFailed(waypoints.toList())
                updateSubmitButtonState()
            }
        }
    }

    // ── 調轉：整條側溝方向翻轉 ────────────────────────────────────────────
    /**
     * 將 waypoints 整體倒序排列（起點↔終點互換，節點順序全部翻轉），
     * 每個點位的座標與 basicData 隨著一起移動，不做任何內容修改。
     * 翻轉後呼叫 [renumberAll] 重新分配 type 與 label，並通知地圖刷新。
     */
    private fun reverseWaypoints() {
        waypoints.reverse()
        renumberAll()
    }

    // ── 依位置重新命名全部 waypoints ──────────────────────────────────────
    private fun renumberAll() {
        // notifyDataSetChanged 會重繪所有 item，translationX 回歸 0；
        // 若此時有展開的刪除按鈕，一律關閉（不需要動畫，直接重設）
        openedSwipePosition = -1
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
        if (isPreloadingEditDetails) return false
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
        val enabled = !isPreloadingEditDetails && hasEditChanges()
        binding.btnSubmitGutter.isEnabled = enabled
        val tint = if (enabled)
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
            val merged = HashMap(waypoints[index].basicData)
            merged.putAll(data)
            waypoints[index].basicData = merged
            adapter.notifyItemChanged(index)
            // 表單填寫完成後同樣通知 onWaypointsChanged，讓 MainActivity 觸發自動存檔
            onWaypointsChanged?.invoke(waypoints.toList())
            updateSubmitButtonState()
        }
    }

    private fun preloadEditWaypointDetails() {
        val token = LoginActivity.getSavedToken(requireContext()) ?: return
        isPreloadingEditDetails = true
        setEditLoading(true)

        lifecycleScope.launch {
            var hasError = false
            waypoints.forEachIndexed { index, wp ->
                val nodeId = wp.basicData["_nodeId"]?.toIntOrNull() ?: return@forEachIndexed
                when (val result = repository.getNodeDetails(nodeId, token)) {
                    is ApiResult.Success -> {
                        val nd = result.data.data?.firstOrNull() ?: return@forEachIndexed
                        val lat = nd.latitude?.toDoubleOrNull()
                        val lng = nd.longitude?.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            waypoints[index].latLng = LatLng(lat, lng)
                        }

                        val p1 = nd.nodeImg.firstOrNull { it.fileCategory == "1" }?.url ?: ""
                        val p2 = nd.nodeImg.firstOrNull { it.fileCategory == "2" }?.url ?: ""
                        val p3 = nd.nodeImg.firstOrNull { it.fileCategory == "3" }?.url ?: ""

                        val merged = HashMap(waypoints[index].basicData).apply {
                            put("_nodeId", nodeId.toString())
                            put("SPI_NUM", get("SPI_NUM") ?: editSpiNum)
                            put("NODE_TYP", nd.nodeTyP ?: get("NODE_TYP") ?: "")
                            put("MAT_TYP", nd.matTyp ?: get("MAT_TYP") ?: "")
                            put("NODE_X", nd.longitude ?: get("NODE_X") ?: "")
                            put("NODE_Y", nd.latitude ?: get("NODE_Y") ?: "")
                            put("NODE_LE", nd.nodeLe ?: get("NODE_LE") ?: "")
                            put("XY_NUM", nd.xyNum ?: get("XY_NUM") ?: "")
                            put("NODE_DEP", nd.nodeDepAsString.ifEmpty { get("NODE_DEP") ?: "" })
                            put("NODE_WID", nd.nodeWidAsString.ifEmpty { get("NODE_WID") ?: "" })
                            put("IS_CANTOPEN", if (nd.isCantOpenAsBoolean) "1" else "0")
                            put("IS_BROKEN", nd.isBroken ?: get("IS_BROKEN") ?: "")
                            put("IS_HANGING", nd.isHanging ?: get("IS_HANGING") ?: "")
                            put("IS_SILT", nd.isSilt ?: get("IS_SILT") ?: "")
                            put("NODE_NOTE", nd.note ?: get("NODE_NOTE") ?: "")
                            if (p1.isNotEmpty()) put("photo1", p1)
                            if (p2.isNotEmpty()) put("photo2", p2)
                            if (p3.isNotEmpty()) put("photo3", p3)
                        }
                        waypoints[index].basicData = merged
                    }
                    is ApiResult.Error -> {
                        hasError = true
                        android.util.Log.e(
                            "AddGutterSheet",
                            "preload node details failed: nodeId=$nodeId, message=${result.message}, code=${result.code}"
                        )
                    }
                }
            }

            adapter.notifyDataSetChanged()
            originalWaypointsSnapshot = takeWaypointSnapshot()
            isPreloadingEditDetails = false
            setEditLoading(false)
            onWaypointsChanged?.invoke(waypoints.toList())
            updateSubmitButtonState()

            if (hasError) {
                Toast.makeText(requireContext(), getString(R.string.msg_waypoint_load_partial_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setEditLoading(show: Boolean) {
        if (_binding == null) return
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnAddNode.isEnabled = !show
        binding.btnDeleteGutter.isEnabled = !show
        binding.rvWaypoints.isEnabled = !show
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

    // ── storeDitch 請求建構 ───────────────────────────────────────────────

    /**
     * 將 waypoints 轉換為 [StoreDitchRequest]。
     * 更新模式傳入 [spiNum]；新增模式傳入 null（讓後端分配）。
     */
    private fun buildStoreDitchRequest(
        waypoints: List<Waypoint>,
        spiNum: String? = null
    ): StoreDitchRequest {
        var nodeSequence = 1
        fun parseLooseBoolean(raw: String?): Boolean {
            val v = raw?.trim()?.lowercase()
            return when (v) {
                "1", "true", "t", "y", "yes" -> true
                else -> false
            }
        }

        return StoreDitchRequest(
            spiNum = spiNum,
            nodes = waypoints.map { wp ->
                val nodeAtt = when (wp.type) {
                    WaypointType.START -> 1
                    WaypointType.NODE  -> 2
                    WaypointType.END   -> 3
                }
                val isCantOpenBool = parseLooseBoolean(wp.basicData["IS_CANTOPEN"])
                val isCantOpenInt = if (isCantOpenBool) 1 else 0
                StoreDitchNodeRequest(
                    nodeId    = wp.basicData["_nodeId"]?.toIntOrNull(),
                    nodeAtt   = nodeAtt,
                    nodeNum   = if (nodeAtt == 2) nodeSequence++ else null,
                    nodeTyp   = wp.basicData["NODE_TYP"]?.toIntOrNull() ?: 1,
                    matTyp    = wp.basicData["MAT_TYP"]?.toIntOrNull() ?: 1,
                    latitude  = wp.latLng?.latitude  ?: 0.0,
                    longitude = wp.latLng?.longitude ?: 0.0,
                    nodeLe    = wp.basicData["NODE_LE"]?.toDoubleOrNull(),
                    xyNum     = wp.basicData["XY_NUM"] ?: "",
                    isCantOpen = isCantOpenInt,
                    nodeDep   = wp.basicData["NODE_DEP"]?.toIntOrNull() ?: 0,
                    nodeWid   = wp.basicData["NODE_WID"]?.toIntOrNull() ?: 0,
                    isBroken  = wp.basicData["IS_BROKEN"]?.toIntOrNull() ?: 0,
                    isHanging = wp.basicData["IS_HANGING"]?.toIntOrNull() ?: 0,
                    isSilt    = wp.basicData["IS_SILT"]?.toIntOrNull() ?: 0,
                    nodeNote  = wp.basicData["NODE_NOTE"]?.takeIf { it.isNotEmpty() }
                )
            }
        )
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
        fun newInstanceFromDraft(
            draft: GutterSessionDraft,
            forceOffline: Boolean = false
        ): AddGutterBottomSheet =
            AddGutterBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DRAFT_ID,   draft.id)
                    putString(ARG_DRAFT_JSON, Gson().toJson(draft))
                    // 只有「目前處於離線主模式」才強制離線 UI；
                    // draft.isOffline 代表草稿來源，回到線上時仍應允許上傳。
                    if (forceOffline) putBoolean(ARG_OFFLINE_MODE, true)
                }
            }
    }
}
