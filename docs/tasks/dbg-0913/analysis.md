# Repository Analysis

## Current Behavior
- `GutterBasicInfoFragment.setupCantOpen()` 對使用者勾選一律先取消勾選並顯示確認 Alert，之後才清除欄位和照片。
- `GutterFormActivity.handleNavigateBack()` 會直接同步草稿並結束 Activity；其檢視→編輯特殊分支則只回到 Activity 內的預覽。
- 完成送出時的基本資料與照片驗證已分別由 `validateRequiredFields()` 和 `validateAllPhotos()` 提供。

## Expected Behavior
- 僅在無法開蓋切換實際會丟失資料時請求確認。
- 實際離開新增／編輯 Activity 前，復用既有驗證結果決定是否顯示通知型 Alert，確認後仍返回與保存草稿。

## Affected Modules
- `gutter/GutterBasicInfoFragment.kt`：無法開蓋資料存在判斷與 Alert 時機。
- `gutter/GutterFormActivity.kt`：離開表單的完整性檢查與單鍵 Alert。
- `gutter` unit/UI tests：條件判斷與可見互動回歸。

## Dependencies
- Material Alert Dialog、現有表單與照片驗證、`GutterSessionRepository` 草稿同步、Activity result 回傳。

## Risks
- 不能以送出驗證的 Toast／阻擋行為取代返回流程：使用者確認 Alert 後必須仍保存草稿並返回。
- 無法開蓋判斷須涵蓋實際清除的六個欄位群組與第 2、3 張照片，不能把 slot 1 當成清除目標。
- 檢視→編輯→預覽分支不得誤套用離開 Alert。

## Unknowns
- 無

## Follow-up Validation Gap (2026-09-14)
- `handleNavigateBack()` 的未完成 Alert 正確呼叫 `validateRequiredFields()`，但該共用驗證未檢查畫面已標示必填的 `NODE_TYP`、`NODE_X`、`NODE_Y`、`XY_NUM`、`COVER_DEP`。
- 此外，既有 `isUOpen || isCantOpen` 早期返回使明溝跳過所有細節欄位，與確認後的產品規則「明溝僅厚度免填」不一致。
- 因此問題位於共用驗證規則，而非返回 Alert 的觸發路徑；修正共用規則可同時覆蓋上一頁提醒與送出驗證。
