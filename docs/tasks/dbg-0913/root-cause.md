# Root Cause

- `setupCantOpen()` 的勾選監聽器沒有檢查 `clearCantOpenFieldsAndPhotos()` 是否真的會清除非空資料，因此每次使用者勾選都建立確認 Alert。
- `handleNavigateBack()` 未呼叫既有完整性驗證；它直接走 `buildAndFinishWithResult()`，因此離開未完成表單時沒有提醒。
- 檢視→編輯→預覽已由 `handleNavigateBack()` 的早期返回分支處理，不會 finish Activity；此路徑不應視為返回節點列表。

Evidence: `GutterBasicInfoFragment.kt` 的 `setupCantOpen()`／`clearCantOpenFieldsAndPhotos()`，以及 `GutterFormActivity.kt` 的 `handleNavigateBack()`、`saveAndClose()` 與 `validateAllPhotos()` 呼叫點。
