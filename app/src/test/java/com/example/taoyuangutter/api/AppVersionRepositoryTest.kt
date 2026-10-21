package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.ApiBackendTarget
import com.example.taoyuangutter.common.BackendEndpoints
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AppVersionRepositoryTest {
    @After fun resetTarget() = GutterApiClient.resetTargetForTests()

    @Test fun versionCheckUsesSelectedTargetAndItsMappedUrl() = runBlocking {
        val targets = listOf(ApiBackendTarget.BASE, ApiBackendTarget.TAIPEI, ApiBackendTarget.DEMO)
        val routedUrls = mutableListOf<String>()
        val service = versionService()
        val repository = AppVersionRepository { target ->
            routedUrls += BackendEndpoints.apiBaseUrl(target) + "api/appVersion"
            service
        }

        targets.forEach { target ->
            assertTrue(GutterApiClient.selectTarget(target))
            assertEquals("2.0.0", repository.getAvailableAndroidUpdate()?.version.toString())
        }

        assertEquals(
            listOf(
                "http://192.168.10.84/TY_RSGDBIP/api/appVersion",
                "https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion",
                "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion"
            ),
            routedUrls
        )
    }

    @Test fun targetIsCapturedWhenCheckStartsAndNextCheckUsesNewSelection() = runBlocking {
        assertTrue(GutterApiClient.selectTarget(ApiBackendTarget.TAIPEI))
        val requestStarted = CompletableDeferred<ApiBackendTarget>()
        val finishRequest = CompletableDeferred<Unit>()
        val requestedTargets = mutableListOf<ApiBackendTarget>()
        val service = object : GutterApiService by GutterApiClient.serviceFor(ApiBackendTarget.TAIPEI) {
            override suspend fun getAppVersion(): Response<AppVersionResponse> {
                finishRequest.await()
                return Response.success(versionResponse())
            }
        }
        val repository = AppVersionRepository { target ->
            requestedTargets += target
            requestStarted.complete(target)
            service
        }

        val firstCheck = async { repository.getAvailableAndroidUpdate() }
        assertEquals(ApiBackendTarget.TAIPEI, requestStarted.await())
        assertTrue(GutterApiClient.selectTarget(ApiBackendTarget.DEMO))
        finishRequest.complete(Unit)
        firstCheck.await()
        repository.getAvailableAndroidUpdate()

        assertEquals(listOf(ApiBackendTarget.TAIPEI, ApiBackendTarget.DEMO), requestedTargets)
    }

    @Test fun unsuccessfulHttpResponseDoesNotOfferUpdate() = runBlocking {
        val service = object : GutterApiService by GutterApiClient.serviceFor(ApiBackendTarget.BASE) {
            override suspend fun getAppVersion(): Response<AppVersionResponse> =
                Response.error(503, "unavailable".toResponseBody(null))
        }
        assertNull(AppVersionRepository { service }.getAvailableAndroidUpdate())
    }

    private fun versionService(): GutterApiService =
        object : GutterApiService by GutterApiClient.serviceFor(ApiBackendTarget.TAIPEI) {
            override suspend fun getAppVersion(): Response<AppVersionResponse> =
                Response.success(versionResponse())
        }

    private fun versionResponse() =
        AppVersionResponse(true, listOf(AppVersionRecord("android", "2.0.0", "https://example.test/app.apk")))
}
