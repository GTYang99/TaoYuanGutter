package com.example.taoyuangutter.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultAuthTest {
    @Test
    fun authExpiredHelperMatches401Only() {
        assertTrue(ApiResult.Error(message = "尚未登入", code = 401).isAuthExpired())
        assertFalse(ApiResult.Error(message = "bad request", code = 400).isAuthExpired())
        assertFalse(ApiResult.Error(message = "server error", code = 500).isAuthExpired())
        assertFalse(ApiResult.Error(message = "no code").isAuthExpired())
    }
}
