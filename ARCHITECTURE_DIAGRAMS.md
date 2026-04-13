# TaoYuanGutter 架構圖解

## 1. 系統架構層次圖

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  (MainActivity, LoginActivity, GutterFormActivity, etc.)     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Business Logic Layer                        │
│  (GutterRepository, Session/OfflineDraftRepository)         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Data Access Layer                        │
│  (GutterApiService, API Models, Local Database)             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                External Resources                           │
│  (Backend API, Google Maps, Device Camera, Storage)         │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 表單頁面導航流程

```
┌──────────────────┐
│  MainActivity    │
│  (地圖視圖)      │
└────────┬─────────┘
         │
         ↓
┌──────────────────────────────────────────────────────────┐
│         AddGutterBottomSheet / PendingDraftsBottomSheet  │
│         (浮動表單容器)                                   │
└────────┬─────────────────────────────────────────────────┘
         │ 用戶點擊 "確定" 或 "編輯"
         ↓
┌──────────────────────────────────────────────────────────┐
│              GutterFormActivity                           │
│              (表單容器 Activity)                          │
├──────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐ │
│  │         GutterFormPagerAdapter (ViewPager2)        │ │
│  ├─────────────────────────────────────────────────────┤ │
│  │  Page 0: GutterBasicInfoFragment (表單)            │ │
│  │          • 基本資料輸入                            │ │
│  │          • 欄位驗證                                │ │
│  │                                                     │ │
│  │  Page 1: GutterPhotosFragment (相機)              │ │
│  │          • 拍照或選擇照片                          │ │
│  │          • 三張照片驗證                            │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Bottom: fabSubmit Button                         │   │
│  │  點擊 → validateAndSave()                         │   │
│  └──────────────────────────────────────────────────┘   │
└────────┬─────────────────────────────────────────────────┘
         │
         ├─→ 驗證成功 ──→ 上傳至 API ──→ setResult(RESULT_OK)
         │                                        ↓
         │                              MainActivity 刷新地圖
         │
         └─→ 驗證失敗 ──→ Toast 提示 ──→ 留在表單
            或取消 ──→ setResult(RESULT_CANCELED) + finish()
```

---

## 3. 編輯模式與新增模式對比

```
┌─────────────────────────────────────────────────────────────┐
│                     GutterFormActivity                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  EXTRA_IS_EDIT_MODE = false (新增模式)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ GutterBasicInfoFragment                              │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ ✓ 側溝編號欄位: 可見 + 可編輯                        │   │
│  │ ✓ 測量座標編號: 空白或用戶輸入                       │   │
│  │ ✓ 驗證 gutterId 為必填                              │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  EXTRA_IS_EDIT_MODE = true (編輯模式)                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ GutterBasicInfoFragment                              │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ ✗ 側溝編號欄位: 隱藏                                 │   │
│  │ ✓ 測量座標編號: 預填 (API 傳入的 xy_num)            │   │
│  │ ✓ 驗證跳過 gutterId 檢查                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  兩種模式共同:                                               │
│  • GutterPhotosFragment 功能相同 (拍照驗證)                 │
│  • 回傳 Intent result 包含更新後資料                        │
│  • 編輯模式不回傳 RESULT_DATA_GUTTER_ID                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 數據模型關係圖

```
Waypoint (核心模型)
│
├─ id: Long (路點 ID)
├─ nodeId: String (API 節點 ID)
├─ index: Int (序列索引)
├─ name: String (路點名稱)
├─ latLng: LatLng (經緯度座標)
├─ waypointType: WaypointType (路點類型)
│   ├─ NORMAL (普通)
│   ├─ EDITED (已編輯)
│   └─ PENDING (待上傳)
├─ basicData: Map<String, String>
│   ├─ "gutterId" / "spi_num"
│   ├─ "gutterType" / "node_typ"
│   ├─ "material" / "mat_typ"
│   ├─ "coordX" / "node_x"
│   ├─ "coordY" / "node_y"
│   ├─ "coordZ" / "node_le"
│   ├─ "depth" / "node_dep"
│   ├─ "topWidth" / "node_wid"
│   ├─ "xyNum" / "xy_num"
│   ├─ "broken" / "is_broken"
│   ├─ "hanging" / "is_hanging"
│   ├─ "silt" / "is_silt"
│   ├─ "cantOpen" / "is_cantopen"
│   └─ "remarks" / "node_note"
│
└─ photoUris: List<Uri>
    ├─ photoUriSlot1 (第一張照片)
    ├─ photoUriSlot2 (第二張照片)
    └─ photoUriSlot3 (第三張照片)
```

---

## 5. API 數據流

```
┌─────────────────────────────────┐
│  GutterFormActivity             │
│  saveAndClose()                 │
└────────────┬────────────────────┘
             │ 1. 驗證表單
             ↓
┌─────────────────────────────────┐
│  GutterBasicInfoFragment        │
│  validateRequiredFields()       │
│  getBasicData()                 │
└────────────┬────────────────────┘
             │ 2. 驗證照片
             ↓
┌─────────────────────────────────┐
│  GutterPhotosFragment           │
│  validateAllPhotos()            │
│  getPhotoPaths()                │
└────────────┬────────────────────┘
             │ 3. 構建 Waypoint
             ↓
┌─────────────────────────────────────────────┐
│  GutterFormActivity                         │
│  buildAndFinishWithResult()                 │
│  ├─ 構建 Waypoint 對象                      │
│  └─ 準備 Intent Extra                      │
└────────────┬────────────────────────────────┘
             │ 4. 提交到 API
             ↓
┌─────────────────────────────────────────────┐
│  GutterRepository                           │
│  submitGutter(waypoint)                     │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐   │
│  │ 1. 轉換為 DitchRequest              │   │
│  │    • basicData → DitchNodeRequest   │   │
│  │    • waypoint → Ditch               │   │
│  └──────────────┬──────────────────────┘   │
│                 ↓                          │
│  ┌─────────────────────────────────────┐   │
│  │ 2. 調用 GutterApiService            │   │
│  │    api.submitDitch(request)         │   │
│  └──────────────┬──────────────────────┘   │
│                 ↓                          │
│  ┌─────────────────────────────────────┐   │
│  │ 3. 解析 Response                    │   │
│  │    • 成功: ApiResult.Success        │   │
│  │    • 失敗: ApiResult.Error          │   │
│  └──────────────┬──────────────────────┘   │
│                 ↓                          │
│  ┌─────────────────────────────────────┐   │
│  │ 4. 上傳照片 (如果需要)               │   │
│  │    uploadPhotos(photoUris)          │   │
│  └─────────────────────────────────────┘   │
└────────────┬────────────────────────────────┘
             │ 5. 回傳結果
             ↓
┌─────────────────────────────────┐
│  MainActivity                   │
│  onActivityResult()             │
│  ├─ 刷新地圖標記                │
│  ├─ 更新路點列表                │
│  └─ 顯示成功提示                │
└─────────────────────────────────┘
```

---

## 6. 草稿管理系統

```
                    用戶編輯表單
                         │
              ┌──────────┴──────────┐
              │                     │
              ↓                     ↓
         保存成功              保存失敗 / 無網絡
              │                     │
              ↓                     ↓
    ┌─────────────────┐   ┌─────────────────┐
    │ GutterSession   │   │ OfflineDraft    │
    │ (內存暫存)       │   │ (本地持久化)     │
    └────────┬────────┘   └────────┬────────┘
             │ 應用運行期間         │ 應用關閉仍保留
             │ 用戶可快速恢復       │ 下次啟動可繼續
             ↓                     ↓
    ┌────────────────────────────────────┐
    │  應用內 FloatingUI               │
    │  (PendingDraftsBottomSheet)      │
    │  (OfflineDraftsActivity)         │
    └────────┬───────────────────────────┘
             │ 用戶操作
             ├─→ 點擊: 恢復草稿
             ├─→ 長按: 刪除選項
             └─→ 確定: 上傳至 API
```

---

## 7. 權限和授權流程

```
GutterPhotosFragment
│
├─ 首次拍照
│  │
│  ├─ checkSelfPermission(CAMERA)
│  │  │
│  │  ├─→ 已授予: 直接打開相機
│  │  │
│  │  └─→ 未授予: 請求權限
│  │      │
│  │      ↓
│  │  cameraPermissionLauncher.launch()
│  │      │
│  │      ├─→ 用戶同意
│  │      │   └─→ 打開相機
│  │      │
│  │      └─→ 用戶拒絕
│  │          └─→ Toast 提示
│  │
│  ↓
│  takePictureLauncher.launch()
│  │
│  ├─→ 相機應用啟動
│  ├─→ 用戶拍照確認
│  └─→ 回調 onActivityResult()
│      └─→ photoUriSlotX = resultUri
│
├─ 照片驗證
│  │
│  └─→ validateAllPhotos()
│      │
│      ├─→ photoUriSlot1 != null ✓
│      ├─→ photoUriSlot2 != null ✓
│      ├─→ photoUriSlot3 != null ✓
│      │
│      └─→ 允許提交表單
│
└─ 提交
   │
   └─→ uploadPhotos(photoUris)
       │
       ├─→ 每個 URI 構建 MultipartBody
       └─→ GutterApiService.uploadPhotos()
```

---

## 8. 編輯流程詳細圖

```
┌──────────────────────────────────────────────────────────────┐
│                      MainActivity                            │
│                    (地圖視圖)                                 │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       │ 用戶點擊地圖上的側溝標記
                       ↓
┌──────────────────────────────────────────────────────────────┐
│              GutterInspectActivity                           │
│         (查看側溝詳細資訊)                                    │
├──────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐   │
│  │ GutterInspectBasicFragment                          │   │
│  │ GutterInspectPhotosFragment                         │   │
│  │ (只讀模式展示數據)                                   │   │
│  └──────────────┬───────────────────────────────────────┘   │
│                 │                                            │
│                 │ 用戶點擊 "編輯" 按鈕                       │
│                 ↓                                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ inspectLauncher.launch()                            │   │
│  │ (啟動 GutterFormActivity)                           │   │
│  │ ├─ EXTRA_IS_EDIT_MODE = true                        │   │
│  │ ├─ EXTRA_DATA_XY_NUM = api返回的xy_num              │   │
│  │ └─ EXTRA_SESSION_WAYPOINTS_JSON = JSON格式的數據    │   │
│  └──────────────┬───────────────────────────────────────┘   │
│                 ↓                                            │
└─────────────────┼────────────────────────────────────────────┘
                  │
                  ↓
    ┌─────────────────────────────────────────────────────┐
    │         GutterFormActivity (編輯模式)                │
    ├─────────────────────────────────────────────────────┤
    │ isEditMode = true                                  │
    │                                                     │
    │ GutterBasicInfoFragment                            │
    │ ├─ tilGutterId 隱藏                                │
    │ ├─ 其他欄位預填 (從 Intent Extra)                 │
    │ └─ etMeasureId 預填 (xy_num)                      │
    │                                                     │
    │ GutterPhotosFragment                               │
    │ ├─ 顯示現有三張照片                                │
    │ └─ 允許重新拍照替換                                │
    │                                                     │
    │ fabSubmit.setOnClickListener {                     │
    │   saveAndClose()                                   │
    │   ├─ 驗證表單 (跳過 gutterId)                      │
    │   ├─ GutterRepository.updateGutter()              │
    │   └─ setResult(RESULT_OK, Intent)                 │
    │ }                                                   │
    └──────────────┬─────────────────────────────────────┘
                   │
                   ↓
    ┌─────────────────────────────────────────────────────┐
    │         MainActivity                                 │
    │  onActivityResult()                                  │
    │  ├─ 更新 AddGutterBottomSheet 中的路點              │
    │  ├─ 刷新地圖標記                                    │
    │  └─ 顯示更新成功提示                                │
    └─────────────────────────────────────────────────────┘
```

---

## 9. 本地存儲層次

```
┌─────────────────────────────────────────────────┐
│         內存存儲 (應用運行時)                    │
├─────────────────────────────────────────────────┤
│ GutterSessionRepository                         │
│ ├─ 當前編輯的 Waypoint 列表                     │
│ ├─ 表單暫存數據                                 │
│ └─ 應用關閉後丟失                              │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│     SharedPreferences (簡單 KV 存儲)             │
├─────────────────────────────────────────────────┤
│ ├─ 用戶登入 Token                              │
│ ├─ 用戶首選項                                   │
│ └─ 應用持久化                                   │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│     Room Database (本地關聯式數據庫)             │
├─────────────────────────────────────────────────┤
│ OfflineDraftRepository                          │
│ ├─ 表: offline_drafts                          │
│ │  ├─ id (主鍵)                                │
│ │  ├─ basicData (JSON)                         │
│ │  ├─ photoUris (JSON 列表)                    │
│ │  ├─ timestamp                                │
│ │  └─ waypointName                             │
│ └─ 應用關閉仍保留                              │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│     文件系統存儲                                 │
├─────────────────────────────────────────────────┤
│ getUriForFile()                                 │
│ ├─ 應用緩存目錄 (app-specific files)           │
│ │  ├─ 臨時照片文件                             │
│ │  └─ 清除應用數據時刪除                       │
│ ├─ 應用公共目錄 (app-public files)            │
│ │  └─ 與其他應用共享的數據                     │
│ └─ 外部存儲 (External Storage)               │
│    └─ 公共相冊目錄                            │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│     遠程服務器存儲                               │
├─────────────────────────────────────────────────┤
│ GutterRepository + GutterApiService             │
│ ├─ /auth/login                                  │
│ ├─ /ditches (查詢、新增、更新)                  │
│ ├─ /ditches/{id} (詳情)                        │
│ ├─ /photos/upload (照片上傳)                    │
│ └─ 網絡可用時同步本地數據                       │
└─────────────────────────────────────────────────┘
```

---

## 10. 類結構關係圖

```
Activity 層
├── MainActivity
│   ├── extends AppCompatActivity
│   ├── implements OnMapReadyCallback
│   ├── implements AddGutterBottomSheet.LocationPickerHost
│   └── members:
│       ├─ googleMap: GoogleMap
│       ├─ sessionDraftRepository: GutterSessionRepository
│       ├─ gutterRepository: GutterRepository
│       └─ fusedLocationProviderClient: FusedLocationProviderClient
│
├── GutterFormActivity
│   ├── extends AppCompatActivity
│   ├── members:
│   │   ├─ pagerAdapter: GutterFormPagerAdapter
│   │   ├─ gutterRepository: GutterRepository
│   │   └─ offlineDraftRepository: OfflineDraftRepository
│   └── inner classes:
│       └─ GutterFormPagerAdapter
│
├── GutterInspectActivity
│   ├── extends AppCompatActivity
│   ├── members:
│   │   ├─ gutterRepository: GutterRepository
│   │   └─ ditchDetails: DitchDetails
│   └── views:
│       ├─ GutterInspectBasicFragment
│       └─ GutterInspectPhotosFragment
│
└── LoginActivity
    ├── extends AppCompatActivity
    └── members:
        └─ gutterRepository: GutterRepository

Fragment 層
├── GutterBasicInfoFragment
│   ├── extends Fragment
│   ├── companion object:
│   │   ├─ ARG_IS_EDIT_MODE
│   │   └─ ARG_DATA_XY_NUM
│   └── methods:
│       ├─ newInstance()
│       ├─ validateRequiredFields()
│       ├─ prefillData()
│       └─ getBasicData()
│
├── GutterPhotosFragment
│   ├── extends Fragment
│   ├── members:
│   │   ├─ photoUriSlot1: Uri?
│   │   ├─ photoUriSlot2: Uri?
│   │   └─ photoUriSlot3: Uri?
│   └── methods:
│       ├─ validateAllPhotos()
│       ├─ getPhotoPaths()
│       └─ createPhotoUriForSlot()
│
├── AddGutterBottomSheet
│   ├── extends BottomSheetDialogFragment
│   ├── interface LocationPickerHost
│   └── members:
│       ├─ waypoints: List<Waypoint>
│       ├─ waypointAdapter: WaypointAdapter
│       └─ sessionDraftId: Long
│
└── PendingDraftsBottomSheet
    ├── extends BottomSheetDialogFragment
    └── members:
        ├─ drafts: List<GutterSessionDraft>
        └─ draftAdapter: PendingDraftAdapter

數據模型層
├── Waypoint
│   ├── id: Long
│   ├── nodeId: String
│   ├── latLng: LatLng
│   ├── basicData: Map<String, String>
│   └─ photoUris: List<Uri>
│
├── WaypointType (enum)
│   ├─ NORMAL
│   ├─ EDITED
│   └─ PENDING
│
├── GutterSessionDraft
│   ├── id: Long
│   ├── waypoints: List<Waypoint>
│   └── timestamp: Long
│
└── OfflineDraft
    ├── id: Long
    ├── basicData: Map<String, String>
    ├── photoUris: List<String>
    ├── timestamp: Long
    └── waypointName: String

API 層
├── GutterRepository
│   ├── api: GutterApiService
│   ├── suspend fun login()
│   ├── suspend fun submitGutter()
│   ├── suspend fun updateGutter()
│   ├── suspend fun uploadPhotos()
│   └── suspend fun getDitchDetails()
│
├── GutterApiService (interface)
│   ├── @POST /auth/login
│   ├── @POST /ditches
│   ├── @PATCH /ditches/{id}
│   ├── @POST /photos/upload
│   └── @GET /ditches/{id}
│
└── GutterApiClient
    ├── Retrofit instance
    ├── OkHttp interceptors
    └── Gson converter

存儲層
├── GutterSessionRepository
│   ├── saveDraft()
│   ├── getDraft()
│   ├── getAllDrafts()
│   └── deleteDraft()
│
└── OfflineDraftRepository
    ├── saveDraft()
    ├── getDraft()
    ├── getAllDrafts()
    └── deleteDraft()
```

---

**圖解版本**: 1.0
**最後更新**: 2026-04-01
