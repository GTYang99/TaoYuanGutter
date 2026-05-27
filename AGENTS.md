# AGENTS.md - 側溝管理系統 (TaoYuanGutter) 開發指南

**適用代理（AI Agents）**：適用於所有後續的代碼修改、維護和擴展任務。
**最後更新**：2026-05-27 | **維護者**：Development Team

---

## 🎯 專案概述

**TaoYuanGutter** 是一個 Android Kotlin 應用，用於現場巡查人員收集側溝（下水道）檢測資料。核心工作流程：地圖標記位置 → 分頁表單填寫 → 拍攝照片 → 上傳數據與圖片。

**關鍵技術棧**：
- Google Maps SDK (位置標記、路線繪製)
- CameraX (現代相機API，自動方向管理)
- Retrofit + OkHttp + Gson (API 通訊)
- Room SQLite (離線草稿存儲)
- ViewPager2 (分頁表單)
- ViewBinding (UI 聲明式綁定)

---

## 🏗️ 架構關鍵概念

### 1. 數據流向：Waypoint 橋接地圖與表單
```
地圖點擊 → Waypoint(經緯度) → 表單填寫 → basicData(側溝規格) 
     ↓                                           ↓
 AddGutterBottomSheet ← 用戶交互 → GutterFormActivity 
     ↓
submitButton → GutterRepository.storeDitch() → API → nodeId 存回 Waypoint
```

**核心數據結構** (`Waypoint.kt`):
```kotlin
data class Waypoint(
    var id: Long = 0,
    var nodeId: String = "",           // API 返回的節點 ID
    var index: Int = 0,
    var latLng: LatLng,                // 經緯度座標
    var waypointType: WaypointType,    // START/NODE/END
    var basicData: Map<String, String>, // 側溝資料（NODE_TYP, MAT_TYP 等）
    var photoUris: List<Uri> = emptyList()
)
```

### 2. 主要組件職責
| 組件 | 職責 | 關鍵文件 |
|------|------|---------|
| **MainActivity** | 地圖入口、位置追蹤、路線繪製、測距模式、scope 檢視 | `MainActivity.kt` |
| **GutterFormActivity** | 新增/編輯表單容器、數據驗證、API 提交 | `GutterFormActivity.kt` |
| **GutterInspectActivity** | 唯讀檢視既有側溝詳細資料 | `GutterInspectActivity.kt` |
| **AddGutterBottomSheet** | 浮動表單面板、路點新增/編輯 | `ui/AddGutterBottomSheet.kt` |
| **CameraOverlayFragment** | 智慧橫向相機 Overlay、自動方向校正 | `gutter/CameraOverlayFragment.kt` |
| **DistanceMeasureManager** | 測距虛線、起點標記、距離計算 | `map/DistanceMeasureManager.kt` |
| **GutterRepository** | 遠端 API 通訊（submitGutter, storeDitch, 查詢等） | `api/GutterRepository.kt` |
| **GutterSessionRepository** | 待上傳側溝草稿持久化（Room + SharedPreferences 遷移） | `pending/GutterSessionRepository.kt` |

### 3. 測距模式的粗度配置
`DistanceMeasureManager.kt` 支持動態粗度調整：

**自動調整（根據距離）**：
- < 50m：6f（細線）
- 50-200m：8f（標準線）  
- 200-500m：12f（粗線）
- > 500m：16f（很粗線）

**手動調整**：
```kotlin
// 在 MainActivity 中
distanceMeasureManager?.setLineWidth(10f)  // 手動設定粗度
```

**配置預設值**：
```kotlin
val config = MeasureConfig(lineWidth = 12f)  // 預設粗度
```

---

## 🔄 主要工作流程

### 🗺️ 地圖側溝標記流程
1. **進入應用** → MainActivity 初始化地圖 + 位置追蹤
2. **點擊 "新增側溝"** → AddGutterBottomSheet 彈出
3. **地圖點擊選點** → 生成 Waypoint(經緯度)
4. **填寫表單** → 存儲 basicData (NODE_TYP, MAT_TYP 等)
5. **拍攝照片** → 存儲 photoUris (最多 3 張)
6. **提交** → GutterFormActivity 驗證 → GutterRepository.submitGutter() 上傳

### 🔍 檢視既有側溝流程（Scope 檢視）
1. **Scope 檢視激活** → 地圖請求 API `getScopeSearch()` 取得可視區域側溝
2. **渲染線段** → `ScopeGutterPolylineController` 繪製多條側溝線段
3. **點擊側溝** → 啟動 `GutterInspectActivity`，顯示詳細資料（分頁：基本資訊 + 點位照片）
4. **編輯按鈕** → 返回 MainActivity，觸發 `AddGutterBottomSheet` 載入預設值進行編輯
5. **拍攝補充照片** → 新建 `CameraOverlayFragment` 會話、上傳至 API

### 📊 數據上傳流程（已更新）
**階段 1：結構化數據** (`submitGutter` 或 `storeDitch`)
- 新增側溝：呼叫 `GutterRepository.submitGutter()` → `POST /api/gutters`，返回 `nodeId` 清單
- 編輯側溝：呼叫 `GutterRepository.storeDitch()` → `POST /api/v1/ditch/storeDitch`，帶 `SPI_NUM` 參數
- 若 `IS_CANTOPEN=true`，自動清除深度/寬度欄位

**階段 2：照片上傳** (`uploadNodeImage`)
- 逐張上傳點位照片：`POST /api/v1/node/nodeImage`（multipart/form-data）
- 傳入參數：`node_id`, `fileCategory`（1=概況、2=寬度、3=深度）、`file`

**查詢既有側溝**：
- Scope 檢視：`getScopeSearch(minLat, maxLat, minLng, maxLng)` → GeoJSON 線段清單
- 詳細資料：`getDitchDetails(spiNum)` → 單條線段的所有點位摘要 + 照片 URL
- 點位資料：`getNodeDetails(nodeId, xyNum 或座標)` → 單一點位的完整資訊

### 📝 離線草稿機制
- **自動保存**：`GutterSessionRepository` 透過 `GutterDraftDatabase` (Room ORM) 定期存儲當前會話 Waypoint
- **恢復編輯**：從 Room 讀取 draft，重新加載至地圖；支援舊版 SharedPreferences 自動遷移
- **架構**：`GutterSessionRepository`（業務邏輯層）vs `DraftDao`（持久化層）vs `GutterSessionRepository` 內存快取（待上傳臨時層）

---

## 📸 相機拍攝系統架構

### 兩種相機模式

**模式 1：Overlay 模式（推薦用於表單流程）**
- **組件**：`CameraOverlayFragment`（嵌入 GutterFormActivity）
- **優點**：不中斷表單狀態、seamless 轉場
- **限制**：Activity 保持直立，用戶必須橫放手機拍照
- **位置**：`gutter/CameraOverlayFragment.kt`

**模式 2：全屏獨立模式（備選方案）**
- **組件**：`LandscapeCameraActivity`（獨立 Activity）
- **優點**：全屏體驗、沈浸式拍照
- **缺點**：需要 Activity 切換、狀態管理複雜
- **位置**：`gutter/LandscapeCameraActivity.kt`

### 架構：拍攝視窗 vs 最終照片

**拍攝視窗（PreviewView）**：
- **位置**：`fragment_camera_overlay.xml` 第 31-35 行
- **比例**：固定 4:3（代表相機實際感光芯片比例）
- **縮放方式**：`scaleType="fitCenter"` 讓相機畫面在預覽容器內完整顯示
- **預覽容器**：父容器固定 3:4（扁寬形），相機 4:3 畫面在其中顯示
- **作用**：即時顯示給用戶看的預覽畫面

**最終照片（JPEG 文件）**：
- **保存路徑**：`/data/data/{packageName}/files/Pictures/GUTTER_{slot}_{timestamp}.jpg`
- **實際尺寸**：4:3 比例，由 CameraX 根據設備相機傳感器決定（通常 4000x3000 像素）
- **方向元數據**：CameraX 自動在 JPEG EXIF 中寫入正確的 Orientation Tag
- **保存方式**：`ImageCapture.takePicture()` 直接保存到文件，無中間轉換

**關鍵差異**：
| 面向 | 預覽窗口 | 最終照片 |
|------|--------|---------|
| **尺寸** | App UI 布局尺寸（固定容器） | 相機傳感器原始尺寸（4000x3000+） |
| **方向** | UI 顯示方向（可旋轉） | EXIF 元數據（自動計算） |
| **延遲** | 實時（<100ms） | 捕捉時一次性保存 |
| **用途** | 用戶構圖確認 | 最終存檔與上傳 |

---

### 照片上下方向確定機制

**核心流程**：
```
設備物理方向 (OrientationEventListener)
         ↓
轉換為 Surface.ROTATION_* 常量
         ↓
設置 imageCapture.targetRotation = rotation
         ↓
CameraX 自動在保存的 JPEG 寫入 EXIF Orientation
         ↓
相冊/查看器根據 EXIF 自動旋轉顯示
```

**代碼位置**：`gutter/CameraOverlayFragment.kt` 第 190-226 行

**具體實現**：
```kotlin
// 1. 監聽設備實際方向（感測器檢測）
setupOrientationListener() {
    orientationListener = object : OrientationEventListener(requireContext()) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            // orientation 範圍 0~359 度，人拿手機的物理角度
            
            // 2. 檢查是否橫放（60~120 或 240~300 範圍）
            val landscape = orientation in 60..120 || orientation in 240..300
            updateOrientationUi(landscape)  // 控制快門啟用
            
            // 3. 轉換為相機需要的 targetRotation
            val rotation = when {
                orientation in 60..120   -> Surface.ROTATION_270   // 左旋 (90° 代表左側朝上)
                orientation in 240..300  -> Surface.ROTATION_90    // 右旋 (270° 代表右側朝上)
                else                     -> Surface.ROTATION_0     // 直立（禁用拍照）
            }
            
            // 4. 設置到相機使用案例
            preview?.targetRotation = rotation
            imageCapture?.targetRotation = rotation  // ← 這決定了照片方向
        }
    }
}
```

**上下方向確定規則**：
- **landscape = false** (直立)：禁用快門；拍照時為 ROTATION_0，照片上下正常
- **orientation in 60~120** (左傾)：快門啟用；標記為 ROTATION_270（需左轉90°顯示）
- **orientation in 240~300** (右傾)：快門啟用；標記為 ROTATION_90（需右轉90°顯示）

**關鍵點**：
1. `targetRotation` **不改變照片像素**，只寫 EXIF 方向標簽
2. CameraX 保證預覽顯示方向 ≈ 最終照片方向（已考慮 targetRotation）
3. 保存後自動呼叫 `normalizeCapturedPhotoOrientation(file)` 將 EXIF 旋轉應用至像素、重設方向標簽
4. 上傳時只上傳 JPEG 文件，照片像素與 EXIF 均已正規化

**調試技巧**：
```kotlin
// 若照片方向錯誤，檢查這些位置：
// 1. OrientationEventListener 是否正確啟用 (onResume/onPause)
// 2. landscape 檢查邏輯是否正確（orientation 範圍 60~120 或 240~300）
// 3. updateOrientationUi() 是否正確啟用/禁用快門按鈕
// 4. 文件手動檢查 EXIF：adb shell exiftool GUTTER_1_xxx.jpg
```

---

## 🎥 相機權限與 Android 12+ 廣播接收器

**Android 12+ 變更**：
所有 `Context.registerReceiver()` 必須明確指定導出方式（RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED）。

**本項目做法**：
```kotlin
// MainActivity.kt 第 236-240 行
ContextCompat.registerReceiver(
    this,
    waypointLocationChangedReceiver,
    android.content.IntentFilter(LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED),
    ContextCompat.RECEIVER_NOT_EXPORTED  // ✅ 正確做法
)
```

**三種廣播接收器模式**：
| 方式 | 説明 | 使用場景 |
|------|------|---------|
| `RECEIVER_EXPORTED` | 接收其他 APP 發起的廣播 | 系統廣播（連接 WiFi、電池等） |
| `RECEIVER_NOT_EXPORTED` | 只接收本 APP 內部廣播（私有） | 應用內邏輯（位置更新、相機完成） |
| `RECEIVER_VISIBLE_TO_INSTANT_APPS` | 允許 Instant App 訪問 | Google Play Instant 應用 |

本項目位置更新廣播為**內部私有**，故使用 `RECEIVER_NOT_EXPORTED`。

---

## 🔧 開發者常用任務

### 🔍 檢視側溝工作流（Scope 檢視）
**路徑**：MainActivity → 地圖點擊側溝線段 → GutterInspectActivity（分頁檢視）→ 點擊編輯 → AddGutterBottomSheet + GutterFormActivity

**關鍵組件**：
- `ScopeMapCoordinator.kt` - 協調 scope 視圖中的多條線段渲染
- `GutterInspectActivity.kt` - 唯讀檢視（基本資訊 + 點位照片分頁）
- `InspectMarkerController.kt` - 點位大頭針管理

### ✏️ 修改測距虛線粗度
**文件**: `app/src/main/java/com/example/taoyuangutter/map/DistanceMeasureManager.kt`
**位置**: 第 129 行
```kotlin
.width(8f)  // 改為 .width(12f) 加粗或 .width(4f) 細化
```

### 🎨 自訂測距線顏色
**位置**: 第 128 行
```kotlin
.color(Color.parseColor("#3F51B5"))  // 改為其他十六進制顏色
```

### 📐 虛線樣式（Dash 間距）
**位置**: 第 49 行
```kotlin
private val dashPattern: List<PatternItem> = listOf(Dash(30f), Gap(15f))
// Dash(30f) = 30px 實線，Gap(15f) = 15px 空白
```

### ✅ 表單必填欄位驗證
**文件**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `validateWaypointList()` 檢查 Waypoint 完整性
- `basicData.size >= MIN_FIELDS` 與 `photoUris.size == 3` 必須通過

### 🌐 API 端點修改
**文件**: `api/GutterApiService.kt`
- `POST /api/gutters` (`submitGutter()`) - 上傳單條側溝資料
- `POST /api/v1/ditch/storeDitch` → 修改 `@POST("api/v1/ditch/storeDitch")`
- `GET /api/v1/ditch/ditchDetails` → 取得側溝詳細資料
- `GET /api/v1/node/nodeDetails` → 取得點位資料（多個查詢方式：`node_id`, `XY_NUM`, 座標）
- `POST /api/v1/node/nodeImage` → 上傳單張點位照片（fileCategory: 1=概況, 2=寬度, 3=深度）
- 修改 Request/Response 模型位置：`api/` 目錄 (GutterApiModels.kt)

---

## 🛠️ 構建與測試

### 構建變體
```bash
# Debug APK（包含日誌）
./gradlew assembleDebug

# Release APK（混淆）
./gradlew assembleRelease
```

### 關鍵 Gradle 配置
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36
- **ViewBinding**: 在 `build.gradle.kts` 啟用
- **Google Maps API Key**: `local.properties` 或 `secrets.properties`

### 常見問題排查
| 問題 | 原因 | 解決 |
|------|------|------|
| Google Maps 空白 | API Key 未設定或 SHA-1 指紋不符 | 檢查 `local.properties` 與 Firebase Console |
| Room 數據庫版本衝突 | 迴圈遷移 | 增加版本號、更新 `@Database(version=X)` |
| Retrofit 序列化失敗 | JSON 欄位名稱不符 | 使用 `@SerializedName("field_name")` |

---

## 📂 重要文件速查

### UI 層
- `MainActivity.kt` - 地圖主容器、測距管理、位置追蹤、scope 檢視
- `GutterFormActivity.kt` - 側溝表單容器、驗證與提交
- `GutterInspectActivity.kt` - 唯讀檢視既有側溝（API 資料）
- `ui/AddGutterBottomSheet.kt` - 浮動表單面板
- `gutter/CameraOverlayFragment.kt` - 全螢幕相機 Overlay（嵌入 GutterFormActivity）
- `gutter/LandscapeCameraActivity.kt` - 獨立全屏橫向相機 Activity

### 表單層
- `gutter/GutterBasicInfoFragment.kt` - 側溝規格表單
- `gutter/GutterPhotosFragment.kt` - 照片拍攝與驗證
- `gutter/GutterInspectBasicFragment.kt` - 檢視側溝基本資料（唯讀）
- `gutter/GutterInspectPhotosFragment.kt` - 檢視點位照片（唯讀）

### 倉儲層
- `api/GutterRepository.kt` - 遠端 API 通訊
- `pending/GutterSessionRepository.kt` - 待上傳側溝草稿管理（內存 + Room 持久化）
- `pending/GutterDraftDatabase.kt` - Room 資料庫
- `pending/DraftDao.kt` - 草稿 DAO（CRUD 操作）

### 地圖管理
- `map/GutterMapController.kt` - 管理「工作中」側溝標記與線段（新增/編輯）
- `map/DistanceMeasureManager.kt` - 測距虛線、線寬配置（第 128-129 行可調粗度/顏色）
- `map/InspectMarkerController.kt` - 檢視模式側溝標記
- `map/ScopeMapCoordinator.kt` - 協調 scope 檢視中的多條側溝線段
- `map/ScopeGutterPolylineController.kt` - 管理 scope 視圖中的側溝線段
- `map/MarkerIconFactory.kt` - 大頭針圖示工廠
- `map/MapCameraController.kt` - 地圖鏡頭動畫與定位

### 數據模型
- `gutter/Waypoint.kt` - 地圖/表單橋接數據
- `api/GutterApiModels.kt` - API 請求/響應 DTO（多個數據類）

---

## 🔐 環境變量與 Secrets

### 必須設定
```properties
# local.properties 或 secrets.properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
API_BASE_URL=https://api.example.com/
```

### Secrets 最佳實踐
- 勿提交 `secrets.properties` 至版本控制
- 使用 `.gitignore` 忽略敏感文件
- CI/CD 透過環境變量注入

---

## 🚀 快速參考命令

```bash
# 清潔構建
./gradlew clean build

# 運行 Debug APK 到連接設備
./gradlew installDebug

# 只編譯（不裝置）
./gradlew compile

# 查看依賴樹
./gradlew dependencies

# 檢查 lint 問題
./gradlew lint
```

---

## 💡 專案特色模式

### 1. 「無法開蓋」欄位邏輯
若 `IS_CANTOPEN=true`，上傳時自動清除 `depth`、`width` 等測量欄位（無法測量）。
```kotlin
if (basicData["IS_CANTOPEN"] == "true") {
    storeDitchRequest.depth = null
    storeDitchRequest.width = null
}
```

### 2. 路點序號遞增
新增路點時自動分配 `index`（起點=0, 節點=1,2,..., 終點=最後一個）。
遵循 API 期望的路點順序。

### 3. 測距準星 UI
測距模式進入時顯示螢幕中央準星（`ImageView`），用戶拖曳地圖時虛線即時更新。
配置在 `MeasureConfig.crosshairResId`。

---

## 📋 修改清單示例

需要修改粗度？參考下列步驟：
1. 開啟 `app/src/main/java/com/example/taoyuangutter/map/DistanceMeasureManager.kt`
2. 找到第 129 行：`.width(8f)`
3. 修改數值（如 `12f` 加粗、`4f` 細化）
4. 構建並測試：`./gradlew installDebug`

### 📋 試用者草稿管理
**工作流程**：用戶編輯側溝後退出，數據自動存入 Room → 下次進入彈出「待上傳清單」

**關鍵代碼路徑**：
1. 自動保存：`GutterFormActivity.onPause()` 呼叫 `GutterSessionRepository.save()`
2. 恢復編輯：`PendingDraftsBottomSheet` 讀取 `GutterSessionRepository.getAll()`
3. 清理照片：`GutterDraftCoordinator` 在刪除草稿時清理孤立照片文件

**查看/修改**：
```kotlin
// 在 GutterFormActivity 中
val repo = GutterSessionRepository(context)
val drafts = repo.getAll()  // 列出所有待上傳
repo.delete(draftId)        // 刪除草稿
```

---

**最後更新**: 2026-05-27 | **維護者**: AI Agents
