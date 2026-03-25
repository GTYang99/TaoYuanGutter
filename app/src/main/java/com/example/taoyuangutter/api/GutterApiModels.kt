package com.example.taoyuangutter.api

import com.google.gson.annotations.SerializedName

// ════════════════════════════════════════════════════════════════
//  POST /api/gutters  ── 上傳單條側溝資料
// ════════════════════════════════════════════════════════════════

/**
 * 單一點位（起點 / 節點 / 終點）的上傳格式。
 * 欄位名稱對應文件「側溝點位基本資料欄位」與「側溝點位照片欄位」。
 *
 * NODE_ATT 固定值：1 = 起點、2 = 節點、3 = 終點
 */
data class WaypointRequest(

    // ── 點位識別（後端自動補齊 / App 帶入）──────────────────────
    /** 點位流水 UUID；由後端產生，App 可帶入空字串 */
    @SerializedName("NODE_ID")     val nodeId: String   = "",
    /** 點位屬性：1=起點、2=節點、3=終點 */
    @SerializedName("NODE_ATT")    val nodeAtt: Int,
    /** 點位排序序號（從 0 起始） */
    @SerializedName("NODE_NUM")    val nodeNum: Int,

    // ── 基本資料（使用者填寫）────────────────────────────────────
    /** 側溝型式：1=U型溝(明溝)、2=U型溝(加蓋)、3=L型溝與暗溝渠併用、4=其他 */
    @SerializedName("NODE_TYP")    val nodeTyP: String  = "",
    /** 側溝材質：1=混凝土、2=卵礫石、3=紅磚 */
    @SerializedName("MAT_TYP")     val matTyp: String   = "",
    /** 側溝 X(E) 座標（來自地圖選點或使用者輸入） */
    @SerializedName("NODE_X")      val nodeX: Double?   = null,
    /** 側溝 Y(N) 座標 */
    @SerializedName("NODE_Y")      val nodeY: Double?   = null,
    /** 側溝 Z 座標（高程）*/
    @SerializedName("NODE_LE")     val nodeLe: String   = "",
    /** 測量座標編號，供後續匯入比對 */
    @SerializedName("XY_NUM")      val xyNum: String    = "",
    /** 側溝測量深度（公分）；合理區間 35–110 cm */
    @SerializedName("NODE_DEP")    val nodeDep: String  = "",
    /** 側溝頂寬度（公分）；合理區間 > 25 cm */
    @SerializedName("NODE_WID")    val nodeWid: String  = "",
    /** 溝體結構受損：0=否、1=是 */
    @SerializedName("IS_BROKEN")   val isBroken: String = "",
    /** 附掛或過路管線：0=無、1=有 */
    @SerializedName("IS_HANGING")  val isHanging: String = "",
    /** 淤積程度：0=無、1=輕度、2=中度、3=嚴重 */
    @SerializedName("IS_SILT")     val isSilt: String   = "",
    /** 補充說明（非必填） */
    @SerializedName("NODE_NOTE")   val nodeNote: String = "",

    // ── 照片（URI 字串；multipart 分離上傳待實作）───────────────
    /** 測量位置及側溝概況（橫向 4:3） */
    @SerializedName("PHOTO_OV")    val photoOv: String  = "",
    /** 側溝內徑寬度尺寸（橫向 4:3） */
    @SerializedName("PHOTO_WID")   val photoWid: String = "",
    /** 側溝深度尺寸（橫向 4:3） */
    @SerializedName("PHOTO_DEP")   val photoDep: String = ""
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

/** 地圖 API 回傳的單一點位格式（欄位名稱與 WaypointRequest 對齊） */
data class WaypointResponse(
    @SerializedName("NODE_ID")     val nodeId: String?    = null,
    @SerializedName("NODE_ATT")    val nodeAtt: Int       = 2,
    @SerializedName("NODE_NUM")    val nodeNum: Int       = 0,
    @SerializedName("NODE_TYP")    val nodeTyP: String?   = null,
    @SerializedName("MAT_TYP")     val matTyp: String?    = null,
    @SerializedName("NODE_X")      val nodeX: Double?     = null,
    @SerializedName("NODE_Y")      val nodeY: Double?     = null,
    @SerializedName("NODE_LE")     val nodeLe: String?    = null,
    @SerializedName("XY_NUM")      val xyNum: String?     = null,
    @SerializedName("NODE_DEP")    val nodeDep: String?   = null,
    @SerializedName("NODE_WID")    val nodeWid: String?   = null,
    @SerializedName("IS_BROKEN")   val isBroken: String?  = null,
    @SerializedName("IS_HANGING")  val isHanging: String? = null,
    @SerializedName("IS_SILT")     val isSilt: String?    = null,
    @SerializedName("NODE_NOTE")   val nodeNote: String?  = null,
    @SerializedName("PHOTO_OV")    val photoOv: String?   = null,
    @SerializedName("PHOTO_WID")   val photoWid: String?  = null,
    @SerializedName("PHOTO_DEP")   val photoDep: String?  = null
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
