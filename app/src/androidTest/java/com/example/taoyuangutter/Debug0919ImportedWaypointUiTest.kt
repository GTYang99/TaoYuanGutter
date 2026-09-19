package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.taoyuangutter.gutter.GutterFormActivity
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class Debug0919ImportedWaypointUiTest {
    @Test
    fun importedVirtualWaypointKeepsLocationAndVirtualToggleLockedAcrossEditAndRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val data = hashMapOf(
            "NODE_TYP" to "1",
            "NODE_X" to "121.000000",
            "NODE_Y" to "24.000000",
            "XY_NUM" to "A-0919",
            "_isImported" to "1",
            "is_virtual" to "1"
        )

        ActivityScenario.launch<GutterFormActivity>(
            GutterFormActivity.newViewIntent(
                context,
                "起點",
                24.0,
                121.0,
                0,
                data
            )
        ).use { scenario ->
            assertImportedControlsLocked()

            onView(withId(R.id.btnEdit)).perform(click())
            assertImportedControlsLocked()

            scenario.recreate()
            assertImportedControlsLocked()
        }
    }

    private fun assertImportedControlsLocked() {
        onView(withId(R.id.cbIsVirtual)).check(matches(isChecked()))
        onView(withId(R.id.cbIsVirtual)).check(matches(not(isEnabled())))
        onView(withId(R.id.btnPickLocation)).check(matches(not(isEnabled())))
    }
}
