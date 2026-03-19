package com.example.taoyuangutter.api

import com.google.gson.annotations.SerializedName

// ════════════════════════════════════════════════════════════════
//  POST /api/gutters  ── 上傳單條側溝資料
// ════════════════════════════════════════════════════════════════

/**
 * 單一點位（起點 / 節點 / 終點）的上傳格式。
 * type 固定值："START" | "NODE" | "END"
 */
data class WaypointRequest(
    @SerializedName("type")       val type: String,
    @SerializedName("label")      val label: String,
    @SerializedName("latitude")   val latitude: Double?,
    @SerializedName("longitude")  val longitude: Double?,

    // 基本資料（若未填寫則為空字串）
    @SerializedName("gutter_id")    val gutterId: String   = "",
    @SerializedName("gutter_type")  val gutterType: String = "",
    @SerializedName("coord_x")      val coordX: String     = "",
    @SerializedName("coord_y")      val coordY: String     = "",
    @SerializedName("coord_z")      val coordZ: String     = "",
    @SerializedName("measure_id")   val measureId: String  = "",
    @SerializedName("depth")        val depth: String      = "",
    @SerializedName("top_width")    val topWidth: String   = "",
    @SerializedName("remarks")      val remarks: String    = ""
)

/** 上傳整條側溝的 Request Body */
data class SubmitGutterRequest(
    @SerializedName("waypoints") val waypoints: List<WaypointRequest>
)

/** 上傳側溝的回應 */
data class SubmitGutterResponse(
    @SerializedName("success")    val success: Boolean,
    /** 後端分配的側溝唯一識別碼 */
    @SerializedName("gutter_id") val gutterId: String?,
    @SerializedName("message")   val message: String?
)

// ════════════════════════════════════════════════════════════════
//  GET /api/gutters/map  ── 取得地圖側溝列表
// ════════════════════════════════════════════════════════════════

/** 地圖 API 回傳的單一點位格式 */
data class WaypointResponse(
    @SerializedName("type")       val type: String,
    @SerializedName("label")      val label: String,
    @SerializedName("latitude")   val latitude: Double?,
    @SerializedName("longitude")  val longitude: Double?,

    @SerializedName("gutter_id")    val gutterId: String?   = null,
    @SerializedName("gutter_type")  val gutterType: String? = null,
    @SerializedName("coord_x")      val coordX: String?     = null,
    @SerializedName("coord_y")      val coordY: String?     = null,
    @SerializedName("coord_z")      val coordZ: String?     = null,
    @SerializedName("measure_id")   val measureId: String?  = null,
    @SerializedName("depth")        val depth: String?      = null,
    @SerializedName("top_width")    val topWidth: String?   = null,
    @SerializedName("remarks")      val remarks: String?    = null
)

/** 地圖 API 回傳的單條側溝 */
data class GutterMapItem(
    @SerializedName("id")         val id: String,
    @SerializedName("waypoints")  val waypoints: List<WaypointResponse>
)

/** GET /api/gutters/map 的回應 */
data class GetGuttersMapResponse(
    @SerializedName("gutters") val gutters: List<GutterMapItem>
)

// ════════════════════════════════════════════════════════════════
//  通用錯誤包裝
// ════════════════════════════════════════════════════════════════

/**
 * 封裝 API 呼叫結果，避免直接在 UI 層處理例外。
 * [Success] 含回傳資料；[Error] 含人類可讀的錯誤訊息。
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
