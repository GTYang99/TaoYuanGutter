package com.example.taoyuangutter.dashboard

import com.example.taoyuangutter.api.DashboardQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardViewModelTest {
    @Test fun cumulativeQueryHasNoDateRange() {
        val query = DashboardQuery()
        assertEquals(null, query.startDate)
        assertEquals(null, query.endDate)
    }

    @Test fun reversedDateSelectionIsNormalized() {
        val normalized = DashboardFragment.normalizeDateRange(
            LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-01")
        )
        assertEquals(LocalDate.parse("2026-09-01"), normalized.first)
        assertEquals(LocalDate.parse("2026-09-14"), normalized.second)
    }
}
