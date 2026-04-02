package com.example.taoyuangutter.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.SheetLayersBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LayersBottomSheet : BottomSheetDialogFragment() {

    interface Host {
        fun onLayerSelected(layer: String)
    }

    private var _binding: SheetLayersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetLayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
        isCancelable = true

        val selected = arguments?.getString(ARG_SELECTED_LAYER) ?: LAYER_EMAP
        updateRadio(selected)

        binding.rowNormalMap.setOnClickListener {
            (activity as? Host)?.onLayerSelected(LAYER_EMAP)
            dismissAllowingStateLoss()
        }
        binding.rowSatelliteMap.setOnClickListener {
            (activity as? Host)?.onLayerSelected(LAYER_PHOTO2)
            dismissAllowingStateLoss()
        }
    }

    private fun updateRadio(layer: String) {
        binding.icRadioNormal.setImageResource(
            if (layer == LAYER_EMAP) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
        )
        binding.icRadioSatellite.setImageResource(
            if (layer == LAYER_PHOTO2) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
        )
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.setCanceledOnTouchOutside(true)
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
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

    companion object {
        private const val ARG_SELECTED_LAYER = "selected_layer"

        const val LAYER_EMAP = "EMAP"
        const val LAYER_PHOTO2 = "PHOTO2"

        fun newInstance(selectedLayer: String): LayersBottomSheet =
            LayersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LAYER, selectedLayer)
                }
            }
    }
}
