package com.example.taoyuangutter.api

import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.LatLng

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
                waypoints = waypoints.map { wp ->
                    WaypointRequest(
                        type      = wp.type.name,          // "START" | "NODE" | "END"
                        label     = wp.label,
                        latitude  = wp.latLng?.latitude,
                        longitude = wp.latLng?.longitude,
                        gutterId  = wp.basicData["gutterId"]   ?: "",
                        gutterType = wp.basicData["gutterType"] ?: "",
                        coordX    = wp.basicData["coordX"]     ?: "",
                        coordY    = wp.basicData["coordY"]     ?: "",
                        coordZ    = wp.basicData["coordZ"]     ?: "",
                        measureId = wp.basicData["measureId"]  ?: "",
                        depth     = wp.basicData["depth"]      ?: "",
                        topWidth  = wp.basicData["topWidth"]   ?: "",
                        remarks   = wp.basicData["remarks"]    ?: ""
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
                        val type = when (wpResp.type.uppercase()) {
                            "START" -> WaypointType.START
                            "END"   -> WaypointType.END
                            else    -> WaypointType.NODE
                        }
                        val latLng = if (wpResp.latitude != null && wpResp.longitude != null)
                            LatLng(wpResp.latitude, wpResp.longitude) else null

                        Waypoint(
                            type      = type,
                            label     = wpResp.label,
                            latLng    = latLng,
                            basicData = hashMapOf(
                                "gutterId"   to (wpResp.gutterId   ?: ""),
                                "gutterType" to (wpResp.gutterType ?: ""),
                                "coordX"     to (wpResp.coordX     ?: ""),
                                "coordY"     to (wpResp.coordY     ?: ""),
                                "coordZ"     to (wpResp.coordZ     ?: ""),
                                "measureId"  to (wpResp.measureId  ?: ""),
                                "depth"      to (wpResp.depth      ?: ""),
                                "topWidth"   to (wpResp.topWidth   ?: ""),
                                "remarks"    to (wpResp.remarks    ?: "")
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
}
