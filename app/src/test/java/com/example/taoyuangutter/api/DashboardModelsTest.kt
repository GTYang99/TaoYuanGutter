package com.example.taoyuangutter.api

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardModelsTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(DashboardLengthGroup::class.java, DashboardLengthGroupDeserializer())
        .registerTypeAdapter(DashboardProgressGroup::class.java, DashboardProgressGroupDeserializer())
        .create()

    @Test
    fun parsesLengthGroupAccountsAndTotals() {
        val json = """
            {
              "總長": "3.99",
              "10362": "2.34",
              "10396": "0.89",
              "10397": "0.05",
              "10419": "0.71"
            }
        """.trimIndent()

        val group = gson.fromJson(json, DashboardLengthGroup::class.java)

        assertEquals("3.99", group.totalLength)
        assertEquals("2.34", group.accounts["10362"])
        assertEquals("0.71", group.accounts["10419"])
    }

    @Test
    fun parsesProgressGroupCountsAndOtherBuckets() {
        val json = """
            {
              "總數": 80,
              "待匯入": 75,
              "待繪製": 0,
              "待修正": 4,
              "已完成": 1,
              "其他": {
                "淤積_輕度": 53,
                "淤積_嚴重": 5,
                "溝體結構受損": 45,
                "附掛或過路管線": 31
              }
            }
        """.trimIndent()

        val group = gson.fromJson(json, DashboardProgressGroup::class.java)

        assertEquals(80, group.totalCount)
        assertEquals(75, group.pendingImport)
        assertEquals(1, group.completed)
        assertEquals(53, group.other["淤積_輕度"])
    }

    @Test
    fun dashboardQueryKeepsFiltersMutuallyExclusive() {
        val dateQuery = DashboardQuery(startDate = "2026-08-01", endDate = "2026-08-31")
        val monthQuery = DashboardQuery(monthYear = "2026", month = "8")

        assertEquals(true, dateQuery.isDateRangeSelected)
        assertEquals(false, dateQuery.isMonthRangeSelected)
        assertEquals(false, monthQuery.isDateRangeSelected)
        assertEquals(true, monthQuery.isMonthRangeSelected)
    }
}
