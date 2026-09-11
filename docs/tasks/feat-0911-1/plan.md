# Implementation Plan

## Goal
- 讓既有點位更新只送出資料欄位與使用者新照片，避免已下載的既有照片重傳，且不在既有節點 JSON 帶 `captured_at`、`img_ids`。

## Scope
- 限於既有／新增點位的 `storeDitch` 照片 metadata 分流、既有照片略過上傳的回歸保護，以及相應測試；不包含 WMS 圖層功能。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：依 `requestNodeId` 決定 `StoreDitchNodeRequest` 是否附照片 metadata，且不改動 app 內的 photo state。
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`（新增純 Kotlin mapper）：承擔單一 waypoint 的欄位與照片 metadata 分流；`AddGutterBottomSheet` 只負責傳入模式與 waypoint 組合，不清除或改寫 waypoint 的 `basicData`。
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadCandidateResolver.kt`（新增純 Kotlin resolver）：集中既有照片 snapshot-diff 與 upload-copy 清除規則，供 `MainActivity` 與 `MapWorkspaceFragment` 共用；`PhotoUploadManager` 只依 resolver 產生的候選執行上傳。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：移除各自重複的未變更照片判定，改呼叫共用 resolver；保留一般 edit 與 `currentSessionResumedFromDraft` 的分支語意。
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt`：保留 slot、imgId、virtual/cant-open 過濾與 multipart 上傳，改由 resolver 結果建立候選。
- `app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt`：以實際 Gson JSON 驗證既有 node 欄位缺席、新增 node metadata 保留。
- `app/src/test/java/com/example/taoyuangutter/gutter/PhotoUploadCandidateResolverTest.kt`：覆蓋三個 slot、一般 edit、draft-resume、metadata 保留與 upload-copy 清除。
- `app/src/test/java/com/example/taoyuangutter/gutter/PhotoUploadManagerTest.kt`（必要時以既有測試擴充）：驗證候選轉換為正確 `nodeImage` category。
- `GutterApiModels.kt`：只使用既有 nullable `capturedAt`／`imgIds` DTO 欄位，不變更 API model contract。

## Implementation Steps
1. 將 `buildStoreDitchRequest()` 內的單節點欄位組裝移至 `StoreDitchNodeRequestMapper.map(waypoint, requestNodeId, nodeSequence)`；mapper 以 `requestNodeId != null` 判定既有點位，既有點位回傳 `capturedAt = null`、`imgIds = null`，新增點位沿用目前有效 slot 收集規則，且不修改輸入 `basicData`。
2. 由 `AddGutterBottomSheet` 呼叫 mapper 並組成 `StoreDitchRequest`；`StoreDitchNodeRequest` 與 Retrofit endpoint 不變。保留 preload 的 `photo*ImgId`、`photo*CapturedAt`，使 app 內部照片辨識不受 request metadata 分流影響。
3. 將 MainActivity 與 MapWorkspaceFragment 的未變更照片 snapshot-diff 抽成 `PhotoUploadCandidateResolver.resolve(waypoints, originalWaypoints, resumedFromDraft)`；一般 edit 只保留替換 slot 的 upload-copy，draft-resume 保留現行草稿語意，但仍以 `photo*ImgId` 排除已成功上傳照片。
4. 讓 `PhotoUploadManager.countPendingPhotos()` 與 `uploadWaypointPhotos()` 使用 resolver 的候選結果，維持 virtual、cant-open slot 2/3、imgId 與 slot 1/2/3 mapping 規則；兩個 host 只負責傳入各自 session 狀態，不再各自實作判定。
5. 以 `StoreDitchNodeRequestMapperTest` 驗證實際 Gson JSON：既有 node 的 `captured_at`／`img_ids` key 缺席；新增 node 的 metadata key 與值仍存在；並驗證 mapper 不會改變 waypoint metadata。
6. 以 `PhotoUploadCandidateResolverTest` 覆蓋未修改既有照片、替換 slot 1、slot 2、slot 3、一般 edit 的 upload-copy 清除、draft-resume 的全量草稿候選、metadata 保留；以 `PhotoUploadManagerTest` 驗證候選轉換為正確 `nodeImage` category。
7. 在兩個 host 各執行受控 smoke：既有點位只改文字、各替換一個 slot、草稿回復後儲存；以 MockWebServer／repository fake 記錄 `storeDitch` JSON 與 `nodeImage` 次數／category。若 emulator 或受控後端不可用，逐項記為 `NOT VERIFIED`，不得以 log 物件字串代替 JSON assertion。

## Test Plan
- `./gradlew testDebugUnitTest --tests '*StoreDitchNodeRequestMapperTest' --tests '*PhotoUploadCandidateResolverTest' --tests '*PhotoUploadManagerTest'`：驗證 request JSON、三個 slot、一般 edit 與 draft-resume candidate。
- 執行相關 `testDebugUnitTest` 與 `assembleDebug`。
- 以兩個 host 的受控測試後端／MockWebServer 完成 smoke，直接解析 request body；未修改既有照片為 0 筆 `nodeImage`，替換單張為 1 筆且 category 正確，draft-resume 仍符合其既有全量候選語意。

## Regression Plan
- 檢查新增側溝與新增節點仍將新照片 metadata 送入 `storeDitch`，並在後續以原 slot 上傳。
- 檢查三個照片 slot、無法開蓋的 slot 2／3 排除、虛擬點略過、草稿回復和編輯重開仍維持既有行為。
- `MainActivity` 與 `MapWorkspaceFragment` 各自驗證一般 edit 的未變更照片不列入候選、slot 替換只列入該 slot；各自再驗證 `currentSessionResumedFromDraft` 的草稿回復分支不遺失照片。
- 回歸檢查 metadata（imgId／capturedAt）仍留在表單、草稿與重開編輯資料中；只有 request payload 省略既有 node 的兩個欄位。

## Risks
- 將 request metadata 與 app 內照片辨識 metadata 混為同一份資料會重新引入重傳。
- JSON 序列化設定可能使 null key 出現在 payload，需以實際 payload 測試鎖定。
- 既有 request builder 的 private 可見性可能限制測試；若需抽出 helper，限於此 mapping，不做跨功能泛化。

## Rollback Plan
- 回退本任務單一 commit 即可恢復目前既有點位 metadata payload 與照片上傳行為，不影響已存資料。

## Current Behavior
- 既有點位下載照片後會在 app 內保留照片 ID／拍攝時間，且 `storeDitch` 對所有節點都帶入 `captured_at`、`img_ids`。
- 多數未變更照片已由 image ID 與 snapshot-diff 略過 `nodeImage`，但這項判定與對外 request metadata 尚未分離。

## Expected Behavior
- 既有點位更新的節點 JSON 不出現 `captured_at`、`img_ids`，但 app 繼續識別其既有下載照片，因而不重新上傳。
- 使用者替換的照片仍能依原 category 上傳；新增點位不受影響。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1、2、4 | request JSON unit test，確認 key 缺席 |
| AC-002 | 3、5、6 | upload candidate test 與既有點位 smoke test |
| AC-003 | 3、5、6 | 每一 slot 的替換候選與 `nodeImage` category test |
| AC-004 | 2、4 | 新增點位 request JSON regression test |
| AC-005 | 3、5、6 | targeted unit tests、草稿／特殊模式回歸與 manual smoke |

## Failure Behavior
- 既有的 storeDitch、照片下載與照片上傳錯誤處理維持不變；若照片 preload 或替換上傳失敗，沿用目前的警示、草稿與重試流程。

## Security and Privacy
- 不增加任何照片傳輸、權限或外部資料；預期降低既有照片的重複傳輸。測試紀錄不得輸出 token 或照片內容。

## Open Questions
無。
