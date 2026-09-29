package com.example.taoyuangutter.common

import android.util.Log
import com.example.taoyuangutter.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

/**
 * Shared HTTPS client for API, remote images and GeoServer tiles.
 *
 * The temporary hostname exception is deliberately narrow: the exact Taipei
 * host only. Certificate-chain validation is still performed. This release
 * override must be removed after the Taipei certificate includes the Taipei
 * host.
 */
object BackendHttpClient {
    /** Temporary release workaround; remove when the server certificate is fixed. */
    const val TEMPORARY_ALLOW_TAIPEI_HOSTNAME_MISMATCH = true

    private val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()

    val hostnameVerifier = HostnameVerifier { hostname, session ->
        if (TEMPORARY_ALLOW_TAIPEI_HOSTNAME_MISMATCH &&
            hostname.equals(BackendEndpoints.ACTIVE_HOST, ignoreCase = true)
        ) {
            true
        } else {
            defaultHostnameVerifier.verify(hostname, session)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OkHttp", message)
    }.apply {
        level = if (!BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.NONE
        } else {
            HttpLoggingInterceptor.Level.BODY
        }
        redactHeader("Authorization")
    }

    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .hostnameVerifier(hostnameVerifier)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
