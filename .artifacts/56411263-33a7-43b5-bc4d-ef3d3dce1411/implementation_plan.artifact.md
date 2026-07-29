# 手動控制模擬參數計畫

將 `ENABLE_GROUP_SIMULATION` 從 `BuildConfig.DEBUG` 解耦，改為手動控制，並統一全域引用。

## 擬定變更

### [API 控制層]

#### [MODIFY] [GutterApiService.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt)
- 將 `GutterApiClient.ENABLE_GROUP_SIMULATION` 的初始值由 `BuildConfig.DEBUG` 改為 `true` (手動控制)。

### [UI 介面層]

#### [MODIFY] [AddGutterBottomSheet.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt)
- 將兩處 `btnSubmitGutter.setOnLongClickListener` 中的 `BuildConfig.DEBUG` 判斷改為引用 `GutterApiClient.ENABLE_GROUP_SIMULATION`。

## 驗證計畫

### 自動化檢查
- 使用 `grep` 確認專案中是否還有遺漏的 `BuildConfig.DEBUG` 應改為 `ENABLE_GROUP_SIMULATION` 的地方（僅限於模擬測試邏輯）。

### 手動驗證
- 說明如何透過修改該值來開啟或關閉測試功能。
