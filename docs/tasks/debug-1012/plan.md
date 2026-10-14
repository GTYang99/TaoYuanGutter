# Implementation Plan

## Goal
- 在既有側溝編輯上傳失敗時結束 blocking loading，修正 403 後選擇存入草稿仍持續轉圈的路徑。

## Scope
- 平衡 AddGutterBottomSheet 編輯 storeDitch 的 API error 與一般 exception loading callback。
- 加入可注入 fake repository 的測試接點與 Android instrumentation regression test，以在模擬器控制 403 並驗證 retry/save-draft 路徑。
- 不修改 API 分類或既有草稿資料儲存策略。

## Affected Files
- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt — 失敗時通知 host 停止 loading，並更新 callback 說明。
- app/src/androidTest/java/com/example/taoyuangutter/gutter/Debug1012UploadFailureInstrumentedTest.kt — 在 fake HTTP 403 下驗證 spinner 收尾、存草稿與重傳路徑。

## Implementation Steps
- 在 edit ApiResult.Error 與一般 Exception 分支於顯示錯誤對話框前呼叫 onGutterSubmitFailed。
- 在 AddGutterBottomSheet 提供僅供 instrumentation 注入 fake GutterRepository 的 internal test seam，其他執行仍建立正式 repository。
- 使用 instrumentation host 與 fake GutterApiService 回傳 403；host 以 MainBlockingUiController 驅動實際 overlay/spinner，驗證錯誤、存入草稿與重傳路徑。
- Source review 確認一般非取消例外分支在顯示既有錯誤 UI 前結束 loading。
- 更新 execution-report.md 與 state.yaml 記錄 emulator/build/test 證據。

## Test Plan
- 執行 ./gradlew :app:testDebugUnitTest。
- 執行 ./gradlew :app:connectedDebugAndroidTest，目標限定 Medium_Phone 模擬器；新增 regression test 驗證 HTTP 403 下 AC-001 至 AC-003。
- 執行 ./gradlew :app:assembleDebug，記錄 build 結果與 APK 路徑。
- Physical device testing is not requested; connected instrumentation is run on the Android emulator.

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android emulator Medium_Phone, app variant debug
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003, AC-004
- Regression risk: error callback 在 dialog 前清除 shared loading；retry 必須重新開啟 loading。
- Full regression required: No
- Full regression trigger: 無
- Stop condition: 新增 403 regression cases 與指定 module validation 完成，或取得足夠失敗證據。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Medium_Phone Android emulator | 注入 403 edit storeDitch response | 錯誤對話框顯示，blocking overlay/spinner 已停止 | Instrumentation assertion |
| AC-002 | Medium_Phone Android emulator | 在失敗對話框按「存入草稿」 | sheet 依 host 預設流程關閉，spinner 維持停止 | Instrumentation assertion |
| AC-003 | Medium_Phone Android emulator | 在失敗對話框按「重傳」，第二次仍回 403 | 第二次請求進入 loading，失敗後再次停止 loading | Instrumentation request/callback assertions |
| AC-004 | Source review | Trace edit submit generic exception catch branch | host loading is cleared before the existing failure dialog | Diff/source evidence |

## Regression Plan
- 驗證編輯 403 的「存入草稿」選項不再留下 blocking overlay。
- 驗證重傳重新發出 storeDitch 並重新顯示 loading，第二次失敗也平衡 loading。
- 確認 create-new-gutter failure handler、403 classifier 與 API request contract 未變。

## Risks
- 清理 callback 若漏在某個 edit failure branch，該分支仍可能留下 loading；instrumentation fake response 覆蓋 API error，source review 覆蓋 exception branch。
- Instrumentation test seam 必須保持 internal 且預設使用正式 repository。

## Rollback Plan
- 回退本 task commit，恢復錯誤 callback 呼叫與測試接點的原狀。

## Current Behavior
- Edit submit 開啟 host loading 後，403 的 ApiResult.Error 與一般 Exception 分支未呼叫 onGutterSubmitFailed。
- MainBlockingUiController 因 inspect loading 持續為 true 而維持 overlay 與 spinner。

## Expected Behavior
- storeDitch edit 失敗結果在錯誤對話框顯示前清除 host loading。
- Retry 重新顯示 loading；選擇存入草稿後 spinner 保持停止。
- 既有 host draft handling 與 API 互動維持不變。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Step 1 | Instrumentation HTTP 403 case asserts hidden overlay and stopped progress |
| AC-002 | Step 1, Step 3 | Instrumentation selects save-draft action and asserts sheet/overlay state |
| AC-003 | Step 1, Step 3 | Instrumentation retries, checks request count and loading callback sequence |
| AC-004 | Step 1 | Source review confirms cleanup before the exception dialog |

## Failure Behavior
- Cancellation continues to propagate without being treated as a submission failure.
- API errors and non-cancellation exceptions clear host loading before their current error UI is shown.

## Security and Privacy
- No credentials, personal data, or production endpoints are added to test evidence; instrumentation uses an in-process fake API service.

## Open Questions
- Single-gutter draft creation when no current/matching draft exists remains outside this spinner fix and requires a separate product requirement.
