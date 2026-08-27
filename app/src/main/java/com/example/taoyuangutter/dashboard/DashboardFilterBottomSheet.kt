package com.example.taoyuangutter.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.DashboardQuery
import com.example.taoyuangutter.databinding.SheetDashboardFilterBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar
import java.util.Locale

class DashboardFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDashboardFilterBinding? = null
    private val binding get() = _binding!!

    private var mode: FilterMode? = null
    private var startDate: String = ""
    private var endDate: String = ""
    private var monthYear: String = ""
    private var month: String = ""

    var onApply: ((DashboardQuery) -> Unit)? = null
    var onClear: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDashboardFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
        binding.toggleMode.clearChecked()
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                mode = null
                renderMode()
                return@addOnButtonCheckedListener
            }
            mode = when (checkedId) {
                binding.btnDateMode.id -> FilterMode.DATE
                binding.btnMonthMode.id -> FilterMode.MONTH
                else -> null
            }
            renderMode()
        }
        binding.btnStartDate.setOnClickListener { pickDate { startDate = it; ensureDateMode(); renderMode() } }
        binding.btnEndDate.setOnClickListener { pickDate { endDate = it; ensureDateMode(); renderMode() } }
        binding.btnMonth.setOnClickListener { pickMonth { year, selectedMonth ->
            monthYear = year
            month = selectedMonth
            ensureMonthMode()
            renderMode()
        } }
        binding.btnClear.setOnClickListener {
            clearSelection()
            onClear?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnApply.setOnClickListener {
            when (mode) {
                FilterMode.DATE -> {
                    if (startDate.isNotBlank() && endDate.isNotBlank()) {
                        onApply?.invoke(
                            DashboardQuery(
                                startDate = startDate,
                                endDate = endDate
                            )
                        )
                        dismissAllowingStateLoss()
                    }
                }
                FilterMode.MONTH -> {
                    if (monthYear.isNotBlank() && month.isNotBlank()) {
                        onApply?.invoke(
                            DashboardQuery(
                                monthYear = monthYear,
                                month = month
                            )
                        )
                        dismissAllowingStateLoss()
                    }
                }
                null -> Unit
            }
        }

        renderMode()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
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

    private fun renderMode() {
        binding.tvEmptyHint.visibility = if (mode == null) View.VISIBLE else View.GONE
        binding.dateSection.visibility = if (mode == FilterMode.DATE) View.VISIBLE else View.GONE
        binding.monthSection.visibility = if (mode == FilterMode.MONTH) View.VISIBLE else View.GONE
        binding.btnApply.isEnabled = when (mode) {
            FilterMode.DATE -> startDate.isNotBlank() && endDate.isNotBlank()
            FilterMode.MONTH -> monthYear.isNotBlank() && month.isNotBlank()
            null -> false
        }
        binding.btnStartDate.text = if (startDate.isBlank()) {
            getString(R.string.dashboard_pick_start_date)
        } else startDate
        binding.btnEndDate.text = if (endDate.isBlank()) {
            getString(R.string.dashboard_pick_end_date)
        } else endDate
        binding.btnMonth.text = if (monthYear.isBlank() || month.isBlank()) {
            getString(R.string.dashboard_pick_month)
        } else {
            String.format(Locale.getDefault(), "%s-%s", monthYear, month)
        }
    }

    private fun clearSelection() {
        mode = null
        startDate = ""
        endDate = ""
        monthYear = ""
        month = ""
        binding.toggleMode.clearChecked()
        renderMode()
    }

    private fun ensureDateMode() {
        if (mode != FilterMode.DATE) {
            mode = FilterMode.DATE
            binding.toggleMode.check(binding.btnDateMode.id)
        }
    }

    private fun ensureMonthMode() {
        if (mode != FilterMode.MONTH) {
            mode = FilterMode.MONTH
            binding.toggleMode.check(binding.btnMonthMode.id)
        }
    }

    private fun pickDate(onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                onSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickMonth(onSelected: (String, String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                onSelected(year.toString(), String.format(Locale.getDefault(), "%02d", month + 1))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    companion object {
        fun newInstance(): DashboardFilterBottomSheet = DashboardFilterBottomSheet()
    }

    private enum class FilterMode {
        DATE,
        MONTH
    }
}
