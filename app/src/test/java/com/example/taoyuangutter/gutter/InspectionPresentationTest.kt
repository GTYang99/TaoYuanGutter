package com.example.taoyuangutter.gutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InspectionPresentationTest {

    @Test
    fun editableWaypointLabelDoesNotExposeTieInPointStatus() {
        val item = Waypoint(
            type = WaypointType.NODE,
            label = "節點1",
            basicData = hashMapOf(
                "IS_TIEINPOINT" to "1",
                "IS_PENDING_DEPLOY" to "0"
            )
        )

        val label = editableWaypointDisplayLabel(item)

        assertEquals("節點1", label)
        assertFalse(label.contains("銜接點"))
    }

    @Test
    fun inspectionDetailFieldsFollowApprovedOrder() {
        assertEquals(
            listOf("側溝材質", "溝體結構受損", "附掛或過路管線", "淤積程度", "連結管"),
            inspectionDetailFieldOrder()
        )
    }

    @Test
    fun hangingAndConnectingPresenceUseHasOrNoneWording() {
        assertEquals("有", inspectionPresenceValue(true))
        assertEquals("無", inspectionPresenceValue(false))
        assertEquals("", inspectionPresenceValue(null))
    }
}
