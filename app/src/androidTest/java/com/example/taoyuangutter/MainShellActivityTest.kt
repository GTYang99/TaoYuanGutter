package com.example.taoyuangutter

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.fragment.app.Fragment
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainShellActivityTest {

    class TestMapFragment : Fragment()
    class TestDashboardFragment : Fragment()

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
}
