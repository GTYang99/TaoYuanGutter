package com.example.taoyuangutter.api

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class GutterRepositoryAuthTest {
    @Test
    fun logout401IsTreatedAsSuccessfulLocalLogout() = runBlocking {
        val repository = GutterRepository(
            fakeApi(
                "logout" to Response.error<LogoutResponse>(
                    401,
                    """{"success":false,"message":"Unauthenticated."}"""
                        .toResponseBody("application/json".toMediaType())
                )
            )
        )

        val result = repository.logout("expired-token")

        assertTrue(result is ApiResult.Success)
        assertEquals("尚未登入", (result as ApiResult.Success).data.message)
    }

    @Test
    fun login401RemainsLoginError() = runBlocking {
        val repository = GutterRepository(
            fakeApi(
                "login" to Response.error<LoginResponse>(
                    401,
                    """{"success":false,"message":"帳號或密碼錯誤"}"""
                        .toResponseBody("application/json".toMediaType())
                )
            )
        )

        val result = repository.login("bad-user", "bad-password")

        assertTrue(result is ApiResult.Error)
        assertEquals(401, (result as ApiResult.Error).code)
        assertEquals("帳號或密碼錯誤", result.message)
    }

    private fun fakeApi(vararg responses: Pair<String, Response<*>>): GutterApiService {
        val responseByName = responses.toMap()
        return Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, _ ->
            responseByName[method.name]
                ?: error("Unexpected API call: ${method.name}")
        } as GutterApiService
    }
}
