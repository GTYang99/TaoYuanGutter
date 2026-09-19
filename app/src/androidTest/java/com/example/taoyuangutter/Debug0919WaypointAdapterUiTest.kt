package com.example.taoyuangutter

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointAdapter
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Debug0919WaypointAdapterUiTest {
    @Test
    fun listShowsNoDataForBlankAndPartialRowsButFilledForUploadCompleteRow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<GutterFormActivity>(
            GutterFormActivity.newIntent(
                context,
                arrayListOf("test"),
                doubleArrayOf(24.0),
                doubleArrayOf(121.0),
                basicData = hashMapOf("NODE_TYP" to "1"),
                sessionIsOffline = true
            )
        ).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("暫無資料", renderStatus(activity, Waypoint(
                    type = WaypointType.START,
                    label = "起點",
                    basicData = hashMapOf()
                )))

                assertEquals("暫無資料", renderStatus(activity, Waypoint(
                    type = WaypointType.START,
                    label = "起點",
                    latLng = LatLng(24.0, 121.0),
                    basicData = hashMapOf(
                        "NODE_TYP" to "1",
                        "NODE_X" to "121.0",
                        "NODE_Y" to "24.0",
                        "MAT_TYP" to "1",
                        "COVER_DEP" to "3",
                        "NODE_DEP" to "10",
                        "NODE_WID" to "26",
                        "IS_BROKEN" to "0",
                        "IS_HANGING" to "0",
                        "IS_SILT" to "0",
                        "photo1" to "https://example.test/photo1.jpg",
                        "photo2" to "https://example.test/photo2.jpg"
                    )
                )))

                assertEquals("已填寫資料", renderStatus(activity, Waypoint(
                    type = WaypointType.START,
                    label = "起點",
                    latLng = LatLng(24.0, 121.0),
                    basicData = hashMapOf(
                        "NODE_TYP" to "1",
                        "NODE_X" to "121.0",
                        "NODE_Y" to "24.0",
                        "MAT_TYP" to "1",
                        "COVER_DEP" to "3",
                        "NODE_DEP" to "10",
                        "NODE_WID" to "26",
                        "IS_BROKEN" to "0",
                        "IS_HANGING" to "0",
                        "IS_SILT" to "0",
                        "photo1" to "https://example.test/photo1.jpg",
                        "photo2" to "https://example.test/photo2.jpg",
                        "photo3" to "https://example.test/photo3.jpg"
                    )
                )))
            }
        }
    }

    private fun renderStatus(activity: GutterFormActivity, waypoint: Waypoint): String {
        val recycler = RecyclerView(activity)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = WaypointAdapter(mutableListOf(waypoint)) {}
        activity.addContentView(
            recycler,
            FrameLayout.LayoutParams(1080, 128)
        )
        recycler.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(128, View.MeasureSpec.EXACTLY)
        )
        recycler.layout(0, 0, 1080, 128)
        val holder = recycler.findViewHolderForAdapterPosition(0)
            ?: error("waypoint row was not laid out")
        return holder.itemView.findViewById<TextView>(R.id.tvWaypointStatus).text.toString()
    }
}
