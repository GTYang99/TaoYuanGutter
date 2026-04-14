# 桃園側溝 - Android GutterForm 應用程式

這是基於 Android Kotlin 開發的側溝管理系統，旨在提供現場巡查人員一個高效、直覺的工具，用於收集側溝基本資訊、拍攝現場照片，並透過地圖進行精確的位置標記與管理。

---

## 🏗️ 專案架構 (Project Architecture)

本專案採用 **Repository 模式** 與 **MVVM 近似架構**，並結合 **Google Maps SDK** 進行空間資料處理。

### 1. 核心模組架構
- **UI 層 (MainActivity, GutterFormActivity)**: 處理地圖呈現、表單導航及用戶交互。
- **表單層 (gutter/ 模組)**: 包含 `ViewPager2` 分頁表單 (`GutterBasicInfoFragment` & `GutterPhotosFragment`)。
- **倉儲層 (api/ & offline/ & pending/)**:
    - `GutterRepository`: 處理遠端 API 通訊。
    - `OfflineDraftRepository`: 處理本地 SQLite (Room) 離線存檔。
    - `GutterSessionRepository`: 管理當前會話的臨時草稿（不持久化）。
- **API 層 (api/)**: 使用 `Retrofit` + `OkHttp` + `Gson` 進行 JSON 資料交換。

### 2. 主要檔案職責
- `MainActivity.kt`: 應用的入口，管理地圖視圖、位置追蹤、以及點位標記的渲染。
- `GutterFormActivity.kt`: 表單容器，負責驗證資料並呼叫 API 提交。
- `AddGutterBottomSheet.kt`: 地圖上的浮動表單，用於新增/編輯路點 (Waypoint)。
- `GutterInspectActivity.kt`: 查看已提交側溝的詳細資訊，並支援進入編輯模式。

---

## 📊 數據結構 (Data Structure)

### 1. UI 數據模型: `Waypoint`
路點是地圖與表單之間的橋樑數據結構：
```kotlin
data class Waypoint(
    var id: Long = 0,
    var nodeId: String = "",      // API 返回的識別 ID
    var index: Int = 0,           // 序號
    var name: String = "",        // 標記名稱
    var latLng: LatLng,           // 經緯度座標
    var waypointType: WaypointType, // START(起點), NODE(節點), END(終點)
    var basicData: Map<String, String> = emptyMap(), // 存儲表單欄位 (如 NODE_TYP, MAT_TYP)
    var photoUris: List<Uri> = emptyList()           // 照片路徑
)
```

### 2. 側溝基本資料 (`basicData`) 關鍵欄位
- `XY_NUM`: 測量座標編號 (必填)
- `NODE_TYP`: 側溝類型 (1=U型溝明溝, 2=U型溝加蓋...)
- `MAT_TYP`: 材質 (1=混凝土, 2=卵礫石...)
- `IS_CANTOPEN`: 無法開蓋旗標 (Boolean 字符串)
- `IS_BROKEN`, `IS_HANGING`, `IS_SILT`: 狀態旗標 (0=無, 1=有/輕微...)

---

## 🛠️ 核心功能 (Functionalities)

- **地圖管理**: 支援 EMAP/PHOTO2 切換、WMS/WMTS 圖層疊加、以及路徑 (Polyline) 繪製。
- **分頁表單**:
    - **第 0 頁 (基本資料)**: 收集側溝規格與損壞狀態。
    - **第 1 頁 (現場照片)**: 支援拍攝最多 3 張照片，並進行持久化 URI 管理。
- **編輯模式 (Edit Mode)**:
    - 進入編輯時自動隱藏「側溝 ID」欄位。
    - 自動帶入 API 返回的 `XY_NUM` 資料。
- **驗證機制**: 提交前嚴格檢查必填欄位與照片數量 (需拍攝 3 張)。
- **草稿管理**: 支援手動/自動保存草稿，並能從列表快速恢復編輯。

---

## 📤 數據上傳流程 (Data & Upload Workflow)

數據上傳分為兩個階段：**結構化數據上傳** 與 **照片上傳**。

### 階段 1: 結構化數據上傳 (`storeDitch`)
1. **觸發**: 用戶在 `AddGutterBottomSheet` 點擊「提交」。
2. **轉換**: 呼叫 `buildStoreDitchRequest()` 將 `List<Waypoint>` 轉換為 `StoreDitchRequest` (JSON)。
    - 若為**無法開蓋** (`IS_CANTOPEN=true`)，自動將深度、寬度等欄位設為 `null`。
3. **API 調用**: `POST /api/v1/ditch/storeDitch`
4. **回應處理**: 
    - 成功時取得 `spiNum` (側溝編號) 及各節點的 `nodeId`。
    - 將 `nodeId` 保存回 `Waypoint.basicData["_nodeId"]` 以供未來編輯使用。

### 階段 2: 照片上傳 (`uploadPhotos`)
1. **觸發**: 數據上傳成功後，自動檢查 `Waypoint` 中的 `photoUris`。
2. **處理**: 將本地 `Uri` 轉換為 `MultipartBody.Part`。
3. **API 調用**: `POST /api/v1/ditch/uploadPhotos`
4. **鏈結**: 照片會透過 `nodeId` 或 `spiNum` 與側溝資料進行鏈結。

---

## 📸 照片管理細節
- **儲存**: 使用 `FileProvider` 取得持久化 `Uri`，避免 Android 11+ 的權限問題。
- **顯示**: 採用 `Glide` 庫優化圖片載入性能。
- **驗證**: `FragmentGutterPhotos.validateAllPhotos()` 確保上傳前三張照片皆已就緒。

---

## 🚀 開發者快速入門

### 環境需求
- Min SDK: 26
- ViewBinding: Enabled
- Google Maps API Key: 需設定於 `local.properties` 或 `secrets.properties`