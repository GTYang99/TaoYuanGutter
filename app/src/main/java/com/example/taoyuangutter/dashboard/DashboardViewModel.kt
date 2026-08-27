package com.example.taoyuangutter.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DashboardQuery
import com.example.taoyuangutter.api.DashboardResponse
import com.example.taoyuangutter.api.DashboardResponseData
import com.example.taoyuangutter.api.GutterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DASHBOARD_TOTAL_GROUP_KEY = "全部"

data class DashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
    val response: DashboardResponseData? = null,
    val selectedLengthGroups: Set<String> = emptySet(),
    val selectedLengthDetailGroup: String? = null,
    val selectedProgressGroup: String = DASHBOARD_TOTAL_GROUP_KEY,
    val availableLengthGroups: List<String> = emptyList(),
    val availableProgressGroups: List<String> = emptyList(),
    val query: DashboardQuery = DashboardQuery()
)

class DashboardViewModel(
    private val repository: GutterRepository = GutterRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    fun loadDashboard(token: String) {
        viewModelScope.launch {
            val query = _uiState.value.query
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorCode = null) }
            when (val result = repository.getDashboard(token, query)) {
                is ApiResult.Success -> handleSuccess(result.data)
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        errorCode = result.code
                    )
                }
            }
        }
    }

    fun selectLengthGroups(groups: Set<String>) {
        _uiState.update { current ->
            val filtered = groups.filter { it != TOTAL_GROUP_KEY }.toSet()
            val nextDetail = current.selectedLengthDetailGroup?.takeIf { it in filtered }
                ?: filtered.firstOrNull()
            current.copy(
                selectedLengthGroups = filtered,
                selectedLengthDetailGroup = nextDetail ?: current.selectedLengthDetailGroup
            )
        }
    }

    fun selectLengthDetailGroup(group: String) {
        _uiState.update { it.copy(selectedLengthDetailGroup = group) }
    }

    fun selectProgressGroup(group: String) {
        _uiState.update { it.copy(selectedProgressGroup = group) }
    }

    fun setDateRange(startDate: String, endDate: String) {
        _uiState.update {
            it.copy(
                query = DashboardQuery(
                    startDate = startDate,
                    endDate = endDate,
                    monthYear = null,
                    month = null
                )
            )
        }
    }

    fun setMonthRange(year: String, month: String) {
        _uiState.update {
            it.copy(
                query = DashboardQuery(
                    startDate = null,
                    endDate = null,
                    monthYear = year,
                    month = month
                )
            )
        }
    }

    fun setQuery(query: DashboardQuery) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(query = DashboardQuery()) }
    }

    fun consumeAuthError() {
        _uiState.update { it.copy(errorCode = null) }
    }

    private fun handleSuccess(response: DashboardResponse) {
        val data = response.data
        val lengthGroups = data?.surveyLength?.keys?.filterNot { it == TOTAL_GROUP_KEY } ?: emptyList()
        val progressGroups = data?.surveyProgress?.keys?.toList() ?: emptyList()
        val current = _uiState.value
        val selectedLengthGroups = when {
            current.selectedLengthGroups.isNotEmpty() -> current.selectedLengthGroups.intersect(lengthGroups.toSet())
            else -> lengthGroups.toSet()
        }
        val selectedDetailGroup = current.selectedLengthDetailGroup
            ?.takeIf { it in lengthGroups }
            ?: lengthGroups.firstOrNull { it in selectedLengthGroups }
            ?: lengthGroups.firstOrNull()
        val selectedProgressGroup = when {
            current.selectedProgressGroup == TOTAL_GROUP_KEY -> TOTAL_GROUP_KEY
            current.selectedProgressGroup in progressGroups -> current.selectedProgressGroup
            else -> TOTAL_GROUP_KEY
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                errorCode = null,
                response = data,
                availableLengthGroups = lengthGroups,
                availableProgressGroups = progressGroups,
                selectedLengthGroups = selectedLengthGroups,
                selectedLengthDetailGroup = selectedDetailGroup,
                selectedProgressGroup = selectedProgressGroup
            )
        }
    }

    companion object {
        const val TOTAL_GROUP_KEY = DASHBOARD_TOTAL_GROUP_KEY
    }
}
