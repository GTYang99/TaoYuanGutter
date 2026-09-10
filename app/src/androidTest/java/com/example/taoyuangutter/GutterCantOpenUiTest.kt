package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.CoreMatchers.not
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.gutter.GutterFormActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterCantOpenUiTest {
    @Test
    fun cancelDialogKeepsOriginalState() {
        launchForm().use {
            it.onActivity { activity -> activity.findViewById<android.widget.EditText>(R.id.etDepth).setText("10") }
            onView(withId(R.id.cbCantOpen)).perform(click())
            onView(withText("無法開蓋照片與已填寫資訊將被清除")).check(matches(isDisplayed()))
            onView(withText("取消")).perform(click())
            onView(withId(R.id.cbCantOpen)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isNotChecked()))
            onView(withId(R.id.etDepth)).check(matches(withText("10")))
        }
    }

    @Test
    fun confirmDialogClearsAffectedFields() {
        launchForm().use {
            onView(withId(R.id.cbCantOpen)).perform(click())
            onView(withText("確認")).perform(click())
            onView(withId(R.id.cbCantOpen)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()))
            onView(withId(R.id.etDepth)).check(matches(withText("")))
        }
    }

    @Test
    fun viewModeDoesNotShowCantOpenDialog() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val data = hashMapOf("NODE_TYP" to "1", "IS_CANTOPEN" to "")
        ActivityScenario.launch<GutterFormActivity>(
            GutterFormActivity.newViewIntent(context, "test", 0.0, 0.0, 0, data)
        ).use {
            onView(withId(R.id.cbCantOpen)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun snapshotSurvivesConfigurationRecreation() {
        launchForm().use {
            it.onActivity { activity -> activity.findViewById<android.widget.EditText>(R.id.etDepth).setText("10") }
            onView(withId(R.id.cbCantOpen)).perform(click())
            onView(withText("確認")).perform(click())
            it.recreate()
            onView(withId(R.id.cbCantOpen)).perform(click())
            onView(withId(R.id.etDepth)).check(matches(withText("10")))
        }
    }

    private fun launchForm(): ActivityScenario<GutterFormActivity> {
        val data = hashMapOf(
            "NODE_TYP" to "1", "NODE_DEP" to "10", "NODE_WID" to "20",
            "COVER_DEP" to "3", "MAT_TYP" to "1", "IS_CANTOPEN" to ""
        )
        val intent = GutterFormActivity.newIntent(
            ApplicationProvider.getApplicationContext(),
            arrayListOf("test"), doubleArrayOf(0.0), doubleArrayOf(0.0),
            basicData = data, sessionDraftId = 0L, sessionIsOffline = true
        )
        return ActivityScenario.launch(intent)
    }
}
