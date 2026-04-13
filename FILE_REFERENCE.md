# TaoYuanGutter 文件參考指南

## 快速查找

此文件提供所有 30+ 個 Kotlin 源文件的快速參考，按層級和模塊組織。

---

## 📍 應用層 (Activities)

### 1. **MainActivity.kt** (最大的文件 ~61KB)
**位置**: `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`

**職責**: 核心應用 Activity，地圖主界面

**主要功能**:
- Google Maps 集成和顯示
- 側溝標記渲染和交互
- GPS 位置追蹤
- 底部表單管理 (AddGutterBottomSheet, PendingDraftsBottomSheet)
- 表單打開/關閉時的鏡頭調整

**關鍵成員**:
```kotlin
private lateinit var binding: ActivityMainBinding
private var googleMap: GoogleMap? = null
private var currentSessionDraftId: Long = -1L
private var activeSheet: AddGutterBottomSheet? = null
private val sessionDraftRepository: GutterSessionRepository
private val gutterRepository: GutterRepository
```

**關鍵方法**:
- `onMapReady()` - 初始化地圖
- `fitCameraToWaypoints()` - 調整鏡頭邊界
- `openWaypointForEdit()` - 打開編輯表單
- `onActivityResult()` - 處理表單返回結果
- `refreshMarkersOnMap()` - 刷新地圖標記

**與其他文件的關係**:
```
MainActivity
├─→ GutterRepository (API 調用)
├─→ GutterSessionRepository (會話草稿)
├─→ AddGutterBottomSheet (表單容器)
├─→ PendingDraftsBottomSheet (草稿列表)
├─→ GutterFormActivity (啟動)
├─→ GutterInspectActivity (啟動)
└─→ MarkerIconFactory (生成標記)
```

**常見編輯點**:
- 地圖樣式和圖層修改
- 標記點擊行為
- 鏡頭調整邏輯
- 底部表單交互

---

### 2. **GutterFormActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`

**職責**: 表單容器 Activity，管理分頁表單

**主要功能**:
- ViewPager2 管理多頁表單
- 驗證邏輯協調
- 表單資料提交
- 離線/在線模式切換

**關鍵常數** (Intent Keys):
```kotlin
const val EXTRA_IS_EDIT_MODE = "is_edit_mode"           // 編輯模式
const val EXTRA_SESSION_DRAFT_ID = "session_draft_id"   // 會話 ID
const val EXTRA_OFFLINE_MODE = "offline_mode"           // 離線模式
const val EXTRA_DATA_GUTTER_ID = "ex_spi_num"          // 側溝編號
const val EXTRA_DATA_PHOTO_1/2/3 = "ex_photo1/2/3"     // 照片 URI
const val EXTRA_DATA_XY_NUM = "ex_xy_num_value"        // 測量座標編號
```

**關鍵成員**:
```kotlin
private lateinit var binding: ActivityGutterFormBinding
private lateinit var pagerAdapter: GutterFormPagerAdapter
private val gutterRepository: GutterRepository
private val offlineDraftRepository: OfflineDraftRepository
```

**關鍵方法**:
- `saveAndClose()` - 驗證和提交表單
- `buildAndFinishWithResult()` - 構建返回結果
- `enterEditMode()` - 激活編輯模式
- `validateForm()` - 表單驗證

**狀態管理**:
- `isEditMode: Boolean` - 當前編輯狀態
- `currentWaypoint: Waypoint?` - 當前編輯的路點

**常見編輯點**:
- 驗證邏輯修改
- 表單頁面配置
- 提交流程修改
- 錯誤提示

---

### 3. **GutterInspectActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`

**職責**: 查看側溝詳細資訊

**主要功能**:
- 顯示只讀側溝信息
- 支援進入編輯模式
- 照片查看

**包含的 Fragment**:
- `GutterInspectBasicFragment` - 基本資訊顯示
- `GutterInspectPhotosFragment` - 照片查看

**關鍵方法**:
- `ditchToWaypoints()` - 將 API 數據轉換為 Waypoint

---

### 4. **LoginActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/login/LoginActivity.kt`

**職責**: 用戶登入界面

**主要功能**:
- 帳號密碼輸入
- 登入驗證
- Token 保存

**關鍵方法**:
- `performLogin()` - 執行登入
- `saveToken()` - 保存 Token 到 SharedPreferences

---

## 📋 表單 Fragment 層

### 5. **GutterBasicInfoFragment.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`

**職責**: 表單第一頁 - 基本資訊輸入

**所屬**: GutterFormActivity → GutterFormPagerAdapter (第 0 頁)

**表單欄位**:
| ID | 名稱 | 對應 API Key | 編輯模式 |
|----|------|-------------|--------|
| `tilGutterId` | 側溝編號 | `spi_num` | 隱藏 |
| `etMeasureId` | 測量座標編號 | `xy_num` | 預填 |
| `etGutterType` | 側溝類型 | `node_typ` | 顯示 |
| `etMaterial` | 材料類型 | `mat_typ` | 顯示 |
| `etCoordX` | 經度 | `node_x` | 顯示 |
| `etCoordY` | 緯度 | `node_y` | 顯示 |
| `etCoordZ` | 高程 | `node_le` | 顯示 |
| `etDepth` | 深度 | `node_dep` | 顯示 |
| `etTopWidth` | 頂部寬度 | `node_wid` | 顯示 |
| `cbBroken` | 破損 | `is_broken` | 顯示 |
| `cbHanging` | 懸掛 | `is_hanging` | 顯示 |
| `cbSilt` | 淤泥 | `is_silt` | 顯示 |
| `cbCantOpen` | 無法開啟 | `is_cantopen` | 顯示 |
| `etRemarks` | 備註 | `node_note` | 顯示 |

**關鍵常數**:
```kotlin
companion object {
    const val ARG_IS_EDIT_MODE = "is_edit_mode"
    const val ARG_DATA_XY_NUM = "xy_num"
}
```

**關鍵方法**:
- `newInstance(isEditMode, xyNum)` - Factory method
- `validateRequiredFields()` - 驗證必填 (編輯模式跳過 gutterId)
- `prefillData()` - 預填數據
- `getBasicData()` - 收集表單數據

**必填欄位** (新增模式):
- `gutterId` ✓
- `gutterType` ✓
- `material` ✓
- `coordX` ✓
- `coordY` ✓
- 其他字段可選

**必填欄位** (編輯模式):
- 跳過 `gutterId`
- 保持其他驗證

---

### 6. **GutterPhotosFragment.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterPhotosFragment.kt`

**職責**: 表單第二頁 - 照片拍攝和管理

**所屬**: GutterFormActivity → GutterFormPagerAdapter (第 1 頁)

**照片管理**:
```kotlin
private var photoUriSlot1: Uri? = null  // 第一張照片
private var photoUriSlot2: Uri? = null  // 第二張照片
private var photoUriSlot3: Uri? = null  // 第三張照片
```

**使用的 Activity Result Launcher**:
```kotlin
private val cameraPermissionLauncher: ActivityResultLauncher<String>
  // 申請相機權限

private val takePictureLauncher: ActivityResultLauncher<Uri>
  // 啟動相機拍照
```

**關鍵方法**:
- `validateAllPhotos()` - 驗證三張照片都已拍攝
- `getPhotoPaths()` - 返回照片 URI 字符串列表
- `createPhotoUriForSlot(slotNumber)` - 為槽位創建臨時 URI
- `openCamera(slotNumber)` - 打開指定槽位的相機

**權限需求**:
- `Manifest.permission.CAMERA` - 相機權限

---

## 🔧 底部表單 Fragment 層

### 7. **AddGutterBottomSheet.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`

**職責**: 地圖上的浮動底部表單，新增/編輯路點

**所屬**: 由 MainActivity 顯示

**內部使用**:
- `WaypointAdapter` - 路點列表適配器

**路點管理**:
```kotlin
private var waypoints: MutableList<Waypoint> = mutableListOf()
```

**Factory Methods**:
```kotlin
fun newInstanceForCreate()       // 新增模式
fun newInstanceForEdit()         // 編輯模式
```

**關鍵方法**:
- `addWaypoint()` - 新增路點
- `updateWaypointLocation()` - 更新座標
- `removeWaypoint()` - 刪除路點
- `getWaypoints()` - 取得所有路點

**事件回調**:
- `onWaypointsChanged()` - 路點列表變更回調

**特殊處理**:
- 底部表單顯示時，自動調整地圖鏡頭以顯示完整內容

---

### 8. **PendingDraftsBottomSheet.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftsBottomSheet.kt`

**職責**: 顯示當前會話的草稿列表

**所屬**: 由 MainActivity 顯示

**草稿列表**:
```kotlin
private val drafts: List<GutterSessionDraft>
```

**交互方式**:
- 單擊項目 → 恢復草稿到 AddGutterBottomSheet
- 長按項目 → 顯示刪除選項

**使用的適配器**:
- `PendingDraftAdapter` - 草稿列表項適配器

---

## 🗺️ 地圖和導航

### 9. **MapPointPickerActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt`

**職責**: 地圖點位選擇界面 (未在主流程中使用)

---

### 10. **LandscapeCameraActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/LandscapeCameraActivity.kt`

**職責**: 橫屏相機界面 (已被新的相機契約替代)

---

### 11. **MarkerIconFactory.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/map/MarkerIconFactory.kt`

**職責**: 生成地圖標記圖標

**功能**:
- 根據 `WaypointType` 選擇顏色
- 繪製自訂標記 Bitmap
- 返回 `BitmapDescriptor` 供 GoogleMaps 使用

**標記類型**:
- `NORMAL` - 藍色標記
- `EDITED` - 黃色標記
- `PENDING` - 紅色標記

---

## 📦 數據模型

### 12. **Waypoint.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/Waypoint.kt`

**職責**: 核心數據模型 - 代表地圖上的一個側溝點位

```kotlin
@Serializable
data class Waypoint(
    var id: Long = 0,
    var nodeId: String = "",
    var index: Int = 0,
    var name: String = "",
    var latLng: LatLng,
    var waypointType: WaypointType = WaypointType.NORMAL,
    var basicData: Map<String, String> = emptyMap(),
    var photoUris: List<Uri> = emptyList()
)
```

**使用位置**:
- AddGutterBottomSheet - 路點列表管理
- GutterFormActivity - 表單數據
- GutterSessionDraft - 會話暫存
- GutterRepository - API 轉換

---

### 13. **WaypointType.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/WaypointType.kt`

**職責**: 路點類型枚舉

```kotlin
enum class WaypointType {
    NORMAL,    // 普通路點
    EDITED,    // 已編輯
    PENDING    // 待上傳
}
```

---

### 14. **GutterSessionDraft.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionDraft.kt`

**職責**: 會話級別的草稿模型

```kotlin
data class GutterSessionDraft(
    val id: Long,
    val waypoints: List<Waypoint>,
    val timestamp: Long
)
```

**使用位置**:
- GutterSessionRepository
- PendingDraftsBottomSheet

---

### 15. **OfflineDraft.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/offline/OfflineDraft.kt`

**職責**: 本地持久化的草稿模型

```kotlin
@Entity(tableName = "offline_drafts")
data class OfflineDraft(
    @PrimaryKey val id: Long,
    val basicData: String,        // JSON
    val photoUris: String,         // JSON
    val timestamp: Long,
    val waypointName: String
)
```

---

## 🔌 API 層

### 16. **GutterRepository.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`

**職責**: 高級 API 操作接口

**主要方法**:
```kotlin
// 認證
suspend fun login(username: String, password: String): ApiResult<LoginResponse>

// 側溝操作
suspend fun fetchGuttersInBounds(bounds: LatLngBounds): ApiResult<List<Waypoint>>
suspend fun submitGutter(waypoint: Waypoint): ApiResult<String>  // 返回側溝 ID
suspend fun updateGutter(waypoint: Waypoint): ApiResult<Unit>
suspend fun getDitchDetails(gutterId: String): ApiResult<DitchDetails>

// 照片
suspend fun uploadPhotos(photoUris: List<Uri>): ApiResult<List<String>>
```

**特點**:
- 所有方法都是 suspend function
- 返回 `ApiResult<T>` (Success/Error)
- 自動管理 Token
- 包含重試邏輯

**使用位置**:
- MainActivity - 查詢和刷新
- GutterFormActivity - 提交/更新
- LoginActivity - 登入

---

### 17. **GutterApiService.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`

**職責**: Retrofit API 接口定義

**定義的端點**:
```kotlin
@POST("/auth/login")
suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

@GET("/ditches/bounds")
suspend fun fetchGuttersInBounds(...): Response<GuttersResponse>

@POST("/ditches")
suspend fun submitDitch(@Body request: DitchRequest): Response<DitchResponse>

@PATCH("/ditches/{id}")
suspend fun updateDitch(@Path("id") id: String, @Body request: DitchRequest)

@POST("/photos/upload")
suspend fun uploadPhotos(@Body body: MultipartBody): Response<PhotoUploadResponse>

@GET("/ditches/{id}")
suspend fun getDitchDetails(@Path("id") id: String): Response<DitchDetailsResponse>
```

---

### 18. **GutterApiModels.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`

**職責**: 所有 API 請求/回應資料類

**主要模型**:
- `LoginRequest/Response` - 登入
- `DitchRequest/Response` - 側溝提交/返回
- `DitchDetails` - 詳細側溝信息
- `DitchNode` - 路點節點信息
- `PhotoUploadRequest/Response` - 照片上傳
- `ApiError` - 錯誤響應

**關鍵轉換邏輯**:
```kotlin
// Waypoint → DitchRequest (提交到 API)
fun Waypoint.toDitchRequest(): DitchRequest

// DitchDetails → Waypoint (接收 API 數據)
fun DitchDetails.toWaypoint(): Waypoint
```

---

### 19. **GutterApiClient.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/api/GutterApiClient.kt`

**職責**: Retrofit 客戶端單例配置

**功能**:
- Retrofit 實例化
- OkHttp 攔截器配置 (Token 注入、日誌)
- Gson 序列化器配置
- 基礎 URL 設置

**成員**:
```kotlin
object GutterApiClient {
    val instance: GutterApiService by lazy { ... }
}
```

---

## 💾 存儲層

### 20. **GutterSessionRepository.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionRepository.kt`

**職責**: 會話級別的草稿管理 (內存存儲)

**主要方法**:
```kotlin
fun saveDraft(draft: GutterSessionDraft)
fun getDraft(draftId: Long): GutterSessionDraft?
fun getAllDrafts(): List<GutterSessionDraft>
fun deleteDraft(draftId: Long)
fun updateDraft(draftId: Long, waypoints: List<Waypoint>)
```

**特點**:
- 應用運行期間保留在內存
- 應用關閉時丟失
- 高性能讀寫

**使用位置**:
- MainActivity - 會話管理
- AddGutterBottomSheet - 暫存路點
- GutterFormActivity - 草稿恢復

---

### 21. **OfflineDraftRepository.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/offline/OfflineDraftRepository.kt`

**職責**: 本地持久化草稿管理 (Room Database)

**主要方法**:
```kotlin
suspend fun saveDraft(draft: OfflineDraft)
suspend fun getDraft(draftId: Long): OfflineDraft?
suspend fun getAllDrafts(): List<OfflineDraft>
suspend fun deleteDraft(draftId: Long)
```

**特點**:
- 使用 Room Database
- 應用關閉仍保留
- 所有方法都是 suspend

**使用位置**:
- GutterFormActivity - 離線模式保存
- OfflineDraftsActivity - 草稿列表

---

## 🎨 UI 適配器和工具

### 22. **GutterFormPagerAdapter.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormPagerAdapter.kt`

**職責**: ViewPager2 頁面適配器

```kotlin
class GutterFormPagerAdapter(
    fragment: Fragment,
    isEditMode: Boolean = false,
    xyNum: String = ""
) : FragmentStateAdapter(fragment)
```

**頁面配置**:
- 頁面 0: `GutterBasicInfoFragment` - 基本資訊
- 頁面 1: `GutterPhotosFragment` - 照片拍攝

**關鍵方法**:
- `getItemCount()` - 返回 2
- `createFragment(position)` - 創建對應位置的 Fragment

---

### 23. **WaypointAdapter.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/gutter/WaypointAdapter.kt`

**職責**: 路點列表的 RecyclerView 適配器

**功能**:
- 顯示路點列表
- 支援滑動刪除 (ItemTouchHelper)
- 支援點擊事件

**使用位置**:
- AddGutterBottomSheet - 路點列表

---

### 24. **PendingDraftAdapter.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftAdapter.kt`

**職責**: 待審草稿列表的 RecyclerView 適配器

**交互**:
- 短按: 恢復草稿
- 長按: 刪除操作

**使用位置**:
- PendingDraftsBottomSheet

---

### 25. **OfflineDraftAdapter.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/offline/OfflineDraftAdapter.kt`

**職責**: 離線草稿列表的 RecyclerView 適配器

**功能**:
- 顯示草稿列表
- 支援查看和刪除

**使用位置**:
- OfflineDraftsActivity

---

## 🎨 UI 主題

### 26-28. **Color.kt / Type.kt / Theme.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/ui/theme/`

**職責**: Jetpack Compose 主題定義

**內容**:
- `Color.kt` - 應用色板 (Material Design 3)
- `Type.kt` - 字體排版
- `Theme.kt` - 主題組合

---

## 🔔 事件和通信

### 29. **LocationPickEvents.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/common/LocationPickEvents.kt`

**職責**: Broadcast 事件常數定義

```kotlin
object LocationPickEvents {
    const val ACTION_WAYPOINT_LOCATION_CHANGED = "..."
    const val EXTRA_SESSION_DRAFT_ID = "..."
    const val EXTRA_WAYPOINT_INDEX = "..."
    const val EXTRA_LATITUDE = "..."
    const val EXTRA_LONGITUDE = "..."
}
```

**使用位置**:
- 跨 Activity/Fragment 通訊
- 位置變更事件廣播

---

## 📊 API 結果模型

### 30. **ApiResult.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/api/ApiResult.kt`

**職責**: 統一的 API 返回結果模型

```kotlin
sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int = -1) : ApiResult<T>()
}
```

**使用模式**:
```kotlin
when (val result = repository.fetchGutters()) {
    is ApiResult.Success -> { /* 使用 result.data */ }
    is ApiResult.Error -> { /* 顯示 result.message */ }
}
```

---

## 活動 (Activities)

### 31. **OfflineDraftsActivity.kt**
**位置**: `app/src/main/java/com/example/taoyuangutter/offline/OfflineDraftsActivity.kt`

**職責**: 離線草稿列表界面

**功能**:
- 列出所有離線草稿
- 點擊恢復/編輯
- 長按刪除

---

## 文件依賴快速查詢表

| 文件 | 依賴項 | 被依賴項 |
|------|--------|---------|
| MainActivity | GutterRepository, GutterSessionRepository, AddGutterBottomSheet | - |
| GutterFormActivity | GutterRepository, OfflineDraftRepository, GutterFormPagerAdapter | MainActivity |
| GutterBasicInfoFragment | - | GutterFormActivity |
| GutterPhotosFragment | - | GutterFormActivity |
| AddGutterBottomSheet | GutterSessionRepository, WaypointAdapter | MainActivity |
| WaypointAdapter | Waypoint | AddGutterBottomSheet |
| GutterRepository | GutterApiService, GutterApiModels | 所有 Activity |
| GutterApiService | GutterApiModels | GutterRepository |
| GutterSessionRepository | GutterSessionDraft | MainActivity, AddGutterBottomSheet |
| OfflineDraftRepository | OfflineDraft | GutterFormActivity |
| MarkerIconFactory | WaypointType | MainActivity |

---

**參考文件版本**: 1.0
**最後更新**: 2026-04-01
