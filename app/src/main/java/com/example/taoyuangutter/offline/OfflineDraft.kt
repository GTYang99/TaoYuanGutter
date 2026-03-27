package com.example.taoyuangutter.offline

/**
 * 離線草稿：在不需要登入 / 選地圖點位的情況下，
 * 讓使用者先行填寫側溝基本資料與照片，儲存於本機。
 *
 * 座標固定為 (0.0, 0.0)，待後續匯入正式座標資訊。
 */
data class OfflineDraft(
    /** 唯一識別碼，以建立時間毫秒數作為主鍵 */
    val id: Long = System.currentTimeMillis(),
    /** 建立時間（毫秒），供列表排序與顯示 */
    val savedAt: Long = System.currentTimeMillis(),

    // ── 基本資料欄位 ─────────────────────────────────────────────────────
    val gutterId:   String = "",
    val gutterType: String = "",
    val matTyp:     String = "",   // 側溝材質：混凝土/卵礫石/紅磚
    val coordX:     String = "",   // 待匯入，預設 "0.0"
    val coordY:     String = "",   // 待匯入，預設 "0.0"
    val coordZ:     String = "",
    val xyNum:      String = "",
    val depth:      String = "",
    val topWidth:   String = "",
    val isBroken:   String = "",   // 溝體結構受損：否/是
    val isHanging:  String = "",   // 附掛或過路管線：無/有
    val isSilt:     String = "",   // 淤積程度：無/輕度/中度/嚴重
    val remarks:    String = "",

    // ── 照片路徑 ─────────────────────────────────────────────────────────
    val photo1: String = "",
    val photo2: String = "",
    val photo3: String = ""
)
