package com.example.taoyuangutter.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardViewModelTest {

    @Test
    fun monthRangeClearsDateRange() {
        val viewModel = DashboardViewModel()

        viewModel.setDateRange("2026-08-01", "2026-08-31")
        viewModel.setMonthRange("2026", "08")

        val query = viewModel.uiState.value.query
        assertEquals(null, query.startDate)
        assertEquals(null, query.endDate)
        assertEquals("2026", query.monthYear)
        assertEquals("08", query.month)
    }

    @Test
    fun dateRangeClearsMonthRange() {
        val viewModel = DashboardViewModel()

        viewModel.setMonthRange("2026", "08")
        viewModel.setDateRange("2026-08-01", "2026-08-31")

        val query = viewModel.uiState.value.query
        assertEquals("2026-08-01", query.startDate)
        assertEquals("2026-08-31", query.endDate)
        assertEquals(null, query.monthYear)
        assertEquals(null, query.month)
    }

    @Test
    fun selectingGroupsKeepsRequestedDetailGroup() {
        val viewModel = DashboardViewModel()
        viewModel.selectLengthGroups(setOf("A組", "B組"))
        viewModel.selectLengthDetailGroup("B組")

        assertEquals(setOf("A組", "B組"), viewModel.uiState.value.selectedLengthGroups)
        assertEquals("B組", viewModel.uiState.value.selectedLengthDetailGroup)
    }
}
