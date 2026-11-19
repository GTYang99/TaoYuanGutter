package com.example.taoyuangutter.api

import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GutterRepositoryStoreDitchBoundaryTest {
    @Test
    fun mapperOmitsLegacyTieInMarkerFromRequestModel() {
        val waypoint = Waypoint(
            type = WaypointType.NODE,
            label = "節點1",
            latLng = LatLng(24.99, 121.31),
            basicData = hashMapOf(
                "NODE_TYP" to "1",
                "MAT_TYP" to "2",
                "IS_TIEINPOINT" to "1",
                "NODE_NOTE" to "legacy tie-in",
            )
        )

        val request = StoreDitchNodeRequestMapper.map(waypoint, requestNodeId = null, nodeSequence = 1)

        val requestText = request.toString()
        assertFalse(requestText.contains("IS_TIEINPOINT"))
        assertNull(request.javaClass.declaredFields.firstOrNull { it.name == "isTieInPoint" })
        assertTrue(request.isVirtual.not())
    }
}
