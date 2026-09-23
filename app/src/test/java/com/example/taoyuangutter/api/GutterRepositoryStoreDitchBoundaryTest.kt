package com.example.taoyuangutter.api

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class GutterRepositoryStoreDitchBoundaryTest {
    @Test
    fun markerRunsBeforeRemoteRequest() {
        val events = mutableListOf<String>()

        executeStoreDitchSubmissionBoundary(
            onRequestEntered = { events += "marker" },
            request = {
                events += "request"
                Unit
            }
        )

        assertEquals(listOf("marker", "request"), events)
    }

    @Test
    fun markerFailurePreventsRemoteStoreDitchCall() = runBlocking {
        val apiCalls = AtomicInteger()
        val repository = GutterRepository(
            fakeApi {
                apiCalls.incrementAndGet()
                Response.error<StoreDitchResponse>(
                    500,
                    "{}".toResponseBody("application/json".toMediaType())
                )
            }
        )

        val result = repository.storeDitch(
            request = StoreDitchRequest(spiTyp = 1, nodes = emptyList()),
            token = "test-token",
            onRequestEntered = { error("local marker failure") }
        )

        assertTrue(result is ApiResult.Error)
        assertEquals(0, apiCalls.get())
    }

    private fun fakeApi(
        storeDitchResponse: () -> Response<StoreDitchResponse>
    ): GutterApiService {
        return Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "storeDitch") storeDitchResponse()
            else error("Unexpected API call: ${method.name}")
        } as GutterApiService
    }
}
