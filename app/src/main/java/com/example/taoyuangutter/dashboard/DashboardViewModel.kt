package com.example.taoyuangutter.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DashboardQuery
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.accountMileage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
    val todayMileage: String? = null,
    val cumulativeMileage: String? = null,
    val searchMileage: String? = null,
    val searchExecuted: Boolean = false,
    val todayQuery: DashboardQuery? = null,
    val cumulativeQuery: DashboardQuery? = null,
    val searchQuery: DashboardQuery? = null
)

class DashboardViewModel(private val repository: GutterRepository = GutterRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    fun loadDashboard(token: String, username: String) {
        val today = LocalDate.now().toString()
        val todayQuery = DashboardQuery(today, today)
        val cumulativeQuery = DashboardQuery()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorCode = null, todayQuery = todayQuery, cumulativeQuery = cumulativeQuery) }
            val todayResult = async { repository.getDashboard(token, todayQuery) }.await()
            val cumulativeResult = async { repository.getDashboard(token, cumulativeQuery) }.await()
            val errors = listOf(todayResult, cumulativeResult).filterIsInstance<ApiResult.Error>()
            _uiState.update { it.copy(isLoading = false, errorMessage = errors.firstOrNull()?.message, errorCode = errors.firstOrNull()?.code, todayMileage = (todayResult as? ApiResult.Success)?.data?.data?.accountMileage(username), cumulativeMileage = (cumulativeResult as? ApiResult.Success)?.data?.data?.accountMileage(username)) }
        }
    }

    fun search(token: String, username: String, start: String, end: String) {
        val query = DashboardQuery(start, end)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorCode = null, searchQuery = query, searchExecuted = true) }
            when (val result = repository.getDashboard(token, query)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, searchMileage = result.data.data?.accountMileage(username)) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message, errorCode = result.code) }
            }
        }
    }

    fun clearSearch() = _uiState.update { it.copy(searchMileage = null, searchQuery = null, searchExecuted = false, errorMessage = null, errorCode = null) }
    fun consumeAuthError() = _uiState.update { it.copy(errorCode = null) }
}
