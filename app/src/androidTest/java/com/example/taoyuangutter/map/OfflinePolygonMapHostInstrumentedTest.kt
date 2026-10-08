package com.example.taoyuangutter.map

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.MainShellActivity
import com.example.taoyuangutter.R
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.MapPointPickerActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class OfflinePolygonMapHostInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers() {
        repeat(HOST_REOPEN_COUNT) {
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OFFLINE_MAIN, true)
            val scenario = ActivityScenario.launch<MainActivity>(intent)
            try {
                val map = awaitMainMap(scenario)
                scenario.onActivity { exerciseCameraProfile(map) }
                onView(withId(R.id.btnLayers)).perform(click())
                assertLayerToggle(R.id.cbWaterOld, "水務局舊資料（.gpkg）")
                assertLayerToggle(R.id.cbPossible, "可能側溝位置（.gpkg）")
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun mapWorkspaceFragmentShowsAndTogglesBothGeoPackageLayers() {
        repeat(HOST_REOPEN_COUNT) {
            val scenario = ActivityScenario.launch<MainShellActivity>(Intent(context, MainShellActivity::class.java))
            try {
                val mapReady = CountDownLatch(1)
                val mapReference = AtomicReference<GoogleMap>()
                scenario.onActivity { activity ->
                    val workspace = activity.supportFragmentManager
                        .findFragmentById(R.id.shell_container) as? MapWorkspaceFragment
                    assertNotNull("MainShellActivity should host MapWorkspaceFragment", workspace)
                    val map = workspace!!.childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                    assertNotNull("MapWorkspaceFragment should host its Google map", map)
                    map!!.getMapAsync { googleMap ->
                        mapReference.set(googleMap)
                        mapReady.countDown()
                    }
                }
                assertTrue("MapWorkspaceFragment Google map should become ready", mapReady.await(60, TimeUnit.SECONDS))
                scenario.onActivity { exerciseCameraProfile(mapReference.get()) }

                onView(withId(R.id.btnLayers)).perform(click())
                assertLayerToggle(R.id.cbWaterOld, "水務局舊資料（.gpkg）")
                assertLayerToggle(R.id.cbPossible, "可能側溝位置（.gpkg）")
            } finally {
                scenario.close()
            }
        }
    }

    @Test
    fun gutterFormAndPointPickerMapHostsBecomeReady() {
        repeat(HOST_REOPEN_COUNT) {
            val formScenario = ActivityScenario.launch<GutterFormActivity>(
                GutterFormActivity.newViewIntent(
                    context = context,
                    label = "FEAT-1006 emulator test",
                    lat = 24.9929,
                    lng = 121.3011,
                    waypointIndex = 0,
                    basicData = hashMapOf(),
                    showWaterOld = true,
                    showPossible = true
                )
            )
            try {
                val formMapReady = CountDownLatch(1)
                val formMapReference = AtomicReference<GoogleMap>()
                formScenario.onActivity { activity ->
                    val map = activity.supportFragmentManager
                        .findFragmentById(R.id.formMapContainer) as? SupportMapFragment
                    assertNotNull("GutterFormActivity should host its Google map", map)
                    map!!.getMapAsync { googleMap ->
                        formMapReference.set(googleMap)
                        formMapReady.countDown()
                    }
                }
                assertTrue("Gutter form Google map should become ready", formMapReady.await(60, TimeUnit.SECONDS))
                formScenario.onActivity { exerciseCameraProfile(formMapReference.get()) }
            } finally {
                formScenario.close()
            }

            val pickerScenario = ActivityScenario.launch<MapPointPickerActivity>(
                MapPointPickerActivity.newIntent(
                    context = context,
                    initialLat = 24.9929,
                    initialLng = 121.3011,
                    showWaterOld = true,
                    showPossible = true
                )
            )
            try {
                val pickerMapReady = CountDownLatch(1)
                val pickerMapReference = AtomicReference<GoogleMap>()
                pickerScenario.onActivity { activity ->
                    val map = activity.supportFragmentManager
                        .findFragmentById(R.id.mapPicker) as? SupportMapFragment
                    assertNotNull("MapPointPickerActivity should host its Google map", map)
                    map!!.getMapAsync { googleMap ->
                        pickerMapReference.set(googleMap)
                        pickerMapReady.countDown()
                    }
                }
                assertTrue("Point picker Google map should become ready", pickerMapReady.await(60, TimeUnit.SECONDS))
                pickerScenario.onActivity { exerciseCameraProfile(pickerMapReference.get()) }
            } finally {
                pickerScenario.close()
            }
        }
    }

    private fun awaitMainMap(scenario: ActivityScenario<MainActivity>): GoogleMap {
        val mapReady = CountDownLatch(1)
        val mapReference = AtomicReference<GoogleMap>()
        scenario.onActivity { activity ->
            val map = activity.supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            assertNotNull("MainActivity should host its Google map", map)
            map!!.getMapAsync { googleMap ->
                mapReference.set(googleMap)
                mapReady.countDown()
            }
        }
        assertTrue("MainActivity Google map should become ready", mapReady.await(60, TimeUnit.SECONDS))
        return mapReference.get()
    }

    private fun exerciseCameraProfile(map: GoogleMap) {
        listOf(16f, 18f, 20f).forEach { zoom ->
            val startedAt = System.nanoTime()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(TAOYUAN_TEST_COORDINATE, zoom))
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            assertEquals("Map host should reach zoom $zoom", zoom, map.cameraPosition.zoom, 0.01f)
            assertTrue("Camera update should not block the UI thread", elapsedMs < MAX_CAMERA_UPDATE_MS)
        }
        map.moveCamera(CameraUpdateFactory.newLatLng(PAN_TARGET))
        assertEquals(PAN_TARGET.latitude, map.cameraPosition.target.latitude, 0.0001)
        assertEquals(PAN_TARGET.longitude, map.cameraPosition.target.longitude, 0.0001)
    }

    private fun assertLayerToggle(viewId: Int, label: String) {
        onView(withId(viewId)).check(matches(withText(label)))
        onView(withId(viewId)).check(matches(isChecked()))
        onView(withId(viewId)).perform(click())
        onView(withId(viewId)).check(matches(isNotChecked()))
        onView(withId(viewId)).perform(click())
        onView(withId(viewId)).check(matches(isChecked()))
    }

    private companion object {
        val TAOYUAN_TEST_COORDINATE = LatLng(24.9929, 121.3011)
        val PAN_TARGET = LatLng(24.997, 121.305)
        const val MAX_CAMERA_UPDATE_MS = 1_000L
        const val HOST_REOPEN_COUNT = 2
    }
}
