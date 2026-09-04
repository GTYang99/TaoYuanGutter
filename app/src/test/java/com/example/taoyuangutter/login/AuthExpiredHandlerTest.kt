package com.example.taoyuangutter.login

import com.example.taoyuangutter.api.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthExpiredHandlerTest {
    @Test
    fun non401ErrorIsNotHandled() {
        val guard = AuthExpiredOnceGuard()

        val handled = guard.handleIfAuthExpired(
            error = ApiResult.Error(message = "Bad request", code = 400),
            onHandle = { error("Should not handle non-auth errors") }
        )

        assertFalse(handled)
    }

    @Test
    fun duplicate401OnlySavesDraftAndHandlesOnce() {
        val guard = AuthExpiredOnceGuard()
        var saveCount = 0
        var handleCount = 0
        val error = ApiResult.Error(message = "Unauthorized", code = 401)

        assertTrue(
            guard.handleIfAuthExpired(
                error = error,
                onSaveDraft = { saveCount++ },
                onHandle = { handleCount++ }
            )
        )
        assertTrue(
            guard.handleIfAuthExpired(
                error = error,
                onSaveDraft = { saveCount++ },
                onHandle = { handleCount++ }
            )
        )

        assertEquals(1, saveCount)
        assertEquals(1, handleCount)
    }

    @Test
    fun draftSaveFailureStillContinuesHandling() {
        val guard = AuthExpiredOnceGuard()
        var saveFailureCount = 0
        var handleCount = 0

        val handled = guard.handleIfAuthExpired(
            error = ApiResult.Error(message = "Unauthorized", code = 401),
            onSaveDraft = { error("Draft write failed") },
            onDraftSaveFailed = { saveFailureCount++ },
            onHandle = { handleCount++ }
        )

        assertTrue(handled)
        assertEquals(1, saveFailureCount)
        assertEquals(1, handleCount)
    }

    @Test
    fun resetAllowsHandlingNext401() {
        val guard = AuthExpiredOnceGuard()
        var handleCount = 0
        val error = ApiResult.Error(message = "Unauthorized", code = 401)

        guard.handleIfAuthExpired(error = error, onHandle = { handleCount++ })
        guard.reset()
        guard.handleIfAuthExpired(error = error, onHandle = { handleCount++ })

        assertEquals(2, handleCount)
    }
}
