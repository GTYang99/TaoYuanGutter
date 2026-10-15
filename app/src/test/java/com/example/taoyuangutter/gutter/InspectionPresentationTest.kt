package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.api.NodeDetails
import com.google.gson.Gson
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
            listOf("側溝材質", "溝體結構受損", "附掛或過路管線", "淤積程度", "連接管"),
            inspectionDetailFieldOrder()
        )
    }

    @Test
    fun hangingAndConnectingPresenceUseHasOrNoneWording() {
        assertEquals("有", inspectionPresenceValue(true))
        assertEquals("無", inspectionPresenceValue(false))
        assertEquals("", inspectionPresenceValue(null))
    }

    @Test
    fun siltCodesExposeOnlyTheApprovedThreeLabels() {
        assertEquals("無", inspectionSiltValue("0"))
        assertEquals("輕度", inspectionSiltValue("1"))
        assertEquals("嚴重", inspectionSiltValue("2"))
        assertEquals("嚴重", inspectionSiltValue("3"))
    }

    @Test
    fun tieInInspectionHidesTheSameDetailFieldsAsCantOpen() {
        val tieIn = nodeDetails("{\"IS_TIEINPOINT\":\"1\"}")
        val cantOpen = nodeDetails("{\"IS_CANTOPEN\":\"1\"}")
        val normal = nodeDetails("{\"IS_CANTOPEN\":\"0\",\"IS_TIEINPOINT\":\"0\"}")

        assertFalse(shouldShowInspectionDetailFields(tieIn, isVirtual = false))
        assertFalse(shouldShowInspectionDetailFields(cantOpen, isVirtual = false))
        assertEquals(true, shouldShowInspectionDetailFields(normal, isVirtual = false))
        assertFalse(shouldShowInspectionDetailFields(normal, isVirtual = true))
    }

    private fun nodeDetails(json: String): NodeDetails = Gson().fromJson(json, NodeDetails::class.java)
}
