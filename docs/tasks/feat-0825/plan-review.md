# Plan Review

## Decision
- `APPROVED`

## Review Summary
- 這份 `plan.md` 已經達到 implementation gate。
- 目前沒有看到會直接阻止進入 implementation 的邏輯缺口。
- 以門檻式 review 來看，剩下的是實作與驗證風險，不是 plan 本身無法啟動的阻塞點。

## Implementation Gate
- 三條入口已經分清楚：一般互動查詢、背景刷新、強制重載。
- `pendingForceReload`、`forceReloadInFlight` 與 request ID 的單一責任邊界已經寫明。
- gesture / programmatic move / idle 的判定與清旗標規則已經足夠進 implementation。
- 低縮放不消耗 debounce 的順序風險已經被明確寫入。
- 新底部指示器與既有 `MainBlockingUiController` 的責任邊界已經分離。

## Gate Status

### 已達門檻
- 一般查詢、背景刷新、強制重載三條路徑已經有明確入口與用途限制。
- `pendingForceReload` 已經明確定義為單一最新值排隊，不再是模糊的多點補做。
- `canRunMainMapScopeQuery` 與 `isInspectPreviewBlocking` 的分工已經能支撐主地圖可查判定。
- `onCameraMoveStarted(REASON_GESTURE)`、程式化移鏡、`onCameraIdle` 的互動判定已足以驅動實作。
- 低縮放 UI、loading、error 的狀態優先序已經能直接對應到元件行為。
- 測試計畫已經覆蓋入口分流、force queue、response 與指示器行為。

### 未達門檻
- 無。

## Main Concern Check

### 1. 三條入口的責任邊界是否足以實作
- `plan.md` 已經把一般互動查詢、背景刷新、強制重載拆成不同入口，且有各自的觸發來源與 UI 行為。
- 目前沒有看到還會讓開發者無法判斷應該走哪條入口的歧義。
- 這不是阻塞 implementation 的問題。

### 2. `pendingForceReload` 是否仍有雙重消耗風險
- `plan.md` 已明確指定 `MainActivity` 是唯一 owner，並定義 replay 的唯一入口與清除條件。
- 雖然實作時仍要小心不要誤清 pending，但 plan 已經足夠清楚。
- 這是實作風險，不是 plan 阻塞。

### 3. gesture / idle 狀態是否會讓主地圖查詢卡死
- `plan.md` 已經定義 gesture active 的設置與清除時機，也補上 programmatic move 與 location recenter 的分流。
- 目前沒有看到會讓主地圖永遠無法進入查詢的規則衝突。
- 這不是阻止 implementation 的問題。

### 4. 低縮放拒絕是否仍會偷吃 debounce
- `plan.md` 已把低縮放拒絕放在 debounce 之前，並明寫不更新 timestamp。
- 這個風險已被 plan 直接處理。
- 不是阻塞點。

### 5. 底部指示器是否會誤用全螢幕 overlay
- `plan.md` 已明定新指示器不得重用 `MainBlockingUiController` 的全螢幕 overlay 與按鈕禁用行為。
- 責任邊界已足夠進入 implementation。
- 不是阻塞點。

## Missing Test Coverage
- 目前 plan 已經有足夠的測試方向，沒有看到缺到會讓 implementation 無法開始的空洞。
- 後續實作時仍應落實：
  - 一般互動查詢與背景刷新分流測試
  - `pendingForceReload` 最新值覆蓋測試
  - gesture / programmatic move 的 idle 判定測試
  - 低縮放不消耗 debounce 測試
  - 底部指示器不重用 overlay 的驗證

## Final Recommendation
- 可以進 implementation。
- 這份 `plan.md` 已經具備足夠明確的入口分流、狀態邊界與驗證方向，不需要再因為 blocker 級疑慮而停在 planning。
- 後續請直接依 plan 實作，並在 verification 階段檢查 response 順序、indicator 狀態與 force queue 是否如預期運作。

## Conclusion
- 目前沒有足以阻止 implementation 的問題。
