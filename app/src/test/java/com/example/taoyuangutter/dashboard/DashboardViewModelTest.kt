package com.example.taoyuangutter.dashboard

import com.example.taoyuangutter.api.DashboardQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardViewModelTest {
    @Test fun cumulativeQueryHasNoDateRange() {
        val query = DashboardQuery()
        assertEquals(null, query.startDate)
        assertEquals(null, query.endDate)
    }
}
