package com.example.taoyuangutter.gutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.R
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.WaypointSnapshot
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubmittedDraftReadOnlyUiTest {
    @Test
    fun submittedDraftDisablesFormControlsAfterPagerCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val draftId = System.currentTimeMillis()
        val repository = GutterSessionRepository(context)
        repository.save(
            GutterSessionDraft(
                id = draftId,
                hasSubmittedStoreDitch = true,
                waypoints = listOf(
                    WaypointSnapshot(
                        type = WaypointType.START.name,
                        label = "起點",
                        latitude = 24.0,
                        longitude = 121.0,
                        basicData = hashMapOf(
                            "NODE_TYP" to "1",
                            "NODE_X" to "121.000000",
                            "NODE_Y" to "24.000000",
                            "XY_NUM" to "A-1",
                            "COVER_DEP" to "3",
                            "NODE_DEP" to "10",
                            "NODE_WID" to "26",
                            "MAT_TYP" to "1",
                            "IS_BROKEN" to "0",
                            "IS_HANGING" to "0",
                            "IS_SILT" to "0"
                        )
                    )
                )
            )
        )

        try {
            ActivityScenario.launch<GutterFormActivity>(
                GutterFormActivity.newIntent(
                    context = context,
                    labels = arrayListOf("起點"),
                    lats = doubleArrayOf(24.0),
                    lngs = doubleArrayOf(121.0),
                    index = 0,
                    sessionDraftId = draftId,
                    submittedDraftReadOnly = true
                )
            ).use {
                onView(withId(R.id.submittedReadOnlyOverlay)).check(matches(isDisplayed()))
                onView(withId(R.id.btnBack)).check(matches(isEnabled()))
                onView(withId(R.id.fabSubmit)).check(matches(not(isDisplayed())))
                onView(withId(R.id.cbIsVirtual)).check(matches(isNotEnabled()))
                onView(withId(R.id.etRemarks)).check(matches(isNotEnabled()))
                onView(withId(R.id.btnPickLocation)).check(matches(isNotEnabled()))
                onView(withId(R.id.btnTakePhotoSlot1)).check(matches(isNotEnabled()))
            }
        } finally {
            repository.delete(draftId)
        }
    }
}
