package com.example.taoyuangutter.api

import android.net.Uri
import android.content.Context
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
     *
     * @param context      用於從 Uri 取得實體檔案路徑
     * @param nodeId       點位 ID
     * @param fileCategory 照片類別（1 / 2 / 3）
     * @param imageUri     已拍攝或選取的圖片 Uri
     * @param token        已儲存的 Bearer token
     * @return [ApiResult.Success] 含 [NodeImageUploadResponse]（data.url 為新圖片網址）；
     *         [ApiResult.Error]   含錯誤訊息（401 尚未登入、422 欄位未填、500 伺服器錯誤）
     */
    suspend fun uploadNodeImage(
        context: Context,
        nodeId: Int,
        fileCategory: Int,
        imageUri: Uri,
        token: String
    ): ApiResult<NodeImageUploadResponse> {
        return try {
            // 支援 content:// 與 file:// URI，統一先複製到暫存檔再上傳
            val tempFile = withContext(Dispatchers.IO) {
                copyUriToTempFile(context, imageUri)
            }
                ?: return ApiResult.Error("無法讀取圖片檔案")
            try {
                // 使用 INFO 等級，避免部分裝置 / 篩選條件看不到 DEBUG log
                android.util.Log.i(
                    "PhotoUpload",
                    "request nodeId=$nodeId, category=$fileCategory, uri=$imageUri, temp=${tempFile.name} (${tempFile.length()} bytes)"
                )
                val requestFile  = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart     = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val nodeIdBody       = nodeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val fileCategoryBody = fileCategory.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadNodeImage(
                    nodeId        = nodeIdBody,
                    fileCategory  = fileCategoryBody,
                    file          = filePart,
                    authorization = "Bearer $token"
                )
                val rawReq = response.raw().request
                android.util.Log.i("PhotoUpload", "http ${rawReq.method} ${rawReq.url}")
                val body = response.body()
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                val apiMsg = parseApiErrorMessage(errorBody)
                android.util.Log.i(
                    "PhotoUpload",
                    "response nodeId=$nodeId, category=$fileCategory, code=${response.code()}, body=$body, errorBody=$errorBody"
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
                            message = detail ?: body.message ?: "上傳失敗",
                            code    = response.code()
                        )
                    }
                    else -> ApiResult.Error(
                        message = apiMsg ?: "上傳失敗（${response.code()}）",
                        code    = response.code()
                    )
                }
            } finally {
                withContext(Dispatchers.IO) {
                    tempFile.delete()   // 上傳完畢（無論成敗）清除暫存檔
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(
                "PhotoUpload",
                "exception nodeId=$nodeId, category=$fileCategory, uri=$imageUri: ${e.message}",
                e
            )
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
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
}
