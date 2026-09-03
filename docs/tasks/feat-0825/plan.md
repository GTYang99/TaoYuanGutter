# Implementation Plan

## Goal
- 只在主地圖新增底部載入指示器，並將 `/v1/map/scopeSearch?` 分成一般互動查詢、背景刷新與強制重載三條無歧義入口。

## Scope
- 修改主地圖的鏡頭事件判定、scope query 路由、force reload 排隊、側溝繪製完成回饋與底部指示器；不修改其他頁面、表單、照片、登入、草稿資料或非主地圖功能。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` - 接收地圖事件、建立三條 query 入口、計算主地圖可查狀態，並單一持有 force reload 執行中與 pending 狀態。
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt` - 執行已完成分流的 query、管理一般查詢 debounce、忽略過期 response，並在側溝線段完成畫面更新後回報結果。
- `app/src/main/java/com/example/taoyuangutter/main/MainMapLoadIndicatorController.kt` - 新增主地圖專用底部指示器 controller，集中處理 hidden、low zoom、loading 與 error 狀態。
- `app/src/main/res/layout/activity_main.xml` - 新增主地圖底部 toast 風格指示器容器。
- `app/src/main/res/values/strings.xml` - 新增固定低縮放文案、級數、載入中與錯誤文案。
- `app/src/main/res/drawable/` - 新增或沿用明確指定給底部指示器的 `X`、`V` 狀態圖示。
- `app/src/test/java/com/example/taoyuangutter/map/ScopeQueryRoutingTest.kt` - 新增入口分流、鏡頭事件、debounce、blocked 與 force queue 單元測試。
- `app/src/test/java/com/example/taoyuangutter/main/MainMapLoadIndicatorControllerTest.kt` - 新增指示器狀態優先順序與狀態轉換測試。
- `app/src/androidTest/java/com/example/taoyuangutter/MainMapLoadIndicatorTest.kt` - 新增主地圖指示器位置、內容、常駐與非 blocking 行為驗證。

## Implementation Steps

### 1. 建立三條唯一載入入口

| 入口 | 唯一觸發來源 | UI | zoom 門檻 | debounce | blocked 行為 |
| --- | --- | --- | --- | --- | --- |
| `requestUserInteractionScopeSearch()` | `onCameraMoveStarted(REASON_GESTURE)` 後的 `onCameraIdle` | 一律走可見底部指示器，不得傳入 `showFeedback = false` | `< 10` 只顯示低縮放狀態；`>= 10` 才送 query | 只套用此入口 | 直接丟棄本次查詢，不轉 background、不建立 force pending，等待下一次使用者操作 |
| `requestBackgroundScopeRefresh()` | 初始地圖第一次 ready、系統重建後第一次恢復 | silent，不顯示 loading 或 error；若 zoom `< 10`，仍更新低縮放常駐狀態 | `>= 10` 才送 query；`< 10` 直接結束該次背景刷新，後續由有效 gesture 決定是否查詢 | 不使用一般 debounce | 初始化／恢復 owner 只在主地圖 ready 且 `canRunMainMapScopeQuery == true` 後呼叫一次，不在 blocked 狀態提前送出 |
| `requestForceScopeReload(reason)` | 編輯返回、檢視返回、單一側溝檢視返回、草稿恢復、`inspect` 內部任何重新整理、使用者主動定位回正完成 | 使用者可感知，固定走同一個可見底部指示器；不得走 silent error | 不受 10 級 query 門檻阻擋 | 不使用一般 debounce | blocked 或已有 force request 執行中時，只覆蓋保存最新一筆 `pendingForceReload` |

- 除初始地圖載入與系統重建恢復外，任何流程都不得呼叫 `requestBackgroundScopeRefresh()`。
- 除表格列出的 force 來源外，任何流程都不得呼叫 `requestForceScopeReload(reason)`；`inspect` 內部任何重新整理不分操作類型，一律使用此入口。
- 使用者主動定位回正雖由程式化鏡頭移動完成，但必須在該次定位動畫的 `onCameraIdle` 走 `force reload`，不得走一般 gesture query，也不得完全略過刷新。
- `pendingInspectPreviewReload` 只保存 inspect preview 重開所需資料，不得保存、覆蓋、消耗或清除 `pendingForceReload`。

### 2. 寫死主地圖可查條件

- 在 `MainActivity` 建立單一衍生判定 `canRunMainMapScopeQuery`，三條入口與 force replay 都只能使用此判定，不得各自組合 blocked 條件。
- `canRunMainMapScopeQuery` 只有在以下條件全部成立時為 true：Activity lifecycle 至少為 `STARTED`、`googleMap != null`、主地圖內容目前可見、`isInEditingMode == false`、`inspectSheet == null`，且獨立旗標 `isInspectPreviewBlocking == false`。
- `isBlocked` 定義為 `!canRunMainMapScopeQuery`，不得再使用「其他暫時狀態」或另一組較窄判定。
- inspect preview 開啟、preview 內部刷新，以及 preview 關閉後等待主地圖 UI 恢復期間，`isInspectPreviewBlocking` 保持 true；`restoreMainUiAfterSheetClosed()` 完成且主地圖重新可接受 query 後設為 false，再呼叫 force queue 的單一消耗點。
- `pendingInspectPreviewReload` 是否存在不得直接列入 `canRunMainMapScopeQuery`，因為它需要等 force scope 載入完成後才被消耗並重開 preview；若用它阻擋 query 會形成永遠無法完成載入的循環。
- force scope 載入完成並準備重開 preview 時，先把 `isInspectPreviewBlocking` 設回 true，再消耗 `pendingInspectPreviewReload`；preview 正常關閉且主地圖恢復後才再次解除。

### 3. 分離 UI 級數同步與 query 資格判定

- 每一次 `onCameraIdle` 都先讀取目前 zoom level 並更新指示器的級數狀態，不論來源是 gesture、API、developer animation、初始鏡頭或定位回正。
- zoom `< 10` 時，如果目前沒有 loading 或 error，固定顯示 `X` icon 與「放大以加載側溝圖層（級數）」；此狀態不自動消失。
- zoom `>= 10` 時，若目前沒有 query、loading 或 error，隱藏低縮放提示。
- UI 級數同步不得直接送 query；是否送 query 必須再依入口來源判定。
- 指示器狀態優先順序固定為 `loading > error > low zoom > hidden`。
- zoom `< 10` 的 force reload 開始時，以 `V` 與 loading bar 暫時取代 `X`；成功完成後立即依目前 zoom 重新計算，恢復常駐 `X`，不得直接隱藏。
- zoom `< 10` 的 force reload 失敗時顯示 error；使用者下一次操作開始時清除 error，並立即依目前 zoom 恢復 `X`，再等待該次操作結束決定是否查詢。

### 4. 固定 camera gesture 旗標生命週期

| 事件 | gesture active | query 行為 |
| --- | --- | --- |
| `onCameraMoveStarted(REASON_GESTURE)` | 設為 true | 清除既有 error，等待 `onCameraIdle` |
| `onCameraMoveStarted(REASON_API)` | 立即設為 false | 不可進一般查詢 |
| `onCameraMoveStarted(REASON_DEVELOPER_ANIMATION)` | 立即設為 false | 不可進一般查詢 |
| 初始鏡頭設定 | 保持 false | 由 background owner 在 map ready 後處理，不進一般查詢 |
| 使用者主動定位回正開始 | 設為 false，另記錄一次性 location-recenter reason | 動畫 idle 後走 force reload |
| `fitCameraToWaypoints()`／其他非定位程式化移鏡 | 設為 false | 不進一般查詢；需要刷新者必須已有明確的 background 或 force owner |
| `onCameraIdle` 且 gesture active 為 false | 保持 false | 只同步 zoom UI；若有 location-recenter reason，消耗該 reason 並走 force reload |
| `onCameraIdle` 且 gesture active 為 true | 判定完成後立即設為 false | zoom `< 10` 只顯示低縮放；zoom `>= 10` 才可進一般查詢 |

- gesture active 必須在同一次 `onCameraIdle` 完成判定後清除，即使 blocked、offline、缺 token 或被 debounce 拒絕也不得殘留。
- location-recenter reason 只能由定位回正入口建立，並在對應 idle 消耗；任何後續 idle 不得重複使用。

### 5. 在 debounce 前執行低縮放與可查檢查

- `requestUserInteractionScopeSearch()` 的固定順序為：同步 zoom UI、檢查 zoom、檢查 `canRunMainMapScopeQuery`／offline／token、檢查 debounce、建立 request、更新 debounce timestamp、送出 query。
- zoom `< 10` 是 UI 拒絕狀態，不算 query attempt，不得呼叫 coordinator、不得更新 `lastLoadTime` 或任何等價 timestamp。
- blocked、offline 或缺 token 的一般查詢也不更新 debounce timestamp。
- 只有 zoom `>= 10`、可查條件成立且即將實際送出的 user interaction query，才能更新一般查詢 debounce timestamp。
- background refresh 與 force reload 不讀取也不更新一般查詢 debounce timestamp。

### 6. 以單一版本化 queue 管理 force reload

- `MainActivity` 是 `forceReloadInFlight`、`pendingForceReload` 與遞增 `forceRequestId` 的唯一 owner；`ScopeMapCoordinator` 不得持有或操作這些狀態。
- `ForceReloadRequest` 至少保存唯一遞增 request ID 與 reason；重播時使用當下主地圖 viewport，不保存舊 viewport，避免離開 edit／inspect 後畫出過期範圍。
- `requestForceScopeReload(reason)` 每次建立新 ID；若 blocked 或已有 force request 執行中，直接覆蓋 `pendingForceReload`，只保留最新一筆。
- `tryConsumePendingForceReload()` 是唯一 replay 入口；只有 `canRunMainMapScopeQuery == true`、沒有 force request 執行中且 pending 存在時，才能把該 pending 標記為 `forceReloadInFlight` 並執行。
- replay 執行期間保留該 request ID；若期間收到更新 force request，新 request 覆蓋 pending，但不得取消或改寫目前 in-flight ID。
- force request 完成時，只清除 ID 相同的 `forceReloadInFlight`；不得無條件清空 `pendingForceReload`。
- force request 完成後若 pending 仍存在，重新檢查 `canRunMainMapScopeQuery`，成立時立即由 `tryConsumePendingForceReload()` 執行最新一筆；不成立時保留至下一個明確的主地圖 ready／blocked 解除事件。
- 新 pending 覆蓋舊 pending 後，舊 ID 永久失效，不得被 replay。

### 7. 防止過期 response 覆蓋新 viewport 與 UI

- 每一筆實際送出的 scope query 都取得遞增 execution ID，`ScopeMapCoordinator` 回傳 success／error 時必須帶回同一 ID。
- 只有仍為最新有效 execution ID 的 response 可以清除並重畫 scope polylines、切換底部指示器或回報完成；過期 response 只能結束自己的工作，不得更新地圖或 UI。
- force queue 的 in-flight 完成判定使用 force request ID；畫面更新有效性使用 execution ID，兩者不得混為同一狀態。
- 「畫面完成更新」固定定義為：有效 response 已在主執行緒完成舊 scope polyline 移除、完成 `drawFeatures()` 新 polyline 建立，並執行對應 `onLoadingFinished(executionId)` callback。
- API success 但尚未完成上述 callback 時仍保持 loading；過期 success 不得觸發 loading finished。

### 8. 建立獨立底部指示器

- 在 `activity_main.xml` 的主地圖底部 toast 區域加入 icon、目前 zoom level、提示文字與 loading bar，並處理 system inset、bottom sheet／panel 與現有地圖控制元件的間距。
- `MainMapLoadIndicatorController` 只控制新指示器，不得呼叫 `MainBlockingUiController.showLoading()`、不得切換 `inspectLoadingOverlay`、不得共用 visibility／loading／error state，也不得禁用地圖按鈕或其他操作。
- user interaction query、force reload 與使用者主動定位回正都使用可見指示器；`onSilentError()` 只可由 initial load 或 system recreation background refresh 使用。
- visible query 失敗後保持 error，直到下一次使用者 gesture、定位回正或其他可見 force request 開始；新操作開始時先清除 error，再依 zoom 顯示 low zoom 或 loading。
- visible query 成功後等畫面完成更新 callback；callback 完成時若目前 zoom `< 10` 顯示 low zoom，否則隱藏指示器。

## Test Plan

### Entry Routing Tests
- 驗證 `REASON_GESTURE` 後的 idle 在 zoom `>= 10` 只走 user interaction query，顯示可見指示器且不走 silent。
- 驗證 gesture idle 在 zoom `< 10` 只顯示常駐 `X`，不呼叫 API、不更新 debounce；隨後立即 zoom 到 `>= 10` 的有效 gesture 可正常查詢。
- 驗證 `REASON_API`、`REASON_DEVELOPER_ANIMATION`、初始鏡頭、`fitCameraToWaypoints()` 與一般 `animateCamera()` 只同步 zoom UI，不進 user interaction query。
- 驗證程式化移鏡到 zoom `< 10` 時，即使不查詢仍會顯示常駐低縮放提示。
- 驗證使用者主動定位回正只在對應 idle 走一次 force reload，不進 user interaction query，也不在後續 idle 重複送出。
- 驗證 background refresh 只有初始 map ready 與 system recreation restore owner 可呼叫，必須等主地圖 ready 且 unblocked 才執行一次；zoom `< 10` 時只保留低縮放 UI、不呼叫 API、不建立 retry。
- 分別驗證編輯返回、檢視返回、單一側溝檢視返回、草稿恢復與每一種 inspect 內部重新整理都走 force reload，不受 zoom `< 10` 或一般 debounce 阻擋。
- 驗證一般 query blocked 時直接丟棄，不轉 background、不建立 force pending，也不消耗 debounce。

### Force Queue Tests
- 驗證 blocked 時 force request 只保留最新 ID，解除 blocked 且主地圖 ready 後只 replay 最新一筆。
- 驗證 inspect preview 關閉但主地圖 UI 尚未恢復時仍 blocked；主地圖恢復後解除 `isInspectPreviewBlocking` 並 replay，且 `pendingInspectPreviewReload` 本身不會造成 query 死鎖。
- 驗證 force replay 執行期間收到新 force request 時，新 request 成為 pending；舊 request 完成不得清掉新 pending。
- 驗證新 pending 覆蓋舊 pending 後，舊 ID 永遠不會執行。
- 驗證 replay 使用執行當下 viewport，不使用建立 pending 時的舊 viewport。
- 驗證 force request 完成後若仍 blocked，最新 pending 保留到下一次 ready；若已可查，立即串行執行最新 pending。

### Response And Indicator Tests
- 驗證較舊 execution response 晚於新 response 返回時，舊 response 不會重畫 polyline、不會隱藏 loading，也不會覆蓋 error／low zoom 狀態。
- 驗證 loading 顯示 `V`、目前 zoom level、提示文字與 loading bar，且只在有效 response 完成主執行緒繪線 callback 後結束。
- 驗證 zoom `< 10` 的 force reload 開始時顯示 loading，成功後恢復常駐 `X`，不會隱藏。
- 驗證 zoom `< 10` 的 force reload 失敗時顯示 error，下一次使用者操作開始後恢復 `X`。
- 驗證 zoom `>= 10` 的 visible query 成功後隱藏，失敗後 error 保持到下一次使用者操作。
- 驗證 background refresh 不顯示 loading 或 error，但 zoom `< 10` 時仍維持低縮放提示。
- 以 instrumentation 驗證指示器位於主地圖底部 toast 區域，包含 icon、等待條、縮放級數與提示文字，且不顯示全螢幕 overlay、不禁用按鈕。

## Regression Plan
- 驗證圖層切換、測距、無側溝回報、選點、定位回正、編輯、檢視、單一側溝檢視、草稿恢復與 inspect preview 流程仍可正常操作。
- 驗證初始載入與系統重建後恢復仍可取得 scope 資料，且不新增可見 loading 或錯誤提示。
- 驗證非主地圖頁面不出現新指示器，既有 `MainBlockingUiController` 與 `inspectLoadingOverlay` 行為不變。
- 驗證快速連續拖拉、force reload 與返回流程不會因過期 response 畫回舊 viewport 或提前隱藏指示器。

## Risks
- Google Maps 的 camera callback 可能交錯，必須以 gesture active 與一次性 location-recenter reason 防止誤判或重複查詢。
- force queue 與非同步 scope response 具有兩種不同 ID；若混用可能清錯 pending 或讓舊 response 更新畫面。
- 新底部指示器可能與 bottom sheet、測距面板或 system inset 重疊，需以 instrumentation 在 Android 9 以上代表裝置驗證。
- force reload 在 zoom `< 10` 仍會查詢，這是編輯／檢視／草稿／inspect／定位後同步資料的明確例外，UI 完成後仍須回到低縮放常駐提示。

## Rollback Plan
- 若驗證失敗，回退本次新增的三入口路由、force queue、camera 旗標與底部指示器檔案，恢復原本 `Toast + onCameraIdle` 流程，不保留半套新門檻或狀態。

## Current Behavior
- 主地圖目前在每次 `onCameraIdle` 後直接進入固定 silent 的 debounce 流程，無法區分 gesture 與程式化移鏡。
- `ScopeMapCoordinator.loadDebounced()` 會先更新 `lastLoadTime`，載入回饋使用 Toast，且現有 feedback pending 無法區分並行 response。
- 編輯、檢視、草稿與 inspect refresh 共用載入流程，主地圖尚無專用底部指示器與 force reload queue。

## Expected Behavior
- 主地圖依每次 idle 的目前 zoom 顯示正確底部狀態；低於 10 級保持可見，符合條件的使用者互動才進一般 query。
- 初始／系統恢復、一般 gesture 與強制同步各走唯一入口；blocked force 只保留最新一筆，且不會被舊 callback 清除。
- 可見 query 在有效 response 完成畫面繪製後才收斂 UI，過期 response 不得覆蓋新 viewport；新指示器不影響既有 blocking overlay 或非主地圖功能。

## Open Questions
- 無
