package com.example.taoyuangutter.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.SheetLegendBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LegendBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetLegendBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetLegendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
        isCancelable = true
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.setCanceledOnTouchOutside(true)
        // Fit height to content (unless content exceeds screen, then it will scroll).
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            // Make top-left/top-right rounded corners visible on the actual bottom sheet container.
            sheet.setBackgroundResource(R.drawable.bg_form_sheet)
            sheet.clipToOutline = true
            sheet.outlineProvider = ViewOutlineProvider.BACKGROUND

            sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.isFitToContents = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
