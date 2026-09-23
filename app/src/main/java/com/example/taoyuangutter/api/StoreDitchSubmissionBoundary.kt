package com.example.taoyuangutter.api

import kotlinx.coroutines.CancellationException

internal class StoreDitchSubmissionBoundaryException(cause: Throwable) :
    RuntimeException("Unable to persist the storeDitch submission marker", cause)

/** Executes the local submission marker before the remote storeDitch request. */
internal inline fun <T> executeStoreDitchSubmissionBoundary(
    onRequestEntered: () -> Unit,
    request: () -> T
): T {
    try {
        onRequestEntered()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw StoreDitchSubmissionBoundaryException(e)
    }
    return request()
}
