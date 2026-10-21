package com.example.taoyuangutter.gutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.MainShellActivity
import com.example.taoyuangutter.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Debug1021IncompleteSubmitUiTest {

    @Test
    fun incompleteRequiredDataRestoresAddButtonAfterDialogDismissal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        val previousToken = prefs.getString("auth_token", null)
        prefs.edit().putString("auth_token", "debug-1021-test-token").commit()

        val hostRef = AtomicReference<Debug1012UploadHostFragment?>()
        MainShellActivity.fragmentFactoryForTests = { _ ->
            Debug1012UploadHostFragment().also { hostRef.set(it) }
        }
        val scenario = ActivityScenario.launch(MainShellActivity::class.java)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        val sheetRef = AtomicReference<AddGutterBottomSheet?>()

        try {
            waitForHost(scenario, hostRef)
            scenario.onActivity {
                val host = checkNotNull(hostRef.get())
                val sheet = AddGutterBottomSheet.newInstance()
                host.activeSheet = sheet
                sheetRef.set(sheet)
                sheet.showNow(host.childFragmentManager, "debug-1021-incomplete-submit")
                sheet.updateWaypointLocation(0, LatLng(24.99, 121.30))
                sheet.updateWaypointLocation(1, LatLng(24.991, 121.301))
                sheet.updateWaypointBasicData(0, hashMapOf("SPI_TYP" to "1"))
                val submit = sheet.requireView().findViewById<MaterialButton>(R.id.btnSubmitGutter)
                assertTrue("Add button should start enabled", submit.isEnabled)
                submit.performClick()
            }

            onView(withText("資料/照片不足"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText("確定"))
                .inRoot(isDialog())
                .perform(click())

            scenario.onActivity {
                val sheet = checkNotNull(sheetRef.get())
                val submit = sheet.requireView().findViewById<MaterialButton>(R.id.btnSubmitGutter)
                assertEquals(context.getString(R.string.btn_add_gutter), submit.text.toString())
                assertTrue("Add button should be enabled after dismissing validation dialog", submit.isEnabled)
                assertEquals("Validation failure must not submit the gutter", 0, hostRef.get()?.submittingCount)
            }
        } finally {
            MainShellActivity.fragmentFactoryForTests = null
            if (previousToken == null) {
                prefs.edit().remove("auth_token").commit()
            } else {
                prefs.edit().putString("auth_token", previousToken).commit()
            }
            scenario.close()
        }
    }

    private fun waitForHost(
        scenario: ActivityScenario<MainShellActivity>,
        hostRef: AtomicReference<Debug1012UploadHostFragment?>
    ) {
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val ready = AtomicReference(false)
            scenario.onActivity {
                val host = hostRef.get()
                ready.set(host?.isResumed == true && !host.childFragmentManager.isStateSaved)
            }
            if (ready.get()) return
            Thread.sleep(50)
        }
        throw AssertionError("Test host FragmentManager did not resume")
    }
}
