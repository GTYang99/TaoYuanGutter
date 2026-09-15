package com.example.taoyuangutter

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.fragment.app.Fragment
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.taoyuangutter.gutter.AddGutterListAdapter
import com.example.taoyuangutter.gutter.GutterSessionFlowCoordinator
import com.example.taoyuangutter.gutter.MultiGutterSessionCoordinator
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.GutterDraftCoordinator
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.example.taoyuangutter.pending.WORKFLOW_MULTI_GUTTER
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainShellActivityTest {

    class TestMapFragment : Fragment()
    class TestDashboardFragment : Fragment()
    class TestAddGutterHostFragment : Fragment(), com.example.taoyuangutter.gutter.AddGutterListBottomSheet.Host {
        var addClicked = false
        var confirmedClose = false

        fun showList(withContent: Boolean = true) {
            com.example.taoyuangutter.gutter.AddGutterListBottomSheet().also {
                it.drafts = listOf(
                    GutterSessionDraft(
                        waypoints = if (withContent) {
                            listOf(WaypointSnapshot(latitude = 25.0, longitude = 121.0))
                        } else {
                            listOf(WaypointSnapshot())
                        }
                    )
                )
            }.showNow(childFragmentManager, "test-add-gutter-list")
        }

        override fun onAddGutterListAdd() { addClicked = true }
        override fun onAddGutterListSelect(draft: GutterSessionDraft) = Unit
        override fun onAddGutterListConfirmedClose() { confirmedClose = true }
    }

    @Test
    fun shellLayoutInflatesBottomNavigation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_TaoYuanGutter)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.activity_main_shell, null, false)
        val bottomNav = root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        assertNotNull(bottomNav)
        assertEquals(2, bottomNav.menu.size())
    }

    @Test
    fun shellSwitchesBetweenMapAndDashboardTabs() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "settings put system always_finish_activities 0"
        ).close()
        instrumentation.uiAutomation.executeShellCommand(
            "settings put global always_finish_activities 0"
        ).close()
        instrumentation.waitForIdleSync()

        MainShellActivity.fragmentFactoryForTests = { tabId ->
            when (tabId) {
                R.id.nav_dashboard -> TestDashboardFragment()
                else -> TestMapFragment()
            }
        }

        val scenario = ActivityScenario.launch(MainShellActivity::class.java)
        try {
            fun currentActivity(): MainShellActivity? {
                val ref = AtomicReference<MainShellActivity?>()
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    ref.set(
                        ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .firstOrNull { it is MainShellActivity } as? MainShellActivity
                    )
                }
                return ref.get()
            }

            fun withCurrentActivity(block: MainShellActivity.() -> Unit) {
                val activity = currentActivity() ?: return
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    block(activity)
                }
            }

            fun waitForFragment(expected: Class<out Fragment>) {
                repeat(30) {
                    val activity = currentActivity()
                    if (activity != null) {
                        val currentRef = AtomicReference<Class<out Fragment>?>()
                        InstrumentationRegistry.getInstrumentation().runOnMainSync {
                            activity.supportFragmentManager.executePendingTransactions()
                            currentRef.set(
                                activity.supportFragmentManager.findFragmentById(R.id.shell_container)?.javaClass
                            )
                        }
                        if (currentRef.get() == expected) return
                    }
                    Thread.sleep(100)
                }
                val activity = currentActivity()
                if (activity != null) {
                    val currentRef = AtomicReference<Class<out Fragment>?>()
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        activity.supportFragmentManager.executePendingTransactions()
                        currentRef.set(
                            activity.supportFragmentManager.findFragmentById(R.id.shell_container)?.javaClass
                        )
                    }
                    assertEquals(expected, currentRef.get())
                }
            }

            waitForFragment(TestMapFragment::class.java)
            withCurrentActivity {
                findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
                    .selectedItemId = R.id.nav_dashboard
            }
            waitForFragment(TestDashboardFragment::class.java)
            withCurrentActivity {
                findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
                    .selectedItemId = R.id.nav_map
            }
            waitForFragment(TestMapFragment::class.java)
        } finally {
            MainShellActivity.fragmentFactoryForTests = null
            scenario.close()
        }
    }

    @Test
    fun addGutterListMatchesFigmaGeometryAndRequirementControlOrder() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_TaoYuanGutter
        )
        val parent = FrameLayout(context)
        val root = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_add_gutter_list, parent, false)
        val toolbar = root.findViewById<LinearLayout>(R.id.btnAddGutterListAdd).parent.parent as LinearLayout
        val add = root.findViewById<LinearLayout>(R.id.btnAddGutterListAdd)
        val close = root.findViewById<ImageButton>(R.id.btnAddGutterListClose)
        val row = LayoutInflater.from(context).inflate(R.layout.item_add_gutter_list, parent, false)
        fun pxToDp(px: Int): Int = (px / context.resources.displayMetrics.density).toInt()

        assertEquals(70, pxToDp(toolbar.layoutParams.height))
        assertEquals(88, pxToDp(row.layoutParams.height))
        assertEquals(32, pxToDp(row.paddingLeft))
        val widthPx = (402 * context.resources.displayMetrics.density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        root.layout(0, 0, widthPx, root.measuredHeight)
        assertTrue(close.left < add.left)
        assertNotNull(root.findViewById<View>(R.id.rvAddGutterList))
    }

    @Test
    fun addGutterListRendersSecondsAndEffectiveNodeCount() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_TaoYuanGutter
        )
        val parent = FrameLayout(context)
        val adapter = AddGutterListAdapter {}
        adapter.submitList(
            listOf(
                GutterSessionDraft(
                    id = 101L,
                    createdAt = 1_725_312_345_000L,
                    waypoints = listOf(
                        WaypointSnapshot(latitude = 25.0, longitude = 121.0),
                        WaypointSnapshot(basicData = hashMapOf("is_virtual" to "true")),
                        WaypointSnapshot()
                    )
                )
            )
        )

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("側溝草稿 1", holder.binding.tvAddGutterListTitle.text.toString())
        assertTrue(holder.binding.tvAddGutterListTime.text.toString().matches(Regex("建立時間：\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}")))
        assertEquals("已存節點：2", holder.binding.tvAddGutterListNodes.text.toString())
    }

    @Test
    fun multiGutterDraftsKeepIndependentIdsAcrossCoordinatorRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = GutterSessionRepository(context)
        val coordinator = MultiGutterSessionCoordinator(repository)
        val first = coordinator.addItem()
        val second = coordinator.addItem()
        try {
            repository.save(
                GutterSessionDraft(
                    id = first.draftId,
                    createdAt = first.createdAt,
                    workflowOwnership = WORKFLOW_MULTI_GUTTER,
                    waypoints = listOf(WaypointSnapshot(latitude = 25.0, longitude = 121.0))
                )
            )
            repository.save(
                GutterSessionDraft(
                    id = second.draftId,
                    createdAt = second.createdAt + 1,
                    workflowOwnership = WORKFLOW_MULTI_GUTTER,
                    waypoints = listOf(WaypointSnapshot(latitude = 25.1, longitude = 121.1))
                )
            )

            val recreated = MultiGutterSessionCoordinator(repository)
            val recreatedIds = recreated.items().map { it.draftId }
            assertTrue(recreatedIds.containsAll(listOf(first.draftId, second.draftId)))
            assertTrue(recreatedIds.indexOf(first.draftId) < recreatedIds.indexOf(second.draftId))
            assertTrue(recreated.drafts().map { it.id }.toSet().containsAll(setOf(first.draftId, second.draftId)))

            repository.delete(first.draftId)
            val afterSingleDelete = MultiGutterSessionCoordinator(repository)
            assertTrue(afterSingleDelete.items().none { it.draftId == first.draftId })
            assertTrue(afterSingleDelete.items().any { it.draftId == second.draftId })

            recreated.clearSession()
            assertTrue(recreated.items().isEmpty())
        } finally {
            repository.delete(first.draftId)
            repository.delete(second.draftId)
        }
    }

    @Test
    fun addGutterListWiresAddAndCloseConfirmationCallbacks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf("animator_duration_scale", "transition_animation_scale", "window_animation_scale").forEach { key ->
            instrumentation.uiAutomation.executeShellCommand("settings put global $key 0").close()
        }
        MainShellActivity.fragmentFactoryForTests = { TestAddGutterHostFragment() }
        val scenario = ActivityScenario.launch(MainShellActivity::class.java)
        try {
            val hostRef = AtomicReference<TestAddGutterHostFragment?>()
            scenario.onActivity { activity ->
                hostRef.set(
                    activity.supportFragmentManager
                        .findFragmentById(R.id.shell_container) as TestAddGutterHostFragment
                )
                hostRef.get()!!.showList(withContent = false)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withId(R.id.btnAddGutterListAdd)).perform(click())
            assertTrue(hostRef.get()!!.addClicked)

            onView(withId(R.id.btnAddGutterListClose)).perform(click())
            onView(withText("未上傳側溝草稿將儲存到草稿中")).check(matches(isDisplayed()))
            onView(withText("取消")).perform(click())
            assertTrue(!hostRef.get()!!.confirmedClose)

            onView(withId(R.id.btnAddGutterListClose)).perform(click())
            onView(withText("確定")).perform(click())
            assertTrue(hostRef.get()!!.confirmedClose)
        } finally {
            MainShellActivity.fragmentFactoryForTests = null
            scenario.close()
        }
    }

    @Test
    fun requestedDraftIdIsRoutedIntoTheAddSession() {
        val start = GutterSessionFlowCoordinator().createAddSessionStart(
            isOfflineMainMode = false,
            requestedDraftId = 987654321L
        )

        assertEquals(987654321L, start.draftId)
    }

    @Test
    fun closingMultiGutterListFinalizesDraftsForLaterUse() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = GutterSessionRepository(context)
        val coordinator = GutterDraftCoordinator(context, repository)
        val draftId = repository.allocateDraftId()
        val draft = GutterSessionDraft(
            id = draftId,
            workflowOwnership = WORKFLOW_MULTI_GUTTER,
            waypoints = listOf(WaypointSnapshot(latitude = 25.0, longitude = 121.0))
        )

        try {
            coordinator.finalizeDrafts(listOf(draft))
            val saved = repository.getById(draftId)
            assertNotNull(saved)
            assertEquals(WORKFLOW_MULTI_GUTTER, saved!!.workflowOwnership)
            assertEquals(25.0, saved.waypoints.first().latitude ?: -1.0, 0.0)
        } finally {
            repository.delete(draftId)
        }
    }
}
