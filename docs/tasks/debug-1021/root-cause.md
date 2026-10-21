# Root Cause Investigation — 2026-10-21

## Classification

implementation regression / UI state cleanup defect. Priority P2: 新增表單的單一送出失敗路徑卡在 loading UI，沒有證據顯示資料遺失或建立 API 被呼叫。

## Observed and Expected Behavior

使用者回報：從 AddGutterBottomSheet 按「新增側溝」，必要資料或照片不足時顯示「資料/照片不足」Dialog；按「確定」關閉後，按鈕仍顯示「上傳中…」。

預期：驗證失敗後 Dialog 關閉，新增模式按鈕回到「新增側溝」並可再次操作。

## Root Cause

新增模式的送出 listener 在啟動 coroutine 前就將 btnSubmitGutter 設為 disabled，文字改為「上傳中…」。必要資料/照片檢查是在 coroutine 內才執行。驗證失敗時，程式呼叫 updateSubmitButtonState() 再退出 coroutine；但該函式以 editSpiNum.isEmpty() 作為首個 return 條件，所以新增模式不會重設任何狀態。驗證函式建立 Dialog 時將「確定」listener 設為 null，也沒有 Dialog dismiss 後的恢復邏輯。

因此按鈕未恢復的直接原因是驗證失敗分支呼叫了只服務編輯模式的狀態更新函式；Dialog 只是讓失敗原因可見，關閉它不會改變按鈕狀態。

## Evidence

- AddGutterBottomSheet.kt:1100-1102：在 coroutine 開始前將按鈕設為 disabled 並設文字「上傳中…」。
- AddGutterBottomSheet.kt:1106-1108：驗證 false 後呼叫 updateSubmitButtonState() 並立即 return。
- AddGutterBottomSheet.kt:1893-1895：updateSubmitButtonState() 遇到新增模式的空 editSpiNum 即 return。
- AddGutterBottomSheet.kt:2010-2018：缺少欄位或照片時建立 Dialog；「確定」沒有 callback，驗證函式回傳 false。
- AddGutterBottomSheet.kt:1110-1124：只有驗證通過後才進入照片補傳及側溝建立請求；本次不足資料路徑在此之前 return。

## Acceptance Criteria Mapping

- AC-001 — FAIL by static code-path analysis: 使用者觀察與程式路徑一致；驗證 false 後新增模式沒有把按鈕重設為 enabled 或「新增側溝」。

## Affected Files

- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt

## Regression Risk

低至中。修正應限定在新增模式驗證失敗時清除該次 loading 狀態，並保留編輯模式的 update button 狀態規則。不得移動驗證到照片補傳之後，也不得讓驗證失敗進入 storeDitch。

## Evidence Limits

此判斷依目前分支 815540aa 的靜態程式碼路徑與使用者描述；本回合沒有進行裝置重現、logcat 蒐集或測試。裝置層驗證仍待後續 implementation/verification 階段。

