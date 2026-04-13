# StoreDitchNodeRequest 完整上傳格式

## 📤 整體結構

```
POST /api/v1/ditch/storeDitch
Content-Type: application/json

StoreDitchRequest {
    spiNum: String?              // 新增時 null，更新時帶入
    nodes: List<StoreDitchNodeRequest>
}
```

---

## 🔍 StoreDitchNodeRequest 完整字段

### 類定義（GutterApiModels.kt 第 378-405 行）

```kotlin
data class StoreDitchNodeRequest(
    // ① 識別字段
    @SerializedName("node_id")     val nodeId: Int?      = null,  // 更新時才帶入

    // ② 點位屬性（必填）
    @SerializedName("NODE_ATT")    val nodeAtt: Int,     // 1=起點、2=節點、3=終點
    @SerializedName("NODE_NUM")    val nodeNum: Int?     // 節點序號（NODE_ATT==2 時帶入）

    // ③ 側溝屬性（必填）
    @SerializedName("NODE_TYP")    val nodeTyp: Int,     // 1=U型溝(明溝)、2=U型溝(加蓋)...
    @SerializedName("MAT_TYP")     val matTyp: Int?,     // 1=混凝土、2=卵礫石、3=紅磚

    // ④ 位置信息（必填）
    @SerializedName("latitude")    val latitude: Double,
    @SerializedName("longitude")   val longitude: Double,
    @SerializedName("NODE_LE")     val nodeLe: Double?   // 高程（Z值），目前固定 null

    // ⑤ 測量座標（必填）
    @SerializedName("XY_NUM")      val xyNum: String,

    // ⑥ 狀態標誌（可選，但通常都要）
    @SerializedName("IS_CANTOPEN") val isCantOpen: Boolean,  // 無法開蓋
    @SerializedName("IS_BROKEN")   val isBroken: Int?       // 破損（0/1/null）
    @SerializedName("IS_HANGING")  val isHanging: Int?      // 懸掛（0/1/null）
    @SerializedName("IS_SILT")     val isSilt: Int?         // 淤積（0=無、1=輕、2=中、3=嚴重）

    // ⑦ 尺寸測量（可選）
    @SerializedName("NODE_DEP")    val nodeDep: Int?,    // 深度（公分）
    @SerializedName("NODE_WID")    val nodeWid: Int?,    // 寬度（公分）

    // ⑧ 備註（可選）
    @SerializedName("NODE_NOTE")   val nodeNote: String? = null
)
```

---

## 📋 JSON 範例

### 新增側溝（包含起點、節點、終點）

```json
{
  "spiNum": null,
  "nodes": [
    {
      "node_id": null,
      "NODE_ATT": 1,
      "NODE_NUM": null,
      "NODE_TYP": 1,
      "MAT_TYP": 1,
      "latitude": 25.033333,
      "longitude": 121.466667,
      "NODE_LE": null,
      "XY_NUM": "2024001",
      "IS_CANTOPEN": false,
      "NODE_DEP": 50,
      "NODE_WID": 100,
      "IS_BROKEN": 0,
      "IS_HANGING": 0,
      "IS_SILT": 0,
      "NODE_NOTE": "起點正常"
    },
    {
      "node_id": null,
      "NODE_ATT": 2,
      "NODE_NUM": 1,
      "NODE_TYP": 1,
      "MAT_TYP": 2,
      "latitude": 25.034444,
      "longitude": 121.467778,
      "NODE_LE": null,
      "XY_NUM": "2024002",
      "IS_CANTOPEN": true,
      "NODE_DEP": null,
      "NODE_WID": null,
      "IS_BROKEN": null,
      "IS_HANGING": null,
      "IS_SILT": null,
      "NODE_NOTE": "無法開蓋"
    },
    {
      "node_id": null,
      "NODE_ATT": 3,
      "NODE_NUM": null,
      "NODE_TYP": 2,
      "MAT_TYP": 1,
      "latitude": 25.035555,
      "longitude": 121.468889,
      "NODE_LE": null,
      "XY_NUM": "2024003",
      "IS_CANTOPEN": false,
      "NODE_DEP": 45,
      "NODE_WID": 100,
      "IS_BROKEN": 1,
      "IS_HANGING": 0,
      "IS_SILT": 2,
      "NODE_NOTE": "終點有破損"
    }
  ]
}
```

### 更新側溝（帶入 SPI_NUM 和 node_id）

```json
{
  "spiNum": "SPI2024001",
  "nodes": [
    {
      "node_id": 101,
      "NODE_ATT": 1,
      "NODE_NUM": null,
      "NODE_TYP": 1,
      "MAT_TYP": 1,
      "latitude": 25.033333,
      "longitude": 121.466667,
      "NODE_LE": null,
      "XY_NUM": "2024001",
      "IS_CANTOPEN": false,
      "NODE_DEP": 50,
      "NODE_WID": 100,
      "IS_BROKEN": 0,
      "IS_HANGING": 0,
      "IS_SILT": 0,
      "NODE_NOTE": "更新：狀態正常"
    },
    {
      "node_id": 102,
      "NODE_ATT": 3,
      "NODE_NUM": null,
      "NODE_TYP": 2,
      "MAT_TYP": 1,
      "latitude": 25.035555,
      "longitude": 121.468889,
      "NODE_LE": null,
      "XY_NUM": "2024003",
      "IS_CANTOPEN": false,
      "NODE_DEP": 45,
      "NODE_WID": 100,
      "IS_BROKEN": 1,
      "IS_HANGING": 0,
      "IS_SILT": 2,
      "NODE_NOTE": "更新：破損變嚴重"
    }
  ]
}
```

---

## 🔧 buildStoreDitchRequest() 轉換邏輯

### 位置：AddGutterBottomSheet.kt 第 923-965 行

```kotlin
private fun buildStoreDitchRequest(
    waypoints: List<Waypoint>,
    spiNum: String? = null
): StoreDitchRequest {
    var nodeSequence = 1  // 節點序號計數器

    fun parseLooseBoolean(raw: String?): Boolean {
        val v = raw?.trim()?.lowercase()
        return when (v) {
            "1", "true", "t", "y", "yes" -> true
            else -> false
        }
    }

    return StoreDitchRequest(
        spiNum = spiNum,
        nodes = waypoints.map { wp ->
            // ① 根據 WaypointType 轉換 NODE_ATT
            val nodeAtt = when (wp.type) {
                WaypointType.START -> 1          // 起點
                WaypointType.NODE  -> 2          // 節點
                WaypointType.END   -> 3          // 終點
            }

            // ② 解析 IS_CANTOPEN（支援多種格式）
            val isCantOpen = parseLooseBoolean(wp.basicData["IS_CANTOPEN"])

            // ③ 構建 StoreDitchNodeRequest
            StoreDitchNodeRequest(
                // 識別
                nodeId = wp.basicData["_nodeId"]?.toIntOrNull(),

                // 屬性
                nodeAtt = nodeAtt,
                nodeNum = if (nodeAtt == 2) nodeSequence++ else null,  // 只有NODE才有序號

                // 側溝類型（預設值 1）
                nodeTyp = wp.basicData["NODE_TYP"]?.toIntOrNull() ?: 1,

                // 材質（無法開蓋時為 null，否則預設 1）
                matTyp = if (isCantOpen) null else (wp.basicData["MAT_TYP"]?.toIntOrNull() ?: 1),

                // 位置（無座標時默認 0.0）
                latitude = wp.latLng?.latitude ?: 0.0,
                longitude = wp.latLng?.longitude ?: 0.0,
                nodeLe = null,  // 高程永遠 null

                // 測量座標（必填）
                xyNum = wp.basicData["XY_NUM"] ?: "",

                // 狀態（無法開蓋時為 true）
                isCantOpen = isCantOpen,

                // 尺寸（無法開蓋時為 null，否則預設 0）
                nodeDep = if (isCantOpen) null else (wp.basicData["NODE_DEP"]?.toIntOrNull() ?: 0),
                nodeWid = if (isCantOpen) null else (wp.basicData["NODE_WID"]?.toIntOrNull() ?: 0),

                // 其他狀態（無法開蓋時為 null）
                isBroken = if (isCantOpen) null else (wp.basicData["IS_BROKEN"]?.toIntOrNull() ?: 0),
                isHanging = if (isCantOpen) null else (wp.basicData["IS_HANGING"]?.toIntOrNull() ?: 0),
                isSilt = if (isCantOpen) null else (wp.basicData["IS_SILT"]?.toIntOrNull() ?: 0),

                // 備註（空字串則為 null）
                nodeNote = wp.basicData["NODE_NOTE"]?.takeIf { it.isNotEmpty() }
            )
        }
    )
}
```

---

## 📊 數據來源對應表

| Request 字段 | 來自 Waypoint | 說明 |
|-------------|-------------|------|
| `node_id` | `basicData["_nodeId"]` | 更新時才有（API 回傳） |
| `NODE_ATT` | `type` (enum) | START→1, NODE→2, END→3 |
| `NODE_NUM` | 內部計數 | 只有 NODE 類型才有序號 |
| `NODE_TYP` | `basicData["NODE_TYP"]` | 預設 1（U型溝明溝） |
| `MAT_TYP` | `basicData["MAT_TYP"]` | 無法開蓋時為 null，否則預設 1 |
| `latitude` | `latLng?.latitude` | 無座標時預設 0.0 |
| `longitude` | `latLng?.longitude` | 無座標時預設 0.0 |
| `NODE_LE` | 固定值 | 永遠 `null`（高程） |
| `XY_NUM` | `basicData["XY_NUM"]` | 測量座標編號（必填） |
| `IS_CANTOPEN` | `basicData["IS_CANTOPEN"]` | 解析為 Boolean |
| `NODE_DEP` | `basicData["NODE_DEP"]` | 無法開蓋時為 null，否則預設 0 |
| `NODE_WID` | `basicData["NODE_WID"]` | 無法開蓋時為 null，否則預設 0 |
| `IS_BROKEN` | `basicData["IS_BROKEN"]` | 無法開蓋時為 null，否則預設 0 |
| `IS_HANGING` | `basicData["IS_HANGING"]` | 無法開蓋時為 null，否則預設 0 |
| `IS_SILT` | `basicData["IS_SILT"]` | 無法開蓋時為 null，否則預設 0 |
| `NODE_NOTE` | `basicData["NODE_NOTE"]` | 空字串轉 null |

---

## 🎯 調用位置

### 新增側溝流程

```kotlin
// AddGutterBottomSheet.kt 第 647 行
binding.btnSubmitGutter.setOnClickListener {
    // ... 驗證邏輯 ...
    val validWaypoints = waypoints.filter { /* 驗證 */ }

    // 構建 request（spiNum=null 表示新增）
    val request = buildStoreDitchRequest(validWaypoints, null)

    // 調用 API
    when (val result = repository.storeDitch(request, token)) {
        is ApiResult.Success -> { /* 成功 */ }
        is ApiResult.Error -> { /* 失敗 */ }
    }
}
```

### 更新側溝流程

```kotlin
// AddGutterBottomSheet.kt 第 708 行
binding.btnSubmitGutter.setOnClickListener {
    // ... 驗證邏輯 ...

    // 構建 request（spiNum != null 表示更新）
    val request = buildStoreDitchRequest(waypoints.toList(), editSpiNum)

    // 調用 API
    when (val result = repository.storeDitch(request, token)) {
        is ApiResult.Success -> { /* 成功 */ }
        is ApiResult.Error -> { /* 失敗 */ }
    }
}
```

---

## ⚠️ 關鍵特性

### 1. **無法開蓋（IS_CANTOPEN=true）時的特殊處理**

當 `IS_CANTOPEN=true` 時，以下字段**會被強制設為 null**：
- `matTyp` → null（材質不適用）
- `nodeDep` → null（深度不適用）
- `nodeWid` → null（寬度不適用）
- `isBroken` → null（破損狀態不適用）
- `isHanging` → null（懸掛狀態不適用）
- `isSilt` → null（淤積狀態不適用）

### 2. **NODE_ATT 與 NODE_NUM 的關係**

| NODE_ATT | NODE_NUM | 說明 |
|----------|----------|------|
| 1（起點） | null | 起點不需序號 |
| 2（節點） | 1, 2, 3... | 自動遞增序號 |
| 3（終點） | null | 終點不需序號 |

### 3. **預設值策略**

| 字段 | 預設值 | 條件 |
|------|--------|------|
| `nodeTyp` | 1 | 未提供時 |
| `matTyp` | 1 | 未提供且不是無法開蓋 |
| `nodeDep` | 0 | 未提供且不是無法開蓋 |
| `nodeWid` | 0 | 未提供且不是無法開蓋 |
| `latitude` | 0.0 | 無座標時 |
| `longitude` | 0.0 | 無座標時 |
| `xyNum` | "" | 未提供時 |

---

## 🔗 相關檔案

- **API 模型定義**: `api/GutterApiModels.kt` (第 378-415 行)
- **請求構建函數**: `gutter/AddGutterBottomSheet.kt` (第 923-965 行)
- **API 服務**: `api/GutterApiService.kt`
- **調用位置**: `gutter/AddGutterBottomSheet.kt` (第 647、708 行)

---

**最後更新**: 2026-04-01
