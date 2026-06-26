package com.example.taoyuangutter.api

import android.net.Uri
import android.content.Context
import android.os.Build
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import android.os.Environment
import com.example.taoyuangutter.common.buildRequestBody

/**
 * GutterRepository
 *
 * 負責：
 *  1. 將 App 內部的 [Waypoint] 列表轉換成 API Request 並呼叫後端
 *  2. 將後端回傳的 Response 轉換回 App 內部格式
 *
 * 所有公開方法皆為 suspend function，請在 CoroutineScope 內呼叫。
 */
class GutterRepository(
    private val api: GutterApiService = GutterApiClient.instance
) {
    private val rawHttp = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private data class ApiErrorEnvelope(
        val success: Boolean? = null,
        val message: String? = null,
        val errors: Map<String, List<String>>? = null
    )

    private fun parseApiErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching { Gson().fromJson(errorBody, ApiErrorEnvelope::class.java) }
            .getOrNull()
            ?.message
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseApiErrorEnvelope(errorBody: String?): ApiErrorEnvelope? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching { Gson().fromJson(errorBody, ApiErrorEnvelope::class.java) }.getOrNull()
    }

    // ── 登入 ──────────────────────────────────────────────────────────────

    /**
     * 呼叫登入 API。
     *
     * @param username 帳號
     * @param password 密碼
     * @return [ApiResult.Success] 含 [LoginResponse]（success=true 時 data.token 可使用）；
     *         [ApiResult.Error]   含後端回傳的錯誤訊息（401 帳密錯誤、422 驗證失敗、500 伺服器錯誤）
     */
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username = username, password = password))
            val body = response.body()
            when {
                // 200 且 success=true → 登入成功
                response.isSuccessful && body?.success == true -> {
                    ApiResult.Success(body)
                }
                // 200 但 success=false（理論上不應出現）→ 取 body 的 message
                response.isSuccessful && body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "登入失敗",
                        code    = response.code()
                    )
                }
                // 非 200（401/422/500）→ Retrofit 不解析 body()，改從 errorBody() 取 message
                else -> {
                    val errMsg = try {
                        val json = response.errorBody()?.string()
                        val errBody = Gson().fromJson(json, LoginResponse::class.java)
                        val detail = errBody?.errors?.values?.firstOrNull()?.firstOrNull()
                        detail ?: errBody?.message ?: "登入失敗（${response.code()}）"
                    } catch (_: Exception) {
                        "登入失敗（${response.code()}）"
                    }
                    ApiResult.Error(message = errMsg, code = response.code())
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 登出 ──────────────────────────────────────────────────────────────

    /**
     * 呼叫登出 API。
     * 無論 200 或 401（token 已過期），呼叫端都應清除本機 token 並跳回登入頁。
     * 僅 500 伺服器錯誤時回傳 [ApiResult.Error]。
     *
     * @param token 已儲存的 Bearer token
     */
    suspend fun logout(token: String): ApiResult<LogoutResponse> {
        return try {
            val response = api.logout("Bearer $token")
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> {
                    // token 已失效，視為正常登出（讓 UI 清除本機資料即可）
                    ApiResult.Success(
                        LogoutResponse(success = true, message = "尚未登入", errors = null)
                    )
                }
                body != null -> ApiResult.Error(
                    message = body.message ?: "登出失敗",
                    code    = response.code()
                )
                else -> ApiResult.Error(
                    message = "登出失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 上傳側溝 ──────────────────────────────────────────────────────────

    /**
     * 將一條側溝（含所有點位資料）上傳至後端。
     *
     * @param waypoints 已通過驗證的點位列表（起點 / 節點 / 終點）
     * @return [ApiResult.Success] 含後端回傳的 [SubmitGutterResponse]；
     *         [ApiResult.Error]   含錯誤訊息
     */
    suspend fun submitGutter(waypoints: List<Waypoint>): ApiResult<SubmitGutterResponse> {
        return try {
            val request = SubmitGutterRequest(
                waypoints = waypoints.mapIndexed { idx, wp ->
                    WaypointRequest(
                        // 點位識別
                        nodeId    = wp.basicData["gutterId"]   ?: "",
                        nodeAtt   = when (wp.type) {           // 1=起點, 2=節點, 3=終點
                            WaypointType.START -> 1
                            WaypointType.NODE  -> 2
                            WaypointType.END   -> 3
                        },
                        nodeNum   = idx,

                        // 基本資料
                        nodeTyP   = wp.basicData["gutterType"] ?: "",
                        matTyp    = wp.basicData["matTyp"]     ?: "",
                        nodeX     = wp.latLng?.longitude,      // X = 經度(E)
                        nodeY     = wp.latLng?.latitude,       // Y = 緯度(N)
                        nodeLe    = wp.basicData["coordZ"]     ?: "",
                        xyNum     = wp.basicData["xyNum"]      ?: "",
                        nodeDep   = wp.basicData["depth"]      ?: "",
                        nodeWid   = wp.basicData["topWidth"]   ?: "",
                        isBroken  = wp.basicData["isBroken"]   ?: "",
                        isHanging = wp.basicData["isHanging"]  ?: "",
                        isSilt    = wp.basicData["isSilt"]     ?: "",
                        nodeNote  = wp.basicData["remarks"]    ?: "",

                        // 照片 URI（multipart 上傳待實作，暫帶路徑字串）
                        photoOv   = wp.basicData["photo1"]     ?: "",
                        photoWid  = wp.basicData["photo2"]     ?: "",
                        photoDep  = wp.basicData["photo3"]     ?: ""
                    )
                }
            )
            val response = api.submitGutter(request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(
                    message = response.message() ?: "未知錯誤",
                    code    = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 取得側溝座標（依可視範圍）────────────────────────────────────────

    /**
     * 依地圖可視範圍查詢側溝 GeoJSON 線段。
     *
     * @param minLat 可視範圍最小緯度
     * @param maxLat 可視範圍最大緯度
     * @param minLng 可視範圍最小經度
     * @param maxLng 可視範圍最大經度
     * @return [ApiResult.Success] 含 [ScopeSearchResponse]（features 可能為空 list）；
     *         [ApiResult.Error]   含錯誤訊息
     */
    suspend fun getGuttersByScope(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        token: String
    ): ApiResult<ScopeSearchResponse> {
        return try {
            val response = api.getScopeSearch(
                minLat        = minLat,
                maxLat        = maxLat,
                minLng        = minLng,
                maxLng        = maxLng,
                authorization = "Bearer $token"
            )
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "查詢失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "查詢失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 回報無側溝（storeNoDitch）────────────────────────────────────────

    /**
     * 回報「無側溝」座標與備註。
     *
     * @param token Bearer token（不含 "Bearer " 前綴）
     */
    suspend fun storeNoDitch(
        latitude: Double,
        longitude: Double,
        note: String,
        token: String
    ): ApiResult<StoreNoDitchResponse> {
        return try {
            val response = api.storeNoDitch(
                request = StoreNoDitchRequest(
                    note = note,
                    latitude = latitude,
                    longitude = longitude
                ),
                authorization = "Bearer $token"
            )
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.isSuccessful && body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "送出失敗",
                        code = response.code()
                    )
                }
                else -> {
                    val errJson = runCatching { response.errorBody()?.string() }.getOrNull()
                    val env = parseApiErrorEnvelope(errJson)
                    val detail = env?.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: env?.message ?: "送出失敗（${response.code()}）",
                        code = response.code()
                    )
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 取得線段資料 ──────────────────────────────────────────────────────

    /**
     * 取得單條側溝線段的詳細資料。
     *
     * @param spiNum 側溝編號，例如 "BS0003"
     * @param token  已儲存的 Bearer token
     * @return [ApiResult.Success] 含 [DitchDetailsResponse]（data 含線段資訊及 nodes 列表）；
     *         [ApiResult.Error]   含錯誤訊息（401 尚未登入、404 查無側溝、422 欄位未填、500 伺服器錯誤）
     */
    suspend fun getDitchDetails(spiNum: String, token: String): ApiResult<DitchDetailsResponse> {
        return try {
            val response = api.getDitchDetails(
                spiNum        = spiNum,
                authorization = "Bearer $token"
            )
            val reqUrl = runCatching { response.raw().request.url.toString() }.getOrNull()
            if (!reqUrl.isNullOrEmpty()) {
                android.util.Log.d("GutterRepository", "getDitchDetails request url=$reqUrl")
            }
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code    = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "查詢失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "查詢失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: CancellationException) {
            // coroutine cancellation is not an API error; propagate to caller
            throw e
        } catch (e: JsonSyntaxException) {
            android.util.Log.e(
                "GutterRepository",
                "getDitchDetails json parse failed: spiNum=$spiNum",
                e
            )
            ApiResult.Error(message = "資料解析失敗")
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 取得點位資料 ──────────────────────────────────────────────────────

    /**
     * 取得單一點位的完整資料（含座標、深度、寬度、照片 URL 等）。
     *
     * @param nodeId 點位 ID（從 [getDitchDetails] 回傳的 nodes 列表取得）
     * @param token  已儲存的 Bearer token
     * @return [ApiResult.Success] 含 [NodeDetailsResponse]（data 含點位所有欄位）；
     *         [ApiResult.Error]   含錯誤訊息（401 尚未登入、404 查無點位、422 欄位未填、500 伺服器錯誤）
     */
    suspend fun getNodeDetails(nodeId: Int, token: String): ApiResult<NodeDetailsResponse> {
        return try {
            val response = api.getNodeDetails(
                nodeId        = nodeId,
                authorization = "Bearer $token"
            )
            val reqUrl = runCatching { response.raw().request.url.toString() }.getOrNull()
            if (!reqUrl.isNullOrEmpty()) {
                android.util.Log.d("GutterRepository", "getNodeDetails request url=$reqUrl")
            }
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code    = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    android.util.Log.e(
                        "GutterRepository",
                        "getNodeDetails failed: nodeId=$nodeId, code=${response.code()}, message=${body.message}, detail=$detail"
                    )
                    ApiResult.Error(
                        message = detail ?: body.message ?: "查詢失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "查詢失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: CancellationException) {
            // coroutine cancellation is not an API error; propagate to caller
            throw e
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("GutterRepository", "getNodeDetails json parse failed: nodeId=$nodeId", e)
            ApiResult.Error(message = "資料解析失敗")
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    /**
     * 以 XY_NUM 查詢點位完整資料。
     * GET /api/v1/node/nodeDetails?XY_NUM=...
     */
    suspend fun getNodeDetailsByXyNum(xyNum: String, token: String): ApiResult<NodeDetailsResponse> {
        return try {
            val response = api.getNodeDetailsByXyNum(
                xyNum        = xyNum,
                authorization = "Bearer $token"
            )
            val reqUrl = runCatching { response.raw().request.url.toString() }.getOrNull()
            if (!reqUrl.isNullOrEmpty()) {
                android.util.Log.d("GutterRepository", "getNodeDetailsByXyNum request url=$reqUrl")
            }
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code    = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    android.util.Log.e(
                        "GutterRepository",
                        "getNodeDetailsByXyNum failed: xyNum=$xyNum, code=${response.code()}, message=${body.message}, detail=$detail"
                    )
                    ApiResult.Error(
                        message = detail ?: body.message ?: "查詢失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "查詢失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("GutterRepository", "getNodeDetailsByXyNum json parse failed: xyNum=$xyNum", e)
            ApiResult.Error(message = "資料解析失敗")
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    /**
     * 以經緯度查詢最近點位清單。
     * GET /api/v1/node/closestNodeDetails?lng=...&lat=...
     */
    suspend fun getClosestNodeDetails(lng: Double, lat: Double, token: String): ApiResult<NodeDetailsResponse> {
        return try {
            val response = api.getClosestNodeDetails(
                lng = lng,
                lat = lat,
                authorization = "Bearer $token"
            )
            val reqUrl = runCatching { response.raw().request.url.toString() }.getOrNull()
            if (!reqUrl.isNullOrEmpty()) {
                android.util.Log.d("GutterRepository", "getClosestNodeDetails request url=$reqUrl")
            }
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    android.util.Log.e(
                        "GutterRepository",
                        "getClosestNodeDetails failed: lat=$lat, lng=$lng, code=${response.code()}, message=${body.message}, detail=$detail"
                    )
                    ApiResult.Error(
                        message = detail ?: body.message ?: "查詢失敗",
                        code = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "查詢失敗（${response.code()}）",
                    code = response.code()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("GutterRepository", "getClosestNodeDetails json parse failed: lat=$lat, lng=$lng", e)
            ApiResult.Error(message = "資料解析失敗")
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    /**
     * 下載遠端圖片到本機，並回傳 FileProvider content URI（供照片頁視為已拍攝照片）。
     * 下載失敗回傳 null。
     */
    suspend fun downloadImageToLocalContentUri(
        context: Context,
        url: String,
        prefix: String = "IMPORT_"
    ): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).get().build()
            rawHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    android.util.Log.w("GutterRepository", "download image failed: code=${resp.code}, url=$url")
                    return@withContext null
                }
                val body = resp.body ?: return@withContext null
                // 避免使用 cacheDir：cache 可能被系統回收，導致「草稿放久了照片不見」
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: File(context.filesDir, "Pictures").apply { mkdirs() }
                val file = File.createTempFile(prefix, ".jpg", dir)
                FileOutputStream(file).use { out ->
                    body.byteStream().use { input -> input.copyTo(out) }
                }
                return@withContext try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (_: Exception) {
                    android.net.Uri.fromFile(file)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("GutterRepository", "download image exception: url=$url", e)
            null
        }
    }

    // ── 點位照片上傳 ──────────────────────────────────────────────────────

    /**
     * 上傳單張點位照片。同一 nodeId + fileCategory 只保留最新一張，舊圖會被覆蓋。
     * 已加入圖片壓縮處理以加速上傳。
     */
    suspend fun uploadNodeImage(
        context: Context,
        nodeId: Int,
        fileCategory: Int,
        imageUri: Uri,
        token: String
    ): ApiResult<NodeImageUploadResponse> {
        return try {
            val sourceStats = readImageStats(context, imageUri)
            val sourceSizeText = sourceStats?.sizeBytes?.let { "${it / 1024} KB" } ?: "unknown"
            val sourceDimensionText = sourceStats?.let { "${it.width}x${it.height}" } ?: "unknown"
            android.util.Log.i(
                "CameraOverlay",
                "upload-before-compress uri=$imageUri, size=$sourceSizeText, dimensions=$sourceDimensionText"
            )
            logImageStats("upload-source", context, imageUri)
            // 壓縮並縮放圖片後再上傳，顯著減少上傳時間
            val tempFile = withContext(Dispatchers.IO) {
                compressImageToTempFile(context, imageUri)
            } ?: return ApiResult.Error("無法處理圖片檔案")
            logImageStats("upload-temp", tempFile)

            try {
                android.util.Log.i(
                    "PhotoUpload",
                    "request nodeId=$nodeId, category=$fileCategory, size=${tempFile.length() / 1024} KB"
                )

                // 🌟 使用 RequestBodyBuilder DSL 構建封裝的 MultipartBody
                val requestBody = buildRequestBody {
                    addFile("file", tempFile, "image/jpeg")
                    param("node_id", nodeId)
                    param("fileCategory", fileCategory)
                }

                val response = api.uploadNodeImage(
                    body          = requestBody,
                    authorization = "Bearer $token"
                )
                
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    ApiResult.Success(body)
                } else {
                    val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    val apiMsg = parseApiErrorMessage(errorBody)
                    ApiResult.Error(message = apiMsg ?: "上傳失敗（${response.code()}）", code = response.code())
                }
            } finally {
                withContext(Dispatchers.IO) { tempFile.delete() }
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    /**
     * 依據目前連線狀態 (如行動數據、頻寬慢速) 提供自適應圖片品質與尺寸。
     * - Wi-Fi 或好網路：1920px / 品質 80%
     * - 行動數據或慢速網路：1440px / 品質 80%
     */
    private fun getAdaptiveQualityAndSize(context: Context): Pair<Int, Int> {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities != null) {
                    val hasCellular = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                    val linkDownstream = capabilities.linkDownstreamBandwidthKbps
                    val isSlow = linkDownstream in 1..3000 // < 3 Mbps
                    if (hasCellular || isSlow) {
                        return Pair(4000, 80) // 極致壓縮hi
                    }
                }
            }
            Pair(4000, 80) // 預設優化值 (下修自原本的 1440px / 70%)
        } catch (e: Exception) {
            Pair(4000, 80)
        }
    }

    private data class ImageStats(
        val width: Int,
        val height: Int,
        val sizeBytes: Long?
    )

    private fun logImageStats(stage: String, context: Context, uri: Uri) {
        val stats = readImageStats(context, uri)
        val sizeText = stats?.sizeBytes?.let { "${it / 1024} KB" } ?: "unknown"
        val dimensionText = stats?.let { "${it.width}x${it.height}" } ?: "unknown"
        android.util.Log.i(
            "PhotoUpload",
            "$stage uri=$uri, size=$sizeText, dimensions=$dimensionText"
        )
    }

    private fun logImageStats(stage: String, file: File) {
        val stats = readImageStats(file)
        val sizeText = stats?.sizeBytes?.let { "${it / 1024} KB" } ?: "unknown"
        val dimensionText = stats?.let { "${it.width}x${it.height}" } ?: "unknown"
        android.util.Log.i(
            "PhotoUpload",
            "$stage file=${file.absolutePath}, size=$sizeText, dimensions=$dimensionText"
        )
    }

    private fun readImageStats(context: Context, uri: Uri): ImageStats? {
        return runCatching {
            resolveLocalFile(context, uri)?.let { file ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                return ImageStats(
                    width = options.outWidth,
                    height = options.outHeight,
                    sizeBytes = file.length()
                )
            }

            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return null

            val sizeBytes = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.length
            }.getOrNull()

            ImageStats(
                width = options.outWidth,
                height = options.outHeight,
                sizeBytes = sizeBytes
            )
        }.getOrNull()
    }

    private fun readImageStats(file: File): ImageStats? {
        return runCatching {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
            ImageStats(
                width = options.outWidth,
                height = options.outHeight,
                sizeBytes = file.length()
            )
        }.getOrNull()
    }

    private fun resolveLocalFile(context: Context, uri: Uri): File? {
        return when (uri.scheme?.lowercase()) {
            "file" -> uri.path?.let(::File)
            "content" -> resolveOurFileProviderFile(context, uri)
            else -> null
        }
    }

    private fun resolveOurFileProviderFile(context: Context, uri: Uri): File? {
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (uri.authority != expectedAuthority) return null

        val segments = uri.pathSegments
        if (segments.isEmpty()) return null

        val rootName = segments.firstOrNull() ?: return null
        val relativePath = segments.drop(1).joinToString(separator = "/")
        if (relativePath.isBlank()) return null

        val baseDir: File = when (rootName) {
            "gutter_images_external" -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            "gutter_images_internal" -> context.filesDir
            else -> return null
        }

        return File(baseDir, relativePath)
    }

    private fun calculateInSampleSize(context: Context, uri: Uri, maxSize: Int): Int {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            // Ignore
        }
        var inSampleSize = 1
        if (options.outHeight > maxSize || options.outWidth > maxSize) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxSize || halfWidth / inSampleSize >= maxSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 壓縮圖片並儲存至暫存檔。
     * 1. 使用 Android 9.0+ 現代化 ImageDecoder (ALLOCATOR_SOFTWARE) 以降低內存碎片。
     * 2. 自適應尺寸與 JPEG 品質壓縮。
     */
    private fun compressImageToTempFile(context: Context, uri: Uri): File? {
        return try {
            val (maxSize, quality) = getAdaptiveQualityAndSize(context)
            android.util.Log.i(
                "CameraOverlay",
                "adaptive-config uri=$uri, maxSize=$maxSize, quality=$quality"
            )
            val sourceStatsForSize = readImageStats(context, uri)
            android.util.Log.i(
                "CameraOverlay",
                "compress-source-size uri=$uri, bytes=${sourceStatsForSize?.sizeBytes ?: -1L}, dimensions=${sourceStatsForSize?.width}x${sourceStatsForSize?.height}"
            )
            logImageStats("compress-source", context, uri)

            // 1. 使用 ImageDecoder 或 BitmapFactory 載入 Bitmap
            val decodedBitmap: android.graphics.Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(context, uri, maxSize)
                }
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                bitmap
            }

            if (decodedBitmap == null) return null

            // 2. 比例縮放到目標 maxSize 邊界
            val ratio = decodedBitmap.width.toFloat() / decodedBitmap.height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1) { // 橫向
                targetW = maxSize
                targetH = (maxSize / ratio).toInt()
            } else { // 縱向
                targetH = maxSize
                targetW = (maxSize * ratio).toInt()
            }
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
            if (scaledBitmap != decodedBitmap) decodedBitmap.recycle()

            // 3. 處理圖片旋轉
            val rotatedBitmap = handleImageRotation(context, uri, scaledBitmap)

            // 4. 壓縮並儲存為 JPG (品質自適應，體積下降非常有感)
            val tempFile = File.createTempFile("upload_compressed_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            logImageStats("compress-temp", tempFile)
            val originalStats = readImageStats(context, uri)
            val originalSize = originalStats?.sizeBytes ?: 0L
            android.util.Log.i(
                "GutterRepository",
                "圖片壓縮完成 (${maxSize}px, ${quality}%): 原始=${originalSize / 1024}KB, 壓縮後=${tempFile.length() / 1024}KB, 原始尺寸=${originalStats?.width}x${originalStats?.height}, 壓縮尺寸=${rotatedBitmap.width}x${rotatedBitmap.height}"
            )
            
            if (rotatedBitmap != scaledBitmap) scaledBitmap.recycle()
            rotatedBitmap.recycle()
            
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("GutterRepository", "compressImageToTempFile failed: ${e.message}")
            null
        }
    }

    private fun handleImageRotation(context: Context, uri: Uri, bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * 將任意 URI（content:// / file://）複製到 cacheDir 暫存檔並回傳。
     * 失敗時回傳 null。
     */
    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png" -> ".png"
                else        -> ".jpg"
            }
            val tempFile = File.createTempFile("upload_", ext, context.cacheDir)
            tempFile.outputStream().use { out -> inputStream.use { it.copyTo(out) } }
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("GutterRepository", "copyUriToTempFile failed: ${e.message}")
            null
        }
    }

    // ── 新增 / 更新全部點位 ────────────────────────────────────────────────

    /**
     * 新增或更新整條側溝的所有點位。
     * [request.spiNum] 有值時為更新，null 時為新增。
     *
     * @param request 包含 spiNum（可選）及 nodes 列表的 [StoreDitchRequest]
     * @param token   已儲存的 Bearer token
     * @return [ApiResult.Success] 含 [StoreDitchResponse]（data 含完整線段資訊及更新後的 nodes）；
     *         [ApiResult.Error]   含錯誤訊息（401 / 404 / 422 / 500）
     */
    suspend fun storeDitch(
        request: StoreDitchRequest,
        token: String
    ): ApiResult<StoreDitchResponse> {
        return try {
            // 使用 INFO 等級，避免部分裝置 / 篩選條件看不到 DEBUG log
            android.util.Log.i("StoreDitch", "request(json)=${Gson().toJson(request)}")
            val response = api.storeDitch(
                request       = request,
                authorization = "Bearer $token"
            )
            val rawReq = response.raw().request
            android.util.Log.i("StoreDitch", "http ${rawReq.method} ${rawReq.url}")
            val body = response.body()
            val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
            val apiMsg = parseApiErrorMessage(errorBody)
            android.util.Log.i(
                "StoreDitch",
                "response code=${response.code()}, body=$body, errorBody=$errorBody"
            )
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code    = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "儲存失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = apiMsg ?: "儲存失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("StoreDitch", "exception: ${e.message}", e)
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 新增曲線側溝 ────────────────────────────────────────────────────

    /**
     * 新增曲線側溝（以 XY_NUM 陣列指定順序）。
     *
     * @param xyNums 測量座標編號陣列（依順序區分起終點；最少 2 筆）
     * @param token  Bearer token
     */
    suspend fun storeCurveDitch(
        xyNums: List<String>,
        token: String
    ): ApiResult<StoreCurveDitchResponse> {
        return try {
            val request = StoreCurveDitchRequest(xyNums = xyNums)
            android.util.Log.d("StoreCurveDitch", "repository request=${Gson().toJson(request)}")
            val response = api.storeCurveDitch(
                request = request,
                authorization = "Bearer $token"
            )
            val body = response.body()
            val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
            android.util.Log.d(
                "StoreCurveDitch",
                "response code=${response.code()}, body=$body, errorBody=$errorBody"
            )
            val errorParsed: StoreCurveDitchResponse? = errorBody
                ?.takeIf { it.isNotBlank() }
                ?.let { json -> runCatching { Gson().fromJson(json, StoreCurveDitchResponse::class.java) }.getOrNull() }
            val msg = body?.message ?: errorParsed?.message
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = msg ?: "尚未登入，請重新登入",
                    code    = 401
                )
                msg != null -> ApiResult.Error(message = msg, code = response.code())
                else -> ApiResult.Error(
                    message = "儲存失敗",
                    code    = response.code()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("StoreCurveDitch", "json parse failed: xyNums=$xyNums", e)
            ApiResult.Error(message = "資料解析失敗")
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    // ── 刪除側溝 ──────────────────────────────────────────────────────────

    /**
     * 刪除指定側溝（含其所有點位）。
     *
     * @param spiNum 側溝編號
     * @param token  已儲存的 Bearer token
     * @return [ApiResult.Success] 含 [DeleteDitchResponse]；
     *         [ApiResult.Error]   含錯誤訊息（401 / 422 / 500）
     */
    /**
     * 並行查詢一條側溝內所有節點的完整資料（含 WGS84 座標）。
     *
     * 從 [getDitchDetails] 取得的 [DitchNode] 列表中提取 nodeId，
     * 以 [kotlinx.coroutines.async] 同時發出多個 [getNodeDetails] 請求，
     * 任一節點查詢失敗時靜默略過（不中斷整體流程）。
     *
     * @param nodes  DitchDetails.nodes（含 nodeId）
     * @param token  Bearer token
     * @return 成功取得的 [NodeDetails] 列表，保留原始順序
     */
    suspend fun getNodeDetailsForNodes(
        nodes: List<DitchNode>,
        token: String
    ): List<NodeDetails> = coroutineScope {
        nodes.map { node: DitchNode ->
            async<NodeDetails?> {
                when (val result = getNodeDetails(node.nodeId, token)) {
                    is ApiResult.Success -> result.data.data?.firstOrNull()
                    is ApiResult.Error   -> {
                        android.util.Log.w(
                            "GutterRepository",
                            "getNodeDetailsForNodes: 節點 ${node.nodeId} 查詢失敗 – ${result.message}"
                        )
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun deleteDitch(spiNum: String, token: String): ApiResult<DeleteDitchResponse> {
        return try {
            val response = api.deleteDitch(
                spiNum        = spiNum,
                authorization = "Bearer $token"
            )
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code    = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "刪除失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "刪除失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }

    /**
     * 變更側溝狀態（例如恢復狀態）。
     *
     * @param spiNum 側溝編號
     * @param action "restore"
     * @param token  Bearer token
     */
    suspend fun updateDitchState(
        spiNum: String,
        action: String = "restore",
        token: String
    ): ApiResult<UpdateDitchStateResponse> {
        return try {
            val response = api.updateDitchState(
                request = UpdateDitchStateRequest(action = action, spiNum = spiNum),
                authorization = "Bearer $token"
            )
            val body = response.body()
            when {
                response.isSuccessful && body?.success == true -> ApiResult.Success(body)
                response.code() == 401 -> ApiResult.Error(
                    message = "尚未登入，請重新登入",
                    code = 401
                )
                body != null -> {
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "變更狀態失敗",
                        code = response.code()
                    )
                }
                else -> {
                    val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    val apiMsg = parseApiErrorMessage(errorBody)
                    ApiResult.Error(
                        message = apiMsg ?: "變更狀態失敗（${response.code()}）",
                        code = response.code()
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
        }
    }
}
