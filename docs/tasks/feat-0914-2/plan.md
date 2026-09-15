# Implementation Plan

## Goal
- 建立可同時管理多條側溝、獨立保存各條未上傳草稿並符合 Figma 清單面板結構的新增側溝流程。

## Scope
- 調整主地圖新增側溝入口、清單工作階段、草稿持久化與照片／`storeDitch` 的 `captured_at` contract；不重寫既有側溝表單欄位、地圖功能或其他 API contract。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：正式新增入口、`childFragmentManager`、表單／選點回傳、檢視頁關閉回傳、marker、working layer 與送出成功／失敗的單筆狀態協調，全部由 selected multi-gutter item 的 draft ID 協調。
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`：不預期 production change；列入登入後 map tab 的入口驗證。`MainActivity.kt` 是 legacy duplicate，不在本工項產品流程修改範圍。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`、`GutterFormActivity.kt` 與既有 coordinator：新增清單 session 所需的穩定 item／draft ID 傳遞，維持既有單條表單行為。
- 新增 `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterListBottomSheet.kt`、`AddGutterListAdapter.kt`、`MultiGutterSessionCoordinator.kt`：新增側溝清單、列選取、關閉確認、只恢復此清單 ID 集合的 configuration recreation 與即時保存協調。
- `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionDraft.kt`、`DraftEntity.kt`、`DraftDao.kt`、`GutterDraftDatabase.kt`、`GutterSessionRepository.kt`、`GutterDraftCoordinator.kt`：支援 `createdAt`／`savedAt`、workflow ownership、碰撞安全 ID 配置、每條側溝的 ID-only upsert/delete 及舊資料 migration。
- `app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt`：照片狀態更新改用保留 `createdAt` 與 workflow ownership 的草稿寫入 contract。
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`、`common/RequestBodyBuilder.kt`：維持一張照片一個 multipart request；以現有 resolver 時間或裝置目前時間加入 `captured_at`。
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`、`GutterApiModels.kt`：所有 outbound storeDitch node 省略 `capturedAt`；保留 response parsing。
- `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftAdapter.kt`、`PendingDraftsBottomSheet.kt`：使新草稿在既有列表正確顯示、恢復與刪除。
- `app/src/main/res/layout/`、`app/src/main/res/drawable/`、`app/src/main/res/values/strings.xml`：Figma 清單面板、88px row、關閉 Alert 文案及可存取性文字。
- `app/src/test/`、`app/src/androidTest/`：草稿隔離、時間與節點數、工作階段／表單回傳、關閉確認、程序重建及上傳清理測試。

## Implementation Steps
1. 定義 `MultiGutterSessionCoordinator` 與 item model：每條側溝在記憶體建立時取得唯一 `draftId`、`createdAt`、waypoint snapshots、列順序及上傳狀態；空項目不寫入 Room。configuration recreation 僅用 saved item ID 清單重新讀回該 session 的 items；冷啟動／程序死亡後不把所有既有 pending drafts 自動混入清單，而是讓使用者由既有草稿列表各自恢復。
2. 建立草稿 identity/time contract：`GutterSessionDraft`／`DraftEntity` 新增 immutable `createdAt` 與 workflow ownership（`LEGACY_SINGLE`、`MULTI_GUTTER`），保留 `savedAt` 作最後更新時間。Room migration 對既有列以 `created_at = saved_at` backfill；新 multi item 的 ID 由 repository-backed monotonic allocator 以 `max(now, persistedMaxId + 1)` 起始並在同一 process 以同步遞增配置，避免快速新增與重啟後碰撞。
3. 建立唯一寫入 contract：repository 的 `upsertIndependentDraft` 只接受 item draft ID，對已存在列一律保留 `createdAt`／ownership、只更新 `savedAt` 和內容；`ensureDraftExists`、coordinator auto-save、`GutterFormActivity.syncSessionDraftNow` 及 `PhotoSlotUploadCoordinator` 全數改經同一 contract 寫入。insert collision 必須重新配置 ID 並把回傳 ID 回寫至 item／form result。
4. 隔離 legacy SPI_NUM 行為：將 `autoSaveSessionDraft` 的 SPI_NUM dedup 與 `deleteDraftsBySpiNum` 限制為 `LEGACY_SINGLE` drafts；multi-gutter workflow 絕不按 SPI_NUM 查找、重用、覆寫或批次刪除。`MapWorkspaceFragment` 的 multi-gutter submit success、照片清理與失敗保存一律使用 selected item draft ID；legacy 流程不得影響 `MULTI_GUTTER` rows。
5. 將 `MapWorkspaceFragment` 的 `btnAddGutter` 改為正式清單入口，並以 `childFragmentManager` 顯示 list sheet。把 location-pick receiver、`AddGutterBottomSheet`／`GutterFormActivity` result、working markers 與 preview 綁定 selected item draft ID，不再以全域單一 `currentSessionDraftId` 作多側溝識別。
6. 依 Figma `2374:26810` 建立 `AddGutterListBottomSheet`：使用地圖上的 bottom sheet、70px Toolbar、置中標題、左側關閉／刪除、右側新增、可捲動的 88px 清單列與 chevron；列表以 `createdAt` 顯示秒級時間，節點數共用既有有效資料判定並定義虛擬點／pending photo 的計數語意。
7. 實作新增、選取與返回清單：新增後建立 item 並進入既有表單；點選列開啟對應表單；每次有效表單、選點或照片狀態變更都立即寫回該 item。有未上傳項目時，關閉 Alert 的取消不關閉、確定 final-upsert 後關閉；無未上傳項目時直接關閉並只清理此 session 的地圖 UI。
8. 送出成功時維持既有「送出 → 檢視側溝頁面」流程；只在使用者關閉檢視頁並回到同一新增清單時，以成功 item draft ID 刪除其草稿與已驗證歸屬的本機照片並移除該列，不得呼叫後端 delete API。送出／照片失敗則保留 item 與草稿，既有失敗 Alert 確認後回到同一清單供編輯或重送。
9. 在 `GutterRepository.uploadNodeImage()` 組裝每個實際單張 multipart request 時，使用 `PhotoCapturedAtResolver.resolveBestEffort(imageUri)`；若為空值，以裝置目前時間依既有 `yyyy-MM-dd HH:mm:ss` 格式補入 `captured_at`。維持既有 slot、retry 及已上傳不重傳的判定。
10. 移除 `StoreDitchNodeRequestMapper` 對新增、更新、虛擬與一般 node 的 `capturedAt` 組裝，使 outbound JSON 一律省略；保留 `img_ids` 與 response 的 `captured_at` parsing。
11. 補齊 unit 與 instrumentation 測試，並以 build、正式登入後的 map tab、表單、草稿恢復與上傳 smoke test 驗證 Figma 視覺階層及所有 acceptance criteria。

## Test Plan
- Unit：repository-backed allocator 快速建立多筆與重啟後皆為唯一 ID；insert collision retry 能回傳並回寫新 ID。
- Unit：Room migration 對舊草稿 `createdAt = savedAt`；多次表單、location、照片狀態 upsert 後 `createdAt` 不變、`savedAt` 更新、ownership 不被降級。
- Unit：兩筆同 SPI_NUM／後補 SPI_NUM 的 `MULTI_GUTTER` draft 交錯更新與成功刪除其中一筆時，另一筆仍完整存在；legacy SPI_NUM dedup／delete 不可作用於 multi rows。
- Unit：多側溝 item 各自保有 draft ID、建立時間、waypoints 與排序；更新其中一筆不影響其他筆；空項目不生成草稿；節點數固定虛擬點與 pending photo 的既有有效資料計數規則。
- Instrumentation：從 `MainShellActivity` 登入後進入 map tab，主地圖新增入口先開啟清單；新增兩筆、各自進入表單並返回；清單列內容與選取結果正確。
- Instrumentation：Toolbar 左關閉／右新增；有／無未上傳項目時的關閉 Alert 分流；configuration recreation 只恢復此清單 ID 集合；程序重建後 pending list 可各自恢復兩筆草稿。
- Instrumentation：成功送出 → 檢視頁 → 關閉檢視頁 → 同一清單移除成功項目，且不發出 delete API；失敗 Alert 確認 → 同一清單保留失敗項目與草稿；照片與既有 legacy draft 恢復回歸。
- Unit：每張 `nodeImage` multipart 請求都帶該 URI 解析出的 `captured_at`；解析失敗時以裝置目前時間（同一格式）補入；每張仍是一個 request。
- Unit：新增、更新、虛擬及一般 `storeDitch` node JSON 都不含 `captured_at`；`img_ids` 行為與 response parsing 保持。
- Regression：三條既有照片上傳路徑都經共用 repository request；已上傳照片不因本調整變成待重傳。
- Validation：執行 targeted unit tests、相關 Android instrumentation tests、`assembleDebug`，並在可用裝置上進行新增／切換／關閉／恢復／上傳的 smoke test。

## Regression Plan
- 驗證既有單條新增、編輯、檢視模式、無法開蓋、虛擬點、定位選點與地圖控制項持續正常。
- 驗證既有 pending draft 的恢復、刪除與本機照片清理不受資料模型 migration 影響，且 legacy SPI_NUM 流程不能刪除 multi-gutter rows。
- 驗證 `nodeImage` 每張帶正確／補值的 `captured_at`、`storeDitch` 一律不帶；照片 slot、401 導回登入與失敗後草稿保留行為不變。

## Risks
- 多筆工作項目的 ID、Activity Result 與 map marker 對應錯置是主要資料隔離風險；測試必須涵蓋交錯編輯與重建。
- Room migration、ID allocator 或照片檔案生命周期錯誤可能導致舊草稿不可讀、快速新增碰撞或清除錯誤照片；須以舊資料 fixture、collision test 和單筆 ID delete 測試保護。
- 若有上傳路徑繞開 repository 或 resolver 空值被省略，後端 contract 會不一致；以 multipart body unit test 與三條 call path regression 保護。

## Rollback Plan
- 若多側溝流程造成回歸，回復本工項的已提交 revision 即可還原既有單一側溝入口與草稿模型。

## Current Behavior 
- 正式 map tab 的 `MapWorkspaceFragment` 點選新增側溝後直接進入單條側溝的 BottomSheet／表單流程，只有一個 `currentSessionDraftId`。
- 現有草稿列表可恢復單條完整側溝草稿，但沒有同一新增工作階段的多條側溝清單。

## Expected Behavior
- 點選新增側溝先看到 Figma 對應的新增側溝清單，並可在其中新增、切換及編輯多條側溝。
- 每條有效未上傳側溝會立即成為獨立草稿；關閉確認或程序重建後都可從既有草稿列表恢復。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 5, 6 | 正式 map tab 入口 instrumentation |
| AC-002 | 1, 5, 7 | 多筆新增／切換 instrumentation |
| AC-003 | 1, 2, 6 | allocator／adapter unit test、Figma visual smoke test |
| AC-004 | 3, 4, 6, 7 | Toolbar／Alert branch instrumentation、ID-only repository assertions |
| AC-005 | 1, 2, 3, 7 | process/activity recreation instrumentation、createdAt／草稿隔離 unit test |
| AC-006 | 4, 8 | same-SPI isolation and successful-upload cleanup tests |
| AC-007 | 4, 5, 8, 9 | 既有 form／draft／photo／map regression tests |
| AC-008 | 5, 8 | failed-submit Alert-to-list instrumentation |
| AC-009 | 9, 11 | repository multipart unit test、三條上傳路徑 regression |
| AC-010 | 10, 11 | storeDitch mapper JSON unit test、response parsing regression |

## Failure Behavior
- 草稿 upsert 失敗：不關閉清單，顯示可辨識錯誤並保留記憶體內工作項目供重試；不得假稱保存成功。
- 工作項目或草稿讀取失敗：顯示錯誤並隔離該筆，不得用其他草稿覆蓋；其餘清單項目仍可操作。
- 上傳或照片處理失敗：保留且更新僅對應側溝的草稿，沿用既有重試／pending photo 規則。
- 空白新增項目：不生成草稿；關閉確認後直接丟棄，避免污染草稿列表。

## Security and Privacy
- 草稿與照片繼續保存於 app 私有 Room／檔案範圍；不得將照片路徑、帳號、token 或側溝資料寫入日誌。
- 單筆成功上傳或使用者從既有草稿列表刪除時，只清理該筆已驗證歸屬的本機照片。

## Open Questions
- 無。

## Verification test-only simulation amendment — 2026-09-15

### Goal
- Enable the existing no-network failure simulator only in debug builds, so AC-008 can be verified on the logged-in emulator without contacting the production-like backend.

### Scope
- Reuse the existing long-press submit simulation in `AddGutterBottomSheet`; do not add a server, endpoint override, request interceptor, credentials, or production-facing control.

### Affected Files
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`: make `ENABLE_GROUP_SIMULATION` derive from `BuildConfig.DEBUG`.
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`: assert the debug test APK enables the existing simulator gate.
- `docs/tasks/feat-0914-2/{analysis,plan,plan-review,verification,state}.md`: record the bounded test-only scope and evidence.

### Implementation Steps
1. Replace the hard-coded simulator flag with `BuildConfig.DEBUG`; retain its current false behavior for release builds.
2. Add an instrumentation test asserting the debug build enables the existing simulation gate.
3. Build the debug APK, run the targeted instrumentation test on `emulator-5554`, then manually use the existing simulator from logged-in MapWorkspace and capture Alert → list → retained-row → editable-form evidence.

### Test Plan
- Unit/static: verify the flag source is `BuildConfig.DEBUG` and that no source outside the existing simulation gate is changed.
- Instrumentation: assert the debug test APK enables the existing simulation gate.
- Emulator smoke: logged-in debug APK, long-press `新增側溝`, select `模擬網路逾時`, confirm the failure Alert, then verify the same list and editable row. Disable/avoid network at no point because the simulator cannot issue a request.

### Regression Plan
- Run the affected `MainShellActivityTest` and full connected regression on the sole emulator.
- Confirm a release variant keeps the simulator flag false by source/build-variant assertion.

### Risks
- Debug-only menu could inadvertently reach release. `BuildConfig.DEBUG` makes release behavior false; test and source review protect it.
- Simulator coverage proves the user-visible recovery path but not backend integration; it must be reported specifically as test-only evidence, not as a server failure.

### Rollback Plan
- Restore the simulator flag to `false` and remove its targeted test in a single revert commit; no data migration or backend rollback is required.

### Acceptance Criteria Traceability — amendment
| AC | Implementation Step | Validation |
|---|---|---|
| AC-008 | 1–3 | Debug-only simulator Alert confirmation returns to the same list, retains the selected draft, and reopens it for editing; no HTTP request occurs. |

### Failure Behavior
- If debug simulation is unexpectedly unavailable, stop the smoke and report `NOT VERIFIED`; do not substitute a real submission on the production-like endpoint.

### Security and Privacy
- The debug simulator uses no credentials beyond the already logged-in local session and issues no request, photo upload, or log of sensitive values. Release builds keep it disabled.

### Open Questions — amendment
- 無。
