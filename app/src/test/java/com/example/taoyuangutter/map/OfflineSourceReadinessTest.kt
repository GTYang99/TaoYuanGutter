package com.example.taoyuangutter.map

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineSourceReadinessTest {
    @Test
    fun fallbackReadinessDoesNotBlockTheSingleSourceExecutor() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val primary = CompletableFuture<Unit>()
            val fallback = CompletableFuture<Unit>()
            val ready = sourceReadiness(primary, { fallback }, released = { false })

            executor.submit {
                primary.completeExceptionally(IllegalStateException("primary unavailable"))
                fallback.complete(Unit)
            }.get(1, TimeUnit.SECONDS)

            assertEquals(Unit, ready.get(1, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }
}
