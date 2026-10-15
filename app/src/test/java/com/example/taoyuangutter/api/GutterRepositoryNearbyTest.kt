package com.example.taoyuangutter.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class GutterRepositoryNearbyTest {
    @Test
    fun locationCoordinatesAreRedactedFromHttpLogLines() {
        val message = "--> GET /api/v1/node/closestNodeDetails?lng=121.1234&lat=24.5678 http/1.1"

        assertEquals(
            "--> GET /api/v1/node/closestNodeDetails?lng=[REDACTED]&lat=[REDACTED] http/1.1",
            redactLocationCoordinatesForLogging(message)
        )
    }

    @Test
    fun nearbyLookupUsesCoordinatesAndBearerTokenOnSharedEndpoint() = runBlocking {
        var capturedMethod: String? = null
        var capturedArgs: Array<out Any?>? = null
        val api = Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, args ->
            capturedMethod = method.name
            capturedArgs = args
            Response.success(NodeDetailsResponse(true, null, emptyList(), null))
        } as GutterApiService

        GutterRepository(api).getClosestNodeDetails(
            lng = 121.1234,
            lat = 24.5678,
            token = "test-token"
        )

        assertEquals("getClosestNodeDetails", capturedMethod)
        assertEquals(121.1234, capturedArgs!![0])
        assertEquals(24.5678, capturedArgs!![1])
        assertEquals("Bearer test-token", capturedArgs!![2])
    }

    @Test
    fun recentSavedLookupKeepsNoQueryCallShape() = runBlocking {
        var capturedMethod: String? = null
        var capturedArgs: Array<out Any?>? = null
        val api = Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, args ->
            capturedMethod = method.name
            capturedArgs = args
            Response.success(NodeDetailsResponse(true, null, emptyList(), null))
        } as GutterApiService

        GutterRepository(api).getClosestNodeDetails(token = "test-token")

        assertEquals("getClosestNodeDetails", capturedMethod)
        // Suspend functions expose the continuation as the final proxy argument.
        assertEquals(2, capturedArgs?.size)
        assertEquals("Bearer test-token", capturedArgs!![0])
    }
}
