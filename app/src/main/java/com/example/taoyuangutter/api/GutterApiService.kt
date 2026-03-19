package com.example.taoyuangutter.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ════════════════════════════════════════════════════════════════
//  Retrofit 服務接口
// ════════════════════════════════════════════════════════════════

interface GutterApiService {

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
     * 取得地圖上已存在的側溝列表。
     *
     * GET /api/gutters/map
     *
     * @param lat    地圖中心緯度（可選，後端可依此過濾範圍）
     * @param lng    地圖中心經度（可選）
     * @param radius 搜尋半徑（公尺，可選，預設由後端決定）
     *
     * Response: [GetGuttersMapResponse]
     */
    @GET("api/gutters/map")
    suspend fun getGuttersMap(
        @Query("lat")    lat: Double? = null,
        @Query("lng")    lng: Double? = null,
        @Query("radius") radius: Int? = null
    ): Response<GetGuttersMapResponse>
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
    private const val BASE_URL = "https://api.taoyuangutter.example.com/"

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
