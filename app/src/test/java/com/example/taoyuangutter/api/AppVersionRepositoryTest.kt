package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.ApiBackendTarget
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class AppVersionRepositoryTest {
    @After fun resetTarget() = GutterApiClient.resetTargetForTests()

    @Test fun versionCheckAlwaysRequestsBaseRegardlessOfSelectedTarget() = runBlocking {
        GutterApiClient.selectTarget(ApiBackendTarget.DEMO)
        var requestedTarget: ApiBackendTarget? = null
        val delegate = GutterApiClient.serviceFor(ApiBackendTarget.BASE)
        val service = object : GutterApiService by delegate {
            override suspend fun getAppVersion(): Response<AppVersionResponse> {
                requestedTarget = ApiBackendTarget.BASE
                return Response.success(AppVersionResponse(true, listOf(AppVersionRecord("android", "2.0.0", "https://example.test/app.apk"))))
            }
        }
        val update = AppVersionRepository { target -> requestedTarget = target; service }.getAvailableAndroidUpdate()
        assertEquals(ApiBackendTarget.BASE, requestedTarget)
        assertEquals("2.0.0", update?.version.toString())
    }

    @Test fun unsuccessfulHttpResponseDoesNotOfferUpdate() = runBlocking {
        val delegate = GutterApiClient.serviceFor(ApiBackendTarget.BASE)
        val service = object : GutterApiService by delegate {
            override suspend fun getAppVersion(): Response<AppVersionResponse> =
                Response.error(503, "unavailable".toResponseBody(null))
        }
        assertNull(AppVersionRepository { service }.getAvailableAndroidUpdate())
    }
}
