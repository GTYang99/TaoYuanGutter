# TaoYuanGutter 專案架構總覽

## 項目概述

**項目名稱**: TaoYuanGutter
**技術棧**: Android Kotlin + Google Maps
**主要功能**: 侧溝檢查表單系統（GutterForm）- 用於收集侧溝資料、拍攝照片、標記地圖位置
**最小 SDK**: 24 | **目標 SDK**: 36 | **編譯 SDK**: 36
**編譯工具**: Gradle KTS + Kotlin Compose

---

## 目錄結構

```
TaoYuanGutter/
├── app/src/main/java/com/example/taoyuangutter/
│   ├── MainActivity.kt                    # 主地圖應用
│   ├── api/                              # API 層（網路通訊）
│   ├── gutter/                           # 核心表單模組
│   ├── login/                            # 登入模組
│   ├── map/                              # 地圖工具
│   ├── offline/                          # 離線草稿管理
│   ├── pending/                          # 當前會話暫存草稿
│   ├── common/                           # 共用工具
│   ├── form/                             # 表單組件
│   └── ui/                               # UI 主題和樣式
├── build.gradle.kts                      # 主模組構建配置
├── settings.gradle.kts                   # Gradle 設定
├── gradle.properties                     # Gradle 屬性
├── local.properties                      # 本地配置（.gitignore）
├── secrets.defaults.properties           # API 金鑰模板
└── GEMINI.md                            # 開發進度記錄
```

---

## 核心模組詳解

### 1. **主應用層 (MainActivity.kt)**

**職責**: 地圖顯示、會話管理、側溝列表交互

**主要功能**:
- GoogleMap 集成（EMAP / PHOTO2 地圖模式）
- 實時位置追蹤（GPS 定位）
- 側溝標記渲染（自訂圖標）
- 路線繪製（Polyline）
- WMS/WMTS 圖層疊加

**關鍵成員**:
- `googleMap: GoogleMap?` - 地圖實例
- `currentSessionDraftId: Long` - 當前會話草稿 ID
- `activeSheet: AddGutterBottomSheet?` - 當前打開的表單底部表單
- `sessionDraftRepository: GutterSessionRepository` - 會話草稿倉儲

**主要方法**:
- `onMapReady()` - 地圖初始化
- `fitCameraToWaypoints()` - 調整鏡頭以顯示所有路點
- `openWaypointForEdit()` - 打開路點編輯表單
- `openInspectBottomSheet()` - 打開查看底部表單

**與其他模組的互動**:
```
MainActivity
├─→ AddGutterBottomSheet (表單容器)
├─→ GutterSessionRepository (會話草稿存取)
├─→ GutterRepository (API 通訊)
└─→ MarkerIconFactory (標記圖標生成)
```

---

### 2. **表單模組 (gutter/)**

這是項目的核心業務邏輯模組。

#### **GutterFormActivity.kt**
**職責**: 表單容器 Activity，管理表單頁面切換

**核心職責**:
- 管理表單的多頁面導航（ViewPager2）
- 驗證表單資料（基本資料 + 照片）
- 提交表單或保存為草稿
- 支援編輯模式和新增模式

**重要常數**:
```kotlin
const val EXTRA_IS_EDIT_MODE = "is_edit_mode"     // 編輯模式旗標
const val EXTRA_SESSION_DRAFT_ID = "session_draft_id"
const val EXTRA_OFFLINE_MODE = "offline_mode"     // 離線模式
const val EXTRA_OFFLINE_DRAFT_ID = "offline_draft_id"
```

**資料 Keys** (傳入/傳出):
- `EXTRA_DATA_GUTTER_ID` - 側溝編號 (spi_num)
- `EXTRA_DATA_COORD_X/Y` - 經緯度座標
- `EXTRA_DATA_MEASURE_ID` - 測量座標編號 (xy_num)
- `EXTRA_DATA_PHOTO_1/2/3` - 照片 URI

**重要方法**:
- `saveAndClose()` - 驗證並提交表單
- `buildAndFinishWithResult()` - 構建返回結果 Intent
- `enterEditMode()` - 進入編輯模式

#### **GutterFormPagerAdapter.kt**
**職責**: ViewPager2 頁面適配器

**管理的頁面**:
1. `GutterBasicInfoFragment` - 基本資料表單（第 0 頁）
2. `GutterPhotosFragment` - 照片拍攝（第 1 頁）

#### **GutterBasicInfoFragment.kt**
**職責**: 第一頁表單 - 收集基本資訊

**表單欄位**:
- `tilGutterId` - 側溝編號（編輯模式隱藏）
- `etMeasureId` - 測量座標編號（編輯模式預填）
- `etGutterType` - 側溝類型
- `etMaterial` - 材料類型
- `etCoordX/Y` - 經緯度座標
- `etCoordZ` - 高程
- `etDepth` - 深度
- `etTopWidth` - 頂部寬度
- `cbBroken/Hanging/Silt/CantOpen` - 狀態複選框
- `etRemarks` - 備註

**重要方法**:
- `validateRequiredFields()` - 驗證必填欄位（編輯模式跳過 gutterId）
- `prefillData()` - 預填資料
- `getBasicData()` - 取得表單資料

#### **GutterPhotosFragment.kt**
**職責**: 第二頁表單 - 拍攝和管理照片

**照片槽位**:
- `photoUriSlot1/2/3` - 三個照片 URI 存儲位置

**功能**:
- 使用 `ActivityResultContracts.TakePicture` 啟動相機
- 申請相機權限
- 驗證所有三張照片已拍攝

**重要方法**:
- `validateAllPhotos()` - 驗證三張照片
- `getPhotoPaths()` - 取得照片 URI 字符串
- `createPhotoUriForSlot()` - 為每個槽位創建持久化 URI

#### **GutterInspectActivity.kt**
**職責**: 查看已提交的側溝資料

**功能**:
- 展示側溝詳細資訊（只讀）
- 支援進入編輯模式
- 將 API 資料轉換為 Waypoint

**重要方法**:
- `ditchToWaypoints()` - 將 DitchDetails 轉換為 Waypoint 列表

#### **AddGutterBottomSheet.kt**
**職責**: 地圖上的浮動表單底部表單

**功能**:
- 新增路點的交互界面
- 管理路點列表（WaypointAdapter）
- 地圖鏡頭自動調整以顯示表單

**重要方法**:
- `newInstanceForEdit()` - 編輯模式
- `newInstanceForCreate()` - 新增模式
- `updateWaypointLocation()` - 更新路點座標

#### **Waypoint.kt & WaypointType.kt**
**職責**: 核心資料模型

```kotlin
data class Waypoint(
    var id: Long = 0,
    var nodeId: String = "",      // API 端點 ID
    var index: Int = 0,
    var name: String = "",
    var latLng: LatLng,
    var waypointType: WaypointType = WaypointType.NORMAL,
    var basicData: Map<String, String> = emptyMap(),
    var photoUris: List<Uri> = emptyList()
)
```

---

### 3. **API 層 (api/)**

負責所有網路通訊和資料轉換。

#### **GutterRepository.kt**
**職責**: 高級 API 操作接口（應用層與 API 層的橋接）

**主要 API 方法**:
- `login(username, password)` - 登入
- `fetchGuttersInBounds()` - 查詢邊界內的側溝
- `submitGutter()` - 提交新側溝
- `updateGutter()` - 更新側溝資料
- `uploadPhotos()` - 上傳照片
- `getDitchDetails()` - 取得側溝詳情

**特點**:
- 所有方法都是 `suspend function`，必須在協程上下文中調用
- 返回 `ApiResult<T>` (Success/Error)
- 自動處理 Token 管理和錯誤解析

#### **GutterApiService.kt**
**職責**: Retrofit API 接口定義

```kotlin
interface GutterApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/ditches/bounds")
    suspend fun fetchGuttersInBounds(...): Response<GuttersResponse>

    @POST("/ditches")
    suspend fun submitGutter(@Body request: DitchRequest): Response<DitchResponse>

    // ... 更多方法
}
```

#### **GutterApiModels.kt**
**職責**: 所有 API 請求/回應資料類

主要模型:
- `LoginRequest/Response` - 登入
- `DitchRequest/Response` - 側溝提交
- `DitchDetails` - 側溝詳情
- `DitchNode` - 路點節點
- `PhotoUploadRequest` - 照片上傳

#### **GutterApiClient.kt**
**職責**: Retrofit 客戶端單例配置

**特點**:
- 配置 OkHttp 攔截器（Token 注入、日誌）
- Gson 序列化/反序列化
- 自訂超時設定

---

### 4. **離線草稿模組 (offline/)**

用於本地保存表單草稿，支援無網絡工作。

#### **OfflineDraft.kt**
```kotlin
data class OfflineDraft(
    val id: Long,              // 本地 ID
    val basicData: Map<String, String>,
    val photoUris: List<String>,
    val timestamp: Long,
    val waypointName: String
)
```

#### **OfflineDraftRepository.kt**
**職責**: 本地草稿存取（Room Database）

**主要方法**:
- `saveDraft()` - 保存草稿
- `getDraft()` - 取得指定草稿
- `getAllDrafts()` - 列出所有草稿
- `deleteDraft()` - 刪除草稿

#### **OfflineDraftsActivity.kt**
**職責**: 離線草稿列表界面

---

### 5. **會話暫存模組 (pending/)**

管理當前應用會話中的臨時草稿（不持久化）。

#### **GutterSessionDraft.kt**
```kotlin
data class GutterSessionDraft(
    val id: Long,
    val waypoints: List<Waypoint>,
    val timestamp: Long
)
```

#### **GutterSessionRepository.kt**
**職責**: 內存中的會話草稿管理

**特點**:
- 應用運行期間保持在內存中
- 用戶關閉應用時丟失
- 支援快速保存/恢復表單狀態

#### **PendingDraftsBottomSheet.kt**
**職責**: 顯示當前會話的所有草稿

**交互**:
- 單擊項目 → 恢復草稿到表單
- 長按項目 → 刪除選項

---

### 6. **登入模組 (login/)**

#### **LoginActivity.kt**
**職責**: 用戶認證界面

**功能**:
- 帳號/密碼登入
- 密碼可見/隱藏切換
- 調用 `GutterRepository.login()`
- 保存 Token 到 SharedPreferences

---

### 7. **地圖模組 (map/)**

#### **MarkerIconFactory.kt**
**職責**: 生成自訂地圖標記圖標

**功能**:
- 根據 `WaypointType` 生成不同顏色的標記
- 返回 `BitmapDescriptor` 供地圖使用

---

### 8. **共用工具 (common/)**

#### **LocationPickEvents.kt**
**職責**: 廣播事件常數定義

用於在 MainActivity 和表單之間傳遞位置變更事件

---

## 數據流圖

### 新增側溝流程

```
MainActivity
    ↓ (點擊 FAB 或地圖)
AddGutterBottomSheet
    ↓ (輸入路點名稱)
GutterFormActivity
    ↓ (ViewPager2)
┌─→ GutterBasicInfoFragment (表單頁 1)
│   └─→ 驗證必填欄位
├─→ GutterPhotosFragment (表單頁 2)
│   └─→ 驗證三張照片
└─→ saveAndClose()
    ├─→ GutterRepository.submitGutter()
    │   └─→ GutterApiService.submitGutter()
    ├─→ GutterRepository.uploadPhotos()
    │   └─→ GutterApiService.uploadPhotos()
    └─→ setResult() + finish()
        └─→ MainActivity 更新地圖
```

### 編輯側溝流程

```
MainActivity (地圖點擊)
    ↓
GutterInspectActivity (查看側溝)
    ↓ (點擊編輯按鈕)
openWaypointForEdit()
    ↓
GutterRepository.getDitchDetails()
    ↓
AddGutterBottomSheet (編輯模式)
    ├─→ isEditMode = true
    └─→ GutterFormActivity
        ├─→ GutterBasicInfoFragment (側溝編號隱藏)
        ├─→ GutterPhotosFragment (顯示現有照片)
        └─→ saveAndClose()
            ├─→ GutterRepository.updateGutter()
            └─→ setResult() + finish()
```

### 離線草稿流程

```
GutterFormActivity (編輯模式 = false)
    ↓ (用戶取消或網絡失敗)
保存至 OfflineDraftRepository
    ↓
OfflineDraftsActivity (列表)
    ├─→ 查看草稿詳情
    ├─→ 繼續編輯
    └─→ 刪除草稿
```

---

## 重要設計模式

### 1. **ViewPager2 + Fragment 分頁**
- `GutterFormPagerAdapter` 管理多個表單頁面
- 每個 Fragment 負責獨立的表單部分
- 獲取 Fragment 實例時調用 `getFragment()` 而不是創建新實例

### 2. **Activity Result Contract**
- `takePictureLauncher` 使用 `ActivityResultContracts.TakePicture`
- 優於過時的 `startActivityForResult()`
- 允許在任何 Fragment 中使用

### 3. **Broadcast Receiver 事件通知**
- 位置變更事件透過 Broadcast 廣播
- MainActivity 和表單可以相互通知狀態變化

### 4. **Repository 模式**
- `GutterRepository` 抽象 API 實現
- `OfflineDraftRepository` 和 `GutterSessionRepository` 提供不同的存儲層
- 應用層只依賴 Repository，不直接調用 API

### 5. **Intent Extra 常數集中**
- 所有 Intent Key 定義為 `companion object` 中的常數
- 易於維護和重構

---

## 編輯模式特殊處理

### 編輯模式標誌
- `GutterFormActivity.EXTRA_IS_EDIT_MODE = true`

### GutterBasicInfoFragment 行為
- **隱藏** `tilGutterId` (側溝編號)
- **預填** `etMeasureId` 為 API 返回的 `XY_NUM`
- **跳過驗證** `gutterId` 必填檢查

### GutterPhotosFragment 行為
- 顯示既有照片（如果有）
- 允許替換照片

### ActivityResult 行為
- 不回傳 `RESULT_DATA_GUTTER_ID`
- 回傳更新後的座標和資料

---

## 依賴庫

### 核心框架
- **AndroidX**: core, appcompat, fragment, lifecycle, navigation, viewpager2
- **Google Services**: maps, location
- **Jetpack Compose**: UI 主題定義

### 相機和媒體
- **CameraX**: 相機功能

### 網絡通訊
- **Retrofit** + **OkHttp**: API 調用和 HTTP 客戶端
- **Gson**: JSON 序列化

### UI 增強
- **Material Design 3**: Material 組件

### 圖片加載
- **Glide**: 從 URL 加載照片

---

## 關鍵文件依賴關係

```
MainActivity.kt
├─ AddGutterBottomSheet.kt
│   ├─ GutterSessionRepository.kt
│   ├─ Waypoint.kt
│   └─ WaypointAdapter.kt
├─ GutterRepository.kt
│   ├─ GutterApiService.kt
│   └─ GutterApiModels.kt
├─ GutterSessionRepository.kt
├─ MarkerIconFactory.kt
└─ PendingDraftsBottomSheet.kt

GutterFormActivity.kt
├─ GutterFormPagerAdapter.kt
│   ├─ GutterBasicInfoFragment.kt
│   └─ GutterPhotosFragment.kt
├─ GutterRepository.kt
├─ OfflineDraftRepository.kt
└─ GutterSessionRepository.kt

GutterInspectActivity.kt
├─ GutterRepository.kt
└─ GutterInspectBasicFragment.kt
    └─ GutterInspectPhotosFragment.kt

LoginActivity.kt
└─ GutterRepository.kt
```

---

## 下次開發方向

1. **資料持久化優化**
   - 實裝 Room Database 支援離線更完整的功能

2. **UI/UX 改進**
   - 照片預覽優化
   - 表單驗證錯誤提示改進

3. **錯誤處理加強**
   - 網絡異常重試機制
   - 更詳細的用戶提示

4. **性能優化**
   - 大量照片管理
   - 地圖渲染性能

5. **測試覆蓋**
   - Unit Test（Repository 層）
   - Instrumentation Test（UI 層）

---

**文檔最後更新**: 2026-04-01
**版本**: 1.0
