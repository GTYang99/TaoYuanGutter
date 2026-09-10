# Repository Analysis

## Current Behavior
- `GutterBasicInfoFragment.setupCantOpen()` 目前勾選即清除欄位與第 2、3 張照片，取消不回填。
- 未提交版本曾加入 snapshot／回填，但以 `notifyUploadHost=false` 清除照片，可能使 Activity／Waypoint 保留舊 image ID、upload state 或 pending path。
- 圖片 HTTP 500 的 SQL 指向後端 `xy_num` schema；舊版同 URL 可上傳，因此本次規劃須排除 upload metadata 不同步，不可歸咎 URL。

## Expected Behavior
- 用 Activity-scoped、非 SavedState 的 ViewModel 持有 session snapshot，完整處理 field、photo metadata、pending capture 與 late result。
- 只在確認後清除；取消勾選用 field／slot dirty merge 回填，且不把 snapshot 序列化。

## Affected Modules
- `GutterBasicInfoFragment.kt`：Alert、UI 清除／回填協作。
- `GutterFormActivity.kt`：session owner API、currentFormData 與批次同步。
- `CantOpenSessionViewModel.kt`：新增，非持久化 snapshot／dirty／token state。
- `CameraOverlayFragment.kt`：新增 token input／result contract。
- unit／androidTest：snapshot merge、Alert、草稿隔離與 late result。

## Dependencies
- `PendingPhotoDraftState`、`PhotoUploadSlotState`、既有 draft callbacks、Material Alert、Fragment lifecycle。

## Risks
- Activity recreation、Dialog callback、相機 late result 與 upload callback 競態。
- 暫存 restore 若只處理 URI，會遺漏 capturedAt、imgId、state、error、pending path。
- 不得以 `notifyUploadHost=false` 造成 UI 與 Activity／Waypoint 狀態分裂。

## Unknowns
無。第 1 張概況照保留；configuration change 同 session，process death 為新 session。
