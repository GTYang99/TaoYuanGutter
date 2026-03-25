package com.example.taoyuangutter.api

import android.net.Uri
import android.content.Context
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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
                response.isSuccessful && body?.success == true -> {
                    ApiResult.Success(body)
                }
                body != null -> {
                    // 後端回傳 JSON 但 success=false（401/422/500）
                    // 優先顯示 errors 裡的第一條欄位訊息，否則顯示 message
                    val detail = body.errors?.values?.firstOrNull()?.firstOrNull()
                    ApiResult.Error(
                        message = detail ?: body.message ?: "登入失敗",
                        code    = response.code()
                    )
                }
                else -> {
                    ApiResult.Error(
                        message = response.message().ifEmpty { "登入失敗（${response.code()}）" },
                        code    = response.code()
                    )
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
                        xyNum     = wp.basicData["measureId"]  ?: "",
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

    // ── 取得地圖側溝列表 ──────────────────────────────────────────────────

    /**
     * 從後端取得地圖側溝列表，並轉換為 App 內部的 [Waypoint] 格式。
     *
     * @param centerLat 地圖中心緯度（可選）
     * @param centerLng 地圖中心經度（可選）
     * @param radius    搜尋半徑（公尺，可選）
     * @return [ApiResult.Success] 含 List<List<Waypoint>>，每條側溝一份列表；
     *         [ApiResult.Error]   含錯誤訊息
     */
    suspend fun getGuttersMap(
        centerLat: Double? = null,
        centerLng: Double? = null,
        radius: Int?       = null
    ): ApiResult<List<List<Waypoint>>> {
        return try {
            val response = api.getGuttersMap(lat = centerLat, lng = centerLng, radius = radius)
            if (response.isSuccessful && response.body() != null) {
                val gutters = response.body()!!.gutters.map { gutterItem ->
                    gutterItem.waypoints.map { wpResp ->
                        val type = when (wpResp.nodeAtt) {    // 1=起點, 2=節點, 3=終點
                            1    -> WaypointType.START
                            3    -> WaypointType.END
                            else -> WaypointType.NODE
                        }
                        // NODE_Y = 緯度(N)，NODE_X = 經度(E)
                        val latLng = if (wpResp.nodeY != null && wpResp.nodeX != null)
                            LatLng(wpResp.nodeY, wpResp.nodeX) else null

                        Waypoint(
                            type      = type,
                            label     = when (type) {
                                WaypointType.START -> "起點"
                                WaypointType.END   -> "終點"
                                WaypointType.NODE  -> "節點${wpResp.nodeNum}"
                            },
                            latLng    = latLng,
                            basicData = hashMapOf(
                                "gutterId"   to (wpResp.nodeId    ?: ""),
                                "gutterType" to (wpResp.nodeTyP   ?: ""),
                                "matTyp"     to (wpResp.matTyp    ?: ""),
                                "coordX"     to (wpResp.nodeX?.toString() ?: ""),
                                "coordY"     to (wpResp.nodeY?.toString() ?: ""),
                                "coordZ"     to (wpResp.nodeLe    ?: ""),
                                "measureId"  to (wpResp.xyNum     ?: ""),
                                "depth"      to (wpResp.nodeDep   ?: ""),
                                "topWidth"   to (wpResp.nodeWid   ?: ""),
                                "isBroken"   to (wpResp.isBroken  ?: ""),
                                "isHanging"  to (wpResp.isHanging ?: ""),
                                "isSilt"     to (wpResp.isSilt    ?: ""),
                                "remarks"    to (wpResp.nodeNote  ?: ""),
                                "photo1"     to (wpResp.photoOv   ?: ""),
                                "photo2"     to (wpResp.photoWid  ?: ""),
                                "photo3"     to (wpResp.photoDep  ?: "")
                            )
                        )
                    }
                }
                ApiResult.Success(gutters)
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
        maxLng: Double
    ): ApiResult<ScopeSearchResponse> {
        return try {
            val response = api.getScopeSearch(
                minLat = minLat,
                maxLat = maxLat,
                minLng = minLng,
                maxLng = maxLng
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
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
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
            val file = File(imageUri.path ?: return ApiResult.Error("無效的圖片路徑"))
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val nodeIdBody       = nodeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val fileCategoryBody = fileCategory.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadNodeImage(
                nodeId        = nodeIdBody,
                fileCategory  = fileCategoryBody,
                file          = filePart,
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
                        message = detail ?: body.message ?: "上傳失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "上傳失敗（${response.code()}）",
                    code    = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "網路連線失敗")
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
            val response = api.storeDitch(
                request       = request,
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
                        message = detail ?: body.message ?: "儲存失敗",
                        code    = response.code()
                    )
                }
                else -> ApiResult.Error(
                    message = "儲存失敗（${response.code()}）",
                    code    = response.code()
                )
            }
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
