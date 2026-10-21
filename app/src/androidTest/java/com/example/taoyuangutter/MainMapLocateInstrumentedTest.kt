package com.example.taoyuangutter

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
import com.example.taoyuangutter.map.MapWorkspaceFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator coverage for the primary map's explicit Locate button. */
@RunWith(AndroidJUnit4::class)
class MainMapLocateInstrumentedTest {

    @Test
    fun hostMapLocateButtonMovesCameraToFreshFix() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val currentPoint = LatLng(25.03, 121.5)
        val beforeLocate = LatLng(24.3, 120.8)

        try {
            setMockLocation(currentPoint)
            MainShellActivity.fragmentFactoryForTests = { MapWorkspaceFragment.newInstance() }
            ActivityScenario.launch(MainShellActivity::class.java).use { scenario ->
                val googleMap = AtomicReference<GoogleMap?>()
                val mapReady = CountDownLatch(1)
                scenario.onActivity { activity ->
                    val mapFragment = activity.supportFragmentManager
                        .findFragmentByTag("tab:${R.id.nav_map}") as? MapWorkspaceFragment
                    assertNotNull("MainShellActivity did not create its production map", mapFragment)
                    val nestedMap = mapFragment!!.childFragmentManager
                        .findFragmentById(R.id.map) as? SupportMapFragment
                    assertNotNull("Production map fragment was not attached", nestedMap)
                    nestedMap!!.getMapAsync { map ->
                        googleMap.set(map)
                        mapReady.countDown()
                    }
                }
                assertTrue("GoogleMap did not become ready", mapReady.await(30, TimeUnit.SECONDS))

                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    googleMap.get()!!.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(beforeLocate, 14f)
                    )
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                onView(withId(R.id.btnMyLocation)).perform(click())

                val deadline = System.currentTimeMillis() + 35_000L
                var actual = cameraTarget(scenario, googleMap)
                while (System.currentTimeMillis() < deadline && distanceMeters(currentPoint, actual) > 100.0) {
                    Thread.sleep(250L)
                    actual = cameraTarget(scenario, googleMap)
                }
                assertTrue(
                    "Host Locate left camera at $actual instead of the fresh location $currentPoint",
                    distanceMeters(currentPoint, actual) <= 100.0
                )
            }
        } finally {
            MainShellActivity.fragmentFactoryForTests = null
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

    private fun distanceMeters(expected: LatLng, actual: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(expected.latitude, expected.longitude, actual.latitude, actual.longitude, result)
        return result[0].toDouble()
    }

    private fun cameraTarget(
        scenario: ActivityScenario<MainShellActivity>,
        googleMap: AtomicReference<GoogleMap?>
    ): LatLng {
        val target = AtomicReference<LatLng>()
        scenario.onActivity {
            target.set(googleMap.get()!!.cameraPosition.target)
        }
        return target.get()
    }
}
