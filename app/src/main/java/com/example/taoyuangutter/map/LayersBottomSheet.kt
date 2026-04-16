package com.example.taoyuangutter.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.view.ViewOutlineProvider
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.SheetLayersBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LayersBottomSheet : BottomSheetDialogFragment() {

    interface Host {
        fun onLayerSelected(layer: String)
        fun onOverlayTogglesChanged(showPlan: Boolean, showWaterOld: Boolean, showPossible: Boolean, showRegion: Boolean) { /* optional */ }
    }

    private var _binding: SheetLayersBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetLayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
        isCancelable = true

        val selected = arguments?.getString(ARG_SELECTED_LAYER) ?: LAYER_EMAP
        val showPlan = arguments?.getBoolean(ARG_SHOW_PLAN, true) ?: true
        val showWaterOld = arguments?.getBoolean(ARG_SHOW_WATER_OLD, true) ?: true
        val showPossible = arguments?.getBoolean(ARG_SHOW_POSSIBLE, true) ?: true
        val showRegion = arguments?.getBoolean(ARG_SHOW_REGION, true) ?: true
        updateBasemapUi(selected)

        // Fill layer codes
        binding.tvEMapCode.text = getString(R.string.layers_sheet_layer_code, LAYER_EMAP)
        binding.tvPhoto2Code.text = getString(R.string.layers_sheet_layer_code, LAYER_PHOTO2)
        binding.tvEMapGrayCode.text = getString(R.string.layers_sheet_layer_code, LAYER_EMAP01)

        // Overlays (UI only for now; map behavior can be wired later)
        fun dispatchOverlayToggles() {
            (activity as? Host)?.onOverlayTogglesChanged(
                showPlan = binding.cbPlan.isChecked,
                showWaterOld = binding.cbWaterOld.isChecked,
                showPossible = binding.cbPossible.isChecked,
                showRegion = binding.cbRegion.isChecked
            )
        }
        // Apply initial state from args before wiring listeners.
        binding.cbPlan.isChecked = showPlan
        binding.cbWaterOld.isChecked = showWaterOld
        binding.cbPossible.isChecked = showPossible
        binding.cbRegion.isChecked = showRegion
        binding.cbPlan.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbWaterOld.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbPossible.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbRegion.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        dispatchOverlayToggles()

        binding.cardEMap.setOnClickListener { selectBasemap(LAYER_EMAP) }
        binding.cardPhoto2.setOnClickListener { selectBasemap(LAYER_PHOTO2) }
        binding.cardEMapGray.setOnClickListener { selectBasemap(LAYER_EMAP01) }
    }

    private fun selectBasemap(layer: String) {
        updateBasemapUi(layer)
        (activity as? Host)?.onLayerSelected(layer)
    }

    private fun updateBasemapUi(selected: String) {
        val primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val border = ContextCompat.getColor(requireContext(), R.color.border_grey)
        val bg = ContextCompat.getColor(requireContext(), R.color.colorBackground)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        fun applySelected(card: com.google.android.material.card.MaterialCardView, icon: android.widget.ImageView, isSelected: Boolean) {
            card.strokeColor = if (isSelected) primary else border
            card.setCardBackgroundColor(if (isSelected) white else bg)
            icon.setImageResource(if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
        }

        applySelected(binding.cardEMap, binding.ivEMapRadio, selected == LAYER_EMAP)
        applySelected(binding.cardPhoto2, binding.ivPhoto2Radio, selected == LAYER_PHOTO2)
        applySelected(binding.cardEMapGray, binding.ivEMapGrayRadio, selected == LAYER_EMAP01)
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
        private const val ARG_SHOW_PLAN = "show_plan"
        private const val ARG_SHOW_WATER_OLD = "show_water_old"
        private const val ARG_SHOW_POSSIBLE = "show_possible"
        private const val ARG_SHOW_REGION = "show_region"

        const val LAYER_EMAP = "EMAP"
        const val LAYER_EMAP01 = "EMAP01"
        const val LAYER_PHOTO2 = "PHOTO2"

        fun newInstance(
            selectedLayer: String,
            showPlan: Boolean = true,
            showWaterOld: Boolean = true,
            showPossible: Boolean = true,
            showRegion: Boolean = true
        ): LayersBottomSheet =
            LayersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LAYER, selectedLayer)
                    putBoolean(ARG_SHOW_PLAN, showPlan)
                    putBoolean(ARG_SHOW_WATER_OLD, showWaterOld)
                    putBoolean(ARG_SHOW_POSSIBLE, showPossible)
                    putBoolean(ARG_SHOW_REGION, showRegion)
                }
            }
    }
}
