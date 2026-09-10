# Repository Analysis

## Current Behavior
- 目前提交版本在勾選時顯示確認提示，並加入 snapshot／回填流程；這與最新產品決策不符，必須移除。
- 目前的 `notifyUploadHost=false` 清除照片方式可能使 Activity／Waypoint 保留舊 image ID、upload state 或 pending path。
- 圖片 HTTP 500 的 SQL 指向後端 `xy_num` schema；舊版同 URL 可上傳，因此本次規劃須排除 upload metadata 不同步，不可歸咎 URL。

## Expected Behavior
- 只在確認後清除；取消勾選維持清除狀態，不使用 Activity-scoped ViewModel、session snapshot 或 dirty merge。
- 已啟動但在確認清除後才回傳的第 2、3 張相機結果不得重新填入或觸發上傳。

## Affected Modules
- `GutterBasicInfoFragment.kt`：Alert、UI 清除與取消勾選後維持清除狀態。
- `GutterFormActivity.kt`：移除 session snapshot／token API；維持照片清除同步。
- `CantOpenSessionViewModel.kt`：移除，不再保存暫存。
- `CameraOverlayFragment.kt`：移除本任務不再需要的 token input／result contract。
- unit／androidTest：Alert、確認清除、取消後不回填、特殊模式與既有上傳回歸。

## Dependencies
- `PendingPhotoDraftState`、`PhotoUploadSlotState`、既有 draft callbacks、Material Alert、Fragment lifecycle。

## Risks
- Dialog callback、相機 late result 與 upload callback 競態。
- 不得以 `notifyUploadHost=false` 造成 UI 與 Activity／Waypoint 狀態分裂。

## Unknowns
無。第 1 張概況照保留；configuration change 同 session，process death 為新 session。
