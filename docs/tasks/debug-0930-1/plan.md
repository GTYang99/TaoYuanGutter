# Implementation Plan

## Goal
- 在 debug build 登入頁提供受 `ENABLE_GROUP_SIMULATION` 控制的隱藏 API 環境選擇器，讓測試人員可切換 base、Taipei、DEMO。

## Scope
- 只切換 Retrofit API target；地圖 WMS/WMTS 保持既有 Taipei URL。環境選擇只存在目前 App process，重啟後回到 Taipei。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/common/BackendEndpoints.kt`：集中定義 base、Taipei、DEMO API URL 與 target mapping。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`：提供 process-local selected target 與依 target 取得 Retrofit service；保留既有 TLS client 行為。
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`：在 request 執行時取得目前 service，避免 constructor 捕獲舊 target。
- `app/src/main/java/com/example/taoyuangutter/login/LoginActivity.kt`、`app/src/main/res/layout/activity_login.xml`：新增登入頁入口並依 gate 控制可見性與互動。
- `app/src/main/res/values/strings.xml`：選擇器標題及三個環境顯示名稱。
- `app/src/test/java/com/example/taoyuangutter/common/BackendEndpointsTest.kt`、`app/src/test/java/com/example/taoyuangutter/api/GutterApiClientEnvironmentTest.kt`、login UI verification：驗證 URL mapping、service target 與 gate。

## Implementation Steps
- 建立三種具名 API target，URL 分別為 `http://192.168.10.84/TY_RSGDBIP/`、`https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`、`https://demo.srgeo.com.tw/TY_RSGDBIP_BK/`；預設為 Taipei。
- 將 `GutterApiClient.instance` 改為依 process-local selected target 回傳對應 Retrofit service，選擇變更只影響後續建立的 request，不做自動 fallback。
- 調整 `GutterRepository`，讓新 request 取得目前 selected service；環境選擇發生於登入頁，登入後不提供切換入口。
- 在 login page 加入環境按鈕與選擇對話框，只有 `ENABLE_GROUP_SIMULATION == true` 才顯示；確認選擇後更新 client，並顯示目前 target。
- 新增 endpoint mapping、預設值、切換後 repository 路由及 release/debug gate 的測試。

## Test Plan
- 執行 `BackendEndpointsTest` 與 GutterApiClient/repository endpoint selection 的 targeted unit tests。
- 執行 login UI instrumentation，確認 debug gate 開啟時 selector 可見並可切換，gate 關閉時 selector 不存在。
- 執行 `./gradlew :app:assembleDebug`、相關單元測試及 `git diff --check`。
- 不發出真實 backend write；以 target-to-URL mapping 和不同 target service identity 驗證選擇。

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android emulator instrumentation only
- In-scope Acceptance Criteria: AC-001、AC-002、AC-003、AC-005
- Regression risk: 登入頁控制項可見性、API client target 更新
- Full regression required: No
- Full regression trigger: 無
- Stop condition: selector gating 與 mock request host mapping 均完成驗證，或取得足夠失敗證據

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Emulator | Debug build 開啟登入頁 | 隱藏測試入口可用 | Instrumentation assertion |
| AC-002 | JVM/UI test | 將 gate 設為 false | 登入頁不顯示且不能觸發 selector | Unit/instrumentation result |
| AC-003 | JVM target/service mapping tests | 依序選取 base/Taipei/DEMO 後送 API request | Request URL host/path 與選項一致 | Approved URL mapping and selected service identity |
| AC-004 | JVM target/service mapping tests | 讓所選 server 回傳錯誤 | 不向其他環境發出 retry | Source review confirms no cross-host retry |
| AC-005 | Release variant source/build check | Build release and inspect gate | 測試入口關閉 | Release build/test result |

## Regression Plan
- 確認預設 target 為 Taipei，未選擇時既有 API route 不變。
- 確認 DEMO 與 base 都不會被用作自動 fallback。
- 確認切換只影響 API，WMS/WMTS 還是既有 Taipei URLs。
- 確認登入後地圖及表單流程不提供環境切換入口。
- 確認 release build 不提供 selector。

## Risks
- `base` 是 HTTP 內網 IP；設備須能到達該網段，且此 API target 的 path 與目前 Retrofit service contract 必須相容。
- API environment switch 可能改變讀寫資料目的地，選錯 target 有跨環境寫入風險。
- 修改 API service provider 若有遺漏 consumer，部分 request 可能仍使用舊 service。

## Rollback Plan
- 回退本任務 commit 即恢復目前固定 Taipei Retrofit service 與登入頁 UI。

## Current Behavior
- `GutterApiClient` lazy singleton 固定建立 Taipei Retrofit service；`BackendEndpoints` 的 DEMO 目前未被選用，base endpoint 不在目前 constants 中。
- debug simulation flag 目前只控制既有 failure simulator，沒有 endpoint selector。

## Expected Behavior
- Debug build 的登入頁提供隱藏環境 selector，能讓後續 API request 使用所選 base、Taipei 或 DEMO target。
- release build 不顯示 selector；App 重啟後回到 Taipei；不改 WMS/WMTS，也不做跨站 fallback。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 步驟 4 | debug emulator instrumentation |
| AC-002 | 步驟 4 | gate false unit/UI test |
| AC-003 | 步驟 1–3 | mock request URL tests |
| AC-004 | 步驟 2 | mock server failure/no-fallback test |
| AC-005 | 步驟 4–5 | release variant gate assertion/build |

## Failure Behavior
- 所選環境不可達時沿用既有 API error handling；明確回報該 request 失敗，不改打其他環境。

## Security and Privacy
- 不新增憑證或記錄 token。切換可改變資料讀寫目的地，UI 顯示目前環境名稱，測試人員需能辨認 target。

## Open Questions
- 無
