package com.example.taoyuangutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.gutter.GutterFormActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterBasicInfoUiTest {

    @Test
    fun newFormShowsRequiredOrderLabelsButtonsAndDefaults() {
        launchForm(hashMapOf()).use { scenario ->
            onView(withId(R.id.rbIsBroken0)).check(matches(isChecked()))
            onView(withId(R.id.rbIsSilt0)).check(matches(isChecked()))
            onView(withId(R.id.switchPageBar)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            onView(withId(R.id.btnTakePhotoSlot1)).check(matches(withText("拍攝照片（概況）")))
            onView(withId(R.id.btnTakePhotoSlot2)).check(matches(withText("拍攝照片（寬度）")))
            onView(withId(R.id.btnTakePhotoSlot3)).check(matches(withText("拍攝照片（深度）")))

            scenario.onActivity { activity ->
                val ids = intArrayOf(
                    R.id.tvMeasurementStatusTitle,
                    R.id.tvMeasureIdTitle,
                    R.id.tvLocationTitle,
                    R.id.tvGutterTypeTitle,
                    R.id.tvOverviewPhotoTitle,
                    R.id.tvDepthPhotoTitle,
                    R.id.tvDepthTitle,
                    R.id.tvCoverThicknessTitle,
                    R.id.tvWidthPhotoTitle,
                    R.id.tvTopWidthTitle,
                    R.id.tvMatTypeTitle,
                    R.id.tvBrokenTitle,
                    R.id.tvHangingTitle,
                    R.id.tvSiltTitle,
                    R.id.tvRemarksTitle
                )
                val tops = ids.map { id ->
                    val location = IntArray(2)
                    activity.findViewById<android.view.View>(id).getLocationOnScreen(location)
                    location[1]
                }
                assertTrue("labels must follow the approved order: $tops", tops.zipWithNext().all { it.first < it.second })
            }
        }
    }

    @Test
    fun existingBrokenAndSiltValuesArePreserved() {
        launchForm(hashMapOf("IS_BROKEN" to "1", "IS_SILT" to "2")).use {
            onView(withId(R.id.rbIsBroken1)).check(matches(isChecked()))
            onView(withId(R.id.rbIsSilt2)).check(matches(isChecked()))
        }
    }

    @Test
    fun turningVirtualPointOffRestoresNormalFormInteraction() {
        launchForm(hashMapOf("is_virtual" to "1")).use { scenario ->
            onView(withId(R.id.cbIsVirtual)).check(matches(isChecked()))
            onView(withId(R.id.switchPageBar)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            onView(withId(R.id.cbCantOpen)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            listOf(
                R.id.tvGutterTypeTitle,
                R.id.layoutGutterTypeSelector,
                R.id.tvOverviewPhotoTitle,
                R.id.layoutOverviewPhotoSection,
                R.id.tvDepthPhotoTitle,
                R.id.layoutDepthPhotoSection,
                R.id.tvDepthTitle,
                R.id.tilDepth,
                R.id.tvCoverThicknessTitle,
                R.id.llCoverThicknessWrapper,
                R.id.tvWidthPhotoTitle,
                R.id.layoutWidthPhotoSection,
                R.id.tvTopWidthTitle,
                R.id.tilTopWidth,
                R.id.tvMatTypeTitle,
                R.id.rgMatType,
                R.id.tvBrokenTitle,
                R.id.rgIsBroken,
                R.id.tvHangingTitle,
                R.id.rgIsHanging,
                R.id.tvSiltTitle,
                R.id.rgIsSilt,
                R.id.tvRemarksTitle,
                R.id.tilRemarks
            ).forEach { id ->
                onView(withId(id)).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            }

            onView(withId(R.id.cbIsVirtual)).perform(androidx.test.espresso.action.ViewActions.click())

            onView(withId(R.id.cbIsVirtual)).check(matches(isNotChecked()))
            onView(withId(R.id.switchPageBar)).check(matches(isDisplayed()))
            onView(withId(R.id.cbCantOpen)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val viewPager = activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                assertTrue("turning virtual mode off must restore pager interaction", viewPager.isUserInputEnabled)
            }
        }
    }

    private fun launchForm(data: HashMap<String, String>): ActivityScenario<GutterFormActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return ActivityScenario.launch(
            GutterFormActivity.newIntent(
                context,
                arrayListOf("test"),
                doubleArrayOf(0.0),
                doubleArrayOf(0.0),
                basicData = data,
                sessionDraftId = 0L,
                sessionIsOffline = true
            )
        )
    }
}
