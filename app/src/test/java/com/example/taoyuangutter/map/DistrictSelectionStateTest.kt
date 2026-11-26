package com.example.taoyuangutter.map

import org.junit.Assert.assertEquals
import org.junit.Test

class DistrictSelectionStateTest {
    @Test
    fun mapsZeroPartialAndAllSelectionToThreeStates() {
        assertEquals(DistrictSelectionState.NONE, districtSelectionState(0, 13))
        assertEquals(DistrictSelectionState.PARTIAL, districtSelectionState(1, 13))
        assertEquals(DistrictSelectionState.PARTIAL, districtSelectionState(12, 13))
        assertEquals(DistrictSelectionState.ALL, districtSelectionState(13, 13))
    }
}
