package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GeoFeature
import com.example.taoyuangutter.api.GeoGeometry
import com.example.taoyuangutter.api.GeoProperties
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScopeMapCoordinatorTest {
    @Test
    fun blockedDebounceDoesNotConsumeWindow() = runBlocking {
        val loadCompleted = CompletableDeferred<Unit>()
        var loadCount = 0
        val coordinator = buildCoordinator(
            loader = {
                loadCount += 1
                loadCompleted.complete(Unit)
                ApiResult.Success(ScopeViewportLoader.ScopeLoadResult(emptyList()))
            }
        )
        val hooks = noopHooks()
        val config = ScopeMapCoordinator.Config(
            isOfflineMode = false,
            isBlocked = true,
            token = "token",
            showFeedback = false
        )

        assertEquals(null, coordinator.loadDebounced(this, config, hooks, 10_000L))
        assertEquals(0, loadCount)

        val allowed = coordinator.loadDebounced(
            this,
            config.copy(isBlocked = false),
            hooks,
            10_000L
        )
        assertNotNull(allowed)
        loadCompleted.await()
        assertEquals(1, loadCount)
    }

    @Test
    fun staleResponseDoesNotRedrawLatestViewport() = runBlocking {
        val firstGate = CompletableDeferred<Unit>()
        val drawCompleted = CompletableDeferred<Unit>()
        val drawCalls = mutableListOf<List<GeoFeature>>()
        var loadCount = 0
        val featureA = feature("A")
        val featureB = feature("B")

        val coordinator = buildCoordinator(
            loader = {
                loadCount += 1
                when (loadCount) {
                    1 -> {
                        firstGate.await()
                        ApiResult.Success(ScopeViewportLoader.ScopeLoadResult(listOf(featureA)))
                    }
                    else -> ApiResult.Success(ScopeViewportLoader.ScopeLoadResult(listOf(featureB)))
                }
            },
            drawFeatures = { features, _ ->
                drawCalls += features
                drawCompleted.complete(Unit)
            }
        )
        val hooks = noopHooks()
        val config = ScopeMapCoordinator.Config(
            isOfflineMode = false,
            isBlocked = false,
            token = "token",
            showFeedback = false
        )

        coordinator.load(this, config, hooks)
        coordinator.load(this, config, hooks)
        firstGate.complete(Unit)
        drawCompleted.await()

        assertEquals(2, loadCount)
        assertEquals(1, drawCalls.size)
        assertEquals(listOf(featureB), drawCalls.single())
    }

    @Test
    fun latestVisibleRequestKeepsLoadingStateBoundToNewestExecution() = runBlocking {
        val firstGate = CompletableDeferred<Unit>()
        val drawCompleted = CompletableDeferred<Unit>()
        var loadCount = 0
        var loadingStarted = 0
        var loadingFinished = 0
        val featureA = feature("A")
        val featureB = feature("B")
        val coordinator = buildCoordinator(
            loader = {
                loadCount += 1
                when (loadCount) {
                    1 -> {
                        firstGate.await()
                        ApiResult.Success(ScopeViewportLoader.ScopeLoadResult(listOf(featureA)))
                    }
                    else -> ApiResult.Success(ScopeViewportLoader.ScopeLoadResult(listOf(featureB)))
                }
            },
            drawFeatures = { _, _ ->
                drawCompleted.complete(Unit)
            }
        )
        val hooks = ScopeMapCoordinator.Hooks(
            onBeforeDraw = { _ -> },
            onLoadingStarted = { loadingStarted += 1 },
            onLoadingFinished = { loadingFinished += 1 },
            onLoadingFailed = { _, _ -> },
            onSilentError = { _, _ -> }
        )
        val config = ScopeMapCoordinator.Config(
            isOfflineMode = false,
            isBlocked = false,
            token = "token",
            showFeedback = true
        )

        coordinator.load(this, config, hooks)
        coordinator.load(this, config, hooks)
        firstGate.complete(Unit)
        drawCompleted.await()

        assertEquals(2, loadCount)
        assertEquals(1, loadingStarted)
        assertEquals(1, loadingFinished)
    }

    private fun buildCoordinator(
        loader: suspend (String) -> ApiResult<ScopeViewportLoader.ScopeLoadResult>,
        drawFeatures: (List<GeoFeature>, Int) -> Unit = { _, _ -> }
    ): ScopeMapCoordinator {
        return ScopeMapCoordinator(
            loadViewport = loader,
            drawFeatures = drawFeatures,
            savedGroupIdProvider = { 7 }
        )
    }

    private fun noopHooks(): ScopeMapCoordinator.Hooks {
        return ScopeMapCoordinator.Hooks(
            onBeforeDraw = { _ -> },
            onLoadingStarted = { _ -> },
            onLoadingFinished = { _ -> },
            onLoadingFailed = { _, _ -> },
            onSilentError = { _, _ -> }
        )
    }

    private fun feature(spiNum: String): GeoFeature {
        return GeoFeature(
            type = "Feature",
            geometry = GeoGeometry(
                type = "LineString",
                coordinates = listOf(
                    listOf(121.0, 24.9),
                    listOf(121.1, 25.0)
                )
            ),
            properties = GeoProperties(
                spiNum = spiNum,
                groupId = "7",
                spiState = 1
            )
        )
    }
}
