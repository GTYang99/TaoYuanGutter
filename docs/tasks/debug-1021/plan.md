# Implementation Plan

## Goal
- 修正新增側溝表單在必要資料/照片驗證失敗並關閉提示後，送出按鈕仍停留於 loading 狀態的問題。

## Scope
- 僅處理 AddGutterBottomSheet 新增流程的驗證失敗狀態清理，以及對應回歸驗證；保留既有驗證規則、Dialog、API 行為與編輯模式。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：新增模式驗證失敗時還原按鈕狀態。
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/Debug1021IncompleteSubmitUiTest.kt`：驗證提示 Dialog 關閉後按鈕可重試，且不送出資料。

## Implementation Steps
- 已提交的 production fix：在必要資料/照片驗證回傳 false 的路徑呼叫既有 `setSubmitLoading(false)`，再結束 coroutine。
- 保留現有合併不足案例，並補上資料單獨不足與照片單獨不足的 UI 回歸案例；每一案例檢查 Dialog、關閉後按鈕文字/enabled 狀態及零提交。

## Test Plan
- JVM：執行 `:app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest`。
- Emulator：在 API 34 AVD 分別執行資料與照片各自不足的案例，並保留兩者皆不足的既有案例。
- Build：執行 Android debug APK 與 androidTest 編譯/封裝。

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android API 34 Emulator (`DBG1017_API34_Camera`)
- In-scope Acceptance Criteria: AC-001
- Regression risk: 驗證失敗路徑只在新增模式重置 loading 狀態；須確認沒有進入照片補傳或 storeDitch。
- Full regression required: No
- Full regression trigger: 無
- Stop condition: AC-001 對話框、按鈕恢復及零提交斷言全數通過；若 Emulator 不可用，記錄 NOT VERIFIED。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | API 34 Emulator | 分別測試：A) 欄位不足、必要照片 URI 可用；B) 欄位完整、必要照片不足；另保留兩者皆不足的既有案例。各案送出並關閉 Dialog | 每案都顯示「資料/照片不足」；關閉後按鈕顯示「新增側溝」且 enabled；提交次數為 0 | 三個獨立 instrumentation 案例及 API 34 AVD 報告 |

## Regression Plan
- 確認驗證失敗後不會上傳照片或建立側溝。
- 確認新增模式按鈕可再次點擊；編輯模式既有按鈕規則及提交流程未變更。

## Risks
- 若清理呼叫放在驗證之前或失敗 return 之後，可能造成 loading 狀態殘留或錯誤提交；修正限於失敗分支並以零提交斷言覆蓋。

## Rollback Plan
- 若新增的隔離案例失敗，先調整測試 fixture 或回到 Debug 分類；若生產程式行為不符 AC，才回退/修正 `f9d033d` 的生產變更。

## Current Behavior
- 新增流程先將按鈕設為 disabled「上傳中…」；必要資料/照片不足時顯示 Dialog，而新增模式的 `updateSubmitButtonState()` 提前返回，未還原按鈕。

## Expected Behavior
- 驗證失敗時保持既有 Dialog，並在 coroutine return 前恢復「新增側溝」按鈕的文字與可點擊狀態；不執行照片補傳或側溝建立請求。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 第 1、2 步 | API 34 Emulator：資料-only、照片-only、合併不足三個 UI test 均確認 Dialog、按鈕恢復與零提交 |

## Failure Behavior
- 必填資料或照片不足時顯示現有提示；此次送出立即結束並釋放 loading 狀態，不呼叫上傳或建立 API。

## Security and Privacy
- 無資料格式、權限、憑證或隱私變更。

## Open Questions
- 無
