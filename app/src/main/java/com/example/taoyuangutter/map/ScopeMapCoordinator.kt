package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScopeMapCoordinator(
    private val viewportLoader: ScopeViewportLoader,
    private val polylineController: ScopeGutterPolylineController,
    private val savedGroupIdProvider: () -> Int
) {
    data class Config(
        val isOfflineMode: Boolean,
        val isBlocked: Boolean,
        val token: String?,
        val showFeedback: Boolean
    )

    data class Hooks(
        val onBeforeDraw: () -> Unit,
        val onLoadingStarted: () -> Unit,
        val onLoadingFinished: () -> Unit,
        val onLoadingFailed: () -> Unit,
        val onSilentError: (String) -> Unit
    )

    private var lastLoadTime = 0L
    private var isFeedbackPending = false

    fun loadDebounced(
        scope: CoroutineScope,
        config: Config,
        hooks: Hooks,
        debounceMs: Long
    ) {
        if (config.isOfflineMode) return
        val now = System.currentTimeMillis()
        if (now - lastLoadTime < debounceMs) return
        lastLoadTime = now
        load(scope, config.copy(showFeedback = false), hooks)
    }

    fun load(
        scope: CoroutineScope,
        config: Config,
        hooks: Hooks
    ) {
        if (config.isOfflineMode || config.isBlocked) return
        val token = config.token ?: return

        if (config.showFeedback && !isFeedbackPending) {
            isFeedbackPending = true
            hooks.onLoadingStarted()
        }

        scope.launch {
            when (val result = viewportLoader.load(token)) {
                is ApiResult.Success -> {
                    hooks.onBeforeDraw()
                    polylineController.drawFeatures(
                        features = result.data.features,
                        savedGroupId = savedGroupIdProvider()
                    )
                    if (isFeedbackPending) {
                        isFeedbackPending = false
                        hooks.onLoadingFinished()
                    }
                }
                is ApiResult.Error -> {
                    if (isFeedbackPending) {
                        isFeedbackPending = false
                        hooks.onLoadingFailed()
                    }
                    hooks.onSilentError(result.message)
                }
            }
        }
    }
}
