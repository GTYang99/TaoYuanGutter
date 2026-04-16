package com.example.taoyuangutter.api

import com.google.gson.JsonElement
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
    /** 溝蓋板厚度（公分）；後端欄位名 COVER_DEP */
    @SerializedName("COVER_DEP")   val coverDep: String = "",
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
    /** 溝蓋板厚度（公分）；後端可能回傳字串或數字 */
    @SerializedName("COVER_DEP")   val coverDep: Any?     = null,
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

//  POST /api/login  ── 使用者登入
// ════════════════════════════════════════════════════════════════

/** 登入 Request Body */
data class LoginRequest(
    @SerializedName("username")  val username: String,
    @SerializedName("password")  val password: String,
    @SerializedName("device_id") val deviceId: String = "android"
)

/** 登入成功時 data 欄位 */
data class LoginData(
    @SerializedName("token")    val token: String,
    @SerializedName("name")     val name: String?,
    @SerializedName("group_id") val groupId: Int?,
    @SerializedName("company")  val company: String?
)

/** 登入 API 回應（200 / 401 / 422 / 500 共用同一結構） */
data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: LoginData?,
    /** 422 驗證失敗時後端回傳的欄位錯誤清單 */
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

// ════════════════════════════════════════════════════════════════
//  GET /api/logout  ── 使用者登出
// ════════════════════════════════════════════════════════════════

/** 登出 API 回應（200 / 401 / 500 共用同一結構） */
data class LogoutResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

//  GET /api/v1/map/scopeSearch  ── 依地圖可視範圍取得側溝座標
// ════════════════════════════════════════════════════════════════

/**
 * scopeSearch 回應最外層
 * { "success": true, "message": "查詢成功", "data": { "type": "FeatureCollection", "features": [...] } }
 */
data class ScopeSearchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: GeoFeatureCollection?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

/** GeoJSON FeatureCollection */
data class GeoFeatureCollection(
    @SerializedName("type")     val type: String,
    @SerializedName("features") val features: List<GeoFeature>
)

/** GeoJSON Feature（每條側溝一個） */
data class GeoFeature(
    @SerializedName("type")       val type: String,
    @SerializedName("geometry")   val geometry: GeoGeometry?,
    @SerializedName("properties") val properties: GeoProperties?
)

/** GeoJSON LineString Geometry：coordinates 為 [[lng, lat], [lng, lat], ...] */
data class GeoGeometry(
    @SerializedName("type")        val type: String,
    /** 每個元素為 [longitude, latitude] */
    @SerializedName("coordinates") val coordinates: List<List<Double>>
)

/** Feature 附屬屬性 */
data class GeoProperties(
    @SerializedName("SPI_NUM")  val spiNum: String?,
    @SerializedName("group_id") val groupId: String?,
    /** 1=已完成、2=待修正、3=待匯入座標紀錄 */
    @SerializedName("SPI_STATE") val spiState: Int?
)

// ════════════════════════════════════════════════════════════════
//  GET /api/v1/ditch/ditchDetails  ── 取得線段資料
// ════════════════════════════════════════════════════════════════

/** 取得線段資料 API 回應最外層 */
data class DitchDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: DitchDetails?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

/** 線段詳細資料 */
data class DitchDetails(
    @SerializedName("ditch_id") val ditchId: Int,
    @SerializedName("SPI_NUM")  val spiNum: String,
    @SerializedName("SPI_TYP")  val spiTyp: String?,
    /** 起點 TWD97 X 座標 */
    @SerializedName("STR_X")    val strX: String?,
    /** 起點 TWD97 Y 座標 */
    @SerializedName("STR_Y")    val strY: String?,
    /** 終點 TWD97 X 座標 */
    @SerializedName("END_X")    val endX: String?,
    /** 終點 TWD97 Y 座標 */
    @SerializedName("END_Y")    val endY: String?,
    /** 起點高程 */
    @SerializedName("STR_LE")   val strLe: String?,
    /** 終點高程 */
    @SerializedName("END_LE")   val endLe: String?,
    @SerializedName("NODE_XY")  val nodeXy: String?,
    /** 起點深度（公分） */
    @SerializedName("STR_DEP")  val strDep: Int?,
    /** 終點深度（公分） */
    @SerializedName("END_DEP")  val endDep: Int?,
    /** 起點寬度（公分） */
    @SerializedName("STR_WID")  val strWid: Int?,
    /** 終點寬度（公分） */
    @SerializedName("END_WID")  val endWid: Int?,
    /** 線段長度（公尺） */
    @SerializedName("LENG")     val leng: String?,
    /** 坡度 */
    @SerializedName("SLOP")     val slop: String?,
    @SerializedName("NOTE")     val note: String?,
    /** 所有點位 */
    @SerializedName("nodes")    val nodes: List<DitchNode>
)

/** 線段內的單一點位（摘要，不含座標；完整資料請呼叫 nodeDetails） */
data class DitchNode(
    @SerializedName("node_id")  val nodeId: Int,
    /** 點位屬性：1=起點、2=節點、3=終點 */
    @SerializedName("NODE_ATT") val nodeAtt: String?,
    @SerializedName("NODE_NUM") val nodeNum: String?,
    /** 該點位已上傳的照片 URL 列表（可為空） */
    @SerializedName("url")      val url: List<NodeImageUrl>
)

/** 點位照片 URL */
data class NodeImageUrl(
    @SerializedName("url")          val url: String,
    @SerializedName("node_id")      val nodeId: String?,
    /** 照片類別：1=測量位置及側溝概況、2=側溝內徑寬度尺寸、3=側溝深度尺寸 */
    @SerializedName("fileCategory") val fileCategory: String?,
    @SerializedName("id")           val id: Int?
)

// ════════════════════════════════════════════════════════════════
//  GET /api/v1/node/nodeDetails  ── 取得點位資料
// ════════════════════════════════════════════════════════════════

/** 取得點位資料 API 回應最外層 */
data class NodeDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: List<NodeDetails>?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

/** 單一點位完整資料 */
data class NodeDetails(
    @SerializedName("ditch_id")   val ditchId: String?,
    @SerializedName("NODE_NUM")   val nodeNum: String?,
    /** 點位屬性：1=起點、2=節點、3=終點（API key 為 NODE_ATT，無尾部 R） */
    @SerializedName("NODE_ATT")   val nodeAttr: String?,
    /** 側溝形式：1=U形溝（明溝）2=U形溝（加蓋）3=L形溝與暗溝渠併用 4=其他 */
    @SerializedName("NODE_TYP")   val nodeTyP: String?,
    /** 側溝材質：1=混凝土 2=卵礫石 3=紅磚 */
    @SerializedName("MAT_TYP")    val matTyp: String?,
    /** TWD97 X 座標（經度方向） */
    @SerializedName("NODE_X")     val nodeX: String?,
    /** TWD97 Y 座標（緯度方向） */
    @SerializedName("NODE_Y")     val nodeY: String?,
    /** 高程 */
    @SerializedName("NODE_LE")    val nodeLe: String?,
    /** 測量座標編號 */
    @SerializedName("XY_NUM")     val xyNum: String?,
    /**
     * 溝蓋板厚度（公分）。
     * 後端可能回傳：數字 / 字串數字 / 空字串。
     * 用 Any? 接住，轉字串時使用 [coverDepAsString]。
     */
    @SerializedName("COVER_DEP")  val coverDep: Any?,
    /**
     * 深度（公分）。
     * 後端可能回傳：數字 / 字串數字 / 空字串。
     * 用 Any? 接住，轉字串時使用 [nodeDepAsString]。
     */
    @SerializedName("NODE_DEP")   val nodeDep: Any?,
    /**
     * 寬度（公分）。
     * 後端可能回傳：數字 / 字串數字 / 空字串。
     * 用 Any? 接住，轉字串時使用 [nodeWidAsString]。
     */
    @SerializedName("NODE_WID")   val nodeWid: Any?,
    /**
     * 無法開蓋（可能為空字串 / 0 / 1 / true / false）。
     * 由於型別不穩定，先以 Any? 接住，使用 [isCantOpenAsBoolean] 取值。
     */
    @SerializedName("IS_CANTOPEN") val isCantOpen: Any?,
    /** 溝體結構受損：0=否 1=是 */
    @SerializedName("IS_BROKEN")  val isBroken: String?,
    /** 附掛或過路管線：0=無 1=有 */
    @SerializedName("IS_HANGING") val isHanging: String?,
    /** 淤積程度：0=無 1=輕度 2=中度 3=嚴重 */
    @SerializedName("IS_SILT")    val isSilt: String?,
    @SerializedName("NOTE")       val note: String?,
    /** WGS84 緯度 */
    @SerializedName("latitude")   val latitude: String?,
    /** WGS84 經度 */
    @SerializedName("longitude")  val longitude: String?,
    /** 已上傳的照片列表 */
    @SerializedName("node_img")   val nodeImg: List<NodeImg> = emptyList()
) {
    /**
     * 將深度轉為純數字字串（去掉不必要的小數點，如 1.0 → "1"、1.5 → "1.5"）。
     * 若 API 未回傳則回傳空字串。
     */
    val nodeDepAsString: String
        get() = when (val v = nodeDep) {
            null -> ""
            is Number -> {
                val d = v.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            is String -> v.trim()
            else -> v.toString()
        }.let { s -> if (s == "null") "" else s }

    /** 同 [nodeDepAsString]，適用於寬度。 */
    val nodeWidAsString: String
        get() = when (val v = nodeWid) {
            null -> ""
            is Number -> {
                val d = v.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            is String -> v.trim()
            else -> v.toString()
        }.let { s -> if (s == "null") "" else s }

    /**
     * 將溝蓋板厚度轉為純數字字串（去掉不必要的小數點，如 1.0 → "1"、1.5 → "1.5"）。
     * 若 API 未回傳則回傳空字串。
     */
    val coverDepAsString: String
        get() = when (val v = coverDep) {
            null -> ""
            is Number -> {
                val d = v.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            is String -> v.trim()
            else -> v.toString()
        }.let { s -> if (s == "null") "" else s }

    val isCantOpenAsBoolean: Boolean
        get() = when (val v = isCantOpen) {
            null -> false
            is Boolean -> v
            is Number -> v.toInt() == 1
            is String -> when (v.trim().lowercase()) {
                "1", "true", "t", "y", "yes" -> true
                else -> false
            }
            else -> false
        }
}

/** 點位照片（nodeDetails 回傳格式，僅含 url 與 fileCategory） */
data class NodeImg(
    @SerializedName("url")          val url: String,
    /** 照片類別：1=測量位置及側溝概況、2=側溝內徑寬度尺寸、3=側溝深度尺寸 */
    @SerializedName("fileCategory") val fileCategory: String?
)

// ════════════════════════════════════════════════════════════════
//  POST /api/v1/node/nodeImage  ── 點位照片上傳
// ════════════════════════════════════════════════════════════════

/** 照片上傳成功時 data 欄位（只含新圖片 URL） */
data class NodeImageUploadData(
    @SerializedName("url") val url: String
)

/** 點位照片上傳 API 回應（200 / 401 / 422 / 500 共用） */
data class NodeImageUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: NodeImageUploadData?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

// ════════════════════════════════════════════════════════════════
//  POST /api/v1/ditch/storeDitch  ── 新增 / 更新全部點位
// ════════════════════════════════════════════════════════════════

/**
 * 單一點位上傳格式（storeDitch 專用）。
 * 更新時帶入 [nodeId]；新增時省略（null）。
 * [nodeNote] 為唯一非必填欄位。
 */
data class StoreDitchNodeRequest(
    /** 僅更新時帶入；新增時省略 */
    @SerializedName("node_id")    val nodeId: Int?     = null,
    /** 點位屬性：1=起點、2=節點、3=終點 */
    @SerializedName("NODE_ATT")   val nodeAtt: Int,
    @SerializedName("NODE_NUM")   val nodeNum: Int? = null,
    /** 側溝型式：1=U型溝(明溝)、2=U型溝(加蓋)、3=L型溝與暗溝渠併用、4=其他 */
    @SerializedName("NODE_TYP")   val nodeTyp: Int,
    /** 側溝材質：1=混凝土、2=卵礫石、3=紅磚 */
    @SerializedName("MAT_TYP")    val matTyp: Int?,
    @SerializedName("latitude")   val latitude: Double,
    @SerializedName("longitude")  val longitude: Double,
    /** 高程（Z 值）；目前依 API 規則固定送 null */
    @SerializedName("NODE_LE")    val nodeLe: Double?,
    @SerializedName("XY_NUM")     val xyNum: String,
    /** 無法開蓋：0=false、1=true */
    @SerializedName("IS_CANTOPEN") val isCantOpen: Int,
    /** 深度（公分） */
    @SerializedName("NODE_DEP")   val nodeDep: Int?,
    /** 寬度（公分） */
    @SerializedName("NODE_WID")   val nodeWid: Int?,
    /**
     * 溝蓋板厚度（公分）；對應 UI「溝蓋板厚度」。
     * 後端有時回傳字串，因此用 Any? 接收；送出時仍會以 Int? 填入。
     */
    @SerializedName("COVER_DEP")  val coverDep: Any?,
    @SerializedName("IS_BROKEN")  val isBroken: Int?,
    @SerializedName("IS_HANGING") val isHanging: Int?,
    /** 淤積程度：0=無、1=輕度、2=中度、3=嚴重 */
    @SerializedName("IS_SILT")    val isSilt: Int?,
    /** 補充說明（非必填） */
    @SerializedName("NODE_NOTE")  val nodeNote: String? = null
)

/**
 * storeDitch Request Body。
 * 更新時帶入 [spiNum]；新增時省略（null）。
 */
data class StoreDitchRequest(
    /** 僅更新時帶入；新增時省略 */
    @SerializedName("SPI_NUM") val spiNum: String?                  = null,
    @SerializedName("nodes")   val nodes: List<StoreDitchNodeRequest>
)

/**
 * storeDitch 回應。
 * 新增成功回傳 "新增成功"；更新成功回傳 "修改成功"；data 結構與 [DitchDetails] 相同。
 */
data class StoreDitchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: DitchDetails?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

// ════════════════════════════════════════════════════════════════
//  POST /api/v1/ditch/storeCurveDitch  ── 新增曲線側溝
// ════════════════════════════════════════════════════════════════

/**
 * 新增曲線側溝 Request Body。
 *
 * XY_NUM 陣列依順序區分起點與終點（最少 2 筆）。
 */
data class StoreCurveDitchRequest(
    @SerializedName("XY_NUM") val xyNums: List<String>
)

/**
 * 新增曲線側溝回應。
 *
 * 注意：errors 可能為 object / array([]) / null，因此用 [JsonElement] 容錯，
 * 避免遇到 errors=[] 時 Gson 解析 Map 直接失敗。
 */
data class StoreCurveDitchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: DitchDetails?,
    @SerializedName("errors")  val errors: JsonElement?
)

// ════════════════════════════════════════════════════════════════
//  DELETE /api/v1/ditch/deleteDitch  ── 刪除側溝
// ════════════════════════════════════════════════════════════════

/** 刪除側溝 API 回應（200 / 401 / 422 / 500 共用） */
data class DeleteDitchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
)

// ════════════════════════════════════════════════════════════════
//  GET /api/v1/node/nodeDetails（無參數）── 取得所有點位列表
// ════════════════════════════════════════════════════════════════

/**
 * 取得所有點位列表的 API 回應。
 * 當呼叫 /api/v1/node/nodeDetails 不帶任何參數時，取得所有既有點位。
 */
data class AllNodeDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data")    val data: List<NodeDetails>?,
    @SerializedName("errors")  val errors: Map<String, List<String>>?
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
