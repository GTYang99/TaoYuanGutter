package com.example.taoyuangutter.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.SheetNoDitchReportBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.MotionEvent

class NoDitchReportBottomSheet : BottomSheetDialogFragment() {

    interface Host {
        fun onNoDitchRequestEnterPickMode()
        fun onNoDitchRequestExitPickMode()
        fun onNoDitchRequestResetPick()
        fun onNoDitchSubmitRequested(latLng: LatLng, note: String)
        fun onNoDitchRequestPickFromScreen(rawX: Float, rawY: Float)
    }

    private var _binding: SheetNoDitchReportBinding? = null
    private val binding get() = _binding!!

    private val vm: NoDitchReportViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        (context as? Host)?.onNoDitchRequestEnterPickMode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetNoDitchReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        (activity as? Host)?.onNoDitchRequestEnterPickMode()

        binding.etNote.setText(vm.note.value ?: "")

        binding.etNote.doAfterTextChanged { text ->
            vm.setNote(text?.toString().orEmpty())
            updateRightButtonEnabled()
        }

        binding.btnLeft.setOnClickListener {
            if (vm.selectedLatLng.value == null) {
                dismissAllowingStateLoss()
            } else {
                (activity as? Host)?.onNoDitchRequestResetPick()
                vm.clearPick()
                renderState()
            }
        }

        binding.btnRight.setOnClickListener {
            val latLng = vm.selectedLatLng.value ?: return@setOnClickListener
            val note = (vm.note.value ?: "").trim()
            if (note.isEmpty()) {
                binding.tilNote.error = getString(R.string.no_ditch_note_hint)
                return@setOnClickListener
            }
            binding.tilNote.error = null
            (activity as? Host)?.onNoDitchSubmitRequested(latLng, note)
        }

        renderState()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.setCanceledOnTouchOutside(false)
        dialog?.window?.apply {
            setDimAmount(0f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        // BottomSheetDialogFragment 會攔截地圖點擊；改用「點擊 touch_outside（地圖區域）」來取代 map click
        dialog?.window?.decorView
            ?.findViewById<View>(com.google.android.material.R.id.touch_outside)
            ?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    (activity as? Host)?.onNoDitchRequestPickFromScreen(event.rawX, event.rawY)
                }
                true
            }

        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(R.drawable.bg_form_sheet)
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.isFitToContents = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // 鍵盤彈出時，底部留出 IME inset，讓輸入框與按鈕隨鍵盤上推
            ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                val bottom = maxOf(imeBottom, sysBottom)
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
                insets
            }
            ViewCompat.requestApplyInsets(sheet)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (activity as? Host)?.onNoDitchRequestExitPickMode()
        vm.clearAll()
    }

    fun onLatLngPicked(latLng: LatLng) {
        if (vm.selectedLatLng.value != null) return
        vm.setPick(latLng)
        renderState()
    }

    fun setSubmitting(isSubmitting: Boolean) {
        vm.setSubmitting(isSubmitting)
        binding.btnLeft.isEnabled = !isSubmitting
        binding.btnRight.isEnabled = !isSubmitting && vm.canSubmit()
        binding.btnRight.alpha = if (binding.btnRight.isEnabled) 1f else 0.5f
    }

    private fun renderState() {
        val selected = vm.selectedLatLng.value
        val picked = selected != null
        binding.tvTitle.text = getString(if (picked) R.string.no_ditch_input_title else R.string.no_ditch_mode_title)
        binding.tvSubtitle.text = getString(if (picked) R.string.no_ditch_note_subtitle else R.string.no_ditch_pick_hint)
        binding.tvLatLng.visibility = if (picked) View.VISIBLE else View.GONE
        if (selected != null) {
            binding.tvLatLng.text = getString(R.string.no_ditch_latlng_format, selected.latitude, selected.longitude)
        }
        binding.tilNote.visibility = if (picked) View.VISIBLE else View.GONE
        binding.btnRight.visibility = if (picked) View.VISIBLE else View.GONE
        binding.btnLeft.text = getString(if (picked) R.string.no_ditch_reset else R.string.no_ditch_back)
        updateRightButtonEnabled()
    }

    private fun updateRightButtonEnabled() {
        val enabled = vm.canSubmit()
        binding.btnRight.isEnabled = enabled && (vm.isSubmitting.value != true)
        binding.btnRight.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (binding.btnRight.isEnabled) R.color.colorPrimary else R.color.textColorSecondary
            )
        )
    }

    companion object {
        const val TAG = "NoDitchReportBottomSheet"
    }
}
