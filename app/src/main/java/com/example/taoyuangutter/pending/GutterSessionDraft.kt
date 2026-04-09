package com.example.taoyuangutter.pending

/**
 * 待上傳的完整側溝填報草稿。
 *
 * 當 [submitGutter] 失敗，或使用者中途放棄並選擇「儲存草稿」時，
 * 將目前 AddGutterBottomSheet 的所有 waypoints 序列化並存入此物件。
 */
data class GutterSessionDraft(
    /** 唯一識別碼，以建立時間毫秒數作為主鍵 */
    val id: Long = System.currentTimeMillis(),
    /** 建立／更新時間（毫秒），供列表排序與時間顯示 */
    val savedAt: Long = System.currentTimeMillis(),
    /**
     * 草稿類型：
     * - "gutter"：一般側溝（AddGutterBottomSheet 地圖流程）
     * - "curve"：弧線側溝（AddCurveActivity）
     *
     * 舊版草稿預設 "gutter"，Gson 反序列化時不影響既有資料。
     */
    val kind: String = KIND_GUTTER,
    /** true = 離線草稿（不打 API，只在本機編輯與保存） */
    val isOffline: Boolean = false,
    /**
     * true = 純單點離線填報（由 GutterFormActivity.newOfflineIntent 建立）。
     * false = 地圖流程多點草稿（由 AddGutterBottomSheet 建立）。
     * 舊版草稿預設 false，Gson 反序列化時不影響既有資料。
     */
    val isSinglePoint: Boolean = false,
    /** 所有 waypoints 的快照列表（START / NODE / END） */
    val waypoints: List<WaypointSnapshot> = emptyList()
)

const val KIND_GUTTER = "gutter"
const val KIND_CURVE = "curve"

/**
 * 單一 Waypoint 的可序列化快照。
 * 與 [com.example.taoyuangutter.gutter.Waypoint] 一一對應，
 * 但以純 data class 表示，方便 Gson 序列化。
 */
data class WaypointSnapshot(
    val type: String = "",           // WaypointType.name（START / NODE / END）
    val label: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val basicData: HashMap<String, String> = hashMapOf()
)
