# 完整 API 調用流程示例

## 📖 場景：新增一條側溝（3個點位）

---

## 1️⃣ 準備階段：構建 Request

### Waypoint 數據結構（本地）

```kotlin
val waypoints = listOf(
    // 起點
    Waypoint(
        type = WaypointType.START,
        label = "起點",
        latLng = LatLng(25.033333, 121.466667),
        basicData = hashMapOf(
            "NODE_TYP" to "1",        // U型溝（明溝）
            "MAT_TYP" to "1",         // 混凝土
            "NODE_DEP" to "50",       // 深度 50cm
            "NODE_WID" to "100",      // 寬度 100cm
            "IS_BROKEN" to "0",       // 無破損
            "IS_HANGING" to "0",      // 無懸掛
            "IS_SILT" to "0",         // 無淤積
            "IS_CANTOPEN" to "false", // 可開蓋
            "XY_NUM" to "2024001",    // 測量座標編號
            "NODE_NOTE" to "起點正常"
        )
    ),
    // 中間節點
    Waypoint(
        type = WaypointType.NODE,
        label = "節點1",
        latLng = LatLng(25.034444, 121.467778),
        basicData = hashMapOf(
            "NODE_TYP" to "1",
            "MAT_TYP" to "2",         // 卵礫石
            "NODE_DEP" to "null",
            "NODE_WID" to "null",
            "IS_BROKEN" to "null",
            "IS_HANGING" to "null",
            "IS_SILT" to "null",
            "IS_CANTOPEN" to "true",  // 無法開蓋！
            "XY_NUM" to "2024002",
            "NODE_NOTE" to "無法開蓋"
        )
    ),
    // 終點
    Waypoint(
        type = WaypointType.END,
        label = "終點",
        latLng = LatLng(25.035555, 121.468889),
        basicData = hashMapOf(
            "NODE_TYP" to "2",        // U型溝（加蓋）
            "MAT_TYP" to "1",         // 混凝土
            "NODE_DEP" to "45",
            "NODE_WID" to "100",
            "IS_BROKEN" to "1",       // 有破損
            "IS_HANGING" to "0",
            "IS_SILT" to "2",         // 淤積中度
            "IS_CANTOPEN" to "false",
            "XY_NUM" to "2024003",
            "NODE_NOTE" to "終點有破損"
        )
    )
)
```

### buildStoreDitchRequest() 轉換過程

```kotlin
// 入口：新增模式
val request = buildStoreDitchRequest(waypoints, spiNum = null)

// 內部轉換邏輯
var nodeSequence = 1
val storageRequest = StoreDitchRequest(
    spiNum = null,  // ← 新增模式為 null
    nodes = waypoints.map { wp ->
        // 起點轉換
        when (wp.type) {
            WaypointType.START -> {
                nodeAtt = 1
                nodeNum = null  // 起點不需序號
            }
            WaypointType.NODE -> {
                nodeAtt = 2
                nodeNum = nodeSequence++  // 1
            }
            WaypointType.END -> {
                nodeAtt = 3
                nodeNum = null  // 終點不需序號
            }
        }

        // IS_CANTOPEN 解析
        isCantOpen = parseLooseBoolean("false") → false  // 起點可開蓋
        isCantOpen = parseLooseBoolean("true") → true    // 節點無法開蓋
        isCantOpen = parseLooseBoolean("false") → false  // 終點可開蓋

        // 構建 StoreDitchNodeRequest...
    }
)
```

---

## 2️⃣ 發送 HTTP 請求

### 完整 Request JSON

```json
POST /api/v1/ditch/storeDitch
Content-Type: application/json
Authorization: Bearer {token}

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

### API 接收步驟

```
Retrofit 序列化 (Gson)
    ↓
JSON 字符串
    ↓
POST /api/v1/ditch/storeDitch
    ↓
後端接收並驗證
```

---

## 3️⃣ 後端處理

### 驗證邏輯

```
✓ spiNum = null → 判定為「新增」模式
✓ nodes.length = 3 → 3 個節點
  ✓ nodes[0].NODE_ATT = 1 → 起點
    ✓ latitude/longitude 有效
    ✓ NODE_TYP = 1 有效
    ✓ MAT_TYP = 1 有效
    ✓ NODE_DEP = 50 有效
    ✓ IS_BROKEN = 0 有效
  ✓ nodes[1].NODE_ATT = 2 → 節點
    ✓ 因為 IS_CANTOPEN=true，所以 MAT_TYP/NODE_DEP 等為 null ✓
    ✓ NODE_NUM = 1 有效（表示第 1 個中間節點）
  ✓ nodes[2].NODE_ATT = 3 → 終點
    ✓ 所有驗證通過 ✓

✓ 所有驗證成功，開始存儲
```

### 分配 node_id

```
INSERT INTO nodes ...
  node_id = AUTO_INCREMENT
  └─→ 1001 (起點)
  └─→ 1002 (節點)
  └─→ 1003 (終點)

INSERT INTO ditches ...
  spiNum = AUTO_GENERATE
  └─→ "SPI20240401001"
```

---

## 4️⃣ 後端返回 Response

### 成功回應（200 OK）

```json
{
  "success": true,
  "message": "新增成功",
  "data": {
    "spiNum": "SPI20240401001",
    "nodes": [
      {
        "nodeId": 1001,
        "nodeAtt": 1,
        "nodeNum": null,
        "nodeTyp": 1,
        "matTyp": 1,
        "latitude": 25.033333,
        "longitude": 121.466667,
        "nodeLE": null,
        "xyNum": "2024001",
        "isCantOpen": false,
        "nodeDep": 50,
        "nodeWid": 100,
        "isBroken": 0,
        "isHanging": 0,
        "isSilt": 0,
        "nodeNote": "起點正常"
      },
      {
        "nodeId": 1002,
        "nodeAtt": 2,
        "nodeNum": 1,
        "nodeTyp": 1,
        "matTyp": 2,
        "latitude": 25.034444,
        "longitude": 121.467778,
        "nodeLE": null,
        "xyNum": "2024002",
        "isCantOpen": true,
        "nodeDep": null,
        "nodeWid": null,
        "isBroken": null,
        "isHanging": null,
        "isSilt": null,
        "nodeNote": "無法開蓋"
      },
      {
        "nodeId": 1003,
        "nodeAtt": 3,
        "nodeNum": null,
        "nodeTyp": 2,
        "matTyp": 1,
        "latitude": 25.035555,
        "longitude": 121.468889,
        "nodeLE": null,
        "xyNum": "2024003",
        "isCantOpen": false,
        "nodeDep": 45,
        "nodeWid": 100,
        "isBroken": 1,
        "isHanging": 0,
        "isSilt": 2,
        "nodeNote": "終點有破損"
      }
    ]
  },
  "errors": null
}
```

---

## 5️⃣ 前端處理 Response

### 解析步驟

```kotlin
// AddGutterBottomSheet.kt 第 647-670 行
when (val result = repository.storeDitch(request, token)) {
    is ApiResult.Success -> {
        val response = result.data  // StoreDitchResponse

        // ① 檢查成功標誌
        if (!response.success) {
            // 驗證失敗處理...
            return@setOnClickListener
        }

        // ② 提取側溝編號
        val spiNum = response.data?.spiNum
        // spiNum = "SPI20240401001"

        // ③ 提取節點列表
        val nodes = response.data?.nodes ?: emptyList()
        // nodes[0].nodeId = 1001
        // nodes[1].nodeId = 1002
        // nodes[2].nodeId = 1003

        // ④ 保存 node_id 到本地 waypoint
        val updatedWaypoints = nodes.mapIndexed { idx, node ->
            val originalWp = validWaypoints[idx]
            originalWp.basicData["_nodeId"] = node.nodeId.toString()
            // waypoints[0].basicData["_nodeId"] = "1001"
            // waypoints[1].basicData["_nodeId"] = "1002"
            // waypoints[2].basicData["_nodeId"] = "1003"
            originalWp
        }

        // ⑤ 回調 MainActivity 刷新地圖
        val host = requireActivity() as? LocationPickerHost
        host?.onGutterSaved(spiNum, updatedWaypoints, nodes)
        dismiss()
    }
}
```

### 回調流程

```
onGutterSaved()
    ↓ (MainActivity)
refreshWorkingMarkers(waypoints)
    ↓
清除舊標記
新增新標記（帶入 node_id）
    ↓
繪製 Polyline（連線）
    ↓
fitCameraToWaypoints()
```

---

## 6️⃣ 後續：編輯該側溝

### 編輯時的 Request 對比

```json
// 編輯模式請求（帶入 SPI_NUM 和 node_id）
{
  "spiNum": "SPI20240401001",     // ← 編輯時帶入
  "nodes": [
    {
      "node_id": 1001,            // ← 編輯時帶入（識別該節點）
      "NODE_ATT": 1,
      "NODE_NUM": null,
      "NODE_TYP": 1,
      "MAT_TYP": 1,
      "latitude": 25.033333,
      "longitude": 121.466667,
      "NODE_LE": null,
      "XY_NUM": "2024001",
      "IS_CANTOPEN": false,
      "NODE_DEP": 55,             // ← 修改：50 → 55
      "NODE_WID": 100,
      "IS_BROKEN": 0,
      "IS_HANGING": 0,
      "IS_SILT": 0,
      "NODE_NOTE": "更新：深度改為55"  // ← 修改備註
    },
    // ... 其他節點
  ]
}
```

---

## 🔄 完整流程圖

```
User 操作
    ↓
AddGutterBottomSheet
├─ 驗證 waypoints (validateWaypoints)
├─ 構建 Request (buildStoreDitchRequest)
│   ├─ WaypointType → NODE_ATT
│   ├─ basicData → 各欄位
│   └─ isCantOpen → 特殊邏輯
├─ API 調用 (repository.storeDitch)
│   └─ Retrofit POST
│       └─ /api/v1/ditch/storeDitch
└─ 處理 Response
    ├─ Gson 反序列化
    ├─ StoreDitchResponse 檢查
    ├─ 提取 spiNum 和 nodeId
    ├─ 保存 node_id 到 basicData["_nodeId"]
    └─ onGutterSaved() 回調
        └─ MainActivity
            ├─ 刷新地圖標記
            ├─ 繪製 Polyline
            └─ 調整鏡頭
```

---

## ⚡ 關鍵數據轉換清單

| 階段 | 轉換內容 | 檢查點 |
|------|--------|--------|
| 本地構建 | Waypoint → StoreDitchNodeRequest | ✓ 無法開蓋時 null 化特定字段 |
| 序列化 | StoreDitchNodeRequest → JSON | ✓ @SerializedName 正確 |
| HTTP 傳輸 | JSON → 後端 | ✓ Content-Type: application/json |
| 後端驗證 | JSON → 業務規則驗證 | ✓ 所有必填欄位檢查 |
| 後端存儲 | 驗證通過 → 分配 ID | ✓ node_id, spiNum 生成 |
| API 回應 | DitchDetails → JSON | ✓ 包含新分配的 ID |
| 反序列化 | JSON → StoreDitchResponse | ✓ @SerializedName 對應 |
| 前端處理 | StoreDitchResponse → Waypoint | ✓ node_id 保存至 basicData["_nodeId"] |
| 地圖更新 | Waypoint → 地圖標記和連線 | ✓ 刷新視圖 |

---

## 🎯 常見錯誤和檢查

### ❌ 錯誤 1：IS_CANTOPEN=true 卻帶入其他字段

```json
// 錯誤示例
{
  "IS_CANTOPEN": true,
  "NODE_DEP": 50,     // ✗ 應該是 null
  "NODE_WID": 100,    // ✗ 應該是 null
  "MAT_TYP": 1        // ✗ 應該是 null
}

// 正確示例
{
  "IS_CANTOPEN": true,
  "NODE_DEP": null,
  "NODE_WID": null,
  "MAT_TYP": null
}
```

### ❌ 錯誤 2：編輯時忘記帶 node_id

```json
// 錯誤：直接修改第一個節點，不帶 node_id
{
  "spiNum": "SPI20240401001",
  "nodes": [
    {
      "node_id": null,    // ✗ 應該是 1001
      "NODE_DEP": 55,
      ...
    }
  ]
}

// 後端可能無法識別是哪個節點
```

### ✅ 正確做法

```kotlin
// 編輯時必須帶 node_id
wp.basicData["_nodeId"]?.toIntOrNull()
// 如果返回 null，代表尚未同步到後端（離線狀態）
```

---

**完整示例版本**: 1.0
**最後更新**: 2026-04-01
