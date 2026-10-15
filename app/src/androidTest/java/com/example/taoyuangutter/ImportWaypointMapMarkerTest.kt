package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportWaypointMapMarkerTest {
    @Test
    fun candidateMarkersRenderSelectionHighlightsAndCleanupOnMap() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = GutterFormActivity.newIntent(
            context,
            arrayListOf("map-test"),
            doubleArrayOf(24.5),
            doubleArrayOf(121.1),
            basicData = hashMapOf(),
            sessionDraftId = 0L,
            sessionIsOffline = true
        )

        ActivityScenario.launch<GutterFormActivity>(intent).use { scenario ->
            var map: GoogleMap? = null
            for (attempt in 0 until 30) {
                scenario.onActivity { activity ->
                    map = GutterFormActivity::class.java.getDeclaredField("formMap")
                        .apply { isAccessible = true }
                        .get(activity) as? GoogleMap
                }
                if (map != null) break
                Thread.sleep(500)
            }
            assertNotNull("Google Map should become ready in the emulator", map)

            val first = candidate(101, "MAP-1", 24.5678, 121.1234)
            val second = candidate(102, "MAP-2", 24.5688, 121.1244)
            scenario.onActivity { activity ->
                invoke(activity, "showImportCandidateWaypoints", listOf(first, second))

                val candidateMarkers = markers(activity)
                assertEquals(2, candidateMarkers.size)
                val firstMarker = candidateMarkers.single { it.tag == first }
                val secondMarker = candidateMarkers.single { it.tag == second }
                assertEquals(LatLng(24.5678, 121.1234), firstMarker.position)
                assertEquals("MAP-1", firstMarker.title)
                assertEquals(0.5f, firstMarker.zIndex)

                invoke(activity, "showImportSelectedWaypoint", first)
                assertFalse(firstMarker.isVisible)
                assertTrue(secondMarker.isVisible)
                val selectedMarker = activity.javaClass.getDeclaredField("importCenterMarker")
                    .apply { isAccessible = true }
                    .get(activity) as Marker
                assertEquals(LatLng(24.5678, 121.1234), selectedMarker.position)
                assertEquals("MAP-1", selectedMarker.title)
                assertTrue(selectedMarker.zIndex > firstMarker.zIndex)

                invoke(activity, "showImportSelectedWaypoint", null)
                assertTrue(firstMarker.isVisible)
                assertTrue(secondMarker.isVisible)
                assertNull(
                    activity.javaClass.getDeclaredField("importCenterMarker")
                        .apply { isAccessible = true }
                        .get(activity)
                )

                invoke(activity, "clearImportMarkers")
                assertTrue(markers(activity).isEmpty())
                assertNull(
                    activity.javaClass.getDeclaredField("importCenterMarker")
                        .apply { isAccessible = true }
                        .get(activity)
                )
            }
        }
    }

    private fun invoke(activity: GutterFormActivity, methodName: String, argument: Any?) {
        GutterFormActivity::class.java.getDeclaredMethod(methodName, List::class.java)
            .apply { isAccessible = true }
            .invoke(activity, argument)
    }

    private fun invoke(activity: GutterFormActivity, methodName: String, argument: NodeDetails?) {
        GutterFormActivity::class.java.getDeclaredMethod(methodName, NodeDetails::class.java)
            .apply { isAccessible = true }
            .invoke(activity, argument)
    }

    private fun invoke(activity: GutterFormActivity, methodName: String) {
        GutterFormActivity::class.java.getDeclaredMethod(methodName)
            .apply { isAccessible = true }
            .invoke(activity)
    }

    @Suppress("UNCHECKED_CAST")
    private fun markers(activity: GutterFormActivity): List<Marker> =
        GutterFormActivity::class.java.getDeclaredField("importCandidateMarkers")
            .apply { isAccessible = true }
            .get(activity) as List<Marker>

    private fun candidate(id: Int, xyNum: String, latitude: Double, longitude: Double) = NodeDetails(
        ditchId = "D-$id",
        nodeId = id,
        nodeNum = "N-$id",
        nodeAttr = "2",
        nodeTyP = "1",
        matTyp = "1",
        nodeX = "304000.0",
        nodeY = "2765000.0",
        nodeLe = null,
        xyNum = xyNum,
        coverDep = null,
        nodeDep = null,
        nodeWid = null,
        isCantOpen = null,
        isBroken = "0",
        isHanging = "0",
        isSilt = "0",
        note = null,
        latitude = latitude.toString(),
        longitude = longitude.toString()
    )
}
