# Repository Analysis

## Current Behavior

- 編輯入口會將既有節點的 `_nodeId` 與 `XY_NUM` 放入 waypoint；匯入既有點位時，`GutterFormActivity.handleImportedNodeDetails()` 也會把匯入點的 `nodeId` 寫入表單狀態，並由表單回填 `XY_NUM`。
- 但本案例的 D 是新 waypoint，沒有原本的 `node_id` 是預期結果，不是前端遺失錯誤。匯入 C 的作用是把 C 的資料與 `XY_NUM` 複製到 D，讓後端依 `XY_NUM` 找到 C 的資料做繼承；不應把 C 的 `node_id` 當成 D 的 identity。
- 刪除 C 的 swipe path 會直接從目前 `waypoints` 移除 C，接著重新編號。刪除後，送出中的列表是 A、B、D；D 仍帶 `XY_NUM=C`，但列表已不含 C。
- `storeDitch` request builder 會將 D 的空 `_nodeId` 映射成 `node_id=null`，同時保留 `XY_NUM=C`。這個 payload 對新 D 是正確的 identity 語意，但它依賴後端在 C 尚未被刪除時完成 C 的資料繼承。
- 目前 client 沒有保存／比較「編輯開始時存在、送出時被刪除」的 `XY_NUM` 來源依賴，因此看不出 D 仍引用已刪除的 C。

## Expected Behavior

- D 維持新點語意：不帶 C 的 `node_id`，但保留匯入所得的 C `XY_NUM` 與資料。
- 若後端的繼承需要來源 C 在請求處理時仍可查到，前端必須在送出前拒絕「C 已刪除、D 仍以 C 的 `XY_NUM` 繼承」的組合，並提示使用者保留 C 或改用可接受的點位編號。
- 若產品確實要允許這個操作，後端/API 必須明確支援 atomic inherit-before-delete，先解析／複製 C 的資料再套用 C 的刪除；這不是把 C 的 `node_id` 填給 D。
- `XY_NUM` 同名檢查仍應保留，且應補足對已刪除來源的依賴檢查。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`、`GutterBasicInfoFragment.kt`：匯入 C 的資料與 `XY_NUM` 進入 D 的表單狀態。
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：目前主流程接收結果、回寫 waypoint 並 autosave。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`：legacy host 也需確認相同的 waypoint 回寫行為。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：刪除／重編號、`XY_NUM` duplicate guard 與 `storeDitch` request mapping。
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`、`GutterApiModels.kt`：`node_id` 與 `XY_NUM` 的 request serialization。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`：既有編輯點位的 `_nodeId`／`XY_NUM` 初始來源。

## Dependencies

- `NodeDetails.nodeId`／`xyNum` 與 `DitchNode.nodeId` 的 API readback。
- `GutterFormContract` 的 Intent extras。
- `Waypoint.basicData`、session draft 與 `onWaypointsChanged` autosave。
- `StoreDitchNodeRequest.nodeId` 的更新／新增語意。
- 後端 `storeDitch` 對缺少 `node_id`、重複／既有 `XY_NUM` 的處理；repository 只有 client contract，沒有後端實作。

## Risks

- 只做目前 waypoint list 的 duplicate check 無法檢查已刪除的 C，也無法取代後端的繼承順序／唯一性驗證。
- 直接把匯入點的 `node_id` 套到 D 會把產品要求的「新 D」誤變成「更新 C」，不是本案例的修正方向。
- 若僅由前端阻止此組合，可能影響原本依賴後端 atomic 行為的使用流程；需以 API contract 決定提示文字與替代操作。

## Unknowns

- 未取得本次實際 `storeDitch` request／response log，因此後端刪除與繼承的實際交易順序，尚未獨立驗證。
- 未在裝置上重現整個 ABC → add D → import C → delete C → submit 流程；本次根因由 source path 與使用者重現描述對照確認。
