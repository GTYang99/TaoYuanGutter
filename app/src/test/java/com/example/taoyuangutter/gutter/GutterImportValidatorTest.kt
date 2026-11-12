package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.pending.WaypointSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterImportValidatorTest {
    @Test
    fun rejectsImportedXyNumAlreadyUsedInCurrentDitch() {
        val currentWaypoints = listOf(
            WaypointSnapshot(basicData = hashMapOf("XY_NUM" to " C "))
        )

        assertTrue(GutterImportValidator.hasExistingXyNum("C", currentWaypoints))
    }

    @Test
    fun allowsImportedXyNumNotUsedInCurrentDitch() {
        val currentWaypoints = listOf(
            WaypointSnapshot(basicData = hashMapOf("XY_NUM" to "C"))
        )

        assertFalse(GutterImportValidator.hasExistingXyNum("D", currentWaypoints))
        assertFalse(GutterImportValidator.hasExistingXyNum("", currentWaypoints))
    }
}
