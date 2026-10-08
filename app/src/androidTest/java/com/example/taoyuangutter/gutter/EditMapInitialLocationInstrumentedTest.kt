package com.example.taoyuangutter.gutter

import android.Manifest
import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import android.location.Location
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.R
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator integration coverage for the edit-only, camera-only location behavior. */
@RunWith(AndroidJUnit4::class)
class EditMapInitialLocationInstrumentedTest {

    @Test
    fun missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fallback = LatLng(24.9929, 121.3011)

        launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            awaitLocationFlowFinish(scenario)
            onView(withText(R.string.edit_map_location_unavailable_title)).check(matches(isDisplayed()))
            val actual = cameraTarget(scenario)
            assertNotNull("Map camera was not initialized", actual)
            assertTrue("Map did not retain the Taoyuan fallback", distanceMeters(fallback, actual!!) <= 100.0)

            scenario.onActivity { activity ->
                assertEquals(0.0, readField<Double>(activity, "currentLat"), 0.0)
                assertEquals(0.0, readField<Double>(activity, "currentLng"), 0.0)
                @Suppress("UNCHECKED_CAST")
                val formData = readField<HashMap<String, String>>(activity, "currentFormData")
                assertTrue(formData["NODE_X"].isNullOrBlank())
                assertTrue(formData["NODE_Y"].isNullOrBlank())
                val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                assertEquals(null, savedWaypoint.latitude)
                assertEquals(null, savedWaypoint.longitude)
            }
        }
    }

    @Test
    fun savedCoordinateEditKeepsSavedCameraTarget() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val saved = LatLng(24.1234, 121.5678)

        launchForm(isEditMode = true, waypoint = waypoint(saved.latitude, saved.longitude)).use { scenario ->
            val actual = awaitCameraTarget(scenario, saved, toleranceMeters = 100.0)
            assertNotNull("Saved-coordinate edit did not retain its waypoint target", actual)
            assertTrue(distanceMeters(saved, actual!!) <= 100.0)
            scenario.onActivity { activity ->
                assertEquals(false, readField<Boolean>(activity, "editMapLocationFlowActive"))
            }
        }
    }

    @Test
    fun newPointDoesNotRecenterOnDeviceLocation() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fallback = LatLng(24.9929, 121.3011)

        launchForm(isEditMode = false, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            val actual = awaitCameraTarget(scenario, fallback, toleranceMeters = 100.0)
            assertNotNull("New-point form did not keep its existing fallback camera", actual)
            assertTrue(distanceMeters(fallback, actual!!) <= 100.0)
            Thread.sleep(2_000)
            val afterLocationWindow = cameraTarget(scenario)
            assertNotNull(afterLocationWindow)
            assertTrue(distanceMeters(fallback, afterLocationWindow!!) <= 100.0)
        }
    }

    private fun launchForm(
        isEditMode: Boolean,
        waypoint: WaypointSnapshot
    ): ActivityScenario<GutterFormActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return ActivityScenario.launch(
            GutterFormActivity.newIntent(
                context = context,
                labels = arrayListOf(waypoint.label),
                lats = doubleArrayOf(waypoint.latitude ?: 0.0),
                lngs = doubleArrayOf(waypoint.longitude ?: 0.0),
                isEditMode = isEditMode,
                sessionDraftId = 0L,
                sessionWaypointsJson = Gson().toJson(listOf(waypoint))
            )
        )
    }

    private fun waypoint(latitude: Double?, longitude: Double?) = WaypointSnapshot(
        type = "NODE",
        label = "測試節點",
        latitude = latitude,
        longitude = longitude,
        basicData = hashMapOf("NODE_TYP" to "2")
    )

    private fun grantLocationPermission(permission: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            ApplicationProvider.getApplicationContext<Context>().packageName,
            permission
        )
    }

    private fun cameraTarget(scenario: ActivityScenario<GutterFormActivity>): LatLng? {
        var target: LatLng? = null
        scenario.onActivity { activity ->
            val map = readField<GoogleMap?>(activity, "formMap")
            target = map?.cameraPosition?.target
        }
        return target
    }

    private fun awaitLocationFlowFinish(scenario: ActivityScenario<GutterFormActivity>) {
        val deadline = System.currentTimeMillis() + 65_000L
        var active = true
        while (System.currentTimeMillis() < deadline && active) {
            scenario.onActivity { activity ->
                active = readField<Boolean>(activity, "editMapLocationFlowActive")
            }
            if (active) Thread.sleep(250L)
        }
        assertTrue("Location attempts did not finish within the bounded retry window", !active)
    }

    private fun awaitCameraTarget(
        scenario: ActivityScenario<GutterFormActivity>,
        expected: LatLng,
        toleranceMeters: Double
    ): LatLng? {
        val deadline = System.currentTimeMillis() + 35_000L
        var target = cameraTarget(scenario)
        while (System.currentTimeMillis() < deadline &&
            (target == null || distanceMeters(expected, target!!) > toleranceMeters)
        ) {
            Thread.sleep(250L)
            target = cameraTarget(scenario)
        }
        return target
    }

    private fun distanceMeters(expected: LatLng, actual: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(expected.latitude, expected.longitude, actual.latitude, actual.longitude, result)
        return result[0].toDouble()
    }

    private inline fun <reified T> readField(target: Any, name: String): T {
        val field = target.javaClass.getDeclaredField(name).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return field.get(target) as T
    }
}
