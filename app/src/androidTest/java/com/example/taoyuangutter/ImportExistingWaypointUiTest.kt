package com.example.taoyuangutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.AppVersionUpdateTestHooks
import com.example.taoyuangutter.api.AppVersionRecord
import com.example.taoyuangutter.api.AppVersionRepository
import com.example.taoyuangutter.api.AppVersionResponse
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.api.NodeDetailsResponse
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.ImportExistingWaypointBottomSheet
import com.google.android.gms.maps.model.LatLng
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ImportExistingWaypointUiTest {
    @Test
    fun searchWaitsForSubmissionAndRepeatsLastSubmittedXyNumOnReturn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        val previousToken = prefs.getString("auth_token", null)
        prefs.edit().putString("auth_token", "import-waypoint-search-test-token").commit()
        val searchCalls = CopyOnWriteArrayList<String>()
        val requestApi = Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getClosestNodeDetails" -> Response.success(NodeDetailsResponse(true, null, emptyList(), null))
                "getNodeDetailsByXyNum" -> {
                    searchCalls += args.orEmpty()[0] as String
                    Response.success(NodeDetailsResponse(true, null, emptyList(), null))
                }
                else -> error("Unexpected API call: ${method.name}")
            }
        } as GutterApiService
        val versionApi = object : GutterApiService by requestApi {
            override suspend fun getAppVersion() = Response.success(
                AppVersionResponse(
                    success = true,
                    data = listOf(AppVersionRecord("android", BuildConfig.VERSION_NAME, "https://example.test/app.apk"))
                )
            )
        }
        AppVersionUpdateTestHooks.repositoryFactory = { AppVersionRepository { versionApi } }

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
                val sheet = ImportExistingWaypointBottomSheet().apply {
                    gutterRepository = GutterRepository(requestApi)
                    callbacks = object : ImportExistingWaypointBottomSheet.Callbacks {
                        override fun onMapPickModeChanged(enabled: Boolean) = Unit
                        override fun onNearbyCenterChanged(center: LatLng) = Unit
                        override fun onCandidateWaypointsChanged(items: List<NodeDetails>) = Unit
                        override fun onWaypointSelected(item: NodeDetails?) = Unit
                        override fun onImport(item: NodeDetails) = Unit
                        override fun onDismissed() = Unit
                        override fun onPageSwitched() = Unit
                        override fun onRequestMyLocation() = Unit
                        override fun onAuthExpiredBeforeImportLogout() = Unit
                    }
                }
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    assertFalse(activity.supportFragmentManager.isStateSaved)
                    sheet.show(activity.supportFragmentManager, "ImportExistingWaypointSearchUiTest")
                }

                onView(withContentDescription("搜尋點位")).perform(click())
                onView(withText("請輸入座標編號 (XY_NUM) 後按搜尋")).check(matches(isDisplayed()))
                assertEquals(emptyList<String>(), searchCalls.toList())

                onView(withId(R.id.etSearch)).perform(typeText("A001"), pressImeActionButton())
                assertEquals(listOf("A001"), searchCalls.toList())

                onView(withContentDescription("最近存取節點")).perform(click())
                onView(withContentDescription("搜尋點位")).perform(click())
                assertEquals(listOf("A001", "A001"), searchCalls.toList())
            }
        } finally {
            AppVersionUpdateTestHooks.repositoryFactory = null
            val editor = prefs.edit()
            if (previousToken == null) editor.remove("auth_token") else editor.putString("auth_token", previousToken)
            editor.commit()
        }
    }

    @Test
    fun tabsKeepOrderDefaultAndRefreshEachSubmittedModeOnReturn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        val previousToken = prefs.getString("auth_token", null)
        prefs.edit().putString("auth_token", "import-waypoint-ui-test-token").commit()

        val recentCalls = AtomicInteger()
        val nearbyCalls = CopyOnWriteArrayList<Pair<Double, Double>>()
        val searchCalls = CopyOnWriteArrayList<String>()
        val candidateUpdates = CopyOnWriteArrayList<List<NodeDetails>>()
        val selectedItem = AtomicReference<NodeDetails?>(null)
        val selectionClearCalls = AtomicInteger()
        val imports = CopyOnWriteArrayList<NodeDetails>()
        val candidate = NodeDetails(
            ditchId = "D-1",
            nodeNum = "N-1",
            nodeAttr = "2",
            nodeTyP = "1",
            matTyp = "1",
            nodeX = "304000.0",
            nodeY = "2765000.0",
            nodeLe = null,
            xyNum = "UI-CANDIDATE-1",
            coverDep = null,
            nodeDep = null,
            nodeWid = null,
            isCantOpen = null,
            isBroken = "0",
            isHanging = "0",
            isSilt = "0",
            note = null,
            latitude = "24.5678",
            longitude = "121.1234"
        )
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
                    Response.success(NodeDetailsResponse(true, null, listOf(candidate), null))
                }
                "getNodeDetailsByXyNum" -> {
                    searchCalls += actualArgs[0] as String
                    Response.success(NodeDetailsResponse(true, null, listOf(candidate), null))
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
                        override fun onCandidateWaypointsChanged(items: List<NodeDetails>) {
                            candidateUpdates += items
                        }
                        override fun onWaypointSelected(item: NodeDetails?) {
                            if (item == null) {
                                selectedItem.set(null)
                                selectionClearCalls.incrementAndGet()
                            } else {
                                selectedItem.set(item)
                            }
                        }
                        override fun onImport(item: NodeDetails) {
                            imports += item
                        }
                        override fun onDismissed() = Unit
                        override fun onPageSwitched() = Unit
                        override fun onRequestMyLocation() {
                            locationRequests.incrementAndGet()
                            sheet.startNearbyAutoSearch(LatLng(24.5678, 121.1234))
                        }
                        override fun onAuthExpiredBeforeImportLogout() = Unit
                    }
                }
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    assertTrue(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                    assertFalse(activity.supportFragmentManager.isStateSaved)
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
                onView(withText("UI-CANDIDATE-1")).check(matches(isDisplayed())).perform(click())
                onView(withId(R.id.rbSelect)).check(matches(isChecked()))
                onView(withId(R.id.btnImport)).check(matches(isEnabled()))
                assertEquals(listOf(candidate), candidateUpdates.last())
                assertEquals(candidate, selectedItem.get())

                onView(withContentDescription("附近點位")).perform(click())
                scenario.onActivity { activity ->
                    val shownSheet = activity.supportFragmentManager
                        .findFragmentByTag("ImportExistingWaypointUiTest") as ImportExistingWaypointBottomSheet
                    assertEquals(1, shownSheet.requireView().findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout).selectedTabPosition)
                }
                onView(withId(R.id.btnMyLocation)).check(matches(isDisplayed()))
                assertEquals(listOf(121.1234 to 24.5678), nearbyCalls.toList())
                onView(withText("UI-CANDIDATE-1")).check(matches(isDisplayed()))
                assertEquals(null, selectedItem.get())
                assertTrue(selectionClearCalls.get() >= 2)
                assertEquals(listOf(candidate), candidateUpdates.last())

                onView(withId(R.id.btnMyLocation)).perform(click())
                assertEquals(2, nearbyCalls.size)
                onView(withText("UI-CANDIDATE-1")).check(matches(isDisplayed()))

                onView(withContentDescription("搜尋點位")).perform(click())
                scenario.onActivity { activity ->
                    val shownSheet = activity.supportFragmentManager
                        .findFragmentByTag("ImportExistingWaypointUiTest") as ImportExistingWaypointBottomSheet
                    assertEquals(2, shownSheet.requireView().findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout).selectedTabPosition)
                }
                onView(withText("請輸入座標編號 (XY_NUM) 後按搜尋")).check(matches(isDisplayed()))
                assertEquals(0, searchCalls.size)

                onView(withId(R.id.etSearch)).perform(typeText("A001"), pressImeActionButton())
                assertEquals(listOf("A001"), searchCalls.toList())
                onView(withText("UI-CANDIDATE-1")).check(matches(isDisplayed()))
                assertEquals(listOf(candidate), candidateUpdates.last())

                onView(withContentDescription("最近存取節點")).perform(click())
                scenario.onActivity { activity ->
                    val shownSheet = activity.supportFragmentManager
                        .findFragmentByTag("ImportExistingWaypointUiTest") as ImportExistingWaypointBottomSheet
                    assertEquals(0, shownSheet.requireView().findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout).selectedTabPosition)
                }
                onView(withContentDescription("搜尋點位")).perform(click())
                assertEquals(listOf("A001", "A001"), searchCalls.toList())

                onView(withContentDescription("附近點位")).perform(click())
                assertEquals(3, locationRequests.get())
                assertEquals(3, nearbyCalls.size)
                onView(withText("UI-CANDIDATE-1")).check(matches(isDisplayed())).perform(click())
                onView(withId(R.id.rbSelect)).check(matches(isChecked()))
                onView(withId(R.id.btnImport)).perform(click())
                assertEquals(listOf(candidate), imports)
            }
        } finally {
            val editor = prefs.edit()
            if (previousToken == null) editor.remove("auth_token") else editor.putString("auth_token", previousToken)
            editor.commit()
        }
    }
}
