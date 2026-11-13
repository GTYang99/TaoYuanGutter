package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Test

class WaypointAdapterPresentationTest {
    @Test
    fun existingXyNumRemainsVisibleWhenWaypointIsIncomplete() {
        assertEquals(
            "C",
            waypointStatusText(
                xyNum = "C",
                isComplete = false,
                dataFilledLabel = "已填寫資料",
                noDataLabel = "暫無資料"
            )
        )
    }

    @Test
    fun missingXyNumKeepsExistingCompletionLabels() {
        assertEquals(
            "已填寫資料",
            waypointStatusText(null, true, "已填寫資料", "暫無資料")
        )
        assertEquals(
            "暫無資料",
            waypointStatusText(null, false, "已填寫資料", "暫無資料")
        )
    }
}
