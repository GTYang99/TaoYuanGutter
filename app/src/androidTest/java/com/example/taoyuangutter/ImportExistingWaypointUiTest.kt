package com.example.taoyuangutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetailsResponse
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.ImportExistingWaypointBottomSheet
import com.google.android.gms.maps.model.LatLng
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ImportExistingWaypointUiTest {
    @Test
    fun tabsKeepOrderDefaultAndRefreshEachSubmittedModeOnReturn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        val previousToken = prefs.getString("auth_token", null)
        prefs.edit().putString("auth_token", "import-waypoint-ui-test-token").commit()

        val recentCalls = AtomicInteger()
        val nearbyCalls = CopyOnWriteArrayList<Pair<Double, Double>>()
        val searchCalls = CopyOnWriteArrayList<String>()
        val requestApi = Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, args ->
            val actualArgs = args.orEmpty()
            when (method.name) {
                "getClosestNodeDetails" -> {
                    if (actualArgs.size == 2) {
                        recentCalls.incrementAndGet()
                    } else {
                        nearbyCalls += (actualArgs[0] as Double) to (actualArgs[1] as Double)
                    }
                    Response.success(NodeDetailsResponse(true, null, emptyList(), null))
                }
                "getNodeDetailsByXyNum" -> {
                    searchCalls += actualArgs[0] as String
                    Response.success(NodeDetailsResponse(true, null, emptyList(), null))
                }
                else -> error("Unexpected API call: ${method.name}")
            }
        } as GutterApiService

        try {
            val intent = GutterFormActivity.newIntent(
                context,
                arrayListOf("ui-test"),
                doubleArrayOf(0.0),
                doubleArrayOf(0.0),
                basicData = hashMapOf(),
                sessionDraftId = 0L,
                sessionIsOffline = true
            )
            ActivityScenario.launch<GutterFormActivity>(intent).use { scenario ->
                val locationRequests = AtomicInteger()
                lateinit var sheet: ImportExistingWaypointBottomSheet
                sheet = ImportExistingWaypointBottomSheet().apply {
                    gutterRepository = GutterRepository(requestApi)
                    callbacks = object : ImportExistingWaypointBottomSheet.Callbacks {
                        override fun onMapPickModeChanged(enabled: Boolean) = Unit
                        override fun onNearbyCenterChanged(center: LatLng) = Unit
                        override fun onCandidateWaypointsChanged(items: List<com.example.taoyuangutter.api.NodeDetails>) = Unit
                        override fun onWaypointSelected(item: com.example.taoyuangutter.api.NodeDetails?) = Unit
                        override fun onImport(item: com.example.taoyuangutter.api.NodeDetails) = Unit
                        override fun onDismissed() = Unit
                        override fun onPageSwitched() = Unit
                        override fun onRequestMyLocation() {
                            locationRequests.incrementAndGet()
                            sheet.startNearbyAutoSearch(LatLng(24.5678, 121.1234))
                        }
                        override fun onAuthExpiredBeforeImportLogout() = Unit
                    }
                }
                scenario.onActivity { activity ->
                    sheet.show(activity.supportFragmentManager, "ImportExistingWaypointUiTest")
                }

                listOf("最近存取節點", "附近點位", "搜尋點位").forEach { label ->
                    onView(withText(label)).check(matches(isDisplayed()))
                }
                onView(withId(R.id.btnMyLocation)).check(matches(not(isDisplayed())))
                scenario.onActivity { activity ->
                    val shownSheet = activity.supportFragmentManager
                        .findFragmentByTag("ImportExistingWaypointUiTest") as ImportExistingWaypointBottomSheet
                    assertEquals(0, shownSheet.requireView().findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout).selectedTabPosition)
                }
                assertEquals(1, recentCalls.get())

                onView(withText("附近點位")).perform(click())
                onView(withId(R.id.btnMyLocation)).check(matches(isDisplayed()))
                assertEquals(listOf(121.1234 to 24.5678), nearbyCalls.toList())

                onView(withId(R.id.btnMyLocation)).perform(click())
                assertEquals(2, nearbyCalls.size)

                onView(withText("搜尋點位")).perform(click())
                onView(withText("請輸入座標編號 (XY_NUM) 後按搜尋")).check(matches(isDisplayed()))
                assertEquals(0, searchCalls.size)

                onView(withId(R.id.etSearch)).perform(typeText("A001"), pressImeActionButton())
                assertEquals(listOf("A001"), searchCalls.toList())

                onView(withText("最近存取節點")).perform(click())
                onView(withText("搜尋點位")).perform(click())
                assertEquals(listOf("A001", "A001"), searchCalls.toList())

                onView(withText("附近點位")).perform(click())
                assertEquals(3, locationRequests.get())
                assertEquals(3, nearbyCalls.size)
            }
        } finally {
            val editor = prefs.edit()
            if (previousToken == null) editor.remove("auth_token") else editor.putString("auth_token", previousToken)
            editor.commit()
        }
    }
}
