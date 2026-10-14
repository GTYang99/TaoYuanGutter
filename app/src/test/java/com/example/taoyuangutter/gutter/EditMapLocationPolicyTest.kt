package com.example.taoyuangutter.gutter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditMapLocationPolicyTest {
    @Test
    fun initialCameraLocationIsLimitedToNormalMissingCoordinateEdits() {
        assertTrue(EditMapLocationPolicy.shouldLocateForInitialCamera(true, false, false, false, false))
        assertFalse(EditMapLocationPolicy.shouldLocateForInitialCamera(true, false, false, false, true))
        assertFalse(EditMapLocationPolicy.shouldLocateForInitialCamera(false, false, false, false, false))
        assertFalse(EditMapLocationPolicy.shouldLocateForInitialCamera(true, true, false, false, false))
        assertFalse(EditMapLocationPolicy.shouldLocateForInitialCamera(true, false, true, false, false))
        assertFalse(EditMapLocationPolicy.shouldLocateForInitialCamera(true, false, false, true, false))
    }

    @Test
    fun acceptsEitherForegroundPermission() {
        assertTrue(EditMapLocationPolicy.hasForegroundLocationPermission(true, false))
        assertTrue(EditMapLocationPolicy.hasForegroundLocationPermission(false, true))
        assertFalse(EditMapLocationPolicy.hasForegroundLocationPermission(false, false))
    }

    @Test
    fun permissionRetryIsOfferedOnlyOnceForRecoverableDenial() {
        assertTrue(EditMapLocationPolicy.shouldRetryPermission(true, false))
        assertFalse(EditMapLocationPolicy.shouldRetryPermission(true, true))
        assertFalse(EditMapLocationPolicy.shouldRetryPermission(false, false))
    }

    @Test
    fun locationIsRetriedOnlyAfterFirstAttemptAndNeverOverridesManualPan() {
        assertTrue(EditMapLocationPolicy.shouldRetryLocation(1))
        assertFalse(EditMapLocationPolicy.shouldRetryLocation(2))
        assertTrue(EditMapLocationPolicy.shouldApplyLocation(false, true))
        assertFalse(EditMapLocationPolicy.shouldApplyLocation(true, true))
        assertFalse(EditMapLocationPolicy.shouldApplyLocation(false, false))
    }
}
