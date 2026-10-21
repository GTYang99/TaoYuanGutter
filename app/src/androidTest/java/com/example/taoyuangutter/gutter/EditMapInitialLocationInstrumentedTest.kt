package com.example.taoyuangutter.gutter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.view.View
import android.view.ViewTreeObserver
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.R
import com.example.taoyuangutter.map.MapCoordinatePolicy
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Emulator integration coverage for the edit-only, camera-only location behavior. */
@RunWith(AndroidJUnit4::class)
class EditMapInitialLocationInstrumentedTest {

    @Test
    fun missingCoordinateEditWithFusedMockLocationCentersCameraOnly() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val testPoint = LatLng(25.03, 121.5)
        try {
            setMockLocation(testPoint)
            launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
                val actual = awaitCameraTarget(scenario, testPoint, toleranceMeters = 100.0)
                assertNotNull("Fused mock location did not center the form map", actual)
                assertTrue("Form map did not center on the Fused mock location", distanceMeters(testPoint, actual!!) <= 100.0)
                awaitLocationFlowFinish(scenario)
                scenario.onActivity { activity ->
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.formMapContainer).visibility)
                    assertEquals(0.0, readField<Double>(activity, "currentLat"), 0.0)
                    assertEquals(0.0, readField<Double>(activity, "currentLng"), 0.0)
                    @Suppress("UNCHECKED_CAST")
                    val formData = readField<HashMap<String, String>>(activity, "currentFormData")
                    assertTrue("Location must not populate NODE_X", formData["NODE_X"].isNullOrBlank())
                    assertTrue("Location must not populate NODE_Y", formData["NODE_Y"].isNullOrBlank())
                    val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                    assertEquals(null, savedWaypoint.latitude)
                    assertEquals(null, savedWaypoint.longitude)
                }
                onView(withText(R.string.edit_map_location_unavailable_title)).check(doesNotExist())
            }
        } finally {
            setMockLocation(null)
        }
    }

    @Test
    fun manualPanBeforeFusedMockCallbackKeepsManualCameraTarget() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val injectedLocation = LatLng(25.03, 121.5)
        val hostLocation = recentLocation(25.01, 121.48)
        setMockLocation(null)
        try {
            launchForm(
                isEditMode = true,
                waypoint = waypoint(latitude = null, longitude = null),
                hostInitialLocation = hostLocation
            ).use { scenario ->
                awaitLocationAttempt(scenario, expectedAttempt = 1)
                val initialTarget = cameraTarget(scenario)
                assertNotNull("Fresh host location was not used for the initial map", initialTarget)
                assertTrue(distanceMeters(LatLng(hostLocation.latitude, hostLocation.longitude), initialTarget!!) <= 100.0)
                swipeVisibleMapArea(scenario)
                val manualTarget = cameraTarget(scenario)
                assertNotNull("Manual map gesture did not produce a camera target", manualTarget)
                assertTrue("Manual map gesture did not move the camera", distanceMeters(initialTarget, manualTarget!!) > 100.0)

                setMockLocation(injectedLocation)
                awaitLocationFlowFinish(scenario)
                val afterCallback = cameraTarget(scenario)
                assertNotNull(afterCallback)
                assertTrue("Late Fused callback overrode the manual camera position", distanceMeters(manualTarget, afterCallback!!) <= 100.0)
                assertTrue("Callback location should differ from the manual camera target", distanceMeters(injectedLocation, afterCallback) > 100.0)
                onView(withText(R.string.edit_map_location_unavailable_title)).check(doesNotExist())
            }
        } finally {
            setMockLocation(null)
        }
    }

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

            onView(withText(R.string.edit_map_location_continue))
                .check(matches(isDisplayed()))
                .perform(click())
            onView(withText(R.string.edit_map_location_unavailable_title)).check(doesNotExist())
            val fallbackAfterDismiss = cameraTarget(scenario)
            assertNotNull(fallbackAfterDismiss)
            assertTrue(distanceMeters(fallback, fallbackAfterDismiss!!) <= 100.0)

            swipeVisibleMapArea(scenario)
            val afterManualPan = cameraTarget(scenario)
            assertNotNull(afterManualPan)
            assertTrue("Map did not respond to a manual swipe", distanceMeters(fallback, afterManualPan!!) > 100.0)
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
    fun savedCoordinateEditWithLocationDeniedDoesNotRequestPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        )
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        val saved = LatLng(24.1234, 121.5678)

        launchForm(isEditMode = true, waypoint = waypoint(saved.latitude, saved.longitude)).use { scenario ->
            val actual = awaitCameraTarget(scenario, saved, toleranceMeters = 100.0)
            assertNotNull("Saved-coordinate edit did not retain its waypoint target", actual)
            assertTrue(distanceMeters(saved, actual!!) <= 100.0)
            scenario.onActivity { activity ->
                assertFalse(readField<Boolean>(activity, "editMapLocationFlowActive"))
            }
            onView(withText(R.string.edit_map_location_unavailable_title)).check(doesNotExist())
        }
    }

    @Test
    fun blankFormUsesFreshHostLocationBeforeFirstMapDisplay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prepareLocationPermissionPrompt(context)
        val hostLocation = recentLocation(25.02, 121.53)
        val expected = LatLng(hostLocation.latitude, hostLocation.longitude)
        val firstVisibleCameraTarget = AtomicReference<LatLng?>()
        val firstVisibleFrame = CountDownLatch(1)

        launchForm(
            isEditMode = true,
            waypoint = waypoint(latitude = null, longitude = null),
            hostInitialLocation = hostLocation
        ).use { scenario ->
            scenario.onActivity { activity ->
                val container = activity.findViewById<View>(R.id.formMapContainer)
                container.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (container.visibility == View.VISIBLE) {
                            firstVisibleCameraTarget.set(
                                readField<GoogleMap?>(activity, "formMap")?.cameraPosition?.target
                            )
                            firstVisibleFrame.countDown()
                            container.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        return true
                    }
                })
            }
            assertTrue(
                "Map did not produce its first visible frame after the host fix was applied",
                firstVisibleFrame.await(20, TimeUnit.SECONDS)
            )
            val actual = firstVisibleCameraTarget.get()
            assertNotNull("Form map did not initialize", actual)
            assertTrue(
                "First visible frame did not use the fresh host fix: $actual",
                distanceMeters(expected, actual!!) <= 100.0
            )
            // The OS dialog pauses this Activity; close it before ActivityScenario cleanup.
            runShell("input keyevent KEYCODE_BACK")
        }
    }

    @Test
    fun coarseOnlyPermissionStartsEditLocationWithoutPermissionPrompt() {
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        )
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        )

        launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            val deadline = System.currentTimeMillis() + 10_000L
            var attempt = 0
            while (System.currentTimeMillis() < deadline && attempt == 0) {
                scenario.onActivity { activity ->
                    attempt = readField<Int>(activity, "editMapLocationAttempt")
                }
                if (attempt == 0) Thread.sleep(100L)
            }
            assertTrue("Coarse permission did not start a location attempt", attempt > 0)
            onView(withText(R.string.edit_map_location_permission_title)).check(doesNotExist())
        }
    }

    @Test
    fun returningFromSettingsWithPermissionStartsOneLocationAttempt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preparePermanentLocationDenial(context)

        launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            onView(withText(R.string.edit_map_location_unavailable_title)).check(matches(isDisplayed()))
            onView(withText(R.string.edit_map_location_open_settings)).perform(click())

            runShell("pm grant ${context.packageName} ${Manifest.permission.ACCESS_FINE_LOCATION}")
            runShell("input keyevent KEYCODE_BACK")

            val deadline = System.currentTimeMillis() + 10_000L
            var attempt = 0
            var permissionGranted = false
            while (System.currentTimeMillis() < deadline && (attempt == 0 || !permissionGranted)) {
                scenario.onActivity { activity ->
                    attempt = readField<Int>(activity, "editMapLocationAttempt")
                    permissionGranted = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                }
                if (attempt == 0 || !permissionGranted) Thread.sleep(100L)
            }

            assertTrue("Permission granted in Settings was not visible after returning", permissionGranted)
            assertEquals("Returning from Settings must start only one location attempt", 1, attempt)
            scenario.onActivity { activity ->
                assertFalse(readField<Boolean>(activity, "editMapLocationSettingsLaunched"))
            }
        }
    }

    @Test
    fun returningFromSettingsWithoutPermissionKeepsFallbackUsable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prepareLocationPermissionPrompt(context)
        val fallback = LatLng(24.9929, 121.3011)

        launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            Thread.sleep(1_000L)
            scenario.onActivity { activity ->
                assertEquals(
                    "Blank map must stay hidden while the permission decision is pending",
                    View.INVISIBLE,
                    activity.findViewById<View>(R.id.formMapContainer).visibility
                )
            }
            denySystemLocationPermissionPrompt()
            onView(withText(R.string.edit_map_location_retry)).check(matches(isDisplayed())).perform(click())
            Thread.sleep(1_000L)
            denySystemLocationPermissionPrompt()
            onView(withText(R.string.edit_map_location_unavailable_title)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                assertEquals(
                    "Failure decision must reveal the fallback map",
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.formMapContainer).visibility
                )
            }
            onView(withText(R.string.edit_map_location_open_settings)).perform(click())
            Thread.sleep(1_000L)
            runShell("input keyevent KEYCODE_BACK")
            Thread.sleep(1_000L)

            val deadline = System.currentTimeMillis() + 10_000L
            var settingsLaunched = true
            var flowActive = true
            while (System.currentTimeMillis() < deadline && settingsLaunched) {
                scenario.onActivity { activity ->
                    settingsLaunched = readField<Boolean>(activity, "editMapLocationSettingsLaunched")
                    flowActive = readField<Boolean>(activity, "editMapLocationFlowActive")
                }
                if (settingsLaunched) Thread.sleep(100L)
            }

            assertFalse("Settings return flag was not cleared", settingsLaunched)
            assertFalse("Location flow should stop when permission remains denied", flowActive)
            assertEquals(
                PackageManager.PERMISSION_DENIED,
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            )
            val actual = cameraTarget(scenario)
            assertNotNull("Map camera was not initialized", actual)
            assertTrue("Denied return should retain the Taoyuan fallback", distanceMeters(fallback, actual!!) <= 100.0)
        }
    }

    @Test
    fun freshNewPointWithEmulatorGpsFixCentersCameraOnly() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val testPoint = LatLng(25.03, 121.5)

        try {
            setMockLocation(testPoint)
            launchForm(
                isEditMode = false,
                waypoint = waypoint(latitude = null, longitude = null),
                isNewAddForm = true
            ).use { scenario ->
                val actual = awaitCameraTarget(scenario, testPoint, toleranceMeters = 100.0)
                assertNotNull("Fresh new-point form did not center on the emulator GPS fix", actual)
                scenario.onActivity { activity ->
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.formMapContainer).visibility)
                }
                val distance = distanceMeters(testPoint, actual!!)
                var attempt = 0
                var flowActive = false
                var userMoved = false
                scenario.onActivity { activity ->
                    attempt = readField<Int>(activity, "editMapLocationAttempt")
                    flowActive = readField<Boolean>(activity, "editMapLocationFlowActive")
                    userMoved = readField<Boolean>(activity, "editMapLocationUserMoved")
                }
                assertTrue(
                    "Camera target=$actual, distance=${distance}m, location attempt=$attempt, flowActive=$flowActive, userMoved=$userMoved",
                    distance <= 100.0
                )
                awaitLocationFlowFinish(scenario)
                scenario.onActivity { activity ->
                    assertEquals(0.0, readField<Double>(activity, "currentLat"), 0.0)
                    assertEquals(0.0, readField<Double>(activity, "currentLng"), 0.0)
                    @Suppress("UNCHECKED_CAST")
                    val formData = readField<HashMap<String, String>>(activity, "currentFormData")
                    assertTrue("Location must not populate NODE_X", formData["NODE_X"].isNullOrBlank())
                    assertTrue("Location must not populate NODE_Y", formData["NODE_Y"].isNullOrBlank())
                    val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                    assertEquals(null, savedWaypoint.latitude)
                    assertEquals(null, savedWaypoint.longitude)
                }
                onView(withText(R.string.edit_map_location_unavailable_title)).check(doesNotExist())
            }
        } finally {
            setMockLocation(null)
        }
    }

    @Test
    fun restoredBlankFormWithoutFreshAddFlagUsesLocationCameraOnly() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = GutterSessionRepository(context)
        val draftId = System.currentTimeMillis()
        val blankWaypoint = waypoint(latitude = null, longitude = null)
        repository.save(GutterSessionDraft(id = draftId, waypoints = listOf(blankWaypoint)))
        val testPoint = LatLng(25.03, 121.5)

        try {
            setMockLocation(testPoint)
            launchForm(
                isEditMode = false,
                waypoint = blankWaypoint,
                sessionDraftId = draftId
            ).use { scenario ->
                val actual = awaitCameraTarget(scenario, testPoint, toleranceMeters = 100.0)
                assertNotNull("Restored blank form did not center on the emulator GPS fix", actual)
                assertTrue(distanceMeters(testPoint, actual!!) <= 100.0)
                awaitLocationFlowFinish(scenario)
                scenario.onActivity { activity ->
                    assertEquals(0.0, readField<Double>(activity, "currentLat"), 0.0)
                    assertEquals(0.0, readField<Double>(activity, "currentLng"), 0.0)
                    assertTrue(readField<HashMap<String, String>>(activity, "currentFormData")["NODE_X"].isNullOrBlank())
                    assertTrue(readField<HashMap<String, String>>(activity, "currentFormData")["NODE_Y"].isNullOrBlank())
                    @Suppress("UNCHECKED_CAST")
                    val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                    assertEquals(null, savedWaypoint.latitude)
                    assertEquals(null, savedWaypoint.longitude)
                }
            }
        } finally {
            setMockLocation(null)
            repository.delete(draftId)
        }
    }

    @Test
    fun offlineBlankFormUsesLocationCameraOnly() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val testPoint = LatLng(25.03, 121.5)
        try {
            setMockLocation(testPoint)
            launchForm(
                isEditMode = false,
                waypoint = waypoint(latitude = null, longitude = null),
                isOfflineMode = true
            ).use { scenario ->
                val actual = awaitCameraTarget(scenario, testPoint, toleranceMeters = 100.0)
                assertNotNull("Offline blank form did not center on the emulator GPS fix", actual)
                assertTrue(distanceMeters(testPoint, actual!!) <= 100.0)
                awaitLocationFlowFinish(scenario)
                scenario.onActivity { activity ->
                    assertEquals(0.0, readField<Double>(activity, "currentLat"), 0.0)
                    assertEquals(0.0, readField<Double>(activity, "currentLng"), 0.0)
                    @Suppress("UNCHECKED_CAST")
                    val formData = readField<HashMap<String, String>>(activity, "currentFormData")
                    // Offline form initialization uses 0.0 as its existing missing-coordinate sentinel.
                    // Camera-only location must keep the sentinel instead of persisting the injected fix.
                    assertEquals("0.0", formData["NODE_X"])
                    assertEquals("0.0", formData["NODE_Y"])
                    @Suppress("UNCHECKED_CAST")
                    val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                    assertEquals(0.0, savedWaypoint.latitude!!, 0.0)
                    assertEquals(0.0, savedWaypoint.longitude!!, 0.0)
                }
            }
        } finally {
            setMockLocation(null)
        }
    }

    @Test
    fun zeroZeroWaypointIsNotUsedAsFormMapTarget() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val testPoint = LatLng(25.03, 121.5)
        try {
            setMockLocation(testPoint)
            launchForm(
                isEditMode = false,
                waypoint = waypoint(latitude = 0.0, longitude = 0.0)
            ).use { scenario ->
                val actual = awaitCameraTarget(scenario, testPoint, toleranceMeters = 100.0)
                assertNotNull("(0,0) waypoint was treated as a valid camera target", actual)
                assertTrue(distanceMeters(testPoint, actual!!) <= 100.0)
                awaitLocationFlowFinish(scenario)
                scenario.onActivity { activity ->
                    @Suppress("UNCHECKED_CAST")
                    val savedWaypoint = readField<List<WaypointSnapshot>>(activity, "sessionWaypoints")[0]
                    assertEquals(0.0, savedWaypoint.latitude!!, 0.0)
                    assertEquals(0.0, savedWaypoint.longitude!!, 0.0)
                }
            }
        } finally {
            setMockLocation(null)
        }
    }

    @Test
    fun zeroZeroFusedLocationDoesNotMoveBlankFormMapToOrigin() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val injectedLocation = LatLng(0.0, 0.0)

        try {
            setMockLocation(injectedLocation)
            launchForm(isEditMode = true, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
                awaitLocationFlowFinish(scenario)
                val actual = cameraTarget(scenario)
                assertNotNull("Blank form map camera was not initialized", actual)
                assertTrue(
                    "Blank form camera must never target the unusable (0,0) coordinate: $actual",
                    MapCoordinatePolicy.isUsableCoordinate(actual!!.latitude, actual.longitude)
                )
                // A usable cached/device fix may be returned even when GMS rejects the injected (0,0) fix.
                // The separate no-fix case verifies fallback and the unavailable prompt.
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
        } finally {
            setMockLocation(null)
        }
    }

    @Test
    fun unmarkedNewPointKeepsTaoyuanFallback() {
        grantLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        grantLocationPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        setMockLocation(null)
        val fallback = LatLng(24.9929, 121.3011)

        launchForm(isEditMode = false, waypoint = waypoint(latitude = null, longitude = null)).use { scenario ->
            val actual = awaitCameraTarget(scenario, fallback, toleranceMeters = 100.0)
            assertNotNull("Unmarked form did not keep its existing fallback camera", actual)
            assertTrue(distanceMeters(fallback, actual!!) <= 100.0)
            Thread.sleep(2_000)
            val afterLocationWindow = cameraTarget(scenario)
            assertNotNull(afterLocationWindow)
            assertTrue(distanceMeters(fallback, afterLocationWindow!!) <= 100.0)
        }
    }

    private fun launchForm(
        isEditMode: Boolean,
        waypoint: WaypointSnapshot,
        isNewAddForm: Boolean = false,
        isOfflineMode: Boolean = false,
        sessionDraftId: Long = 0L,
        hostInitialLocation: Location? = null
    ): ActivityScenario<GutterFormActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = GutterFormActivity.newIntent(
            context = context,
            labels = arrayListOf(waypoint.label),
            lats = doubleArrayOf(waypoint.latitude ?: 0.0),
            lngs = doubleArrayOf(waypoint.longitude ?: 0.0),
            isEditMode = isEditMode,
            useCurrentLocationForInitialMap = isNewAddForm,
            sessionDraftId = sessionDraftId,
            sessionIsOffline = isOfflineMode,
            sessionWaypointsJson = Gson().toJson(listOf(waypoint))
        )
        hostInitialLocation?.let { location ->
            intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_LAT, location.latitude)
            intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_LNG, location.longitude)
            intent.putExtra(GutterFormActivity.EXTRA_HOST_LAST_TIME, location.time)
            intent.putExtra(
                GutterFormActivity.EXTRA_HOST_LAST_ACC,
                if (location.hasAccuracy()) location.accuracy else -1f
            )
        }
        return ActivityScenario.launch(intent)
    }

    private fun waypoint(latitude: Double?, longitude: Double?) = WaypointSnapshot(
        type = "NODE",
        label = "測試節點",
        latitude = latitude,
        longitude = longitude,
        basicData = hashMapOf("NODE_TYP" to "2")
    )

    private fun recentLocation(latitude: Double, longitude: Double): Location =
        Location("instrumented-test").apply {
            this.latitude = latitude
            this.longitude = longitude
            time = System.currentTimeMillis()
            accuracy = 12f
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
        runShell("am broadcast -n com.example.taoyuangutter.test/com.example.taoyuangutter.MockLocationProviderReceiver $extras")
        Thread.sleep(1_000L)
    }

    private fun awaitLocationAttempt(scenario: ActivityScenario<GutterFormActivity>, expectedAttempt: Int) {
        val deadline = System.currentTimeMillis() + 10_000L
        var attempt = 0
        while (System.currentTimeMillis() < deadline && attempt < expectedAttempt) {
            scenario.onActivity { activity -> attempt = readField<Int>(activity, "editMapLocationAttempt") }
            if (attempt < expectedAttempt) Thread.sleep(100L)
        }
        assertEquals("Edit map location attempt did not start", expectedAttempt, attempt)
    }

    private fun prepareLocationPermissionPrompt(context: Context) {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).forEach { permission ->
            runShell("pm revoke ${context.packageName} $permission")
            runShell("pm clear-permission-flags ${context.packageName} $permission user-set user-fixed")
        }
    }

    private fun preparePermanentLocationDenial(context: Context) {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).forEach { permission ->
            runShell("pm revoke ${context.packageName} $permission")
            runShell("pm clear-permission-flags ${context.packageName} $permission user-set user-fixed")
            runShell("pm set-permission-flags ${context.packageName} $permission user-set user-fixed")
        }
    }

    private fun denySystemLocationPermissionPrompt() {
        // The fixed Android 14 test emulator is 1080x2400; this is the native "Don't allow" button.
        runShell("input tap 540 1748")
    }

    private fun runShell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }

    private fun cameraTarget(scenario: ActivityScenario<GutterFormActivity>): LatLng? {
        var target: LatLng? = null
        scenario.onActivity { activity ->
            val map = readField<GoogleMap?>(activity, "formMap")
            target = map?.cameraPosition?.target
        }
        return target
    }

    private fun swipeVisibleMapArea(scenario: ActivityScenario<GutterFormActivity>) {
        val gesture = IntArray(4)
        scenario.onActivity { activity ->
            val mapContainer = activity.findViewById<View>(R.id.formMapContainer)
            val screenLocation = IntArray(2)
            mapContainer.getLocationOnScreen(screenLocation)
            gesture[0] = screenLocation[0] + mapContainer.width / 3
            gesture[1] = screenLocation[1] + mapContainer.height / 12
            gesture[2] = gesture[0] + mapContainer.width / 4
            gesture[3] = screenLocation[1] + mapContainer.height / 5
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "input swipe ${gesture[0]} ${gesture[1]} ${gesture[2]} ${gesture[3]} 500"
        ).close()
        Thread.sleep(1_000L)
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
