# Root Cause

- `setupCantOpen()` 的勾選監聽器沒有檢查 `clearCantOpenFieldsAndPhotos()` 是否真的會清除非空資料，因此每次使用者勾選都建立確認 Alert。
- `handleNavigateBack()` 未呼叫既有完整性驗證；它直接走 `buildAndFinishWithResult()`，因此離開未完成表單時沒有提醒。
- 檢視→編輯→預覽已由 `handleNavigateBack()` 的早期返回分支處理，不會 finish Activity；此路徑不應視為返回節點列表。

Evidence: `GutterBasicInfoFragment.kt` 的 `setupCantOpen()`／`clearCantOpenFieldsAndPhotos()`，以及 `GutterFormActivity.kt` 的 `handleNavigateBack()`、`saveAndClose()` 與 `validateAllPhotos()` 呼叫點。

## Follow-up Regression: ISS-DBG-0913-002

- 新建表單在 `prefillData()` 無資料分支預選「溝體結構受損＝否」與「淤積程度＝無」。
- 第一版的 `hasCantOpenContentToClear()` 將任何 RadioGroup 選取都視為使用者已填資料，因此把上述預設值誤判為應確認的清除內容。
- 第一次確認後，既有清除流程移除兩個預設選項；下一次點擊是取消勾選「無法開蓋」，本來不會顯示 Alert。
- 進一步測試也確認：當新建點位預設為明溝，`applyGutterTypeUi()` 強制把溝蓋板厚度顯示為 `0`；這同樣是 UI 衍生值而非使用者填寫，必須排除。
