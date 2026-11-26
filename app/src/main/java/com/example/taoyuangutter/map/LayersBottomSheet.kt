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
        fun onOverlayTogglesChanged(
            showPlan: Boolean,
            showWaterOld: Boolean,
            showPossible: Boolean,
            showRegion: Boolean,
            showNoDitchPoints: Boolean,
            showDeletedArea: Boolean,
            selectedDistricts: Set<String> = DEFAULT_DISTRICTS
        ) { /* optional */ }
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
        val showNoDitchPoints = arguments?.getBoolean(ARG_SHOW_NO_DITCH_POINTS, false) ?: false
        val showDeletedArea = arguments?.getBoolean(ARG_SHOW_DELETED_AREA, false) ?: false
        var selectedDistrictsState = arguments?.getStringArrayList(ARG_SELECTED_DISTRICTS)?.toSet() ?: DEFAULT_DISTRICTS
        var suppressAllListener = false
        updateBasemapUi(selected)

        // Fill layer codes
        binding.tvEMapCode.text = getString(R.string.layers_sheet_layer_code, LAYER_EMAP)
        binding.tvPhoto2Code.text = getString(R.string.layers_sheet_layer_code, LAYER_PHOTO2)
        binding.tvEMapGrayCode.text = getString(R.string.layers_sheet_layer_code, LAYER_EMAP01)

        // Overlays (UI only for now; map behavior can be wired later)
        fun dispatchOverlayToggles() {
            host()?.onOverlayTogglesChanged(
                showPlan = binding.cbPlan.isChecked,
                showWaterOld = binding.cbWaterOld.isChecked,
                showPossible = binding.cbPossible.isChecked,
                showRegion = binding.cbRegion.isChecked,
                showNoDitchPoints = binding.cbNoDitchPoints.isChecked,
                showDeletedArea = binding.cbDeletedArea.isChecked,
                selectedDistricts = selectedDistrictsState
            )
        }
        // Apply initial state from args before wiring listeners.
        binding.cbPlan.isChecked = showPlan
        binding.cbWaterOld.isChecked = showWaterOld
        binding.cbPossible.isChecked = showPossible
        binding.cbRegion.isChecked = showRegion
        binding.cbNoDitchPoints.isChecked = showNoDitchPoints
        binding.cbDeletedArea.isChecked = showDeletedArea
        val districtChecks = listOf(
            binding.cbDistrict0, binding.cbDistrict1, binding.cbDistrict2, binding.cbDistrict3,
            binding.cbDistrict4, binding.cbDistrict5, binding.cbDistrict6, binding.cbDistrict7,
            binding.cbDistrict8, binding.cbDistrict9, binding.cbDistrict10, binding.cbDistrict11,
            binding.cbDistrict12
        )
        fun bindDistrictChecks() {
            districtChecks.forEachIndexed { index, checkBox ->
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = DISTRICTS[index] in selectedDistrictsState
                checkBox.setOnCheckedChangeListener { _, checked ->
                    selectedDistrictsState = if (checked) {
                        selectedDistrictsState + DISTRICTS[index]
                    } else {
                        selectedDistrictsState - DISTRICTS[index]
                    }
                    suppressAllListener = true
                    binding.cbDistrictAll.setCheckedState(allCheckState(selectedDistrictsState))
                    suppressAllListener = false
                    dispatchOverlayToggles()
                }
            }
        }
        bindDistrictChecks()
        binding.cbDistrictAll.setCheckedState(allCheckState(selectedDistrictsState))
        binding.cbDistrictAll.setOnCheckedChangeListener { _, checked ->
            if (suppressAllListener) return@setOnCheckedChangeListener
            selectedDistrictsState = if (checked) DEFAULT_DISTRICTS else emptySet()
            bindDistrictChecks()
            dispatchOverlayToggles()
        }
        binding.cbPlan.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbWaterOld.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbPossible.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbRegion.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbNoDitchPoints.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        binding.cbDeletedArea.setOnCheckedChangeListener { _, _ -> dispatchOverlayToggles() }
        dispatchOverlayToggles()

        binding.cardEMap.setOnClickListener { selectBasemap(LAYER_EMAP) }
        binding.cardPhoto2.setOnClickListener { selectBasemap(LAYER_PHOTO2) }
        binding.cardEMapGray.setOnClickListener { selectBasemap(LAYER_EMAP01) }
    }

    private fun selectBasemap(layer: String) {
        updateBasemapUi(layer)
        host()?.onLayerSelected(layer)
    }

    private fun host(): Host? {
        return parentFragment as? Host ?: activity as? Host
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

    private fun allCheckState(selectedDistricts: Set<String>): Int = when (
        districtSelectionState(selectedDistricts.size, DISTRICTS.size)
    ) {
        DistrictSelectionState.NONE -> com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
        DistrictSelectionState.ALL -> com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
        DistrictSelectionState.PARTIAL -> com.google.android.material.checkbox.MaterialCheckBox.STATE_INDETERMINATE
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
        private const val ARG_SHOW_NO_DITCH_POINTS = "show_no_ditch_points"
        private const val ARG_SHOW_DELETED_AREA = "show_deleted_area"
        private const val ARG_SELECTED_DISTRICTS = "selected_districts"

        private val DISTRICTS = listOf(
            "桃園區", "中壢區", "大溪區", "楊梅區", "蘆竹區", "大園區", "龜山區",
            "八德區", "龍潭區", "平鎮區", "新屋區", "觀音區", "復興區"
        )
        private val DEFAULT_DISTRICTS = DISTRICTS.toSet()

        const val LAYER_EMAP = "EMAP"
        const val LAYER_EMAP01 = "EMAP01"
        const val LAYER_PHOTO2 = "PHOTO2"

        fun newInstance(
            selectedLayer: String,
            showPlan: Boolean = true,
            showWaterOld: Boolean = true,
            showPossible: Boolean = true,
            showRegion: Boolean = true,
            showNoDitchPoints: Boolean = false,
            showDeletedArea: Boolean = false,
            selectedDistricts: Set<String> = DEFAULT_DISTRICTS
        ): LayersBottomSheet =
            LayersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LAYER, selectedLayer)
                    putBoolean(ARG_SHOW_PLAN, showPlan)
                    putBoolean(ARG_SHOW_WATER_OLD, showWaterOld)
                    putBoolean(ARG_SHOW_POSSIBLE, showPossible)
                    putBoolean(ARG_SHOW_REGION, showRegion)
                    putBoolean(ARG_SHOW_NO_DITCH_POINTS, showNoDitchPoints)
                    putBoolean(ARG_SHOW_DELETED_AREA, showDeletedArea)
                    putStringArrayList(ARG_SELECTED_DISTRICTS, ArrayList(selectedDistricts))
                }
            }
    }
}
