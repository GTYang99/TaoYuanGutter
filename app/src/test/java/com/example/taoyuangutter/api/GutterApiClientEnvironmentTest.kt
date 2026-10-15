package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.ApiBackendTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class GutterApiClientEnvironmentTest {
    @After
    fun reset() {
        GutterApiClient.resetTargetForTests()
    }

    @Test
    fun defaultsToTaipeiAndReturnsAServiceForSelectedTarget() {
        assumeTrue(GutterApiClient.ENABLE_GROUP_SIMULATION)
        GutterApiClient.resetTargetForTests()

        val taipeiService = GutterApiClient.instance
        assertEquals(ApiBackendTarget.TAIPEI, GutterApiClient.selectedTarget)
        assertSame(taipeiService, GutterApiClient.instance)

        assertTrue(GutterApiClient.selectTarget(ApiBackendTarget.BASE))
        val baseService = GutterApiClient.instance
        assertNotSame(taipeiService, baseService)

        assertTrue(GutterApiClient.selectTarget(ApiBackendTarget.DEMO))
        assertNotSame(baseService, GutterApiClient.instance)
    }

    @Test
    fun targetSelectionCanBeRejectedWhenDebugGateIsOff() {
        // The release build compiles this same guard with ENABLE_GROUP_SIMULATION=false.
        if (!GutterApiClient.ENABLE_GROUP_SIMULATION) {
            assertFalse(GutterApiClient.selectTarget(ApiBackendTarget.DEMO))
            assertEquals(ApiBackendTarget.TAIPEI, GutterApiClient.selectedTarget)
        }
    }
}
