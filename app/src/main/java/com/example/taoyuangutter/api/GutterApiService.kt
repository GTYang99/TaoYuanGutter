package com.example.taoyuangutter.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ════════════════════════════════════════════════════════════════
//  Retrofit 服務接口
// ════════════════════════════════════════════════════════════════

interface GutterApiService {

    /**
     * 使用者登入。
     *
     * POST /api/login
     * Content-Type: application/json
     *
     * Body: [LoginRequest]
     * Response: [LoginResponse]
     */
    @POST("api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * 使用者登出。
     *
     * GET /api/logout
     * Authorization: Bearer {token}
     *
     * Response: [LogoutResponse]
     */
    @GET("api/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<LogoutResponse>

    /**
     * 上傳單條側溝資料。
     *
     * POST /api/gutters
     * Content-Type: application/json
     *
     * Body: [SubmitGutterRequest]
     * Response: [SubmitGutterResponse]
     */
    @POST("api/gutters")
    suspend fun submitGutter(
        @Body request: SubmitGutterRequest
    ): Response<SubmitGutterResponse>

    /**
     * 依地圖可視範圍取得側溝座標（GeoJSON）。
     *
     * GET /api/v1/map/scopeSearch
     *
     * @param minLat 最小緯度
     * @param maxLat 最大緯度
     * @param minLng 最小經度
     * @param maxLng 最大經度
     *
     * Response: [ScopeSearchResponse]
     */
    @GET("api/v1/map/scopeSearch")
    suspend fun getScopeSearch(
        @Query("minLat") minLat: Double,
        @Query("maxLat") maxLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLng") maxLng: Double
    ): Response<ScopeSearchResponse>
}

// ════════════════════════════════════════════════════════════════
//  Retrofit 單例工廠
// ════════════════════════════════════════════════════════════════

object GutterApiClient {

    /**
     * 後端 API 的 Base URL。
     * 正式環境請替換為真實域名，例如 "https://api.taoyuangutter.gov.tw/"
     * 本機開發（Android Emulator → Host）可改為 "http://10.0.2.2:8080/"
     */
    private const val BASE_URL = "http://192.168.10.37/TY_RSGDBIP/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY   // Debug 模式可改 NONE 減少日誌
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: GutterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GutterApiService::class.java)
    }
}
