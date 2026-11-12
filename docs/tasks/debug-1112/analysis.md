# Repository Analysis

## Current Behavior

- 編輯入口會將既有節點的 `_nodeId` 與 `XY_NUM` 放入 waypoint；匯入既有點位時，`GutterFormActivity.handleImportedNodeDetails()` 也會把匯入點的 `nodeId` 寫入表單狀態，並由表單回填 `XY_NUM`。
- 但本案例的 D 是新 waypoint，沒有原本的 `node_id` 是預期結果，不是前端遺失錯誤。匯入 C 的作用是把 C 的資料與 `XY_NUM` 複製到 D，讓後端依 `XY_NUM` 找到 C 的資料做繼承；不應把 C 的 `node_id` 當成 D 的 identity。
- 刪除 C 的 swipe path 會直接從目前 `waypoints` 移除 C，接著重新編號。刪除後，送出中的列表是 A、B、D；D 仍帶 `XY_NUM=C`，但列表已不含 C。
- `storeDitch` request builder 會將 D 的空 `_nodeId` 映射成 `node_id=null`，同時保留 `XY_NUM=C`。這個 payload 對新 D 是正確的 identity 語意，但它依賴後端在 C 尚未被刪除時完成 C 的資料繼承。
- 目前 client 沒有保存／比較「編輯開始時存在、送出時被刪除」的 `XY_NUM` 來源依賴，因此看不出 D 仍引用已刪除的 C。
- 匯入點位當下也沒有檢查匯入的 `XY_NUM` 是否已存在於目前側溝；因此 C 尚在列表時，D 仍可取得 `XY_NUM=C`。

## Expected Behavior

- D 維持新點語意：不帶 C 的 `node_id`，但保留匯入所得的 C `XY_NUM` 與資料。
- 匯入點位時，若匯入的 `XY_NUM` 已存在於目前側溝，前端必須立即拒絕匯入，並提示「不可匯入本側溝中的同樣點位」。
- 這個匯入時阻擋可避免 D 先取得 C 的 `XY_NUM`，再透過刪除 C 繞過送出時的同名檢查；不需要把 C 的 `node_id` 填給 D。
- `XY_NUM` 同名檢查仍應保留，且應補足對已刪除來源的依賴檢查。
- 主要防線應前移至匯入點位：若匯入的 `XY_NUM` 已存在於目前側溝，阻擋匯入並提示「不可匯入本側溝中的同樣點位」。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`、`GutterBasicInfoFragment.kt`：匯入 C 的資料與 `XY_NUM` 進入 D 的表單狀態。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` 的 `handleImportedNodeDetails()` 可作為匯入時 guard 入口；同一 Activity 已持有由 `GutterFormNavigator` 傳入的 `sessionWaypoints`。
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：目前主流程接收結果、回寫 waypoint 並 autosave。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`：legacy host 也需確認相同的 waypoint 回寫行為。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：刪除／重編號、`XY_NUM` duplicate guard 與 `storeDitch` request mapping。
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`、`GutterApiModels.kt`：`node_id` 與 `XY_NUM` 的 request serialization。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`：既有編輯點位的 `_nodeId`／`XY_NUM` 初始來源。

## Dependencies

- `NodeDetails.nodeId`／`xyNum` 與 `DitchNode.nodeId` 的 API readback。
- `GutterFormContract` 的 Intent extras。
- `Waypoint.basicData`、session draft 與 `onWaypointsChanged` autosave。
- `GutterFormActivity.sessionWaypoints`／`WaypointSnapshot.basicData`，可提供目前側溝的 `XY_NUM` 集合。
- `StoreDitchNodeRequest.nodeId` 的更新／新增語意。
- 後端 `storeDitch` 對缺少 `node_id`、重複／既有 `XY_NUM` 的處理；repository 只有 client contract，沒有後端實作。

## Risks

- 只做目前 waypoint list 的 duplicate check 無法檢查已刪除的 C，也無法取代後端的繼承順序／唯一性驗證。
- 直接把匯入點的 `node_id` 套到 D 會把產品要求的「新 D」誤變成「更新 C」，不是本案例的修正方向。
- 匯入時的檢查必須限定在目前側溝，不可把其他側溝的相同 `XY_NUM` 誤判為重複。

## Unknowns

- 未取得本次實際 `storeDitch` request／response log，因此後端刪除與繼承的實際交易順序，尚未獨立驗證；但匯入時阻擋可直接避免本案例的不安全中間狀態。
- 未在裝置上重現整個 ABC → add D → import C → delete C → submit 流程；本次根因由 source path 與使用者重現描述對照確認。
