package com.example.taoyuangutter.pending

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taoyuangutter.databinding.BottomSheetPendingDraftsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 顯示待上傳側溝草稿的 BottomSheet。
 *
 * 點選 cell → 三選一對話框（繼續編輯 / 刪除 / 取消）。
 * 點選「刪除」→ 二次確認對話框。
 * 點選「繼續編輯」→ 呼叫 [onResumeDraft] 回呼，由 MainActivity 負責建立並展示 AddGutterBottomSheet。
 */
class PendingDraftsBottomSheet : BottomSheetDialogFragment() {

    /** 當使用者選擇「繼續編輯」時，將選定的草稿傳回 MainActivity。 */
    var onResumeDraft: ((GutterSessionDraft) -> Unit)? = null

    private var _binding: BottomSheetPendingDraftsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo:    GutterSessionRepository
    private lateinit var adapter: PendingDraftAdapter

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPendingDraftsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = GutterSessionRepository(requireContext())

        binding.btnPendingClose.setOnClickListener { dismiss() }

        applyWindowInsets()
        setupRecyclerView()
        refreshList()
    }

    override fun onStart() {
        super.onStart()
        val bsDialog  = dialog as? BottomSheetDialog ?: return
        val sheetView = bsDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        // 固定半螢幕高度：頂端剛好在畫面正中央
        val halfScreen = resources.displayMetrics.heightPixels / 2

        sheetView.layoutParams?.height = halfScreen
        sheetView.requestLayout()
        // 清除 design_bottom_sheet 容器的預設背景，讓 bg_form_sheet 圓角正常顯示
        sheetView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        bsDialog.behavior.apply {
            peekHeight    = halfScreen
            state         = BottomSheetBehavior.STATE_EXPANDED
            isHideable    = true
            skipCollapsed = true
        }

        // 半透明遮罩，讓地圖背景稍微變暗，凸顯操作介面
        bsDialog.window?.setDimAmount(0.5f)
    }

    /**
     * 套用 navigation bar inset 作為底部間距，
     * 避免清單最後一項被 Home indicator 遮住。
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.pendingDraftsRoot) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.safeAreaSpacer.layoutParams =
                binding.safeAreaSpacer.layoutParams.also { it.height = navBarHeight }
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── RecyclerView ─────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = PendingDraftAdapter(
            items           = mutableListOf(),
            onItemClick     = { draft -> resumeDraft(draft) },
            onItemLongClick = { draft -> showDraftActionDialog(draft) }
        )
        binding.rvPendingDrafts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PendingDraftsBottomSheet.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun refreshList() {
        val drafts = repo.getAll()
        adapter.items.clear()
        adapter.items.addAll(drafts)
        adapter.notifyDataSetChanged()

        val isEmpty = drafts.isEmpty()
        binding.tvPendingEmpty.visibility    = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPendingDrafts.visibility   = if (isEmpty) View.GONE    else View.VISIBLE
    }

    // ── 對話框 ────────────────────────────────────────────────────────────

    /**
     * 長按 cell 後的對話框：刪除 / 取消。
     */
    private fun showDraftActionDialog(draft: GutterSessionDraft) {
        val dialog = AlertDialog.Builder(requireContext())
            .setItems(arrayOf("刪除草稿", "取消")) { _, which ->
                when (which) {
                    0 -> showDeleteConfirmDialog(draft)
                    // 1 → 取消，不做任何事
                }
            }
            .create()

        dialog.show()

        // 將「刪除草稿」設為紅色
        dialog.listView?.getChildAt(0)?.let { itemView ->
            if (itemView is android.widget.TextView) {
                itemView.setTextColor(Color.parseColor("#D32F2F"))
            }
        }
    }

    /**
     * 刪除確認對話框。
     */
    private fun showDeleteConfirmDialog(draft: GutterSessionDraft) {
        AlertDialog.Builder(requireContext())
            .setTitle("刪除確認")
            .setMessage("請確認是否刪除此份草稿？")
            .setPositiveButton("刪除") { _, _ ->
                repo.delete(draft.id)
                adapter.removeItem(draft)
                if (adapter.itemCount == 0) {
                    binding.tvPendingEmpty.visibility  = View.VISIBLE
                    binding.rvPendingDrafts.visibility = View.GONE
                }
            }
            .setNegativeButton("取消", null)
            .show()
            .also { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(Color.parseColor("#D32F2F"))
            }
    }

    /**
     * 繼續編輯：直接呼叫回呼，由 MainActivity 負責先 dismiss 此 sheet
     * 再以 post 展示 AddGutterBottomSheet，避免兩個 FragmentTransaction 同時競爭。
     */
    private fun resumeDraft(draft: GutterSessionDraft) {
        onResumeDraft?.invoke(draft)
    }

    // ── Companion ────────────────────────────────────────────────────────

    companion object {
        const val TAG = "PendingDraftsBottomSheet"

        fun newInstance() = PendingDraftsBottomSheet()
    }
}
