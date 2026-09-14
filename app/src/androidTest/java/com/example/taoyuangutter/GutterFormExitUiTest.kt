package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.example.taoyuangutter.gutter.GutterFormActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterFormExitUiTest {
    @Test
    fun backButtonWarnsBeforeLeavingIncompleteForm() {
        launchIncompleteForm().use {
            onView(withId(R.id.btnBack)).perform(click())
            onView(withText("尚未填寫完畢")).check(matches(isDisplayed()))
            onView(withText("確認")).perform(click())
        }
    }

    @Test
    fun systemBackWarnsBeforeLeavingIncompleteForm() {
        launchIncompleteForm().use {
            pressBack()
            onView(withText("尚未填寫完畢")).check(matches(isDisplayed()))
            onView(withText("確認")).perform(click())
        }
    }

    @Test
    fun completedVirtualFormLeavesWithoutWarning() {
        launchCompletedVirtualForm().use { scenario ->
            onView(withId(R.id.btnBack)).perform(click())
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun returningFromEditToPreviewDoesNotShowExitWarning() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<GutterFormActivity>(
            GutterFormActivity.newViewIntent(
                context, "test", 1.0, 1.0, 0,
                hashMapOf("NODE_TYP" to "1", "NODE_X" to "1.0", "NODE_Y" to "1.0")
            )
        ).use {
            onView(withId(R.id.btnEdit)).perform(click())
            onView(withId(R.id.btnBack)).perform(click())
            onView(withId(R.id.btnEdit)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backButtonWarnsWhenStandardFormIsMissingAnyNewlyAuditedField() {
        listOf("NODE_TYP", "NODE_X", "NODE_Y", "XY_NUM", "COVER_DEP").forEach { missingKey ->
            launchStandardForm(missingKey).use {
                onView(withId(R.id.btnBack)).perform(click())
                onView(withText("尚未填寫完畢")).check(matches(isDisplayed()))
                onView(withText("確認")).perform(click())
            }
        }
    }

    @Test
    fun backButtonWarnsWhenOpenGutterIsMissingARequiredDetailOtherThanCoverThickness() {
        launchStandardForm(missingKey = "NODE_DEP", gutterType = "1").use {
            onView(withId(R.id.btnBack)).perform(click())
            onView(withText("尚未填寫完畢")).check(matches(isDisplayed()))
            onView(withText("確認")).perform(click())
        }
    }

    @Test
    fun backButtonWarnsWhenCantOpenFormIsMissingPointIdentityData() {
        launchStandardForm(missingKey = "XY_NUM", isCantOpen = true).use {
            onView(withId(R.id.btnBack)).perform(click())
            onView(withText("尚未填寫完畢")).check(matches(isDisplayed()))
            onView(withText("確認")).perform(click())
        }
    }

    private fun launchIncompleteForm(): ActivityScenario<GutterFormActivity> {
        val intent = GutterFormActivity.newIntent(
            ApplicationProvider.getApplicationContext(),
            arrayListOf("test"), doubleArrayOf(0.0), doubleArrayOf(0.0),
            basicData = hashMapOf("NODE_TYP" to "1", "IS_CANTOPEN" to ""),
            sessionDraftId = 0L, sessionIsOffline = true
        )
        return ActivityScenario.launch(intent)
    }

    private fun launchCompletedVirtualForm(): ActivityScenario<GutterFormActivity> {
        val intent = GutterFormActivity.newIntent(
            ApplicationProvider.getApplicationContext(),
            arrayListOf("test"), doubleArrayOf(1.0), doubleArrayOf(1.0),
            basicData = hashMapOf(
                "NODE_TYP" to "1", "NODE_X" to "1.0", "NODE_Y" to "1.0",
                "XY_NUM" to "A-1", "is_virtual" to "1"
            ),
            sessionDraftId = 0L, sessionIsOffline = true
        )
        return ActivityScenario.launch(intent)
    }

    private fun launchStandardForm(
        missingKey: String,
        gutterType: String = "2",
        isCantOpen: Boolean = false
    ): ActivityScenario<GutterFormActivity> {
        val data = hashMapOf(
            "NODE_TYP" to gutterType,
            "NODE_X" to "121.000000",
            "NODE_Y" to "24.000000",
            "XY_NUM" to "A-1",
            "COVER_DEP" to "3",
            "NODE_DEP" to "10",
            "NODE_WID" to "26",
            "MAT_TYP" to "1",
            "IS_BROKEN" to "0",
            "IS_HANGING" to "0",
            "IS_SILT" to "0",
            "IS_CANTOPEN" to if (isCantOpen) "1" else ""
        )
        data[missingKey] = ""
        return ActivityScenario.launch(
            GutterFormActivity.newIntent(
                ApplicationProvider.getApplicationContext(),
                arrayListOf("test"), doubleArrayOf(24.0), doubleArrayOf(121.0),
                basicData = data, sessionDraftId = 0L, sessionIsOffline = true
            )
        )
    }
}
