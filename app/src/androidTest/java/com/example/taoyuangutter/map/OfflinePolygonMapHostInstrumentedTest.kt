package com.example.taoyuangutter.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.BitmapFactory
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
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class OfflinePolygonMapHostInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers() {
        repeat(HOST_REOPEN_COUNT) { reopenIndex ->
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OFFLINE_MAIN, true)
            val scenario = ActivityScenario.launch<MainActivity>(intent)
            var offlineManager: Any? = null
            try {
                val map = awaitMainMap(scenario)
                scenario.onActivity { activity ->
                    exerciseCameraProfile(map)
                    offlineManager = offlineProviderManager(activity)
                    assertProviderVisibility(offlineManager!!, legacyVisible = true, possibleVisible = true)
                }
                onView(withId(R.id.btnLayers)).perform(click())
                assertLayerToggle(R.id.cbWaterOld, "水務局舊資料") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, legacyVisible = visible, possibleVisible = true) }
                }
                assertLayerToggle(R.id.cbPossible, "可能側溝位置") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, legacyVisible = true, possibleVisible = visible) }
                }
                assertLayerToggle(R.id.cbRegion, "桃園行政區") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, true, true, visible) }
                }
                if (reopenIndex == 0) assertRegionTileInHost(offlineManager!!)
                exerciseVisibilityLifecycle(offlineManager!!)
                if (reopenIndex == 0) {
                    setProviderVisibilityOnMain(offlineManager!!, legacyVisible = true, possibleVisible = true, regionVisible = true)
                    val legacySample = findInteriorSample(OfflinePolygonLayer.LEGACY)
                    assertProviderTileAtInterior(offlineManager!!, OfflinePolygonLayer.LEGACY, legacySample)
                    assertProviderReturnsNoTileAtSparseExtent(offlineManager!!, OfflinePolygonLayer.LEGACY)
                    assertMapShowsLayerInteriorColor(map, offlineManager!!, OfflinePolygonLayer.LEGACY, legacySample, true, true, "feat1006-main-legacy.png")
                    val possibleSample = findInteriorSample(OfflinePolygonLayer.POSSIBLE)
                    assertProviderTileAtInterior(offlineManager!!, OfflinePolygonLayer.POSSIBLE, possibleSample)
                    assertProviderReturnsNoTileAtSparseExtent(offlineManager!!, OfflinePolygonLayer.POSSIBLE)
                    assertMapShowsLayerInteriorColor(map, offlineManager!!, OfflinePolygonLayer.POSSIBLE, possibleSample, true, true, "feat1006-main-possible.png")
                }
            } finally {
                scenario.close()
            }
            assertProvidersReleased(offlineManager!!)
        }
    }

    @Test
    fun mapWorkspaceFragmentShowsAndTogglesBothGeoPackageLayers() {
        repeat(HOST_REOPEN_COUNT) { reopenIndex ->
            val scenario = ActivityScenario.launch<MainShellActivity>(Intent(context, MainShellActivity::class.java))
            var offlineManager: Any? = null
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
                scenario.onActivity { activity ->
                    exerciseCameraProfile(mapReference.get())
                    val workspace = activity.supportFragmentManager
                        .findFragmentById(R.id.shell_container) as MapWorkspaceFragment
                    offlineManager = offlineProviderManager(workspace)
                    assertProviderVisibility(offlineManager!!, legacyVisible = true, possibleVisible = true)
                }
                if (reopenIndex == 0) {
                    assertRegionTileInHost(offlineManager!!)
                    assertProviderTileAtInterior(offlineManager!!, OfflinePolygonLayer.LEGACY, findInteriorSample(OfflinePolygonLayer.LEGACY))
                    assertProviderReturnsNoTileAtSparseExtent(offlineManager!!, OfflinePolygonLayer.LEGACY)
                    assertProviderTileAtInterior(offlineManager!!, OfflinePolygonLayer.POSSIBLE, findInteriorSample(OfflinePolygonLayer.POSSIBLE))
                    assertProviderReturnsNoTileAtSparseExtent(offlineManager!!, OfflinePolygonLayer.POSSIBLE)
                }

                onView(withId(R.id.btnLayers)).perform(click())
                assertLayerToggle(R.id.cbWaterOld, "水務局舊資料") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, legacyVisible = visible, possibleVisible = true) }
                }
                assertLayerToggle(R.id.cbPossible, "可能側溝位置") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, legacyVisible = true, possibleVisible = visible) }
                }
                assertLayerToggle(R.id.cbRegion, "桃園行政區") { visible ->
                    scenario.onActivity { assertProviderVisibility(offlineManager!!, true, true, visible) }
                }
                exerciseVisibilityLifecycle(offlineManager!!)
            } finally {
                scenario.close()
            }
            assertProvidersReleased(offlineManager!!)
        }
    }

    @Test
    fun gutterFormAndPointPickerMapHostsBecomeReady() {
        repeat(HOST_REOPEN_COUNT) { reopenIndex ->
            var formManager: Any? = null
            val formScenario = ActivityScenario.launch<GutterFormActivity>(
                GutterFormActivity.newViewIntent(
                    context = context,
                    label = "FEAT-1006 emulator test",
                    lat = 24.9929,
                    lng = 121.3011,
                    waypointIndex = 0,
                    basicData = hashMapOf(),
                    showWaterOld = false,
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
                formScenario.onActivity { activity ->
                    exerciseCameraProfile(formMapReference.get())
                    formManager = offlineProviderManager(activity)
                    assertProviderVisibility(formManager!!, legacyVisible = false, possibleVisible = true)
                }
                if (reopenIndex == 0) {
                    assertRegionTileInHost(formManager!!)
                    assertProviderTileAtInterior(formManager!!, OfflinePolygonLayer.POSSIBLE, findInteriorSample(OfflinePolygonLayer.POSSIBLE))
                    assertProviderReturnsNoTileAtSparseExtent(formManager!!, OfflinePolygonLayer.POSSIBLE)
                }
                exerciseVisibilityLifecycle(formManager!!)
            } finally {
                formScenario.close()
            }
            assertProvidersReleased(formManager!!)

            var pickerManager: Any? = null
            val pickerScenario = ActivityScenario.launch<MapPointPickerActivity>(
                MapPointPickerActivity.newIntent(
                    context = context,
                    initialLat = 24.9929,
                    initialLng = 121.3011,
                    showWaterOld = true,
                    showPossible = false
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
                pickerScenario.onActivity { activity ->
                    exerciseCameraProfile(pickerMapReference.get())
                    pickerManager = offlineProviderManager(activity)
                    assertProviderVisibility(pickerManager!!, legacyVisible = true, possibleVisible = false)
                }
                if (reopenIndex == 0) {
                    assertRegionTileInHost(pickerManager!!)
                    assertProviderTileAtInterior(pickerManager!!, OfflinePolygonLayer.LEGACY, findInteriorSample(OfflinePolygonLayer.LEGACY))
                    assertProviderReturnsNoTileAtSparseExtent(pickerManager!!, OfflinePolygonLayer.LEGACY)
                }
                exerciseVisibilityLifecycle(pickerManager!!)
            } finally {
                pickerScenario.close()
            }
            assertProvidersReleased(pickerManager!!)
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
        val sparseStartedAt = System.nanoTime()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SPARSE_TEST_COORDINATE, 16f))
        val sparseElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sparseStartedAt)
        assertEquals(SPARSE_TEST_COORDINATE.latitude, map.cameraPosition.target.latitude, 0.0001)
        assertEquals(SPARSE_TEST_COORDINATE.longitude, map.cameraPosition.target.longitude, 0.0001)
        assertTrue("Sparse-area camera update should not block the UI thread", sparseElapsedMs < MAX_CAMERA_UPDATE_MS)
    }

    private fun findInteriorSample(layer: OfflinePolygonLayer): OfflineCoordinate {
        val bounds = doubleArrayOf(274000.0, 2755000.0, 276000.0, 2760000.0)
        return OfflinePolygonDataStore.load(context, layer).get().query(bounds)
            .asSequence()
            .map(OfflineGeometryCodec::fromGeoPackage)
            .mapNotNull { geometry ->
                val point = findInteriorPoint(geometry, bounds) ?: return@mapNotNull null
                val containingPolygon = geometry.polygons.firstOrNull { polygon ->
                    val shell = polygon.rings.firstOrNull() ?: return@firstOrNull false
                    pointInRing(point, shell) && polygon.rings.drop(1).none { pointInRing(point, it) }
                } ?: return@mapNotNull null
                point to containingPolygon.rings.minOf { distanceToRing(point, it) }
            }
            .maxByOrNull { it.second }
            ?.first
            ?: throw AssertionError("${layer.name} source should contain a polygon interior in the test extent")
    }

    private fun assertProviderTileAtInterior(manager: Any, layer: OfflinePolygonLayer, interior: OfflineCoordinate) {
        val propertyName = if (layer == OfflinePolygonLayer.LEGACY) "legacyProvider" else "possibleProvider"
        val provider = privateProperty(manager, propertyName) as? OfflinePolygonTileProvider
            ?: throw AssertionError("${layer.name} provider should be active in this map host")
        val (longitude, latitude) = Twd97Projection.twd97ToLongitudeLatitude(interior.x, interior.y)
        val zoom = 20
        val tileCount = 1 shl zoom
        val x = kotlin.math.floor((longitude + 180.0) / 360.0 * tileCount).toInt()
        val latitudeRadians = Math.toRadians(latitude)
        val y = kotlin.math.floor(
            (1.0 - kotlin.math.ln(kotlin.math.tan(latitudeRadians) + 1.0 / kotlin.math.cos(latitudeRadians)) / Math.PI) / 2.0 * tileCount
        ).toInt()
        val tile = provider.getTile(x, y, zoom)
        assertNotSame("${layer.name} host provider should return its local populated tile", com.google.android.gms.maps.model.TileProvider.NO_TILE, tile)
        val png = tile?.data ?: throw AssertionError("${layer.name} host provider tile should contain PNG bytes")
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
            ?: throw AssertionError("${layer.name} host provider tile should decode as PNG")
        try {
            val pixel = WebMercatorTileMath.tilePixel(interior.x, interior.y, x, y, zoom)
            assertEquals(layer.color, bitmap.getPixel(pixel.x.toInt().coerceIn(0, bitmap.width - 1), pixel.y.toInt().coerceIn(0, bitmap.height - 1)))
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertRegionTileInHost(manager: Any) {
        val provider = privateProperty(manager, "regionProvider") as? OfflinePolygonTileProvider
            ?: throw AssertionError("Administrative region provider should be active in this map host")
        val tile = provider.getTile(6856, 3508, 13)
        assertNotSame("Administrative region provider should render locally", com.google.android.gms.maps.model.TileProvider.NO_TILE, tile)
        val png = tile?.data ?: throw AssertionError("Administrative region tile should contain PNG bytes")
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
            ?: throw AssertionError("Administrative region tile should decode as PNG")
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("Administrative region host tile should contain the existing purple boundary", pixels.any { it == OfflinePolygonColors.REGION_ARGB.toInt() })
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertProviderReturnsNoTileAtSparseExtent(manager: Any, layer: OfflinePolygonLayer) {
        val propertyName = if (layer == OfflinePolygonLayer.LEGACY) "legacyProvider" else "possibleProvider"
        val provider = privateProperty(manager, propertyName) as? OfflinePolygonTileProvider
            ?: throw AssertionError("${layer.name} provider should be active in this map host")
        val (longitude, latitude) = Twd97Projection.twd97ToLongitudeLatitude(SPARSE_EASTING, SPARSE_NORTHING)
        val zoom = 16
        val tileCount = 1 shl zoom
        val x = kotlin.math.floor((longitude + 180.0) / 360.0 * tileCount).toInt()
        val latitudeRadians = Math.toRadians(latitude)
        val y = kotlin.math.floor(
            (1.0 - kotlin.math.ln(kotlin.math.tan(latitudeRadians) + 1.0 / kotlin.math.cos(latitudeRadians)) / Math.PI) / 2.0 * tileCount
        ).toInt()
        assertSame("${layer.name} provider should return NO_TILE outside its data extent", com.google.android.gms.maps.model.TileProvider.NO_TILE, provider.getTile(x, y, zoom))
    }

    private fun assertMapShowsLayerInteriorColor(
        map: GoogleMap,
        manager: Any,
        layer: OfflinePolygonLayer,
        interior: OfflineCoordinate,
        restoreLegacy: Boolean,
        restorePossible: Boolean,
        screenshotName: String
    ) {
        val (longitude, latitude) = Twd97Projection.twd97ToLongitudeLatitude(interior.x, interior.y)
        val target = LatLng(latitude, longitude)
        val cameraIdle = CountDownLatch(1)
        val mapLoaded = CountDownLatch(1)
        val snapshotReady = CountDownLatch(1)
        var actualColor: Int? = null
        var samplePoint: Point? = null
        var screenshotFailure: Throwable? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            setProviderVisibility(manager, layer == OfflinePolygonLayer.LEGACY, layer == OfflinePolygonLayer.POSSIBLE)
            map.setOnCameraIdleListener { cameraIdle.countDown() }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 20f))
            assertEquals(target.latitude, map.cameraPosition.target.latitude, 0.0001)
            assertEquals(target.longitude, map.cameraPosition.target.longitude, 0.0001)
        }
        assertTrue("${layer.name} camera should settle on the populated extent", cameraIdle.await(30, TimeUnit.SECONDS))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            map.setOnCameraIdleListener(null)
            map.setOnMapLoadedCallback { mapLoaded.countDown() }
        }
        assertTrue("${layer.name} map should finish loading the populated extent", mapLoaded.await(60, TimeUnit.SECONDS))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            map.snapshot { bitmap ->
                try {
                    val snapshot = bitmap ?: throw AssertionError("${layer.name} map snapshot returned null")
                    val point = map.projection.toScreenLocation(target)
                    samplePoint = point
                    if (point.x !in 0 until snapshot.width || point.y !in 0 until snapshot.height) {
                        throw AssertionError("GPKG interior sample $point is outside map snapshot ${snapshot.width}x${snapshot.height}")
                    }
                    actualColor = snapshot.getPixel(point.x, point.y)
                    FileOutputStream(context.cacheDir.resolve(screenshotName)).use {
                        snapshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    snapshot.recycle()
                } catch (failure: Throwable) {
                    screenshotFailure = failure
                    bitmap?.recycle()
                } finally {
                    snapshotReady.countDown()
                }
            }
        }
        assertTrue("${layer.name} map snapshot should complete", snapshotReady.await(30, TimeUnit.SECONDS))
        screenshotFailure?.let { throw AssertionError("Unable to inspect ${layer.name} map snapshot", it) }
        assertEquals(
            "${layer.name} GPKG interior at $target should render its opaque fill in the map snapshot at $samplePoint",
            layer.color,
            actualColor
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            setProviderVisibility(manager, restoreLegacy, restorePossible)
        }
    }

    private fun findInteriorPoint(geometry: OfflineGeometry, searchBounds: DoubleArray): OfflineCoordinate? {
        var bestPoint: OfflineCoordinate? = null
        var bestClearance = 0.0
        geometry.polygons.forEach { polygon ->
            val shell = polygon.rings.firstOrNull()?.takeIf { it.size >= 4 } ?: return@forEach
            val minX = maxOf(shell.minOf { it.x }, searchBounds[0])
            val maxX = minOf(shell.maxOf { it.x }, searchBounds[2])
            val minY = maxOf(shell.minOf { it.y }, searchBounds[1])
            val maxY = minOf(shell.maxOf { it.y }, searchBounds[3])
            if (minX >= maxX || minY >= maxY) return@forEach
            for (xIndex in 0 until 32) {
                val x = minX + (maxX - minX) * (xIndex + 0.5) / 32.0
                for (yIndex in 0 until 32) {
                    val y = minY + (maxY - minY) * (yIndex + 0.5) / 32.0
                    val point = OfflineCoordinate(x, y)
                    if (!pointInRing(point, shell) || polygon.rings.drop(1).any { pointInRing(point, it) }) continue
                    val clearance = polygon.rings.minOf { ring -> distanceToRing(point, ring) }
                    if (clearance > bestClearance) {
                        bestClearance = clearance
                        bestPoint = point
                    }
                }
            }
        }
        return bestPoint
    }

    private fun distanceToRing(point: OfflineCoordinate, ring: List<OfflineCoordinate>): Double {
        var minimum = Double.POSITIVE_INFINITY
        for (index in ring.indices) {
            val start = ring[index]
            val end = ring[(index + 1) % ring.size]
            val dx = end.x - start.x
            val dy = end.y - start.y
            val lengthSquared = dx * dx + dy * dy
            val ratio = if (lengthSquared == 0.0) 0.0 else
                (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val closestX = start.x + ratio * dx
            val closestY = start.y + ratio * dy
            val distanceX = point.x - closestX
            val distanceY = point.y - closestY
            minimum = minOf(minimum, kotlin.math.sqrt(distanceX * distanceX + distanceY * distanceY))
        }
        return minimum
    }

    private fun pointInRing(point: OfflineCoordinate, ring: List<OfflineCoordinate>): Boolean {
        var inside = false
        var previous = ring.lastIndex
        for (current in ring.indices) {
            val a = ring[current]
            val b = ring[previous]
            if ((a.y > point.y) != (b.y > point.y) &&
                point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
            ) inside = !inside
            previous = current
        }
        return inside
    }

    private fun exerciseVisibilityLifecycle(manager: Any) {
        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = false, regionVisible = false)
        assertProvidersReleased(manager)

        setProviderVisibilityOnMain(manager, legacyVisible = true, possibleVisible = false, regionVisible = false)
        val legacyProvider = privateProperty(manager, "legacyProvider")
        assertNotNull(legacyProvider)
        setProviderVisibilityOnMain(manager, legacyVisible = true, possibleVisible = false, regionVisible = false)
        assertSame("Repeated enable must not create a duplicate legacy provider", legacyProvider, privateProperty(manager, "legacyProvider"))

        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = true, regionVisible = false)
        val possibleProvider = privateProperty(manager, "possibleProvider")
        assertNotNull(possibleProvider)
        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = true, regionVisible = false)
        assertSame("Repeated enable must not create a duplicate possible provider", possibleProvider, privateProperty(manager, "possibleProvider"))

        setProviderVisibilityOnMain(manager, legacyVisible = true, possibleVisible = true, regionVisible = false)
        val restoredLegacyProvider = privateProperty(manager, "legacyProvider")
        val restoredPossibleProvider = privateProperty(manager, "possibleProvider")
        setProviderVisibilityOnMain(manager, legacyVisible = true, possibleVisible = true, regionVisible = false)
        assertSame(restoredLegacyProvider, privateProperty(manager, "legacyProvider"))
        assertSame(restoredPossibleProvider, privateProperty(manager, "possibleProvider"))

        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = false, regionVisible = false)
        assertProvidersReleased(manager)

        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = false, regionVisible = true)
        val regionProvider = privateProperty(manager, "regionProvider")
        assertNotNull("Administrative boundary provider should be created when enabled", regionProvider)
        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = false, regionVisible = true)
        assertSame("Repeated region enable must not create duplicates", regionProvider, privateProperty(manager, "regionProvider"))
        setProviderVisibilityOnMain(manager, legacyVisible = false, possibleVisible = false, regionVisible = false)
        assertProvidersReleased(manager)
    }

    private fun setProviderVisibilityOnMain(manager: Any, legacyVisible: Boolean, possibleVisible: Boolean, regionVisible: Boolean = true) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            setProviderVisibility(manager, legacyVisible, possibleVisible, regionVisible)
        }
    }

    private fun setProviderVisibility(manager: Any, legacyVisible: Boolean, possibleVisible: Boolean, regionVisible: Boolean = true) {
        val method = manager.javaClass.getDeclaredMethod(
            "setVisibility",
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        method.invoke(manager, legacyVisible, possibleVisible, regionVisible)
        assertProviderVisibility(manager, legacyVisible, possibleVisible, regionVisible)
    }

    private fun assertLayerToggle(viewId: Int, label: String, assertProviderVisibility: (Boolean) -> Unit) {
        onView(withId(viewId)).check(matches(withText(label)))
        onView(withId(viewId)).check(matches(isChecked()))
        assertProviderVisibility(true)
        onView(withId(viewId)).perform(click())
        onView(withId(viewId)).check(matches(isNotChecked()))
        assertProviderVisibility(false)
        onView(withId(viewId)).perform(click())
        onView(withId(viewId)).check(matches(isChecked()))
        assertProviderVisibility(true)
    }

    private fun offlineProviderManager(host: Any): Any {
        val controller = when (host) {
            is MainActivity -> privateProperty(host, "mapOverlayController")!!
            is MapWorkspaceFragment -> privateProperty(host, "mapOverlayController")!!
            is GutterFormActivity, is MapPointPickerActivity -> host
            else -> error("Unsupported FEAT-1006 map host: ${host.javaClass.name}")
        }
        return privateProperty(controller, "offlinePolygonOverlays")!!
    }

    private fun assertProviderVisibility(manager: Any, legacyVisible: Boolean, possibleVisible: Boolean, regionVisible: Boolean = true) {
        assertEquals(legacyVisible, privateProperty(manager, "legacyProvider") != null)
        assertEquals(possibleVisible, privateProperty(manager, "possibleProvider") != null)
        assertEquals(regionVisible, privateProperty(manager, "regionProvider") != null)
    }

    private fun assertProvidersReleased(manager: Any) {
        assertProviderVisibility(manager, legacyVisible = false, possibleVisible = false, regionVisible = false)
        assertEquals(null, privateProperty(manager, "legacyOverlay"))
        assertEquals(null, privateProperty(manager, "possibleOverlay"))
        assertEquals(null, privateProperty(manager, "regionOverlay"))
    }

    private fun privateProperty(owner: Any, name: String): Any? {
        val getterName = "get" + name.replaceFirstChar(Char::uppercase)
        val getter = owner.javaClass.declaredMethods.firstOrNull {
            it.name == getterName && it.parameterCount == 0
        }
        if (getter != null) {
            getter.isAccessible = true
            return getter.invoke(owner)
        }
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(owner)
    }

    private companion object {
        val TAOYUAN_TEST_COORDINATE = LatLng(24.9929, 121.3011)
        val PAN_TARGET = LatLng(24.997, 121.305)
        const val MAX_CAMERA_UPDATE_MS = 1_000L
        const val SPARSE_EASTING = 300000.0
        const val SPARSE_NORTHING = 2800000.0
        val SPARSE_TEST_COORDINATE = Twd97Projection.twd97ToLongitudeLatitude(SPARSE_EASTING, SPARSE_NORTHING)
            .let { LatLng(it.second, it.first) }
        const val HOST_REOPEN_COUNT = 2
    }
}
