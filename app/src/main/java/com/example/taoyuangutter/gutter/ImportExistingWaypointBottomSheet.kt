package com.example.taoyuangutter.gutter

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.databinding.BottomSheetImportExistingWaypointBinding
import com.example.taoyuangutter.login.LoginActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ImportExistingWaypointBottomSheet : BottomSheetDialogFragment() {

    interface Callbacks {
        /** Nearby 模式：需要地圖點擊來取得查詢中心點 */
        fun onMapPickModeChanged(enabled: Boolean)

        /** Nearby 模式：使用者點了地圖，先更新中心點 marker（附近 API 後續再接） */
        fun onNearbyCenterChanged(center: LatLng)

        /** 將候選點位更新到地圖 markers（全打） */
        fun onCandidateWaypointsChanged(items: List<NodeDetails>)

        /** 使用者選取某一筆候選點位，地圖需高亮並移鏡頭 */
        fun onWaypointSelected(item: NodeDetails?)

        /** 使用者按下匯入 */
        fun onImport(item: NodeDetails)

        /** BottomSheet 關閉（含滑下/按返回/按關閉） */
        fun onDismissed()

        /** 切換頁面時清除地圖點位（候選點/選取點） */
        fun onPageSwitched()
    }

    var callbacks: Callbacks? = null

    private var _binding: BottomSheetImportExistingWaypointBinding? = null
    private val binding get() = _binding!!

    private val gutterRepository = GutterRepository()
    private lateinit var adapter: ImportWaypointAdapter

    private var selected: NodeDetails? = null
    private var searchJob: Job? = null
    private var closestJob: Job? = null
    private var searchSeq: Int = 0
    private var closestSeq: Int = 0

    private enum class Page { NEARBY, SEARCH }
    private var currentPage: Page = Page.NEARBY

    private var routeToActivity: Boolean = false


    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent swipe-down / outside-tap / back-key dismissal; close only via the top-left button.
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetImportExistingWaypointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupTabs()
        setupSearch()
        setupButtons()

        // Default: Nearby
        renderPage(Page.NEARBY)

        // IME: move the whole sheet up so the search field is never covered
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.translationY = -kotlin.math.max(0, ime.bottom - nav.bottom).toFloat()
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    binding.root.translationY =
                        -kotlin.math.max(0, ime.bottom - nav.bottom).toFloat()
                    return insets
                }
            }
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            // Only allow closing via the top-left back button
            keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP
        }
        dialog.window?.apply {
            // Let the map area (outside this half-height window) remain touchable.
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
        }

        // Configure sheet behavior + touch routing before the dialog is shown (critical).
        dialog.setOnShowListener {
            val sheetView = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            val halfScreen = resources.displayMetrics.heightPixels / 2
            sheetView.layoutParams = sheetView.layoutParams.apply { height = halfScreen }
            sheetView.requestLayout()
            // Let our layout provide the rounded background.
            sheetView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            val behavior = BottomSheetBehavior.from(sheetView).apply {
                peekHeight = halfScreen
                state = BottomSheetBehavior.STATE_EXPANDED
                isHideable = false
                skipCollapsed = true
                isDraggable = false
            }

            // Extra guard: keep the sheet pinned (no collapsing/hidden states).
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            })

            // ── Map touch passthrough: Window.Callback routing (same approach as AddGutterBottomSheet) ──
            val originalCb = dialog.window?.callback ?: return@setOnShowListener
            dialog.window?.callback = object : Window.Callback by originalCb {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        val loc = IntArray(2)
                        sheetView.getLocationOnScreen(loc)
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
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        callbacks?.onMapPickModeChanged(false)
        callbacks?.onWaypointSelected(null)
        callbacks?.onCandidateWaypointsChanged(emptyList())
        callbacks?.onDismissed()
    }

    fun onMapClicked(latLng: LatLng) {
        if (currentPage != Page.NEARBY) return
        callbacks?.onNearbyCenterChanged(latLng)
        // 重新點地圖時才覆蓋清單；同時清掉目前選取
        clearSelection()
        performClosestSearch(latLng)
    }

    private fun setupRecycler() {
        adapter = ImportWaypointAdapter(
            rows = listOf(ImportWaypointAdapter.Row.State("請切換頁籤選擇點位")),
            onItemSelected = { item -> onSelected(item) }
        )
        binding.rvWaypoints.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWaypoints.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("附近點位"), true)
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("搜尋點位"), false)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val page = if (tab?.position == 1) Page.SEARCH else Page.NEARBY
                renderPage(page)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun renderPage(page: Page) {
        if (currentPage == Page.SEARCH && page == Page.NEARBY) {
            // Leaving search: cancel in-flight request to avoid late UI override
            searchJob?.cancel()
        }
        if (currentPage == Page.NEARBY && page == Page.SEARCH) {
            // Leaving nearby: cancel in-flight closest request
            closestJob?.cancel()
        }
        currentPage = page
        clearSelection()
        callbacks?.onPageSwitched()
        when (page) {
            Page.NEARBY -> {
                binding.searchBar.isVisible = false
                binding.tvNearbyHint.isVisible = true
                binding.tvNearbyHint.text = "請點選上方地圖以取得經緯度（附近查詢待接）"
                callbacks?.onMapPickModeChanged(true)
                // 不清列表；只清選取與地圖 marker
            }
            Page.SEARCH -> {
                binding.searchBar.isVisible = true
                binding.tvNearbyHint.isVisible = false
                callbacks?.onMapPickModeChanged(false)
                // 不清列表；只清選取與地圖 marker
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        binding.btnImport.isEnabled = false
        binding.btnImport.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.border_grey))
        binding.btnImport.setOnClickListener {
            val item = selected ?: return@setOnClickListener
            callbacks?.onImport(item)
            dismissAllowingStateLoss()
        }
    }

    private fun showHintState(message: String = "請切換頁籤選擇點位") {
        adapter.updateRows(listOf(ImportWaypointAdapter.Row.State(message)))
    }

    private fun showEmptyState() {
        adapter.updateRows(listOf(ImportWaypointAdapter.Row.State("無資料")))
    }

    private fun showErrorState(message: String) {
        adapter.updateRows(listOf(ImportWaypointAdapter.Row.State(message)))
    }

    private fun updateResultRows(results: List<NodeDetails>) {
        adapter.updateRows(results.map { ImportWaypointAdapter.Row.Waypoint(it) })
    }

    private fun clearSelection() {
        selected = null
        adapter.clearSelection()
        binding.btnImport.isEnabled = false
        binding.btnImport.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.border_grey))
        callbacks?.onWaypointSelected(null)
    }

    private fun onSelected(item: NodeDetails) {
        selected = item
        binding.btnImport.isEnabled = true
        binding.btnImport.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        callbacks?.onWaypointSelected(item)
    }

    private fun setLoading(visible: Boolean, message: String? = null) {
        binding.loadingOverlay.isVisible = visible
        if (!message.isNullOrBlank()) binding.tvLoading.text = message
    }

    private fun performClosestSearch(latLng: LatLng) {
        val ctx = requireContext()
        val token = LoginActivity.getSavedToken(ctx)
        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
            return
        }

        closestJob?.cancel()
        setLoading(true, "載入中…")
        binding.tvNearbyHint.text =
            "已選擇地圖位置：(${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)})"
        val seq = ++closestSeq

        closestJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = gutterRepository.getClosestNodeDetails(
                    lng = latLng.longitude,
                    lat = latLng.latitude,
                    token = token
                )
                if (seq != closestSeq) return@launch
                when (result) {
                    is ApiResult.Success -> {
                        val list = result.data.data ?: emptyList()
                        if (list.isEmpty()) {
                            showEmptyState()
                        } else {
                            updateResultRows(list)
                        }
                        // 初次不在地圖顯示任何候選點；選取後才縮放移動
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(ctx, "載入失敗：${result.message}", Toast.LENGTH_SHORT).show()
                        showErrorState("載入失敗：${result.message}")
                    }
                }
            } catch (e: CancellationException) {
                // Ignore.
            } catch (e: Exception) {
                Toast.makeText(ctx, "發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
                showErrorState("發生錯誤：${e.message ?: "未知錯誤"}")
            } finally {
                if (seq == closestSeq) setLoading(false)
            }
        }
    }

    private fun performSearch(rawQuery: String) {
        if (currentPage != Page.SEARCH) return
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            showHintState(message = "請輸入座標編號 (XY_NUM) 後按搜尋")
            return
        }

        val ctx = requireContext()
        val token = LoginActivity.getSavedToken(ctx)
        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
            return
        }

        searchJob?.cancel()
        clearSelection()
        setLoading(true, "載入中…")
        val seq = ++searchSeq

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = gutterRepository.getNodeDetailsByXyNum(query, token)
                if (seq != searchSeq) return@launch
                when (result) {
                    is ApiResult.Success -> {
                        val list = result.data.data ?: emptyList()
                        if (list.isEmpty()) {
                            showEmptyState()
                        } else {
                            updateResultRows(list)
                        }
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(ctx, "載入失敗：${result.message}", Toast.LENGTH_SHORT).show()
                        showErrorState("載入失敗：${result.message}")
                    }
                }
            } catch (e: CancellationException) {
                // Ignore.
            } catch (e: Exception) {
                Toast.makeText(ctx, "發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
                showErrorState("發生錯誤：${e.message ?: "未知錯誤"}")
            } finally {
                if (seq == searchSeq) setLoading(false)
            }
        }
    }
}
