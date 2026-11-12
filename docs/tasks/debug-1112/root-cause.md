# Root Cause Investigation — 2026-11-12

## Classification

`implementation_regression`, priority `P0`：編輯匯入流程允許新 D 依賴同一批請求中被刪除的 C，可能造成 C、D 都不落庫而只剩 AB。另有一個 `P1` 的 duplicate-check scope gap，使本案例無法被前端擋下。

## Observed and Expected Behavior

使用者流程：既有 `ABC` 側溝已上傳；編輯時新增 D，匯入 C 資料，刪除原 C，再上傳。回報結果是 C、D 都不見，只剩 AB。

預期 D 維持新點語意，不帶 C 的 `node_id`，但能安全繼承 C 的資料；若後端不能在刪除前解析來源，送出前應阻止這個不安全 payload。

## Root Cause

### 1. D 沒有 `node_id` 是正確的新點語意，不是本案例的根因

1. 編輯預載會把既有節點的 `_nodeId` 與 `XY_NUM` 寫入既有 waypoint（`GutterInspectActivity.kt:435-446`）。但 D 是新 waypoint，本來就沒有 `_nodeId`。
2. D 的表單匯入 C 後，`handleImportedNodeDetails()` 會把匯入資料寫入表單狀態，`prefillDataFromImport()` 會把 C 的 `xyNum` 回填至 D 的 `XY_NUM`（`GutterFormActivity.kt:654-661`、`GutterBasicInfoFragment.kt:1437-1444`）。這個流程保留的是繼承來源的編號，不是把 C 的 server identity 轉移給 D。
3. 因此表單 result contract 沒有將 C 的 `_nodeId` 回傳給 D，對本案例不是 bug；把 C 的 `node_id` 補進 D 反而會把新 D 變成對 C 的更新。

### 2. 同一批請求刪除了 D 的繼承來源 C

- 編輯 request 會呼叫 `buildStoreDitchRequest(waypoints, editSpiNum)`（`AddGutterBottomSheet.kt:1284-1288`）。
- D 沒有 `_nodeId` 時，request builder／mapper 會將它送成 `node_id=null`；同一時間仍將 `basicData["XY_NUM"]` 放入 `xyNum`（`AddGutterBottomSheet.kt:2546-2554`、`StoreDitchNodeRequestMapper.kt:38-47`）。model 的 JSON key 分別是 `node_id` 與 `XY_NUM`（`GutterApiModels.kt:529-544`）。
- C 已從目前 waypoint list 被移除，因此送出 list 不再包含 C；D 仍以 `XY_NUM=C` 指向 C 的資料來源。這是新 D 的正確 payload 形狀，但它要求後端在 C 尚未被刪除時完成資料繼承。

如果後端先執行 C 刪除，再以 `XY_NUM=C` 建立 D，C 已不存在，D 的繼承來源就會失效，最後可能只留下 AB。這個刪除／繼承的確切交易順序需用後端程式或 sanitized request／response log 驗證；目前 repository 只有 client contract。

### 3. 目前有 `XY_NUM` 同名檢查，但範圍不足以攔截本案例

目前檢查位於 `validateWaypointPhotosAndFieldsOrAlert()`（`AddGutterBottomSheet.kt:1935-1958`），編輯與新增送出前都會呼叫它（`AddGutterBottomSheet.kt:1102-1108`、`1255-1267`）。它的實際規則是：

- 只掃描送出當下仍在 `waypoints` list 的 `basicData["XY_NUM"]`。
- 對 trim 後的完全相同字串做 group count；兩筆以上才顯示「座標編號重複」並停止送出。
- 不查詢後端既有／已刪除的 C，不在匯入選取當下檢查，也不以 `node_id` 做 identity-aware 比較。

所以若原 C 尚未刪除，C 與 D 同時存在，這段檢查理論上會擋下重複；但依使用者順序先刪除 C 後，列表只剩 D 持有該 `XY_NUM`，count 變成 1，檢查會通過。這回答了「目前是否會進行 xy_num 同名檢查」：**會，但只做目前列表內的送出前同名檢查；本案例刪除 C 後不會被它判定為重複。**

### 4. 已排除的前一個假設

先前曾懷疑是 `GutterFormContract` 沒有回傳 `_nodeId`，造成 D 的 identity 遺失。依產品澄清，這個假設不適用本案例：D 不應攜帶 C 的 `node_id`。`_nodeId` result contract 的缺口可能影響「匯入到既有點、要更新該既有點」的其他流程，但不是本次「新 D 依 `XY_NUM` 繼承 C、同時刪 C」的根因。

## Evidence

| Boundary | Evidence | Finding |
|---|---|---|
| Existing edit preload | `GutterInspectActivity.kt:435-446` | Existing node has `_nodeId` and `XY_NUM`. |
| Import | `GutterFormActivity.kt:654-661`; `GutterBasicInfoFragment.kt:1437-1444` | Imported data enters D; C `xyNum` is copied into D's `XY_NUM`. |
| Form result | `GutterFormContract.kt:148-205,214-260` | Result keeps the imported `XY_NUM`; omitting C's `_nodeId` is correct for a new D in this scenario. |
| Parent handoff | `MapWorkspaceFragment.kt:1351-1374`; `MainActivity.kt:465-476` | Hosts merge imported data into D; no source-deletion dependency check is present here. |
| Delete | `AddGutterBottomSheet.kt:872-895` | Deleting C removes it from current list, then `renumberAll()` persists the remaining list. |
| Duplicate guard | `AddGutterBottomSheet.kt:1935-1958` | Checks only current list, after C deletion. |
| Request mapping | `AddGutterBottomSheet.kt:2546-2554`; `StoreDitchNodeRequestMapper.kt:38-47`; `GutterApiModels.kt:529-544` | New D sends no `node_id` and retains C `XY_NUM`; this requires a live source or atomic backend handling. |

## Failed Acceptance Criteria Mapping

- AC-001 — **PASS for the clarified identity semantics**: D remains a new point without C's `node_id`; the failure is that its C `XY_NUM` source can be deleted in the same submission.
- AC-002 — **PARTIAL**: a same-list `XY_NUM` check exists, but it does not cover deleted source points or backend inheritance ordering.
- AC-003 — **PASS for investigation output**: affected files, root cause, risks and minimum fix boundary are documented below; no production code was changed.

## Regression Risk

- High data-integrity risk if the client allows a new point to reference a source removed in the same edit.
- High semantic risk if a fix copies C's `node_id` into D; that would violate the clarified new-point behavior.
- Medium risk around ordinary edit result handoff, draft autosave, photo uploads and existing `XY_NUM` preservation.

## Evidence Limits

- This branch has no production-code changes and no live backend request capture.
- The backend implementation is not in this repository; the client-side source-dependency gap is verified statically, while the exact backend deletion/creation transaction is based on the user report and remains to be confirmed with sanitized payload logs.
