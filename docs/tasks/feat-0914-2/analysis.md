# Repository Analysis

## Current Behavior
- 正式登入後由 `MainShellActivity` 載入 map tab；`MapWorkspaceFragment` 的 `btnAddGutter` 呼叫 `openAddGutterFlow()`，並以其 `childFragmentManager` 顯示單一 `AddGutterBottomSheet`。舊 `MainActivity` 有相近流程，但不是正式登入後產品入口。
- `MapWorkspaceFragment` 以單一 `currentSessionDraftId`、單一 `activeSheet` 與單一 form-result callback 管理一條側溝的 `START`／`NODE`／`END` 點位，並經由 `GutterFormActivity` 編輯單一點位。
- `GutterSessionDraft`、`GutterSessionRepository` 與 `GutterDraftCoordinator` 將一整條側溝的 waypoint snapshots 寫入 Room 中的一筆草稿；`PendingDraftsBottomSheet` 顯示與恢復這些草稿。
- `autoSaveSessionDraft()` 目前依 `SPI_NUM` 尋找、沿用並刪除同號草稿，`deleteDraftsBySpiNum()` 也可批次清除同號草稿；這不適用於多條未上傳側溝的隔離生命週期。模型只有可覆寫的 `savedAt`，ID 以時間毫秒產生。

## Expected Behavior
- 正式 map tab 的新增入口先顯示可管理多條側溝的清單；清單選定一條側溝後才進入既有編輯流程。
- 每條側溝以獨立 draft ID、獨立 waypoint 集合及獨立上傳／刪除生命週期保存；同一清單中的其他側溝不得被覆寫或移除，即使相同或稍後取得相同 `SPI_NUM`。
- 清單以 Figma 的 bottom sheet 與 list-row 階層呈現，關閉時確認保存，並透過立即持久化支援重建後恢復。

## Affected Modules
- `MainShellActivity.kt`：不預期改動產品邏輯，但列入正式登入後 map tab 的入口驗證範圍。
- `map/MapWorkspaceFragment.kt`：正式新增入口、`childFragmentManager`、activity result、地圖工作層與上傳成功後清理，改為由多側溝清單 session 以 selected item 的 draft ID 協調。
- `MainActivity.kt`：非正式產品入口；本工項不改動它。共用草稿 API 的語意會保持 legacy single-flow 相容，並以 regression test 保護。
- 新增 `gutter/AddGutterListBottomSheet.kt`、`gutter/AddGutterListAdapter.kt` 與必要 layout／drawable／strings：實作 Figma 清單面板、列顯示、操作及關閉確認。
- 新增 `gutter/MultiGutterSessionCoordinator.kt`：維護列順序、每條側溝的預先配置 draft ID、建立時間、完成狀態與表單回傳資料。
- `pending/GutterSessionDraft.kt`、`DraftEntity.kt`、`DraftDao.kt`、`GutterDraftDatabase.kt`、`GutterSessionRepository.kt`、`GutterDraftCoordinator.kt`：增加 immutable `createdAt`、workflow ownership 與碰撞安全 ID 配置；提供獨立草稿 upsert／delete，並把 legacy SPI_NUM 去重限制在 legacy flow。
- `gutter/GutterFormActivity.kt`、`common/PhotoSlotUploadCoordinator.kt`：所有表單即時保存與照片狀態保存須走保留 `createdAt` 的 repository contract。
- `pending/PendingDraftAdapter.kt` 與可能的 `PendingDraftsBottomSheet.kt`：確認恢復列表能顯示本工項新建的獨立草稿而不改變既有草稿流程。
- 相關 unit／instrumentation tests：覆蓋工作項目隔離、草稿持久化、列表內容、關閉確認與主地圖入口回歸。

## Dependencies
- `AddGutterBottomSheet` 的既有 waypoint／表單／送出介面與 `MapWorkspaceFragment` 的 Activity Result 回傳協議。
- Room `gutter_session_drafts`、`GutterSessionRepository`、草稿照片清理與 pending drafts 恢復流程；所有新 multi-gutter write/delete 以 draft ID 為唯一鍵。
- Figma node `2374:26810` 的 Toolbar、Content Area、repeatable `list-row` 及現有 Material BottomSheet／Alert 樣式。
- `MainActivity` 的地圖 marker、working layer、viewport inset、401 與照片上傳流程。

## Risks
- 將單一 `currentSessionDraftId` 擴為多筆工作項目時，若錯誤共用 ID，會造成草稿互相覆寫或上傳後刪錯草稿。
- 若 legacy 的 SPI_NUM dedup／batch delete 套用到 multi-gutter draft，會違反同 SPI_NUM 或後補 SPI_NUM 時的資料隔離；需以持久化 workflow ownership 分流。
- 表單回傳、Activity 重建與並行新增／切換可能使清單項目的 waypoint 快照或 marker 狀態不同步。
- 未完成照片若未在每條草稿的隔離邊界保存與清理，可能導致照片遺失、重複上傳或錯誤清除。
- 現行草稿的 `savedAt` 是更新時間；需求的「建立時間」需以 immutable `createdAt` 呈現，且要有舊列 backfill、所有寫入端保留與快速新增不碰撞的契約。

## Unknowns
- 無阻擋規劃的未知項目。空白列不保存草稿採用知識釐清的明確假設；冷啟動後不自動將所有 pending drafts 混入新清單，使用者應從既有 pending list 各自恢復。
