package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.ApiBackendTarget
import kotlinx.coroutines.CancellationException

class AppVersionRepository internal constructor(
    private val serviceForTarget: (ApiBackendTarget) -> GutterApiService = { target ->
        check(target == ApiBackendTarget.BASE)
        GutterApiClient.versionCheckService()
    }
) {
    /** Uses the fixed BASE Retrofit service even when the login-page debug target selects another API. */
    suspend fun getAvailableAndroidUpdate(): AvailableAppUpdate? = try {
        val response = serviceForTarget(ApiBackendTarget.BASE).getAppVersion()
        if (!response.isSuccessful) null else response.body()?.toAvailableAndroidUpdateOrNull()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
