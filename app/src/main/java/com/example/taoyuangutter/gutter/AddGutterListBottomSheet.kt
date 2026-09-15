package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.BottomSheetAddGutterListBinding
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddGutterListBottomSheet : BottomSheetDialogFragment() {
    interface Host {
        fun onAddGutterListAdd()
        fun onAddGutterListSelect(draft: GutterSessionDraft)
        fun onAddGutterListConfirmedClose()
    }

    var drafts: List<GutterSessionDraft> = emptyList()
    private var _binding: BottomSheetAddGutterListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AddGutterListAdapter

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = BottomSheetAddGutterListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        adapter = AddGutterListAdapter { draft -> (parentFragment as? Host)?.onAddGutterListSelect(draft) }
        binding.rvAddGutterList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAddGutterList.adapter = adapter
        adapter.submitList(drafts)
        binding.btnAddGutterListAdd.setOnClickListener { (parentFragment as? Host)?.onAddGutterListAdd() }
        binding.btnAddGutterListClose.setOnClickListener { confirmClose() }
    }

    private fun confirmClose() {
        // The alert is about leaving the current add-list session, not about
        // whether every row already has enough data to be persisted.
        if (drafts.isEmpty()) {
            (parentFragment as? Host)?.onAddGutterListConfirmedClose()
            dismissAllowingStateLoss()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("未上傳側溝草稿將儲存到草稿中")
            .setNegativeButton("取消", null)
            .setPositiveButton("確定") { _, _ ->
                (parentFragment as? Host)?.onAddGutterListConfirmedClose()
                dismissAllowingStateLoss()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val height = (resources.displayMetrics.heightPixels * 0.8f).toInt()
        bottomSheet.layoutParams.height = height
        bottomSheet.requestLayout()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        dialog?.window?.setDimAmount(0f)
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        confirmClose()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
