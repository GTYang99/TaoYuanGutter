package com.example.taoyuangutter.dashboard

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.DashboardLengthGroup
import com.example.taoyuangutter.api.DashboardProgressGroup
import com.example.taoyuangutter.api.DashboardProgressSummary
import com.example.taoyuangutter.api.DashboardSlice
import com.example.taoyuangutter.databinding.ActivityDashboardBinding
import com.example.taoyuangutter.login.AuthNavigator
import com.example.taoyuangutter.login.LoginActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private var handledAuthError = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        collectState()
        LoginActivity.getSavedToken(requireContext())?.let { viewModel.loadDashboard(it) }
            ?: AuthNavigator(requireContext()).clearAuthAndGoLogin()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupActions() {
        binding.btnLengthSettings.setOnClickListener { showLengthSettingsDialog() }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
        binding.btnProgressGroup.setOnClickListener { showProgressGroupDialog() }
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        render(state)
                        if (state.errorCode == 401 && !handledAuthError) {
                            handledAuthError = true
                            AuthNavigator(requireContext()).clearAuthAndGoLogin()
                            viewModel.consumeAuthError()
                        } else if (state.errorCode != 401) {
                            handledAuthError = false
                        }
                    }
                }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage.orEmpty()
        binding.tvError.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvTitle.text = getString(R.string.dashboard_title)
        binding.btnProgressGroup.text = state.selectedProgressGroup
        binding.tvLengthDetailTitle.text = state.selectedLengthDetailGroup?.let { "$it 帳號明細" }
            ?: getString(R.string.dashboard_length_detail_hint)

        renderLengthSummary(state)
        renderLengthGroups(state)
        renderLengthDetails(state)
        renderProgressSummary(state)
        renderProgressIssues(state)
    }

    private fun renderLengthSummary(state: DashboardUiState) {
        val total = state.response?.surveyLength?.get(DashboardViewModel.TOTAL_GROUP_KEY)
        val totalLength = total?.totalLength ?: "0"
        binding.tvLengthTotalGroup.text = getString(R.string.dashboard_total_group)
        binding.tvLengthTotalValue.text = totalLength
        binding.tvLengthTotalUnit.text = getString(R.string.dashboard_km_unit)
    }

    private fun renderLengthGroups(state: DashboardUiState) {
        binding.lengthGroupContainer.removeAllViews()
        val data = state.response?.surveyLength.orEmpty()
        val selected = if (state.selectedLengthGroups.isEmpty()) {
            state.availableLengthGroups.toSet()
        } else {
            state.selectedLengthGroups
        }
        val cards = selected.mapNotNull { groupName ->
            data[groupName]?.let { groupName to it }
        }

        if (cards.isEmpty()) {
            val empty = createEmptyStateText(getString(R.string.dashboard_no_group_data))
            binding.lengthGroupContainer.addView(empty)
            return
        }

        cards.chunked(2).forEach { rowItems ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            }
            rowItems.forEach { (name, group) ->
                row.addView(buildLengthGroupCard(name, group, selected = name == state.selectedLengthDetailGroup))
            }
            if (rowItems.size == 1) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
                row.addView(spacer)
            }
            binding.lengthGroupContainer.addView(row)
        }
    }

    private fun renderLengthDetails(state: DashboardUiState) {
        binding.lengthDetailContainer.removeAllViews()
        val data = state.response?.surveyLength.orEmpty()
        val groupName = state.selectedLengthDetailGroup
        val group = if (groupName.isNullOrBlank()) null else data[groupName]
        if (group == null) {
            binding.lengthDetailContainer.addView(createEmptyStateText(getString(R.string.dashboard_no_detail_data)))
            return
        }
        if (group.accounts.isEmpty()) {
            binding.lengthDetailContainer.addView(createEmptyStateText(getString(R.string.dashboard_no_account_data)))
            return
        }
        val entries = group.accounts.entries.sortedBy { it.key }
        entries.forEachIndexed { index, entry ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(4), dp(10), dp(4), dp(10))
            }
            val name = TextView(requireContext()).apply {
                text = entry.key
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                textSize = 16f
            }
            val value = TextView(requireContext()).apply {
                text = "${entry.value} ${getString(R.string.dashboard_km_unit)}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary))
                textSize = 16f
            }
            row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(value)
            binding.lengthDetailContainer.addView(row)
            if (index != entries.lastIndex) {
                binding.lengthDetailContainer.addView(divider())
            }
        }
    }

    private fun renderProgressSummary(state: DashboardUiState) {
        val progress = state.response?.surveyProgress?.get(state.selectedProgressGroup)
            ?: state.response?.surveyProgress?.get(DashboardViewModel.TOTAL_GROUP_KEY)
            ?: DashboardProgressGroup()
        val summary = DashboardProgressSummary(
            totalCount = progress.totalCount,
            pendingImport = progress.pendingImport,
            pendingDrawing = progress.pendingDrawing,
            pendingFix = progress.pendingFix,
            completed = progress.completed,
            other = progress.other
        )

        binding.tvProgressTotalValue.text = summary.totalCount.toString()
        binding.tvProgressTotalUnit.text = getString(R.string.dashboard_item_unit)

        bindProgressRow(binding.tvPendingImportCount, binding.tvPendingImportRate, "待匯入座標", summary.pendingImport, summary.totalCount, R.color.dashboard_pending_import)
        bindProgressRow(binding.tvPendingDrawingCount, binding.tvPendingDrawingRate, "待繪製", summary.pendingDrawing, summary.totalCount, R.color.dashboard_pending_drawing)
        bindProgressRow(binding.tvPendingFixCount, binding.tvPendingFixRate, "待修正", summary.pendingFix, summary.totalCount, R.color.dashboard_pending_fix)
        bindProgressRow(binding.tvCompletedCount, binding.tvCompletedRate, "已完成", summary.completed, summary.totalCount, R.color.dashboard_completed)

        val slices = listOf(
            DashboardSlice("待匯入座標", summary.pendingImport, ContextCompat.getColor(requireContext(), R.color.dashboard_pending_import)),
            DashboardSlice("待繪製", summary.pendingDrawing, ContextCompat.getColor(requireContext(), R.color.dashboard_pending_drawing)),
            DashboardSlice("待修正", summary.pendingFix, ContextCompat.getColor(requireContext(), R.color.dashboard_pending_fix)),
            DashboardSlice("已完成", summary.completed, ContextCompat.getColor(requireContext(), R.color.dashboard_completed))
        )
        binding.pieChart.setSlices(slices)
    }

    private fun renderProgressIssues(state: DashboardUiState) {
        val progress = state.response?.surveyProgress?.get(state.selectedProgressGroup)
            ?: state.response?.surveyProgress?.get(DashboardViewModel.TOTAL_GROUP_KEY)
            ?: DashboardProgressGroup()
        val rows = listOf(
            "淤積程度 - 輕度" to (progress.other["淤積_輕度"] ?: 0),
            "淤積程度 - 嚴重" to (progress.other["淤積_嚴重"] ?: 0),
            "溝體結構受損" to (progress.other["溝體結構受損"] ?: 0),
            "附掛或過路管線" to (progress.other["附掛或過路管線"] ?: 0)
        )
        binding.issueContainer.removeAllViews()
        rows.forEachIndexed { index, (label, value) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(4), dp(10), dp(4), dp(10))
            }
            row.addView(TextView(requireContext()).apply {
                text = label
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(requireContext()).apply {
                text = "$value ${getString(R.string.dashboard_item_unit)}"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            binding.issueContainer.addView(row)
            if (index != rows.lastIndex) {
                binding.issueContainer.addView(divider())
            }
        }
    }

    private fun bindProgressRow(
        countView: TextView,
        rateView: TextView,
        label: String,
        count: Int,
        total: Int,
        colorRes: Int
    ) {
        countView.text = count.toString()
        rateView.text = if (total <= 0) "0%" else "${((count * 100f) / total).toInt()}%"
        rateView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun buildLengthGroupCard(name: String, group: DashboardLengthGroup, selected: Boolean): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(requireContext(), if (selected) R.color.colorPrimary else R.color.map_borders_grey)
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), if (selected) R.color.brand_purple_light else android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
            setOnClickListener {
                viewModel.selectLengthDetailGroup(name)
            }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        content.addView(TextView(requireContext()).apply {
            text = name
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(requireContext()).apply {
            text = group.totalLength
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(requireContext()).apply {
            text = getString(R.string.dashboard_km_unit)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary))
            textSize = 12f
        })
        card.addView(content)
        return card
    }

    private fun createEmptyStateText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary))
            textSize = 14f
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
    }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
        }
        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.border_grey))
    }

    private fun showLengthSettingsDialog() {
        val groups = viewModel.uiState.value.availableLengthGroups
        if (groups.isEmpty()) {
            return
        }
        val checks = linkedMapOf<String, CheckBox>()
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        groups.forEach { group ->
            val checkBox = CheckBox(requireContext()).apply {
                text = group
                isChecked = viewModel.uiState.value.selectedLengthGroups.isEmpty() ||
                    group in viewModel.uiState.value.selectedLengthGroups
            }
            checks[group] = checkBox
            container.addView(checkBox)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dashboard_length_settings_title))
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.selectLengthGroups(checks.filterValues { it.isChecked }.keys)
            }
            .show()
    }

    private fun showProgressGroupDialog() {
        val groups = listOf(DashboardViewModel.TOTAL_GROUP_KEY) + viewModel.uiState.value.availableProgressGroups.filterNot { it == DashboardViewModel.TOTAL_GROUP_KEY }
        if (groups.isEmpty()) return
        val checkedIndex = groups.indexOf(viewModel.uiState.value.selectedProgressGroup).takeIf { it >= 0 } ?: 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dashboard_progress_group_title))
            .setSingleChoiceItems(groups.toTypedArray(), checkedIndex) { dialog, which ->
                viewModel.selectProgressGroup(groups[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val state = viewModel.uiState.value.query
        var selectedStart = state.startDate.orEmpty()
        var selectedEnd = state.endDate.orEmpty()
        var selectedYear = state.monthYear.orEmpty()
        var selectedMonth = state.month.orEmpty()
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }
        val modeGroup = RadioGroup(requireContext())
        val dateRadio = android.widget.RadioButton(requireContext()).apply {
            text = getString(R.string.dashboard_filter_date_range)
        }
        val monthRadio = android.widget.RadioButton(requireContext()).apply {
            text = getString(R.string.dashboard_filter_month_range)
        }
        modeGroup.addView(dateRadio)
        modeGroup.addView(monthRadio)
        container.addView(modeGroup)

        val startDate = MaterialTextView(requireContext()).apply {
            text = state.startDate ?: getString(R.string.dashboard_pick_start_date)
            setPadding(0, dp(12), 0, dp(12))
        }
        val endDate = MaterialTextView(requireContext()).apply {
            text = state.endDate ?: getString(R.string.dashboard_pick_end_date)
            setPadding(0, dp(12), 0, dp(12))
        }
        val monthValue = MaterialTextView(requireContext()).apply {
            text = if (!state.monthYear.isNullOrBlank() && !state.month.isNullOrBlank()) {
                "${state.monthYear}-${state.month}"
            } else {
                getString(R.string.dashboard_pick_month)
            }
            setPadding(0, dp(12), 0, dp(12))
        }

        val startButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.dashboard_pick_start_date)
            setOnClickListener {
                pickDate {
                    selectedStart = it
                    startDate.text = it
                }
            }
        }
        val endButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.dashboard_pick_end_date)
            setOnClickListener {
                pickDate {
                    selectedEnd = it
                    endDate.text = it
                }
            }
        }
        val monthButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.dashboard_pick_month)
            setOnClickListener {
                pickMonth { year, month ->
                    selectedYear = year
                    selectedMonth = month
                    monthValue.text = "$year-$month"
                }
            }
        }

        when {
            !state.startDate.isNullOrBlank() && !state.endDate.isNullOrBlank() -> dateRadio.isChecked = true
            !state.monthYear.isNullOrBlank() && !state.month.isNullOrBlank() -> monthRadio.isChecked = true
            else -> dateRadio.isChecked = true
        }

        container.addView(startButton)
        container.addView(startDate)
        container.addView(endButton)
        container.addView(endDate)
        container.addView(monthButton)
        container.addView(monthValue)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dashboard_filter_title))
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.dashboard_filter_clear) { _, _ ->
                viewModel.clearFilters()
                LoginActivity.getSavedToken(requireContext())?.let { viewModel.loadDashboard(it) }
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                val token = LoginActivity.getSavedToken(requireContext()) ?: return@setPositiveButton
                if (monthRadio.isChecked) {
                    if (selectedYear.isNotBlank() && selectedMonth.isNotBlank()) {
                        viewModel.setMonthRange(selectedYear, selectedMonth)
                        viewModel.loadDashboard(token)
                    }
                } else {
                    if (selectedStart.isNotBlank() && selectedEnd.isNotBlank()) {
                        viewModel.setDateRange(selectedStart, selectedEnd)
                        viewModel.loadDashboard(token)
                    }
                }
            }
            .show()
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}
