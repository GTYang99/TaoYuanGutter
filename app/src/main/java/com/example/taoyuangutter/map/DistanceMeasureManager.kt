package com.example.taoyuangutter.map

import android.graphics.Color
import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

/**
 * 測距模式管理器。
 *
 * 職責：
 * - 管理「起點 Marker」與「虛線 Polyline」的生命週期。
 * - 監聽地圖點擊（設定起點）與鏡頭移動（即時更新虛線 + 距離）。
 * - 進入 / 離開測距模式時清理地圖與 listener。
 *
 * 使用流程：
 * 1. 進入：呼叫 [enter]，地圖中央出現準星（由 Activity 負責顯示 ImageView）。
 * 2. 使用者點擊地圖任一位置 → 設定起點大頭針。
 * 3. 拖曳地圖 → 虛線從起點延伸至鏡頭中央，[onDistanceChanged] 即時回呼。
 * 4. 重設：呼叫 [reset] → 清除大頭針與虛線，等待下一次點擊。
 * 5. 離開：呼叫 [exit] → 還原所有 listener，清除地圖疊加。
 *
 * @param map               Google Map 實例。
 * @param config            客製化圖示設定（見 [MeasureConfig]）。
 * @param onDistanceChanged 距離回呼（單位：公尺）；null = 尚未設定起點或已離開模式。
 */
class DistanceMeasureManager(
    private val map: GoogleMap,
    private val config: MeasureConfig = MeasureConfig(),
    private val onDistanceChanged: (meters: Double?) -> Unit
) {

    /** 目前是否處於測距模式 */
    var isMeasuring: Boolean = false
        private set

    private var startPoint: LatLng? = null
    private var startMarker: Marker? = null
    private var measurePolyline: Polyline? = null

    // 虛線樣式：30px 實線、15px 空白，交替
    private val dashPattern: List<PatternItem> = listOf(Dash(30f), Gap(15f))

    // ── 公開 API ──────────────────────────────────────────────────────────────

    /** 進入測距模式。 */
    fun enter() {
        if (isMeasuring) return
        isMeasuring = true
        startPoint = null
        onDistanceChanged(null)

        map.setOnMapClickListener { latLng -> placeStartPoint(latLng) }
        map.setOnCameraMoveListener { updateLine() }
    }

    /**
     * 離開測距模式，清除地圖疊加與 listener。
     * 呼叫後 [isMeasuring] = false。
     */
    fun exit() {
        if (!isMeasuring) return
        isMeasuring = false

        map.setOnMapClickListener(null)
        map.setOnCameraMoveListener(null)
        clearOverlays()
        onDistanceChanged(null)
    }

    /**
     * 重設起點：清除大頭針與虛線，等待使用者重新點擊設定起點。
     * 不會離開測距模式。
     */
    fun reset() {
        clearOverlays()
        startPoint = null
        onDistanceChanged(null)
    }

    /**
     * 直接以指定座標設定測距起點（例如：使用者點擊現有大頭針時呼叫）。
     * 效果等同於使用者點擊地圖，但座標由外部傳入。
     * 必須在 [isMeasuring] 為 true 時才有效。
     */
    fun setStartPoint(latLng: LatLng) {
        if (!isMeasuring) return
        placeStartPoint(latLng)
    }

    // ── 私有邏輯 ──────────────────────────────────────────────────────────────

    /** 使用者點擊地圖時呼叫，放置起點大頭針。 */
    private fun placeStartPoint(latLng: LatLng) {
        // 清除舊的起點與虛線
        clearOverlays()

        startPoint = latLng
        startMarker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .apply { config.startMarkerIcon?.let { icon(it) } }
        )
        // 立即繪製一次虛線（鏡頭可能已不在點擊位置）
        updateLine()
    }

    /**
     * 在鏡頭移動時更新虛線終點 + 距離回呼。
     * 若起點尚未設定則忽略。
     */
    private fun updateLine() {
        val start = startPoint ?: return
        val end = map.cameraPosition.target

        // 移除舊虛線再重新建立（Polyline 沒有 setPoints API，必須 remove + addPolyline）
        measurePolyline?.remove()
        measurePolyline = map.addPolyline(
            PolylineOptions()
                .add(start, end)
                .color(Color.parseColor("#3F51B5"))   // 深藍紫，與截圖配色接近
                .width(8f)
                .pattern(dashPattern)
                .clickable(false)
        )

        val meters = distanceBetween(start, end)
        onDistanceChanged(meters)
    }

    /** 清除地圖上的大頭針與虛線。 */
    private fun clearOverlays() {
        startMarker?.remove();   startMarker = null
        measurePolyline?.remove(); measurePolyline = null
    }

    // ── 工具函式 ─────────────────────────────────────────────────────────────

    /** 使用 android.location.Location 計算兩點間距離（公尺），不需要額外依賴。 */
    private fun distanceBetween(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    companion object {
        /**
         * 將距離（公尺）格式化為顯示用字串。
         * - < 1000 m → 顯示「X 公尺」
         * - ≥ 1000 m → 顯示「X.XX 公里」
         */
        fun formatDistance(meters: Double): String =
            if (meters < 1000.0) {
                "${meters.toInt()} 公尺"
            } else {
                "%.2f 公里".format(meters / 1000.0)
            }
    }
}
