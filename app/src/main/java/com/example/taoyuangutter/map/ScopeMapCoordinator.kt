package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScopeMapCoordinator(
    private val loadViewport: suspend (String) -> ApiResult<ScopeViewportLoader.ScopeLoadResult>,
    private val drawFeatures: (List<com.example.taoyuangutter.api.GeoFeature>, Int) -> Unit,
    private val savedGroupIdProvider: () -> Int
) {
    data class Config(
        val isOfflineMode: Boolean,
        val isBlocked: Boolean,
        val token: String?,
        val showFeedback: Boolean
    )

    data class Hooks(
        val onBeforeDraw: (Long) -> Unit,
        val onLoadingStarted: (Long) -> Unit,
        val onLoadingFinished: (Long) -> Unit,
        val onLoadingFailed: (Long, String) -> Unit,
        val onSilentError: (Long, String) -> Unit
    )

    private var lastLoadTime = 0L
    private var isFeedbackPending = false
    private var nextExecutionId = 0L
    private var latestExecutionId = 0L
    private var latestFeedbackExecutionId = 0L

    fun loadDebounced(
        scope: CoroutineScope,
        config: Config,
        hooks: Hooks,
        debounceMs: Long
    ): Long? {
        if (config.isOfflineMode || config.isBlocked || config.token.isNullOrBlank()) return null
        val now = System.currentTimeMillis()
        if (now - lastLoadTime < debounceMs) return null
        lastLoadTime = now
        return load(scope, config, hooks)
    }

    fun load(
        scope: CoroutineScope,
        config: Config,
        hooks: Hooks
    ): Long? {
        if (config.isOfflineMode || config.isBlocked) return null
        val token = config.token ?: return null

        val executionId = ++nextExecutionId
        latestExecutionId = executionId
        if (config.showFeedback) {
            val shouldNotifyStart = !isFeedbackPending
            isFeedbackPending = true
            latestFeedbackExecutionId = executionId
            if (shouldNotifyStart) {
                hooks.onLoadingStarted(executionId)
            }
        }

        scope.launch {
            when (val result = loadViewport(token)) {
                is ApiResult.Success -> {
                    if (executionId != latestExecutionId) return@launch
                    hooks.onBeforeDraw(executionId)
                    drawFeatures(result.data.features, savedGroupIdProvider())
                    if (isFeedbackPending && latestFeedbackExecutionId == executionId) {
                        isFeedbackPending = false
                        latestFeedbackExecutionId = 0L
                        hooks.onLoadingFinished(executionId)
                    }
                }
                is ApiResult.Error -> {
                    if (executionId != latestExecutionId) return@launch
                    if (isFeedbackPending && latestFeedbackExecutionId == executionId) {
                        isFeedbackPending = false
                        latestFeedbackExecutionId = 0L
                        hooks.onLoadingFailed(executionId, result.message)
                    } else {
                        hooks.onSilentError(executionId, result.message)
                    }
                }
            }
        }
        return executionId
    }
}
