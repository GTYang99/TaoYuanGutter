package com.example.taoyuangutter.api

import android.util.Log
import com.example.taoyuangutter.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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
        @Query("maxLng") maxLng: Double,
        @Header("Authorization") authorization: String
    ): Response<ScopeSearchResponse>

    /**
     * 取得單條側溝線段的詳細資料（含各點位摘要與照片 URL）。
     *
     * GET /api/v1/ditch/ditchDetails?SPI_NUM=BS0003
     * Authorization: Bearer {token}
     *
     * @param spiNum        側溝編號（必填）
     * @param authorization Bearer token
     * Response: [DitchDetailsResponse]
     */
    @GET("api/v1/ditch/ditchDetails")
    suspend fun getDitchDetails(
        @Query("SPI_NUM")              spiNum: String,
        @Header("Authorization")       authorization: String
    ): Response<DitchDetailsResponse>

    /**
     * 取得單一點位的完整資料（含照片 URL）。
     *
     * GET /api/v1/node/nodeDetails?node_id=418
     * Authorization: Bearer {token}
     *
     * @param nodeId        點位 ID（從線段資料的 nodes 列表取得）
     * @param authorization Bearer token
     * Response: [NodeDetailsResponse]
     */
    @GET("api/v1/node/nodeDetails")
    suspend fun getNodeDetails(
        @Query("node_id")        nodeId: Int,
        @Header("Authorization") authorization: String
    ): Response<NodeDetailsResponse>

    /**
     * 以測量座標編號（XY_NUM）查詢點位資料。
     *
     * GET /api/v1/node/nodeDetails?XY_NUM=A002
     * Authorization: Bearer {token}
     */
    @GET("api/v1/node/nodeDetails")
    suspend fun getNodeDetailsByXyNum(
        @Query("XY_NUM")         xyNum: String,
        @Header("Authorization") authorization: String
    ): Response<NodeDetailsResponse>

    /**
     * 取得所有既有點位的列表（不帶任何參數）。
     *
     * GET /api/v1/node/nodeDetails
     * Authorization: Bearer {token}
     *
     * @param authorization Bearer token
     * Response: [AllNodeDetailsResponse]
     */
    @GET("api/v1/node/nodeDetails")
    suspend fun getAllNodeDetails(
        @Header("Authorization") authorization: String
    ): Response<AllNodeDetailsResponse>

    /**
     * 上傳單張點位照片（multipart/form-data）。
     * 同一 node_id + fileCategory 只保留一張，舊圖會被覆蓋。
     *
     * POST /api/v1/node/nodeImage
     * Authorization: Bearer {token}
     *
     * @param nodeId        點位 ID
     * @param fileCategory  1=測量位置及側溝概況 / 2=側溝內徑寬度尺寸 / 3=側溝深度尺寸
     * @param file          圖片 MultipartBody.Part（name="file"）
     * @param authorization Bearer token
     * Response: [NodeImageUploadResponse]
     */
    @Multipart
    @POST("api/v1/node/nodeImage")
    suspend fun uploadNodeImage(
        @Part("node_id")         nodeId: RequestBody,
        @Part("fileCategory")    fileCategory: RequestBody,
        @Part                    file: MultipartBody.Part,
        @Header("Authorization") authorization: String
    ): Response<NodeImageUploadResponse>

    /**
     * 新增或更新整條側溝的所有點位。
     * Body 帶 SPI_NUM 時為更新，省略時為新增。
     *
     * POST /api/v1/ditch/storeDitch
     * Authorization: Bearer {token}
     *
     * Body: [StoreDitchRequest]
     * Response: [StoreDitchResponse]
     */
    @POST("api/v1/ditch/storeDitch")
    suspend fun storeDitch(
        @Body                    request: StoreDitchRequest,
        @Header("Authorization") authorization: String
    ): Response<StoreDitchResponse>

    /**
     * 新增曲線側溝（以 XY_NUM 陣列指定順序）。
     *
     * POST /api/v1/ditch/storeCurveDitch
     * Authorization: Bearer {token}
     *
     * Body: [StoreCurveDitchRequest]
     * Response: [StoreCurveDitchResponse]
     */
    @POST("api/v1/ditch/storeCurveDitch")
    suspend fun storeCurveDitch(
        @Body                    request: StoreCurveDitchRequest,
        @Header("Authorization") authorization: String
    ): Response<StoreCurveDitchResponse>

    /**
     * 刪除指定側溝（含其所有點位）。
     *
     * DELETE /api/v1/ditch/deleteDitch?SPI_NUM=...
     * Authorization: Bearer {token}
     *
     * @param spiNum        側溝編號
     * @param authorization Bearer token
     * Response: [DeleteDitchResponse]
     */
    @DELETE("api/v1/ditch/deleteDitch")
    suspend fun deleteDitch(
        @Query("SPI_NUM")        spiNum: String,
        @Header("Authorization") authorization: String
    ): Response<DeleteDitchResponse>
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
    private const val DEMO_URL = "https://demo.srgeo.com.tw/TY_RSGDBIP_BK/"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OkHttp", message)
    }.apply {
        // Debug 才印完整 request/response，避免 release 外洩資訊與效能問題
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        // 避免把 token 印出來；若你真的要看 token，可以暫時註解這行
        redactHeader("Authorization")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        // 後端偶發回傳 Int 欄位為空字串 ""（例如 END_DEP / END_WID），避免 Gson 解析直接炸掉
        .registerTypeAdapter(Int::class.javaObjectType, EmptyStringToNullIntAdapter())
        .create()

    val instance: GutterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GutterApiService::class.java)
    }
}
