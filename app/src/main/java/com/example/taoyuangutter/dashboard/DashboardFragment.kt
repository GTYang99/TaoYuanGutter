package com.example.taoyuangutter.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.databinding.ActivityDashboardBinding
import com.example.taoyuangutter.login.AuthExpiredHandler
import com.example.taoyuangutter.login.AuthNavigator
import com.example.taoyuangutter.login.LoginActivity
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardFragment : Fragment() {
    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val authExpiredHandler by lazy { AuthExpiredHandler(requireActivity()) }
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        setupActions()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
        val token = LoginActivity.getSavedToken(requireContext())
        val username = LoginActivity.getSavedUsername(requireContext())
        if (token.isNullOrBlank() || username.isNullOrBlank()) AuthNavigator(requireContext()).clearAuthAndGoLogin()
        else viewModel.loadDashboard(token, username)
    }

    override fun onDestroyView() { authExpiredHandler.reset(); _binding = null; super.onDestroyView() }

    private fun setupActions() {
        binding.startDate.setOnClickListener { pickDate(true) }
        binding.endDate.setOnClickListener { pickDate(false) }
        binding.searchButton.setOnClickListener {
            val start = startDate; val end = endDate
            if (start == null || end == null) { binding.tvError.text = getString(R.string.dashboard_date_required); binding.tvError.visibility = View.VISIBLE }
            else if (start.isAfter(end)) { binding.tvError.text = getString(R.string.dashboard_date_invalid); binding.tvError.visibility = View.VISIBLE }
            else LoginActivity.getSavedToken(requireContext())?.let { token -> LoginActivity.getSavedUsername(requireContext())?.let { user -> viewModel.search(token, user, start.toString(), end.toString()) } }
        }
        binding.clearButton.setOnClickListener { startDate = null; endDate = null; binding.startDate.text = getString(R.string.dashboard_pick_start_date); binding.endDate.text = getString(R.string.dashboard_pick_end_date); viewModel.clearSearch() }
    }

    private fun pickDate(isStart: Boolean) {
        val current = (if (isStart) startDate else endDate) ?: LocalDate.now()
        DatePickerDialog(requireContext(), R.style.ThemeOverlay_TaoYuanGutter_DatePicker, { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day)
            if (isStart) startDate = date else endDate = date
            normalizeAndRenderDates()
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    private fun normalizeAndRenderDates() {
        val normalized = normalizeDateRange(startDate, endDate)
        startDate = normalized.first
        endDate = normalized.second
        binding.startDate.text = startDate?.toString() ?: getString(R.string.dashboard_pick_start_date)
        binding.endDate.text = endDate?.toString() ?: getString(R.string.dashboard_pick_end_date)
    }

    private fun render(state: DashboardUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage.orEmpty(); binding.tvError.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.todayValue.text = state.todayMileage ?: "0"; binding.cumulativeValue.text = state.cumulativeMileage ?: "0"
        binding.searchResult.visibility = View.VISIBLE
        val hasResult = state.searchExecuted && !state.searchMileage.isNullOrBlank()
        binding.emptyResult.visibility = if (hasResult) View.GONE else View.VISIBLE
        binding.searchResultValue.visibility = if (hasResult) View.VISIBLE else View.GONE
        binding.searchResultUnit.visibility = if (hasResult) View.VISIBLE else View.GONE
        binding.searchResultValue.text = state.searchMileage.orEmpty()
        binding.searchResult.setBackgroundResource(if (hasResult) R.drawable.bg_dashboard_result_card else android.R.color.transparent)
        if (state.errorCode == 401) { authExpiredHandler.handleIfAuthExpired(ApiResult.Error(state.errorMessage ?: "尚未登入", 401)); viewModel.consumeAuthError() }
    }

    companion object {
        fun newInstance() = DashboardFragment()

        internal fun normalizeDateRange(start: LocalDate?, end: LocalDate?): Pair<LocalDate?, LocalDate?> {
            val dates = listOfNotNull(start, end).sorted()
            return dates.getOrNull(0) to dates.getOrNull(1)
        }
    }
}
