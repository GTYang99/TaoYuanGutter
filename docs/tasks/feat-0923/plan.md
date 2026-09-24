# Implementation Plan

## Goal

- 讓待上傳草稿可持久辨識 `storeDitch` 是否已被呼叫，並在列表以指定樣式顯示對應 tag；一般已提交草稿恢復後進入唯讀表單，只能整份重新上傳；具有效 `SPI_NUM` 的既有側溝草稿顯示「既有側溝編輯中」並維持原有可編輯／重新送出流程。

## Scope

- 更新草稿資料模型、Room migration、提交邊界落盤、待上傳列表三種 tag UI、一般已提交草稿恢復唯讀／遮罩／重新上傳流程、既有側溝編輯 tag 與相關測試；不改 API payload、後端契約或既有上傳結果流程。

## Existing Baseline

- Previous implementation baseline `692991c` already contains the persisted submission flag, Room 3→4 migration, shared `storeDitch` boundary, pending-list tags, and their existing tests.
- This re-plan preserves that baseline and adds the submitted-draft read-only／overlay／full re-upload behavior plus the existing-gutter editing tag policy; the prior implementation commit remains the baseline and the new tag behavior is pending implementation. It also preserves unrelated working-tree changes in `GutterApiService.kt`、`AddGutterBottomSheet.kt`、`gradle/libs.versions.toml` and `.worktrees/`.

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionDraft.kt`、`DraftEntity.kt`、`GutterSessionRepository.kt`：新增並保存提交狀態。
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftDatabase.kt`：增加新欄位的 Room migration。
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftCoordinator.kt`：提供在 API 呼叫前標記目前草稿已提交的持久化入口，並維持既有 auto-save 狀態。
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`：提供新增／編輯共用的 `storeDitch` method-entry boundary，於 Retrofit 呼叫前執行 marker callback。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：將目前 draft marker callback 傳入新增與編輯兩條 `storeDitch` 呼叫；保留既有 Host loading callbacks。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterSessionFlowCoordinator.kt`、`AddGutterBottomSheet.kt`、`GutterFormNavigator.kt`：沿 `ResumeMapSheet` → BottomSheet → `LocationPickerHost` → Navigator → Activity 的完整恢復鏈路傳遞 `submittedDraftReadOnly` 狀態與 `draftId`；submitted 完整資料只在 Room reread 後於 process memory 建立。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`、`GutterFormPagerAdapter.kt`、`GutterBasicInfoFragment.kt`、`GutterPhotosFragment.kt`：鎖定已提交草稿的所有可編輯內容，保留返回與完整重新上傳入口。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：實作提交邊界 callback，解析目前 draft id 並標記草稿；接收 submitted read-only return 時只恢復 BottomSheet，不合併表單結果。
- `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftTagPolicy.kt`：新增 `EXISTING_GUTTER` kind，定義共用 `hasValidSpiNum` predicate 與唯一 tag precedence；valid `SPI_NUM` 時不得回傳一般 submitted／unsubmitted kind。
- `app/src/main/java/com/example/taoyuangutter/gutter/SubmittedDraftResumePolicy.kt`：改用 `PendingDraftTagPolicy.hasValidSpiNum`，確保同一 draft 的 tag identity 與 submitted read-only 判定一致。
- `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftAdapter.kt`、`PendingDraftsBottomSheet.kt`：重用共用 predicate，分別負責唯一 tag rendering 與標題／刪除身份判定。
- `app/src/main/res/layout/item_pending_draft.xml`、`app/src/main/res/values/strings.xml`：支援「既有側溝編輯中」長文案，沿用未提交 tag 的透明底／主色邊框／主色文字與既有單行排版。
- `app/src/main/res/layout/bottom_sheet_add_gutter.xml`、`app/src/main/res/layout/activity_gutter_form.xml`：在不覆蓋返回／重新上傳的前提下提供可編輯區域遮罩與唯讀狀態呈現。
- `app/src/main/res/drawable/bg_pending_draft_unsubmitted_tag.xml`、`app/src/main/res/values/strings.xml`：新增未填滿邊框、三種 tag／唯讀提示與「重新上傳」文案資源，沿用既有 overlay 視覺規範。
- `app/src/test/java/com/example/taoyuangutter/pending/GutterSessionDraftTest.kt`、`app/src/test/java/com/example/taoyuangutter/pending/GutterDraftSubmissionStateTest.kt`、`app/src/test/java/com/example/taoyuangutter/api/GutterRepositoryStoreDitchBoundaryTest.kt`：覆蓋序列化、預設值、狀態 upsert、repository ordering 與 `SPI_NUM` 狀態優先規則。
- `app/src/test/java/com/example/taoyuangutter/pending/PendingDraftTagPolicyTest.kt`：驗證共用 valid-`SPI_NUM` predicate、三種 tag precedence 與所有身份邊界。
- `app/src/test/java/com/example/taoyuangutter/gutter/SubmittedDraftResumePolicyTest.kt`、`SubmittedDraftRequestProjectionTest.kt`：驗證 resume policy 重用共用 predicate、與 tag kind 不分歧，以及不依賴 Android runtime 的 snapshot／mapper projection。
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/GutterFormNavigatorInstrumentedTest.kt`、`MainActivityResumeFlowInstrumentedTest.kt`、`MapWorkspaceResumeFlowInstrumentedTest.kt`：驗證 submitted read-only flag 從兩個 Host 入口傳入表單 Intent、Activity recreation 與 result handling，並覆蓋 `false` 與 `SPI_NUM` 負向分支。
- `app/src/androidTest/java/com/example/taoyuangutter/pending/GutterDraftDatabaseMigrationTest.kt`：手動建立 v3 `gutter_session_drafts` schema，開啟 v4 database 並驗證舊 row 可讀取且新欄位為 false。
- `app/src/androidTest/java/com/example/taoyuangutter/pending/PendingDraftAdapterUiTest.kt`：直接 bind `PendingDraftAdapter`，驗證三種 tag 的文字、visibility、background、stroke、padding、`SPI_NUM` 優先且不重複，以及 click／long-click callback。
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/SubmittedDraftReadOnlyUiTest.kt`：驗證已提交草稿恢復後的遮罩、所有編輯控制鎖定、返回／重新上傳可操作、未提交草稿仍可編輯，以及 Activity recreation 後狀態仍保留。
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/SubmittedDraftRetryFlowTest.kt`：驗證 Room reread 後 immutable snapshot 產生的 storeDitch request projection 與 photo-upload projection、成功清除與失敗／逾時／中斷後可再次上傳且不改寫來源。
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/SubmittedDraftNoWriteFlowTest.kt`：驗證 lifecycle、返回、取消結果、Host result handling 與 photo／import／virtual listener 均不寫回 submitted draft。
- `app/src/androidTest/java/com/example/taoyuangutter/pending/ExistingGutterEditDraftFlowTest.kt`：驗證有效 `SPI_NUM` 草稿顯示唯一 tag、恢復後仍可修改、失敗／逾時／中斷後仍可重送、成功沿用清除流程與長按刪除。

## Implementation Steps

1. 確認並保留 baseline 的 `GutterSessionDraft`／`DraftEntity` `hasSubmittedStoreDitch` 欄位、entity mapping 與 Room 3→4 migration；不得因本次唯讀功能改變 legacy `false` default。
2. 確認並保留 baseline 的 draft id `ensure-and-mark`、auto-save preservation 與 local marker failure policy；已提交草稿的唯讀恢復不得把原始 `true` 或內容覆寫回 `false`／新資料。
3. 確認並保留 `GutterRepository.storeDitch(request, token, onRequestEntered: () -> Unit)` 作為新增／編輯共用 boundary；callback 在 repository method entry 同步執行且先於 Retrofit request，重新上傳仍經同一 boundary，失敗／逾時／中斷後保留 marker。
4. 修改 `PendingDraftTagPolicy.kt`：定義 `hasValidSpiNum(draft)` 為只取 type exactly `START` 的 waypoint，其 `basicData["SPI_NUM"].trim().isNotEmpty()`；缺少 START、空字串、純 whitespace、非 START-only 欄位均無效，不另加格式驗證。新增 `EXISTING_GUTTER`，`kindFor` 固定以 valid `SPI_NUM` → existing kind → `hasSubmittedStoreDitch` → submitted/unsubmitted 的順序回傳唯一 kind；adapter、標題與刪除確認均重用此 predicate。
5. 讓 `SubmittedDraftResumePolicy` 消費 `PendingDraftTagPolicy.hasValidSpiNum` 而非自行解析；對同一 draft，valid `SPI_NUM` 必須回傳 `submittedDraftReadOnly=false`，只有無 valid `SPI_NUM` 且 `hasSubmittedStoreDitch=true` 才進入一般 submitted read-only。以 policy-level assertion 固定 tag kind 與 read-only 結果不分歧，再沿 `ResumeMapSheet` → BottomSheet → Host → Navigator → Activity 傳遞一般 submitted flag；Activity recreation 的 saved state 只保存 `draftId`／Boolean／小型索引，重建後 reread Room。
6. 實作 submitted read-only mode：`AddGutterBottomSheet` 鎖定新增節點、反轉、類型、節點選取／編輯、表單刪除與其他內容操作；`GutterFormActivity`／基本資料／照片控制鎖定欄位、座標、照片、附件、虛擬點與匯入操作，返回只回到外層草稿表單且不寫入修改，隱藏或停用內層「完成」／編輯入口。保留外層唯一的「重新上傳」按鈕，按鈕不取用 mutable form result，而是依第 7 步從 Room snapshot 送出完整原始內容。
7. 定義 no-write guard matrix：submitted mode 的 `onPause`、`queueSessionDraftSync`、`queuePhotoDraftSync`、同步／阻塞同步、photo upload result dispatch、`saveAndFinish`、`buildAndFinishWithResult`、一般返回與取消結果均不得呼叫 Room save、不得發出 `RESULT_OK`、不得同步或回傳內容快照；photo、location、import、virtual toggle、欄位與節點 listeners 皆為 disabled／no-op。新增 `RESULT_SUBMITTED_READ_ONLY_RETURN`，兩個 Host result handler 只重新顯示既有 BottomSheet、重置 pending index，不 merge、clear、refresh 或觸發 `onWaypointsChanged`。未提交模式沿用既有寫入與結果流程。
8. 定義重新上傳資料來源與兩種 API projection：每次恢復及按下「重新上傳」前，以 `draftId` reread `GutterSessionRepository.getById`；row 不存在或無法 decode 時顯示 local error 且不呼叫 photo-upload API 或 `storeDitch`。將 Room row deep-copy 成 process-memory immutable snapshot，保存 top-level kind／spiTyp／offline flags、waypoint 順序與所有欄位、座標、uid、basicData、photo URI／captured-at／img-id／upload-state／error metadata 等完整內容；`AddGutterBottomSheet.waypoints`、Activity result 與 normalization 不得成為 submitted source。StoreDitch projection 只比對 `StoreDitchNodeRequestMapper` 支援的 `spiNum`、`spiTyp`、curve、node order/type/id、座標、virtual／connection、表單欄位與 existing `imgIds`；不要把 URI／upload-state 當成 StoreDitch 欄位，mapper 的 `capturedAt` 維持既有 `null`。Photo projection 另以既有 node-image multipart／photo candidate inputs 比對 URI、captured-at、file category、node ownership、img-id 與 upload-state/error transition。兩種 projection 都只建立 request copy，不回寫 Room、不修改 snapshot、不觸發 `onWaypointsChanged`；retry 重新讀取 Room。
9. 在可編輯內容區使用現有 disabled／loading overlay 視覺與觸控阻擋規範，遮罩不可覆蓋返回與「重新上傳」；將提交按鈕文案改為「重新上傳」，並保留既有成功刪除／照片清理、失敗／逾時保存與再次重試流程。
10. 新增狀態、序列化、Room migration、tag policy／adapter 綁定、兩個 Host 恢復鏈路、唯讀／no-write、兩種 payload projection／request capture、重新上傳成功／失敗與列表回歸測試。JVM 只執行純資料／mapper／policy tests；Host、Intent、Activity recreation、UI、Room 與 photo/storeDitch flow 執行 Android instrumentation。完成 targeted JVM tests、Debug build、Android test compile；若裝置／harness 可用再執行 targeted connected tests，否則明確標記 `NOT VERIFIED`。

## Test Plan

- `GutterSessionDraftTest`：`true`／`false` 序列化 round-trip，舊 JSON 缺欄位使用 `false`。
- Draft repository／Room migration test：3→4 後既有草稿可讀取、upsert 不遺失 `hasSubmittedStoreDitch`，標記後重讀為 `true`。
- `GutterRepositoryStoreDitchBoundaryTest`：以 fake `GutterApiService` 記錄 `onRequestEntered` 與 API invocation 順序；驗證 callback 先於 API、callback 失敗時 API 不被呼叫、API error／exception／未回應後仍保留 true，且 validation 未進入 repository 時維持 false。
- `GutterDraftSubmissionStateTest`：驗證固定 draft id 的 ensure-and-mark、missing row 建立、upsert 不遺失 `hasSubmittedStoreDitch`、auto-save 不會將 true 覆寫回 false，以及 direct inspect-edit 無 draft id 的 no-op 政策。
- `GutterDraftDatabaseMigrationTest`：手動建立 version-3 schema／row，使用 `MIGRATION_3_4` 開啟 version-4 database，驗證 legacy row 讀取與 false default。
- `PendingDraftTagPolicyTest`（JVM）：覆蓋 START 的有效值、空字串、純 whitespace、缺少 START、只有非 START waypoint 有 `SPI_NUM`；驗證共用 valid predicate、`EXISTING_GUTTER` precedence、一般 submitted/unsubmitted fallback 與 exactly-one-kind。
- `SubmittedDraftResumePolicyTest`（JVM）：驗證同一組 fixtures 與 `PendingDraftTagPolicy` 一致；valid `SPI_NUM` 同時得到 `EXISTING_GUTTER` 與 `submittedDraftReadOnly=false`，只有無 valid `SPI_NUM` 且 submitted 才為 true；saved-state transport model 只包含 draft id／Boolean／小型索引。
- `GutterFormNavigatorInstrumentedTest`、`MainActivityResumeFlowInstrumentedTest`、`MapWorkspaceResumeFlowInstrumentedTest`（Android）：分別走兩個 Host 的 `openWaypointForEdit` → `openAddForm`，驗證一般 `true` 旗標抵達 Activity Intent；`false` 與有效 `SPI_NUM` 草稿驗證不觸發唯讀，Activity recreation 驗證只以 id／Boolean 重建並從 Room reread，缺 row 時為 local error 且不進 API。
- `SubmittedDraftReadOnlyUiTest`：恢復已提交草稿後驗證基本欄位、節點新增／刪除／排序／反轉、座標、照片、附件、類型、虛擬點、匯入與表單刪除均不可操作；驗證遮罩只覆蓋可編輯區，返回與「重新上傳」可操作且文案正確。
- `SubmittedDraftNoWriteFlowTest`：逐一觸發 `onPause`、返回、取消結果、photo／location／import／virtual callbacks 與 Host result handler，驗證不呼叫 Room save、不發出 `RESULT_OK`、不 merge／clear／refresh、不改變 Room row、waypoint list、marker 或 payload source。
- `SubmittedDraftRequestProjectionTest`（JVM）：驗證 immutable snapshot 到 `StoreDitchNodeRequestMapper` 的 deterministic projection，涵蓋 top-level `spiNum`／`spiTyp`／curve、node order/type/id、座標、virtual／connection、表單欄位與 existing `imgIds`；確認 `capturedAt` 仍為 mapper 定義的 `null`，且 URI／upload-state 不被虛構成 StoreDitch 欄位。
- `SubmittedDraftRetryFlowTest`（Android）：按下重新上傳前 reread Room，分別 capture storeDitch request projection 與 node-image photo projection；前者比對 API 支援欄位，後者比對 URI、captured-at、file category、node ownership、img-id 與 upload-state/error transition。URI normalization 只存在 request copy。成功沿用刪除／照片清理；失敗／401／網路關閉／逾時／中斷保留 `true`、來源 row、唯讀狀態與再次重新上傳能力，row 缺失時不呼叫任一 API。
- `ExistingGutterEditDraftFlowTest`（Android）：驗證有效 `SPI_NUM` 草稿在未進入 API、更新失敗／401／網路關閉／逾時／中斷後，均保留 `SPI_NUM`、唯一 tag、可編輯狀態與完整草稿內容；重新送出仍使用帶 `SPI_NUM` 的既有 `storeDitch` 流程，成功沿用草稿清除／本機清理，長按刪除沿用既有流程。
- 執行 `./gradlew :app:testDebugUnitTest`、`./gradlew :app:assembleDebug`、`./gradlew :app:compileDebugAndroidTestKotlin`；可用環境再跑 targeted connected test。

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android 9+ 實體裝置或現有可用的 Android instrumentation 裝置；需能啟動草稿列表並完成新增／編輯草稿流程。
- In-scope Acceptance Criteria: AC-001、AC-002、AC-003、AC-004、AC-006、AC-007、AC-008
- Regression risk: API 呼叫前落盤時機、App 重啟後狀態保留、既有側溝 tag 優先規則、既有側溝可編輯／重送邊界、submitted 草稿唯讀邊界、重新上傳 payload、草稿恢復／刪除／成功清除。
- Full regression required: No
- Full regression trigger: 提交狀態在重啟後遺失、既有側溝誤顯示或遺失 tag、既有側溝無法編輯／重新送出、已提交草稿仍可修改、重新上傳 payload 不完整、或草稿恢復／成功刪除失敗；其餘為 `無`。
- Stop condition: 所有指定案例完成並取得結果，或取得足夠失敗證據可分類問題。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Device + local draft | 建立無 `SPI_NUM` 的一般可恢復草稿，確認流程已進入 `GutterRepository.storeDitch` 後在回應前／逾時後回到草稿列表；另測有效 `SPI_NUM` 編輯草稿 | 一般草稿顯示「曾提交過上傳」且重啟後仍相同；既有側溝固定顯示「既有側溝編輯中」 | Result; screenshot on failure |
| AC-002 | Device | 建立無 `SPI_NUM` 的一般草稿，在進入 `GutterRepository.storeDitch` 前退出或模擬呼叫前中斷，再開啟列表；另測尚未送出的有效 `SPI_NUM` 草稿 | 一般草稿顯示「未提交上傳」；既有側溝仍固定顯示「既有側溝編輯中」 | Result; screenshot on failure |
| AC-003 | Device | 檢查一般 submitted／unsubmitted 與有效 `SPI_NUM` 草稿的 tag | 已提交為實心主色／白字；未提交與既有側溝編輯中為透明底／主色邊框／主色字；每筆只顯示一個 tag | Result; screenshot on failure |
| AC-004 | Device | 從檢視進入既有側溝編輯並觸發草稿列表；另測點擊恢復、長按刪除與成功上傳清除 | 只顯示「既有側溝編輯中」；恢復後仍可編輯與重新送出，原流程行為不變 | Result; screenshot on failure |
| AC-006 | Device + submitted draft | 恢復已提交一般草稿，嘗試修改欄位、節點、座標、照片、附件、類型、虛擬點與匯入；再操作返回與「重新上傳」 | 所有內容操作被既有遮罩／唯讀狀態阻擋；返回與重新上傳可用，按鈕文案為「重新上傳」 | Result; screenshot on failure |
| AC-007 | Device + network control | 對已提交草稿執行重新上傳，分別觀察成功、失敗、逾時與 App 中斷後重開 | 成功沿用刪除／清理；其他結果保留草稿、tag 與唯讀狀態，可再次重新上傳 | Result; screenshot/logcat on failure |
| AC-008 | Device + existing-gutter draft | 對有效 `SPI_NUM` 草稿分別測試未進入 API、更新失敗／逾時／中斷、修改後重新送出與成功更新 | 全程只顯示「既有側溝編輯中」；草稿可編輯；失敗保留內容與 tag，成功沿用清除 | Result; screenshot/logcat on failure |

## Regression Plan

- 驗證舊版草稿可 migration、列表載入、點擊恢復與資料內容不變。
- 驗證 tag 判定優先序：有效 `SPI_NUM` 只顯示「既有側溝編輯中」；無 `SPI_NUM` 才依 `hasSubmittedStoreDitch` 顯示一般兩種 tag，任何草稿只顯示一個 tag。
- 驗證列表 tag、草稿標題／刪除確認與恢復 read-only 判定共用同一個 normalized valid-`SPI_NUM` predicate，不因 whitespace、缺少 START 或非 START-only 欄位分歧。
- 驗證離線草稿、弧線草稿、未送出新增草稿仍可編輯／刪除，且不因 tag 改動影響標題、時間、節點數與箭頭。
- 驗證 storeDitch 成功後既有草稿刪除與本機照片清理行為不變。
- 驗證 API 失敗、401、網路關閉與逾時仍依既有流程保存草稿；只有已進入 `GutterRepository.storeDitch` boundary 的資料標記為曾提交。
- 驗證既有側溝更新未進入 API、失敗、401、網路關閉、逾時或中斷後，仍可恢復、修改與重新送出；成功沿用既有草稿清除與本機清理流程。
- 驗證已提交草稿恢復後不會因返回、Activity 重建、auto-save、photo normalization 或 Host result merge 改寫原始內容；未提交草稿仍可正常修改並保存。
- 驗證 `RESULT_SUBMITTED_READ_ONLY_RETURN` 只恢復外層 BottomSheet，不會 merge／clear／refresh waypoint；兩個 Host 的結果處理一致。
- 驗證重新上傳以 Room reread 後的 immutable 完整草稿建立兩種 API projection；StoreDitch 不因照片欄位模型限制而遺漏受支援欄位，node-image photo projection 不會因唯讀表單的 disabled／overlay、mutable list 或 transport-only URI normalization 造成 URI／metadata 漂移或新建草稿。

## Risks

- Room migration 與同步 repository 更新是資料正確性的主要風險。
- 新增／編輯兩個 API 呼叫點若漏掉任一個，會造成 tag 誤判。
- 舊草稿的歷史狀態不可追溯；本計畫採 false 相容預設，顯示「未提交上傳」且仍可編輯。
- 唯讀狀態若只鎖住 `GutterFormActivity` 而未鎖住 `AddGutterBottomSheet`，仍可能透過節點層級操作修改內容；需覆蓋兩層表單控制。
- submitted flag 若未沿兩個 Host 的 `openWaypointForEdit`／`openAddForm` 路徑傳入 Navigator，Activity 會退回一般編輯模式；需以兩條正向與 `false`／`SPI_NUM` 負向測試固定契約。
- lifecycle、result 或 photo listener 若繞過 read-only guard，可能在返回／重建時回寫草稿；需維持 dedicated result code 與 no-write guard matrix。
- 重新上傳若取用 mutable form state，可能遺失 Room 中未展示欄位或改變 photo metadata；需以 Room immutable snapshot 與兩種 API projection capture 驗證。
- 完整 snapshot 若被放入 Fragment／Intent／saved-state transaction，可能觸發 Binder 大小限制；只傳 id／Boolean 並在重建後 reread Room。
- 若把 Android-dependent Host／Intent／recreation assertions 放在 JVM source set，會得到不可執行的驗證目標；需以 instrumentation source set 與既有 AndroidX runtime 執行。
- 若把 photo URI／captured-at／upload-state 當作 `StoreDitchRequest` 的欄位，會違反現有 API contract；需分別驗證 StoreDitch mapper projection 與 node-image photo projection。

## Rollback Plan

- 若驗證失敗，還原本次 submitted read-only flag 傳遞、遮罩、控制鎖定與重新上傳 UI 變更；保留已驗證的提交狀態欄位、migration、呼叫邊界與 tag UI，不回退工作區既存的 `GutterApiService.kt`、`AddGutterBottomSheet.kt`、`gradle/libs.versions.toml` 與 `.worktrees/` 修改。

## Current Behavior

- 一般草稿已有 `hasSubmittedStoreDitch` 與列表 tag，但恢復後尚未有 submitted read-only mode。
- baseline 已在 `GutterRepository.storeDitch` method entry 前完成 marker 落盤；本次缺口是恢復流程沒有依 submitted 狀態鎖住表單。

## Expected Behavior

- 進入 `GutterRepository.storeDitch` method entry 後，先完成提交狀態落盤，再開始 Retrofit request；列表可在失敗、逾時、無回應、重啟後正確顯示對應 tag。
- 若尚未進入 repository boundary 就結束，狀態維持未提交；若 boundary 的本機落盤失敗，停止遠端 request 並回傳 local error，避免出現無法證明的誤標記。
- 一般已提交草稿恢復後，所有可編輯內容由既有 overlay 視覺覆蓋並被鎖定，只能返回或使用「重新上傳」送出原始完整內容；未提交草稿仍可編輯。
- 檢視／編輯既有側溝的草稿只顯示「既有側溝編輯中」，維持既有可編輯／重新送出流程；列表長按刪除與成功清除行為不變。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–3 | `GutterRepositoryStoreDitchBoundaryTest` ordering／error tests、裝置 API 失敗／逾時與重啟流程 |
| AC-002 | 1–3 | validation-before-boundary test、未提交 draft fixture 與裝置恢復流程 |
| AC-003 | 4 | `PendingDraftTagPolicyTest` 邊界／precedence assertions、`PendingDraftAdapterUiTest` 三種 tag text／visibility／background／stroke／padding／single-tag assertions |
| AC-004 | 4–5 | `SPI_NUM` fixture、共用 predicate、唯一 tag／優先規則、adapter click／long-click assertions、草稿恢復／可編輯／重新送出／刪除／成功清除回歸 |
| AC-005 | 1–2、5 | `GutterDraftDatabaseMigrationTest`、Gson missing-field test 與列表恢復檢查 |
| AC-006 | 5–7 | submitted read-only mode／overlay UI test、各類控制 disabled assertions、返回／重新上傳 action assertions |
| AC-007 | 2、6–10 | Room immutable snapshot、StoreDitch request projection、node-image photo projection／retry flow test、成功刪除清理、失敗／逾時／中斷後 marker 與再次重新上傳驗證 |
| AC-008 | 4、5、10 | `PendingDraftTagPolicyTest`／`SubmittedDraftResumePolicyTest` cross-policy assertions、`ExistingGutterEditDraftFlowTest` 的恢復／可編輯／失敗保留／重新送出／成功清除與長按刪除驗證 |

## Failure Behavior

- 無 `SPI_NUM` 的一般草稿在 `storeDitch` 回傳錯誤、網路失敗或逾時時，保留既有草稿與「曾提交過上傳」tag；重試沿用已提交狀態。有效 `SPI_NUM` 草稿則維持唯一「既有側溝編輯中」tag與可編輯／重新送出流程。
- App 在進入 `GutterRepository.storeDitch` 前結束時，若已有草稿 auto-save，提交狀態仍為未提交；method entry 的 marker callback 完成後，即使 Retrofit 尚未發出或尚未回呼，狀態仍為已提交。
- 固定 draft id 但找不到 row、session snapshot 不足或本機 marker 寫入失敗時，不送出 Retrofit request，回傳 local error；direct inspect-edit 無 draft id 則依既有流程送出但不建立 tag draft。
- 有效 `SPI_NUM` 的既有側溝草稿不受一般 submitted read-only policy 影響；即使 `hasSubmittedStoreDitch=true`，仍顯示唯一「既有側溝編輯中」並可繼續修改與重新送出。
- 已提交草稿恢復後若使用者返回，保留原始內容與 `true`；不因唯讀表單觸發修改型 auto-save。長按刪除仍由列表執行既有清理。
- 重新上傳成功刪除草稿；失敗、401、網路關閉、逾時或 App 中斷均保留草稿、`true` 與唯讀狀態，可再次整份重新上傳。
- 既有側溝更新成功沿用原有草稿清除；失敗、401、網路關閉、逾時或 App 中斷均保留 `SPI_NUM`、唯一 tag、可編輯狀態與原始內容，可繼續修改後重新送出。
- 舊資料缺少新欄位時使用 false 相容預設；若草稿資料解析或 migration 失敗，沿用既有錯誤處理並不得假稱已提交。

## Security and Privacy

- 不新增權限、憑證或個資；只在本機 Room 草稿保存一個流程狀態 Boolean，不改變 API request／response。

## Open Questions

- 無待產品決策。已採用的技術決定為：有效 `SPI_NUM` 優先顯示唯一「既有側溝編輯中」tag，沿用未提交 tag 樣式並維持可編輯／重新送出；失敗／逾時／中斷保留草稿與 tag，成功沿用清除，長按刪除沿用既有流程。一般 submitted restore／recreation 仍只傳 `draftId`／Boolean 並以 Room reread 重建 process-memory snapshot；Host／Intent／recreation assertions 放在 `app/src/androidTest`，純 policy／mapper assertions 放在 `app/src/test`；重新上傳拆成 StoreDitch request projection 與 node-image photo projection，不修改既有 API model。
