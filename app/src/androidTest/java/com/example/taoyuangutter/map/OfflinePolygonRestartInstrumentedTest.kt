package com.example.taoyuangutter.map

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.AppVersionUpdateTestHooks
import com.example.taoyuangutter.BuildConfig
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.AppVersionRecord
import com.example.taoyuangutter.api.AppVersionRepository
import com.example.taoyuangutter.api.AppVersionResponse
import com.example.taoyuangutter.api.GutterApiClient
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.common.ApiBackendTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OfflinePolygonRestartInstrumentedTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun suppressUpdatePrompt() {
        val base = GutterApiClient.serviceFor(ApiBackendTarget.BASE)
        val service = object : GutterApiService by base {
            override suspend fun getAppVersion() = retrofit2.Response.success(
                AppVersionResponse(true, listOf(AppVersionRecord("android", BuildConfig.VERSION_NAME, "https://example.test/app.apk")))
            )
        }
        AppVersionUpdateTestHooks.repositoryFactory = { AppVersionRepository { service } }
    }

    @After
    fun resetUpdatePrompt() {
        AppVersionUpdateTestHooks.repositoryFactory = null
    }

    @Test
    fun saveTwoDistrictsAndOneDisabledLayerBeforeProcessStop() {
        context.getSharedPreferences("overlay_state_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OFFLINE_MAIN, true)
        )
        try {
            onView(withId(R.id.btnLayers)).perform(click())
            onView(withId(R.id.cbDistrictAll)).perform(scrollTo(), click())
            onView(withId(R.id.cbDistrict0)).perform(scrollTo(), click())
            onView(withId(R.id.cbDistrict1)).perform(scrollTo(), click())
            onView(withId(R.id.cbPossible)).perform(scrollTo(), click())
            val preferences = context.getSharedPreferences("overlay_state_prefs", Context.MODE_PRIVATE)
            preferences.edit().commit()
            assertEquals(setOf("桃園區", "中壢區"), preferences.getStringSet("selected_districts", emptySet()))
            assertFalse(preferences.getBoolean("show_possible", true))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun verifyTwoDistrictsAndOneDisabledLayerAfterProcessStop() {
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OFFLINE_MAIN, true)
        )
        try {
            onView(withId(R.id.btnLayers)).perform(click())
            onView(withId(R.id.cbDistrictAll)).check(matches(isNotChecked()))
            onView(withId(R.id.cbDistrict0)).perform(scrollTo()).check(matches(isChecked()))
            onView(withId(R.id.cbDistrict1)).perform(scrollTo()).check(matches(isChecked()))
            onView(withId(R.id.cbDistrict12)).perform(scrollTo()).check(matches(isNotChecked()))
            onView(withId(R.id.cbWaterOld)).perform(scrollTo()).check(matches(isChecked()))
            onView(withId(R.id.cbPossible)).perform(scrollTo()).check(matches(isNotChecked()))
            onView(withId(R.id.cbRegion)).perform(scrollTo()).check(matches(isChecked()))
            scenario.onActivity { activity ->
                val controller = privateProperty(activity, "mapOverlayController") as MapOverlayController
                assertEquals(setOf("桃園區", "中壢區"), controller.currentState().selectedDistricts)
                assertFalse(controller.currentState().showPossible)
            }
        } finally {
            scenario.close()
            context.getSharedPreferences("overlay_state_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    private fun privateProperty(owner: Any, name: String): Any? {
        val getterName = "get" + name.replaceFirstChar(Char::uppercase)
        val getter = owner.javaClass.declaredMethods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
        if (getter != null) {
            getter.isAccessible = true
            return getter.invoke(owner)
        }
        val field = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }
        return field.get(owner)
    }
}
