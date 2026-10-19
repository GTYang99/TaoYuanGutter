package com.example.taoyuangutter.gutter

/** Decisions for the edit form's camera-only location flow. */
internal object EditMapLocationPolicy {
    fun shouldLocateForInitialCamera(
        isViewMode: Boolean,
        submittedDraftReadOnly: Boolean,
        hasSavedCoordinates: Boolean
    ): Boolean = !isViewMode && !submittedDraftReadOnly && !hasSavedCoordinates

    fun hasForegroundLocationPermission(fineGranted: Boolean, coarseGranted: Boolean): Boolean =
        fineGranted || coarseGranted

    fun shouldRetryPermission(canShowRationale: Boolean, retryAlreadyUsed: Boolean): Boolean =
        canShowRationale && !retryAlreadyUsed

    fun shouldRetryLocation(attempt: Int): Boolean = attempt == 1

    fun shouldApplyLocation(userMovedMap: Boolean, flowActive: Boolean): Boolean =
        !userMovedMap && flowActive
}
