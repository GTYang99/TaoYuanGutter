# Repository Analysis

## Current Behavior

- `GutterSessionDraft`／`DraftEntity` 目前保存草稿內容、離線旗標、類型與 waypoints，但沒有記錄是否曾進入 `storeDitch`。
- `PendingDraftAdapter` 目前只顯示標題、建立時間、節點數與箭頭；`item_pending_draft.xml` 沒有 tag 元件。
- 新增流程在 `AddGutterBottomSheet` 的 `onGutterSubmitted` 後呼叫 `storeDitch`；編輯流程在 `onGutterSubmitting` 後呼叫帶 `SPI_NUM` 的 `storeDitch`。失敗／網路關閉後才由 Host 保存草稿，無法涵蓋 API 呼叫後 App 立即結束的情況。

## Expected Behavior

- 草稿資料在兩個 `storeDitch` 呼叫邊界前先持久化 `hasSubmittedStoreDitch=true`，使失敗、逾時、無回應與重啟後仍能辨識「曾提交過上傳」。
- 未進入 API 呼叫的草稿保留 `false`，列表顯示「未提交上傳」；可辨識為既有檢視／編輯側溝的草稿則隱藏 tag。
- tag 視覺沿用 `item_waypoint.xml` 的 `tvVirtualBadge` 尺寸／排版語意，新增未填滿的邊框樣式。

## Root Cause

- 原規劃把提交狀態放在 `AddGutterBottomSheet` 的 `onGutterSubmitted`／`onGutterSubmitting` Host 回呼；這些回呼雖然位於 API 呼叫前，但實際 `GutterRepository.storeDitch(...)` 尚未進入，存在「已標記但尚未真正進入 storeDitch」的控制流程空窗。
- 新增與編輯各自從 Host 進入 repository，沒有共用且可測試的 submission boundary；也沒有明確規定 current draft id 缺失或 Room row 不存在時的處理。
- 原 plan 只列出 migration／UI 測試目標，未指定 Room 3→4 fixture、repository ordering spy 或 adapter binding assertions，因此不足以直接產出 AC-001～AC-005 的可重現證據。

## Planning Resolution

- 將 `GutterRepository.storeDitch(...)` method entry 定義為新增／編輯共用的唯一提交邊界，在 Retrofit 呼叫前同步執行 `onRequestEntered` marker callback；callback 完成後才允許發出 request。
- 有固定 draft id 的新增／恢復草稿先以目前 session snapshot ensure Room row，再標記；若 draft id、snapshot 或本機寫入缺失，回傳 local error 並停止遠端 request。直接檢視編輯若沒有 pending draft id，callback 明確 no-op，維持既有 API 流程且不建立可被列表標記的草稿。
- 以 repository ordering test、手動建立 v3 schema 的 Android migration test、以及 `PendingDraftAdapter` instrumentation assertions 補足證據缺口。

## Affected Modules

- `pending`：草稿 data class、Room entity/repository、coordinator、database migration、列表 adapter。
- `api/GutterRepository.kt`：定義共用 `storeDitch` method-entry boundary 並在 Retrofit 呼叫前通知 marker。
- `gutter`：新增／編輯把目前 draft marker callback 傳入共用 repository boundary；既有 Host loading callbacks 維持原責任。
- `MainActivity`、`MapWorkspaceFragment`：接收提交邊界通知並更新目前草稿。
- `res/layout`、`res/drawable`、`res/values`：列表 tag view、未提交 tag drawable、文案資源。
- `app/src/test`：序列化、submission ordering、狀態判定與列表 tag 規則測試。
- `app/src/androidTest`：Room migration fixture、pending adapter/layout assertions 與草稿列表流程回歸。

## Dependencies

- Room schema version 3 與既有 `MIGRATION_1_2`／`MIGRATION_2_3`。
- `GutterDraftCoordinator` 的目前 session draft id 與 `GutterSessionRepository` 的同步 API。
- `AddGutterBottomSheet.LocationPickerHost` 的新增／編輯提交生命週期。
- `tvVirtualBadge` 的現有主色、白字、padding 與文字尺寸樣式。

## Risks

- 若提交狀態只在 API 回傳失敗後才寫入，無法滿足「呼叫後逾時／閃退」的核心需求；必須在兩個實際 API 呼叫前落盤。
- Room migration、Gson Bundle 傳遞及 repository upsert 任一邊界漏欄位，都可能讓狀態重啟後遺失或舊草稿無法讀取。
- Room write 與 HTTP request 不可能跨本機／遠端形成物理 atomic transaction；本任務以進入 `GutterRepository.storeDitch` method 作為可觀測 submission boundary。
- 將既有側溝草稿誤判為一般草稿會顯示不應出現的 tag；需在 adapter 端明確以 `SPI_NUM`／既有編輯身份隱藏。
- tag 若直接塞入現有 ConstraintLayout 而未調整標題與箭頭約束，長文案可能擠壓或截斷既有內容。

## Unknowns

- 舊版草稿沒有 API 呼叫歷史，無法從本機資料可靠推導提交狀態；本計畫先以 migration default false 處理，並以既有 `SPI_NUM` 識別規則優先隱藏 tag。
- 目前沒有現成的 pending draft UI instrumentation；若既有測試環境無法穩定建立 Room 草稿，將以純 JVM 狀態／序列化測試加 targeted build，並把實機 UI 檢查列為 Verification 的必要證據。
