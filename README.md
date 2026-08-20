# 桃園側溝 - Android GutterForm 應用程式

這是一個以 Android Kotlin 開發的側溝巡查與管理應用，供現場人員在地圖上標記側溝位置、填寫基本資料、拍攝照片，並把結構化資料與影像送往後端。

如果你是 Codex 或 Gemini，先看這份 README 取得全貌，再看 [開發日誌.md](/Users/a10362/AndroidStudioProjects/TaoYuanGutter/開發日誌.md) 了解最近的修改紀錄與設計決策。

## 專案重點

- 地圖選點與側溝標記
- 分頁式表單填寫
- CameraX 拍照與方向處理
- 結構化資料上傳與照片上傳
- 草稿暫存與恢復編輯
- 檢視模式與編輯模式切換

## 技術棧

- Android Kotlin
- Google Maps SDK
- CameraX
- Retrofit + OkHttp + Gson
- Room SQLite
- ViewPager2
- ViewBinding

## 核心流程

1. 在地圖上選點或開啟既有側溝
2. 進入 `AddGutterBottomSheet` 或 `GutterFormActivity`
3. 在 `GutterBasicInfoFragment` 填寫基本資料
4. 在 `GutterPhotosFragment` 拍攝或補拍照片
5. 送出 `storeDitch()` 或 `submitGutter()`
6. 再逐張上傳照片
7. 必要時把 `nodeId`、`spiNum` 等識別資訊回寫到本地狀態

## 核心資料

### `Waypoint`

`Waypoint` 是地圖與表單之間的橋接資料結構，會同時承載：

- `latLng`：經緯度座標
- `waypointType`：起點、節點、終點
- `basicData`：表單欄位
- `photoUris`：照片 URI
- `nodeId`：API 回傳的節點識別

### 常見 `basicData` 欄位

- `NODE_TYP`：側溝類型
- `MAT_TYP`：材質
- `XY_NUM`：測量座標編號
- `IS_CANTOPEN`：是否無法開蓋
- `NODE_DEP`：深度
- `NODE_WID`：寬度
- `IS_BROKEN`：破損
- `IS_HANGING`：懸掛
- `IS_SILT`：淤積

## 模組職責

- `MainActivity`：地圖入口、位置追蹤、線段與模式管理
- `AddGutterBottomSheet`：新增與編輯路點的浮動表單
- `GutterFormActivity`：表單容器與驗證中心
- `GutterBasicInfoFragment`：基本資料頁
- `GutterPhotosFragment`：照片頁
- `GutterInspectActivity`：既有側溝的唯讀檢視
- `GutterRepository`：遠端 API 通訊
- `GutterSessionRepository`：草稿暫存與 Room 持久化
- `DistanceMeasureManager`：測距虛線與粗度控制
- `CameraOverlayFragment`：表單內相機拍攝流程

## 模式說明

### 新增模式

- 側溝編號欄位可見
- 必填驗證完整啟用
- 送出後建立新的 `spiNum`

### 編輯模式

- 側溝編號欄位隱藏
- `XY_NUM` 會自動帶入
- `gutterId` 驗證會跳過
- 完成後會重新整理地圖線段

### 檢視模式

- 只讀顯示既有資料
- 可進一步進入編輯
- 會優先載入最新的點位詳情，而不是只依賴地圖上現有座標

## 上傳流程

### 1. 結構化資料

主要 API：

```text
POST /api/v1/ditch/storeDitch
```

這一步會把 `List<Waypoint>` 轉成 `StoreDitchRequest`。

- `NODE_ATT = 1`：起點
- `NODE_ATT = 2`：節點
- `NODE_ATT = 3`：終點

如果 `IS_CANTOPEN = true`，深度與寬度等欄位會自動清空。

### 2. 照片上傳

照片會再逐張上傳，使用 `node_id` 和 `fileCategory` 關聯到對應點位。

## 相機重點

- 使用 CameraX
- 預覽與最終 JPEG 分開看待
- 透過 `OrientationEventListener` 與 `targetRotation` 處理方向
- 不重新編碼照片，以保留 EXIF 方向資訊
- 橫向拍攝相關調整已整理完成

## 介面調整

目前已整理的互動優化包含：

- `IS_BROKEN`、`IS_HANGING`、`IS_SILT` 的選項表現改得更直觀
- 二選項優先採水平 `RadioGroup`
- 三到四選項採垂直排列
- 目標是減少下拉選單的額外點擊與判讀成本

## 錯誤提示

上傳失敗已整理成可讀的分類，避免把原始 log 直接丟給使用者。

常見分類包含：

- 網路連線失敗
- 側溝資料上傳失敗
- 照片處理失敗
- 照片上傳失敗
- 伺服器回應逾時
- 未知錯誤

## 開發環境

- Min SDK: 24
- Target SDK: 36
- Compile SDK: 36
- ViewBinding: 已啟用
- Google Maps API Key：請設定在 `local.properties` 或 `secrets.properties`

## 建議閱讀順序

1. [開發日誌.md](/Users/a10362/AndroidStudioProjects/TaoYuanGutter/開發日誌.md)
2. `MainActivity.kt`
3. `GutterFormActivity.kt`
4. `AddGutterBottomSheet.kt`
5. `GutterRepository.kt`
6. `CameraOverlayFragment.kt`

## 備註

- `README.md` 保留作為快速入口
- 所有修改紀錄、架構說明與流程細節請以 [開發日誌.md](/Users/a10362/AndroidStudioProjects/TaoYuanGutter/開發日誌.md) 為準
