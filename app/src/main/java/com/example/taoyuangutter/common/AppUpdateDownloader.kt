package com.example.taoyuangutter.common

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface AppUpdateFileDownloader {
    suspend fun download(url: okhttp3.HttpUrl, destination: File, onProgress: (Long, Long) -> Unit): File
}

/** Downloads an APK with explicit HTTPS-only redirect handling. */
class AppUpdateDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AppUpdateFileDownloader {
    override suspend fun download(url: okhttp3.HttpUrl, destination: File, onProgress: (Long, Long) -> Unit): File =
        suspendCancellableCoroutine { continuation ->
            fun enqueue(currentUrl: okhttp3.HttpUrl, redirects: Int) {
                if (!currentUrl.isHttps) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("APK URL must remain HTTPS"))
                    return
                }
                val call = client.newCall(Request.Builder().url(currentUrl).get().build())
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.code in 300..399) {
                                if (redirects >= MAX_REDIRECTS) {
                                    fail(IOException("Too many APK redirects"))
                                    return
                                }
                                val location = response.header("Location")
                                val next = location?.let(currentUrl::resolve)
                                if (next == null || !next.isHttps) {
                                    fail(IOException("APK redirect must remain HTTPS"))
                                    return
                                }
                                enqueue(next, redirects + 1)
                                return
                            }
                            if (!response.isSuccessful) {
                                fail(IOException("APK download returned HTTP ${response.code}"))
                                return
                            }
                            val body = response.body ?: run {
                                fail(IOException("APK download returned an empty body"))
                                return
                            }
                            try {
                                destination.parentFile?.mkdirs()
                                val temporary = File(destination.parentFile, destination.name + ".part")
                                temporary.outputStream().buffered().use { output ->
                                    body.byteStream().buffered().use { input ->
                                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                        var total = 0L
                                        val expected = body.contentLength()
                                        while (true) {
                                            if (!continuation.isActive) throw IOException("APK download cancelled")
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            output.write(buffer, 0, count)
                                            total += count
                                            onProgress(total, expected)
                                        }
                                    }
                                }
                                if (temporary.length() == 0L) throw IOException("APK download was empty")
                                if (destination.exists() && !destination.delete()) throw IOException("Could not replace cached APK")
                                if (!temporary.renameTo(destination)) throw IOException("Could not finalize cached APK")
                                if (continuation.isActive) continuation.resume(destination)
                            } catch (error: Exception) {
                                File(destination.parentFile, destination.name + ".part").delete()
                                fail(error)
                            }
                        }
                    }

                    private fun fail(error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                })
            }
            enqueue(url, 0)
        }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
