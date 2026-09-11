# Repository Analysis

## Task Classification
- `feature`：調整既有點位更新時對外送出的照片 metadata，並保留不同於新增點位的可見行為；需要完整 Plan Review。

## Current Behavior
- `GutterInspectActivity.preloadEditableWaypoints()` 和 `AddGutterBottomSheet.preloadEditWaypointDetails()` 會將既有照片 URL／本機下載檔、`photo*CapturedAt` 及 `photo*ImgId` 放入 waypoint 的 `basicData`。
- `AddGutterBottomSheet.buildStoreDitchRequest()` 對所有節點都由上述 metadata 建立 `captured_at` 與 `img_ids`，因此既有更新請求也會帶出這兩個 JSON 欄位。
- `PhotoUploadManager` 以 `photo*ImgId` 判斷已上傳照片；有 ID 時跳過 `nodeImage`。MainActivity 和 MapWorkspaceFragment 另外以原始 snapshot 比對，將未變更的照片自「上傳用副本」清空。

## Expected Behavior
- 有後端 `node_id` 的既有點位在 `storeDitch` request 中不帶 `captured_at` 和 `img_ids`，但表單／草稿內仍可保有足以辨識既有照片的本機 metadata。
- 未替換的既有照片不進入 `nodeImage` 上傳佇列；替換後的照片保留現有 slot 與上傳流程。
- 沒有 `node_id` 的新增點位仍使用現有 metadata 組裝規則。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：唯一實際組裝 `StoreDitchNodeRequest` 的位置；應依 `node_id` 分流 metadata。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`：既有點位照片及其本機辨識 metadata 的載入來源；需確認不因 request 調整而移除本機跳過上傳所需資訊。
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt`、`app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：既有照片跳過上傳的共同上傳與兩個 host 流程；預計以回歸驗證為主。
- `app/src/test/java/com/example/taoyuangutter/`：目前沒有 request mapper 或照片上傳佇列的針對性測試，需補足可重現的單元測試。

## Dependencies
- `StoreDitchNodeRequest` 使用 Gson；值為 `null` 時須確認序列化結果確實省略欄位。
- `PhotoUploadSlotState`、`PhotoCapturedAtResolver` 與 form intent/result contract 目前將 metadata 作為 app 內部狀態傳遞。
- 既有更新可由 `GutterInspectActivity → AddGutterBottomSheet` 或直接由 sheet preload 進入；兩條路徑都必須保留既有照片辨識能力。

## Risks
- 若為滿足 AC-001 而清除 waypoint 內的 image ID／拍攝時間，`PhotoUploadManager` 會將下載後的本機 content URI 判定為待上傳，反而造成重傳。
- 若一律省略 metadata，新增點位在 `storeDitch` 後的照片關聯可能退化。
- 僅驗證 MainActivity 會漏掉 MapWorkspaceFragment 的相同行為；兩個 host 目前有重複的 upload-copy 邏輯。
- Gson 的 null 序列化設定若被改動，可能讓欄位仍以 `null` 形式出現在請求中，未達「不帶」要求。

## Unknown Assumptions
- `node_id != null` 是可靠的既有點位判定；新增流程後端尚未指派 node 時維持新增行為。
- `captured_at`、`img_ids` 僅禁止出現在既有點位的 `storeDitch` JSON，並非要求移除 app 內部用來避免重傳的 metadata。

## Potential Issues
- `implementation_regression`：既有照片 metadata 的 request 篩除若誤影響 in-memory waypoint，可能重傳照片或破壞草稿／重新開啟編輯。
- `requirement_gap`：若後端要求「不帶」是指送出 JSON 的 null 欄位也不可出現，測試須以實際 Gson 序列化 request 確認，而非只檢查 Kotlin 物件。
