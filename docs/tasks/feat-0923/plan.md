# Implementation Plan

## Goal

- 讓待上傳草稿可持久辨識 `storeDitch` 是否已被呼叫，並在列表以指定樣式顯示對應 tag，同時排除檢視／編輯既有側溝。

## Scope

- 更新草稿資料模型、Room migration、提交邊界落盤、待上傳列表 tag UI 與相關測試；不改 API payload、後端契約或既有上傳結果流程。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionDraft.kt`、`DraftEntity.kt`、`GutterSessionRepository.kt`：新增並保存提交狀態。
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftDatabase.kt`：增加新欄位的 Room migration。
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftCoordinator.kt`：提供在 API 呼叫前標記目前草稿已提交的持久化入口，並維持既有 auto-save 狀態。
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`：提供新增／編輯共用的 `storeDitch` method-entry boundary，於 Retrofit 呼叫前執行 marker callback。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：將目前 draft marker callback 傳入新增與編輯兩條 `storeDitch` 呼叫；保留既有 Host loading callbacks。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：實作提交邊界 callback，解析目前 draft id 並標記草稿。
- `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftAdapter.kt`：依草稿狀態與既有側溝身份決定 tag 可見性及文案／樣式。
- `app/src/main/res/layout/item_pending_draft.xml`：加入 tag view 並調整標題區約束，支援兩種長文案。
- `app/src/main/res/drawable/bg_pending_draft_unsubmitted_tag.xml`、`app/src/main/res/values/strings.xml`：新增未填滿邊框背景與 tag 文案資源。
- `app/src/test/java/com/example/taoyuangutter/pending/GutterSessionDraftTest.kt`、`app/src/test/java/com/example/taoyuangutter/pending/GutterDraftSubmissionStateTest.kt`、`app/src/test/java/com/example/taoyuangutter/api/GutterRepositoryStoreDitchBoundaryTest.kt`：覆蓋序列化、預設值、狀態 upsert、repository ordering 與既有側溝隱藏規則。
- `app/src/androidTest/java/com/example/taoyuangutter/pending/GutterDraftDatabaseMigrationTest.kt`：手動建立 v3 `gutter_session_drafts` schema，開啟 v4 database 並驗證舊 row 可讀取且新欄位為 false。
- `app/src/androidTest/java/com/example/taoyuangutter/pending/PendingDraftAdapterUiTest.kt`：直接 bind `PendingDraftAdapter`，驗證兩種 tag 的文字、visibility、background、stroke、padding、`SPI_NUM` 隱藏及 click／long-click callback。

## Implementation Steps

1. 在 `GutterSessionDraft` 與 `DraftEntity` 增加 `hasSubmittedStoreDitch`（預設 `false`），串接 repository entity mapping，並以 Room 3→4 migration 對既有資料填入 `0`。
2. 在 `GutterDraftCoordinator`／repository 增加以 draft id 更新提交狀態的操作；有固定 draft id 時以目前 session snapshot `ensure-and-mark`，先確保 row 存在再標記，且一般 auto-save 與既有欄位更新不會把已提交狀態覆寫回 `false`。若固定 draft id 或 snapshot 缺失、或本機寫入失敗，回傳可辨識的 local error 並停止送出遠端 request；不得以未落盤狀態繼續呼叫 API。
3. 將 `GutterRepository.storeDitch(request, token, onRequestEntered: suspend () -> Unit)` 定義為新增／編輯共用的唯一 submission boundary：method entry 的第一個可失敗操作就是 `onRequestEntered()`，完成後才允許執行 log／Retrofit request。`AddGutterBottomSheet` 的兩個 storeDitch call site 傳入 `LocationPickerHost.onStoreDitchRequestEntered(...)`；callback 內同步完成 draft ensure-and-mark。若直接檢視編輯沒有 pending draft id，callback 明確 no-op，維持既有 API 流程且不建立可被列表顯示的 tag draft。
4. 在待上傳列表加入 tag view，已提交套用現有 `tvVirtualBadge` 的實心主色／白字樣式，未提交套用透明底、邊框／主色字樣式；以 `SPI_NUM` 識別檢視／編輯既有側溝時隱藏 tag。
5. 保留成功上傳刪除草稿、失敗／逾時保存草稿、點擊恢復與長按刪除流程，並檢查 Gson 草稿 JSON 與 Fragment Bundle 恢復不遺失新欄位。
6. 新增狀態、序列化、Room migration 相容性、tag policy／adapter 綁定與列表回歸測試，完成 targeted JVM tests、Debug build、Android test compile；若 UI harness 可用再執行 targeted instrumentation。

## Test Plan

- `GutterSessionDraftTest`：`true`／`false` 序列化 round-trip，舊 JSON 缺欄位使用 `false`。
- Draft repository／Room migration test：3→4 後既有草稿可讀取、upsert 不遺失 `hasSubmittedStoreDitch`，標記後重讀為 `true`。
- `GutterRepositoryStoreDitchBoundaryTest`：以 fake `GutterApiService` 記錄 `onRequestEntered` 與 API invocation 順序；驗證 callback 先於 API、callback 失敗時 API 不被呼叫、API error／exception／未回應後仍保留 true，且 validation 未進入 repository 時維持 false。
- `GutterDraftSubmissionStateTest`：驗證固定 draft id 的 ensure-and-mark、missing row 建立、upsert 不遺失 `hasSubmittedStoreDitch`、auto-save 不會將 true 覆寫回 false，以及 direct inspect-edit 無 draft id 的 no-op 政策。
- `GutterDraftDatabaseMigrationTest`：手動建立 version-3 schema／row，使用 `MIGRATION_3_4` 開啟 version-4 database，驗證 legacy row 讀取與 false default。
- `PendingDraftAdapterUiTest`：直接 bind true／false／含 `SPI_NUM` fixtures，精確驗證文字、visibility、背景／stroke／padding 及 click／long-click 不回歸。
- 執行 `./gradlew :app:testDebugUnitTest`、`./gradlew :app:assembleDebug`、`./gradlew :app:compileDebugAndroidTestKotlin`；可用環境再跑 targeted connected test。

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android 9+ 實體裝置或現有可用的 Android instrumentation 裝置；需能啟動草稿列表並完成新增／編輯草稿流程。
- In-scope Acceptance Criteria: AC-001、AC-002、AC-003、AC-004
- Regression risk: API 呼叫前落盤時機、App 重啟後狀態保留、既有側溝 tag 隱藏、草稿恢復／刪除／成功清除。
- Full regression required: No
- Full regression trigger: 提交狀態在重啟後遺失、既有側溝誤顯示 tag、或草稿恢復／成功刪除失敗；其餘為 `無`。
- Stop condition: 所有指定案例完成並取得結果，或取得足夠失敗證據可分類問題。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Device + local draft | 建立可恢復草稿，確認流程已進入 `GutterRepository.storeDitch` 後在回應前／逾時後回到草稿列表 | 顯示「曾提交過上傳」，重啟後仍相同 | Result; screenshot on failure |
| AC-002 | Device | 建立草稿，在進入 `GutterRepository.storeDitch` 前退出或模擬呼叫前中斷，再開啟列表 | 顯示「未提交上傳」 | Result; screenshot on failure |
| AC-003 | Device | 檢查兩種一般草稿的 tag | 已提交為實心主色／白字；未提交為透明底／邊框／主色字 | Result; screenshot on failure |
| AC-004 | Device | 從檢視進入既有側溝編輯並觸發草稿列表；另測點擊恢復、長按刪除與成功上傳清除 | 既有側溝不顯示 tag；原流程行為不變 | Result; screenshot on failure |

## Regression Plan

- 驗證舊版草稿可 migration、列表載入、點擊恢復與資料內容不變。
- 驗證離線草稿、弧線草稿、未送出新增草稿仍可編輯／刪除，且不因 tag 改動影響標題、時間、節點數與箭頭。
- 驗證 storeDitch 成功後既有草稿刪除與本機照片清理行為不變。
- 驗證 API 失敗、401、網路關閉與逾時仍依既有流程保存草稿；只有已進入 `GutterRepository.storeDitch` boundary 的資料標記為曾提交。

## Risks

- Room migration 與同步 repository 更新是資料正確性的主要風險。
- 新增／編輯兩個 API 呼叫點若漏掉任一個，會造成 tag 誤判。
- 舊草稿的歷史狀態不可追溯；本計畫採 false 相容預設並記錄為待 Plan Review 確認。

## Rollback Plan

- 若驗證失敗，還原本任務的提交狀態欄位、migration、呼叫邊界與 tag UI 變更；不回退工作區既存的 `gradle/libs.versions.toml` 修改。

## Current Behavior

- 草稿沒有持久化的 `storeDitch` 提交狀態，列表也沒有狀態 tag。
- 失敗／網路關閉時才保存草稿；UI callback 早於 repository method entry，無法精確涵蓋「已進入 `storeDitch` 但 Retrofit 尚未回呼」的情況。

## Expected Behavior

- 進入 `GutterRepository.storeDitch` method entry 後，先完成提交狀態落盤，再開始 Retrofit request；列表可在失敗、逾時、無回應、重啟後正確顯示對應 tag。
- 若尚未進入 repository boundary 就結束，狀態維持未提交；若 boundary 的本機落盤失敗，停止遠端 request 並回傳 local error，避免出現無法證明的誤標記。
- 檢視／編輯既有側溝的草稿不顯示 tag，其他草稿的既有操作與成功清除行為不變。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–3 | `GutterRepositoryStoreDitchBoundaryTest` ordering／error tests、裝置 API 失敗／逾時與重啟流程 |
| AC-002 | 1–3 | validation-before-boundary test、未提交 draft fixture 與裝置恢復流程 |
| AC-003 | 4 | `PendingDraftAdapterUiTest` text／visibility／background／stroke／padding assertions |
| AC-004 | 4–5 | `SPI_NUM` fixture、adapter click／long-click assertions、草稿恢復／刪除／成功清除回歸 |
| AC-005 | 1–2、5 | `GutterDraftDatabaseMigrationTest`、Gson missing-field test 與列表恢復檢查 |

## Failure Behavior

- `storeDitch` 回傳錯誤、網路失敗或逾時時，保留既有草稿並保留已提交 tag；重試沿用已提交狀態。
- App 在進入 `GutterRepository.storeDitch` 前結束時，若已有草稿 auto-save，提交狀態仍為未提交；method entry 的 marker callback 完成後，即使 Retrofit 尚未發出或尚未回呼，狀態仍為已提交。
- 固定 draft id 但找不到 row、session snapshot 不足或本機 marker 寫入失敗時，不送出 Retrofit request，回傳 local error；direct inspect-edit 無 draft id 則依既有流程送出但不建立 tag draft。
- 舊資料缺少新欄位時使用 false 相容預設；若草稿資料解析或 migration 失敗，沿用既有錯誤處理並不得假稱已提交。

## Security and Privacy

- 不新增權限、憑證或個資；只在本機 Room 草稿保存一個流程狀態 Boolean，不改變 API request／response。

## Open Questions

- 舊版草稿缺少提交歷史，是否統一顯示「未提交上傳」由 Plan Review 確認；目前採 false 預設，且含 `SPI_NUM` 的既有側溝優先隱藏 tag。
