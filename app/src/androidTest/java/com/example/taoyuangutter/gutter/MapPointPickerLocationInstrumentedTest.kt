package com.example.taoyuangutter.gutter

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.R
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator coverage for the point picker's explicit, viewport-only Locate action. */
@RunWith(AndroidJUnit4::class)
class MapPointPickerLocationInstrumentedTest {

    @Test
    fun explicitLocateMovesCameraToFreshFixWithoutFinishingPicker() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val savedPoint = LatLng(24.1234, 121.5678)
        val currentPoint = LatLng(25.03, 121.5)

        try {
            setMockLocation(currentPoint)
            val intent = MapPointPickerActivity.newIntent(
                context = context,
                initialLat = savedPoint.latitude,
                initialLng = savedPoint.longitude
            )
            ActivityScenario.launch<MapPointPickerActivity>(intent).use { scenario ->
                val initial = awaitCameraTarget(scenario, savedPoint)
                assertNotNull("Picker did not preserve its supplied point", initial)
                assertTrue(distanceMeters(savedPoint, initial!!) <= 100.0)

                onView(withId(R.id.fabMyLocation)).perform(click())

                val located = awaitCameraTarget(scenario, currentPoint)
                assertNotNull("Locate did not move the picker to the current fix", located)
                assertTrue(distanceMeters(currentPoint, located!!) <= 100.0)
                scenario.onActivity { activity ->
                    assertFalse("Locate must leave the picker open; Confirm returns the coordinate", activity.isFinishing)
                }
            }
        } finally {
            setMockLocation(null)
        }
    }

    private fun grantLocationPermission(permission: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            ApplicationProvider.getApplicationContext<Context>().packageName,
            permission
        )
    }

    private fun setMockLocation(location: LatLng?) {
        val extras = if (location == null) {
            "--es action disable"
        } else {
            "--es action enable --ef lat ${location.latitude} --ef lng ${location.longitude}"
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("am broadcast -n com.example.taoyuangutter.test/com.example.taoyuangutter.MockLocationProviderReceiver $extras")
            .close()
        Thread.sleep(1_000L)
    }

    private fun awaitCameraTarget(
        scenario: ActivityScenario<MapPointPickerActivity>,
        expected: LatLng
    ): LatLng? {
        val deadline = System.currentTimeMillis() + 35_000L
        var target = cameraTarget(scenario)
        while (System.currentTimeMillis() < deadline &&
            (target == null || distanceMeters(expected, target!!) > 100.0)
        ) {
            Thread.sleep(250L)
            target = cameraTarget(scenario)
        }
        return target
    }

    private fun cameraTarget(scenario: ActivityScenario<MapPointPickerActivity>): LatLng? {
        var target: LatLng? = null
        scenario.onActivity { activity ->
            val field = MapPointPickerActivity::class.java.getDeclaredField("googleMap").apply {
                isAccessible = true
            }
            val map = field.get(activity) as? com.google.android.gms.maps.GoogleMap
            target = map?.cameraPosition?.target
        }
        return target
    }

    private fun distanceMeters(expected: LatLng, actual: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(expected.latitude, expected.longitude, actual.latitude, actual.longitude, result)
        return result[0].toDouble()
    }
}
