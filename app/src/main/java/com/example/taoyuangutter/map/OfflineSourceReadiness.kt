package com.example.taoyuangutter.map

import java.util.concurrent.CompletableFuture

internal fun sourceReadiness(
    primary: CompletableFuture<*>,
    fallback: (() -> CompletableFuture<*>)?,
    released: () -> Boolean
): CompletableFuture<Unit> {
    val nested: CompletableFuture<CompletableFuture<Unit>> = primary.handle { _, failure ->
        if (released() || failure == null || fallback == null) {
            CompletableFuture.completedFuture(Unit)
        } else {
            fallback.invoke().handle { _, _ -> Unit }
        }
    }
    return nested.thenCompose { ready -> ready }
}
