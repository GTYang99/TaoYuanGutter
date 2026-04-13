# StoreDitch API 完整回應格式

## 📥 API 端點

```
POST /api/v1/ditch/storeDitch
```

---

## ✅ 成功回應（200 OK）

### 新增側溝成功

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

### 更新側溝成功

```json
{
  "success": true,
  "message": "修改成功",
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
        "nodeDep": 55,
        "nodeWid": 100,
        "isBroken": 0,
        "isHanging": 0,
        "isSilt": 0,
        "nodeNote": "更新：深度改為55"
      }
    ]
  },
  "errors": null
}
```

---

## ❌ 失敗回應

### 驗證失敗（422 Unprocessable Entity）

```json
{
  "success": false,
  "message": "驗證失敗",
  "data": null,
  "errors": {
    "nodes.0.NODE_TYP": ["NODE_TYP 為必填欄位"],
    "nodes.0.NODE_DEP": ["NODE_DEP 必須為正整數"],
    "nodes.1.XY_NUM": ["XY_NUM 已存在"]
  }
}
```

### 認證失敗（401 Unauthorized）

```json
{
  "success": false,
  "message": "Token 過期或無效",
  "data": null,
  "errors": null
}
```

### 伺服器錯誤（500 Internal Server Error）

```json
{
  "success": false,
  "message": "伺服器內部錯誤",
  "data": null,
  "errors": null
}
```

---

## 📦 回應數據類

### StoreDitchResponse

```kotlin
data class StoreDitchResponse(
    @SerializedName("success")  val success: Boolean,
    @SerializedName("message")  val message: String?,
    @SerializedName("data")     val data: DitchDetails?,        // 包含完整側溝資訊
    @SerializedName("errors")   val errors: Map<String, List<String>>?
)
```

### DitchDetails（data 部分）

```kotlin
data class DitchDetails(
    @SerializedName("spiNum")   val spiNum: String,             // 側溝編號
    @SerializedName("nodes")    val nodes: List<DitchNode>      // 節點列表
)
```

### DitchNode（nodes 中的每一個元素）

```kotlin
data class DitchNode(
    @SerializedName("nodeId")    val nodeId: Int,               // ← ★ 重要：用於編輯時識別
    @SerializedName("nodeAtt")   val nodeAtt: Int,              // 1=起點、2=節點、3=終點
    @SerializedName("nodeNum")   val nodeNum: Int?,
    @SerializedName("nodeTyp")   val nodeTyp: Int,
    @SerializedName("matTyp")    val matTyp: Int?,
    @SerializedName("latitude")  val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("nodeLE")    val nodeLE: Double?,
    @SerializedName("xyNum")     val xyNum: String,
    @SerializedName("isCantOpen") val isCantOpen: Boolean,
    @SerializedName("nodeDep")   val nodeDep: Int?,
    @SerializedName("nodeWid")   val nodeWid: Int?,
    @SerializedName("isBroken")  val isBroken: Int?,
    @SerializedName("isHanging") val isHanging: Int?,
    @SerializedName("isSilt")    val isSilt: Int?,
    @SerializedName("nodeNote")  val nodeNote: String?
)
```

---

## 🔄 API 調用和回應處理流程

### 調用位置：AddGutterBottomSheet.kt

```kotlin
// 第 647 行（新增模式）
val request = buildStoreDitchRequest(validWaypoints, null)
android.util.Log.d("StoreDitch", "add request=$request")
when (val result = repository.storeDitch(request, token)) {
    is ApiResult.Success -> {
        val response = result.data  // StoreDitchResponse
        val spiNum = response.data?.spiNum  // 側溝編號
        val nodes = response.data?.nodes    // 節點列表

        // 將 DitchNode 轉換回 Waypoint（含新的 node_id）
        val updatedWaypoints = response.data?.nodes?.mapIndexed { idx, node ->
            val wp = validWaypoints[idx]
            wp.basicData["_nodeId"] = node.nodeId.toString()  // ★ 保存 node_id
            wp
        } ?: emptyList()

        // 回調 MainActivity
        val host = requireActivity() as? LocationPickerHost
        host?.onGutterSaved(spiNum, updatedWaypoints, nodes)
    }
    is ApiResult.Error -> {
        // 失敗時保存為待上傳草稿
        val host = requireActivity() as? LocationPickerHost
        host?.onGutterSaveFailed(validWaypoints)
        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
    }
}

// 第 708 行（更新模式）
val request = buildStoreDitchRequest(waypoints.toList(), editSpiNum)
when (val result = repository.storeDitch(request, token)) {
    is ApiResult.Success -> {
        val response = result.data
        val spiNum = response.data?.spiNum ?: editSpiNum

        val updatedWaypoints = response.data?.nodes?.mapIndexed { idx, node ->
            val wp = waypoints[idx]
            wp.basicData["_nodeId"] = node.nodeId.toString()  // ★ 更新 node_id
            wp
        } ?: waypoints.toList()

        val host = requireActivity() as? LocationPickerHost
        host?.onUpdateGutter(updatedWaypoints, spiNum)
        dismiss()
    }
}
```

---

## 💾 node_id 的使用流程

### 新增 → 編輯的關鍵數據流

```
1️⃣ 首次新增側溝
   POST /api/v1/ditch/storeDitch
   └─→ Request: nodeId=null（新增）
   └─→ Response: nodeId=1001, 1002, 1003（後端分配）

2️⃣ 保存 node_id 到本地
   wp.basicData["_nodeId"] = "1001"  // ★ 用下劃線開頭避免衝突

3️⃣ 編輯現有側溝
   POST /api/v1/ditch/storeDitch
   └─→ Request: nodeId=1001（帶入識別）
       {
         "spiNum": "SPI20240401001",
         "nodes": [
           {
             "node_id": 1001,  // ← 帶入以識別是哪個節點
             "NODE_ATT": 1,
             ...
           }
         ]
       }
   └─→ Response: 更新成功，data 中仍包含 nodeId=1001

4️⃣ 再次編輯時
   └─→ 使用之前保存的 "1001" 繼續編輯
```

---

## 📊 回應處理流程圖

```
storeDitch API 呼叫
        │
        ├─→ 200 OK, success=true
        │   ├─→ 解析 DitchDetails
        │   ├─→ 提取 spiNum（側溝編號）
        │   ├─→ 迭代 nodes，提取 nodeId
        │   │   └─→ wp.basicData["_nodeId"] = nodeId.toString()
        │   ├─→ 回調 MainActivity 刷新地圖
        │   └─→ dismiss() 關閉底部表單
        │
        ├─→ 200 OK, success=false（驗證失敗）
        │   ├─→ 解析 errors Map
        │   ├─→ 以 Toast 顯示第一條錯誤
        │   └─→ 留在表單不關閉
        │
        ├─→ 401 Unauthorized
        │   ├─→ Token 失效
        │   ├─→ Toast: "請先登入"
        │   └─→ 跳轉登入頁面
        │
        └─→ 其他（500 等）
            ├─→ 顯示通用錯誤訊息
            └─→ 保存為待上傳草稿（新增模式）
```

---

## 🎯 完整數據對應表

| Response 字段 | Waypoint 對應 | 用途 |
|-------------|-------------|------|
| `spiNum` | - | 返回側溝編號 |
| `nodeId` | `basicData["_nodeId"]` | 編輯時識別該節點 |
| `nodeAtt` | `type` (enum) | 1→START, 2→NODE, 3→END |
| `latitude` | `latLng.latitude` | 點位座標 |
| `longitude` | `latLng.longitude` | 點位座標 |
| 其他字段 | `basicData[...]` | 原值返回 |

---

## ⚡ 關鍵要點

### 1. **node_id 是編輯的關鍵**
   - 新增時：不帶 node_id（null）
   - 編輯時：必須帶入 node_id
   - API 回傳的 node_id 要保存在 `basicData["_nodeId"]`

### 2. **成功與失敗的判斷**
   ```kotlin
   if (response.success && response.data != null) {
       // 成功
   } else {
       // 失敗（即使 HTTP 200 也可能 success=false）
   }
   ```

### 3. **錯誤訊息的提取**
   ```kotlin
   // 優先級：errors Map > message 字段
   val errorMsg = response.errors?.values?.flatten()?.firstOrNull()
                  ?: response.message
                  ?: "未知錯誤"
   ```

### 4. **node_id 的生命週期**
   ```
   API 建立 → 返回 node_id → 保存本地 → 下次編輯帶入 → 更新
   ```

---

## 📚 相關檔案

- **回應模型定義**: `api/GutterApiModels.kt` (第 421-426 行)
- **DitchDetails / DitchNode**: `api/GutterApiModels.kt`
- **API 服務定義**: `api/GutterApiService.kt`
- **Repository 調用**: `api/GutterRepository.kt`
- **使用位置**: `gutter/AddGutterBottomSheet.kt` (第 647、708 行)

---

**最後更新**: 2026-04-01
