package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.ApiBackendTarget
import kotlinx.coroutines.CancellationException

class AppVersionRepository internal constructor(
    private val serviceForTarget: (ApiBackendTarget) -> GutterApiService = GutterApiClient::versionCheckService
) {
    /** Uses the target selected when this check starts; an in-flight request keeps that target. */
    suspend fun getAvailableAndroidUpdate(): AvailableAppUpdate? = try {
        val target = GutterApiClient.selectedTarget
        val response = serviceForTarget(target).getAppVersion()
        if (!response.isSuccessful) null else response.body()?.toAvailableAndroidUpdateOrNull()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
