# Root Cause

Date: 2026-09-03
Task: DBG-0903
Phase: debug

## Summary

這兩個問題目前看起來都不是單一畫面壞掉，而是「事件有進來，但沒有穩定走到下一個動作」。

## Suspected Root Causes

### 1. 登入後地圖先跳到使用者位置，但側溝沒有自動載入

- `MainActivity.onMapReady()` 先把鏡頭移到預設位置，再交給定位流程把鏡頭移到使用者位置。
- 目前主地圖的側溝載入是掛在 `loadGuttersByViewport()`、`requestForceScopeReload()`、`handleMainMapCameraIdle()` 這幾條路徑上，不是「定位成功就立刻載入」。
- 如果使用者位置回調先到，但對應的 camera idle / force reload 條件還沒成立，第一次 scope query 就會被延後或跳過。
- 實機上看起來像是「要再手動移動畫面一次，側溝才開始載入」。

### 2. 點選 `rvWaypoints` 沒有進到 `activity_gutter_form.xml`

- `AddGutterBottomSheet` 目前是用 `RecyclerView.SimpleOnItemTouchListener` + `GestureDetector` 去攔截整個列表點擊。
- 這種做法依賴單擊事件有完整送到 `onSingleTapUp()`，但實機上容易被子元件、拖曳行為或 RecyclerView 的事件分派吃掉。
- `openWaypointAt()` 本身是會呼叫宿主的 `openWaypointForEdit()` / `openWaypointForInspect()`，所以問題比較像是「觸發點沒有穩定進來」，不是表單頁面本身無法啟動。
- 因此 `activity_gutter_form.xml` 沒跳出來，較像是 `rvWaypoints` 的點擊路由不可靠，而不是表單 layout 或 activity 建立流程本身壞掉。

## Evidence To Recheck

- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`

## Notes

- 這份 root cause 目前是依程式路徑與行為推定，還需要實機 log 佐證。
- 下一步應先把「首次定位後是否有觸發 scope reload」與「列表 item tap 是否真的進到 host callback」分開驗證。

## Remaining Issue Annotation: rvWaypoints

此問題的根因已確認。`AddGutterBottomSheet` 是由 `MapWorkspaceFragment.childFragmentManager` 顯示，但 `openWaypointAt()` 原本只用 `requireActivity() as? LocationPickerHost` 取得宿主；實際 Activity 是 `MainShellActivity`，並未實作 `LocationPickerHost`，因此 cast 失敗後直接 return。

因此點擊事件其實可能已抵達 row listener，但在 `AddGutterBottomSheet.openWaypointAt()` 取宿主時被丟棄；問題不是 `activity_gutter_form.xml` 或 `GutterFormNavigator` 缺少導航。

修正方式是優先從 `parentFragment` 取得 `MapWorkspaceFragment` 這個 callback host，並保留 Activity fallback。AC-002 需在真機重新確認。

## Follow-up Root Cause: Submit Long-Press Simulation Alert

`ENABLE_GROUP_SIMULATION` 開啟時，長按送出按鈕會顯示第一層「測試選單」。但使用者點選「模擬網路逾時」或「模擬照片認領失敗(409)」後，`triggerNetworkTimeoutTest()` 與 `triggerStoreDitchConflictTest()` 仍使用 `activity as? LocationPickerHost` 取得 host。

在目前登入後的主流程中，`LocationPickerHost` 一樣由 `MapWorkspaceFragment` 實作，不是 `MainShellActivity`。因此這兩個測試方法會提前 return，後續網路逾時 Alert / 409 Alert 不會穩定出現。

此問題與 `rvWaypoints` 未導向表單屬於同一類根因：BottomSheet 內部 callback host 解析仍有舊 Activity-only 寫法。

## Regression RCA: 2026-09-03 Four-Issue Follow-up

### 1. Main map gutter layer loads below the required zoom

`MapWorkspaceFragment` 的主流程仍用 `zoom < 10f` 決定是否查詢側溝圖層，但主地圖指示器狀態機使用 `MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM`，且 UI 文案要求 16 級。這造成 API 載入門檻、指示器門檻與畫面文案不同步。

### 2. Submit long-press simulation changes the real form flow

長按測試 Alert 原本在關閉後會呼叫 `onStoreDitchNetworkClosed()`，這是正式 storeDitch 失敗後的復原流程，會存草稿、關閉 bottom sheet、清工作圖層並回主地圖。因此測試選項看起來像讓新增/編輯功能失效。

### 3. Delete gutter does not open confirmation

編輯模式的 `btnDeleteGutter` 還使用 `requireActivity() as? LocationPickerHost`。登入後主流程的 callback host 是 `MapWorkspaceFragment`，不是 `MainShellActivity`，所以 cast 失敗後刪除確認流程沒有被呼叫。

### 4. Add gutter shows timeout text for non-timeout failures

新增送出流程把所有 `UploadFailureClassifier.isNetworkFailureMessage()` 命中的錯誤都導向「網路連線逾時」Alert；其中包含 `failed to connect`、`unable to resolve host`、`connection reset` 這類立即連線失敗。409 照片認領衝突的 dialog title 也寫成「網路連線逾時」，造成非 timeout 情境被顯示成 timeout。

## Follow-up RCA: 2026-09-04 Inspect Gutter Route No Longer Fits Above Form

### Issue

檢視側溝時，原本主地圖會自動縮放，讓檢視表單上方能看到整條側溝；目前進入檢視/編輯流程後，路線仍可能被下方表單或檢視頁覆蓋，看起來像自動縮放失效。

### Updated Error Source

- `MapCameraController.fitCameraToWaypointsWithViewportFraction()` 預設 `resetPaddingAfter = true`。
- `MapCameraController.fitCameraToWaypoints()` 在動畫開始前會用 `bottomOffsetRatio` 暫時加大地圖底部 padding，但動畫完成或取消後，如果 `resetPaddingAfter = true`，會把 padding 還原成 `persistentBottomInsetPx`。
- `MapWorkspaceFragment.openInspectBottomSheet()` 仍呼叫 `fitCameraToWaypointsWithViewportFraction(start.routeWaypoints, viewportHeightFraction = 1.0 / 3.0)`，沒有覆寫 `resetPaddingAfter`。
- `MapWorkspaceFragment.handleInspectSheetActivityResult()` 仍用同樣預設值重新 fit `inspectWaypoints`。
- `MapWorkspaceFragment.handleInspectEditResult()` 在 `sheet.show(...)` 後立刻 fit 編輯路線，但這時 bottom sheet 實際高度可能尚未回報；之後 `onSheetViewportInsetChanged()` 只更新 persistent bottom inset 與指示器位置，沒有針對目前 inspect/edit 路線重新 fit。

### Root Cause

根因不是「沒有呼叫自動縮放」，而是檢視/編輯側溝流程需要保留表單上方的可視區，但目前這三個入口仍使用會自動還原 padding 的預設 camera fit 行為。

也就是說，camera fit 當下可能短暫用 1/3 viewport 計算成功；但動畫結束後 padding 被還原，檢視表單或編輯 bottom sheet 覆蓋地圖下方時，整條側溝不再保證留在表單上方的可視區內。

另外，編輯流程還有時序問題：`handleInspectEditResult()` 先顯示 `AddGutterBottomSheet` 再立即 fit，但 bottom sheet 真實高度通常稍後才由 `onSheetViewportInsetChanged()` 回報。目前 inset 回報後只改 padding，不會再根據真實表單高度重算鏡頭，因此實機上會不穩定。

### Why Recent Code Makes The Source Clearer

目前新增側溝與節點返回流程已有多處改成 `resetPaddingAfter = false`，例如新增/成長 refit 與表單返回 refit；因此問題範圍已縮小到檢視側溝專用路徑：

- 進入檢視：`openInspectBottomSheet()`
- 檢視表單返回：`handleInspectSheetActivityResult()`
- 從檢視切到編輯：`handleInspectEditResult()`
- bottom sheet 高度改變後：`onSheetViewportInsetChanged()`

### Minimum Fix Direction

- 將 inspect preview / inspect form return / inspect edit entry 的 viewport fit 改成保留可視區，也就是明確傳入 `resetPaddingAfter = false`。
- 編輯 bottom sheet 顯示後，當 `onSheetViewportInsetChanged()` 收到實際高度時，若目前仍在 inspect/edit 路線，需以目前 `currentWaypoints` 或 `inspectWaypoints` 再做一次 refit。
- 建議新增一個集中方法，例如 `fitInspectRouteAboveSheet(waypoints)`，避免同一組參數散落在三個入口，降低下次 regression 風險。

## Confirmed Follow-up RCA: ISS-DBG-0903-005 Samsung Foldable Long Route Fit

### Error

Samsung 折疊手機檢視長線段時，縮放後 Google Maps logo 會被推到畫面約 1/3 高度，長側溝路線仍可能沒有被正確保留在表單上方可視區。

### Debug Analysis

- `AddGutterBottomSheet.setupBottomSheetBehavior()` 使用 `resources.displayMetrics.heightPixels / 2` 設定 sheet 高度與 `peekHeight`。
- `AddGutterBottomSheet.notifySheetViewportInset()` 回報的是 sheet 目前實際可見高度。
- `MapWorkspaceFragment.onSheetViewportInsetChanged()` 將該高度存成 `currentSheetBottomInsetPx`，再呼叫 `MapCameraController.setPersistentBottomInset()`，因此 GoogleMap 已經保留了一次 bottom sheet 高度。
- `MapWorkspaceFragment.fitInspectRouteAboveSheet()` 又以 `viewportHeightFraction = 1.0 / 3.0` 呼叫 camera fit。
- `MapCameraController.fitCameraToWaypointsWithViewportFraction()` 會把 `1/3` 可視區轉成 `bottomOffsetRatio = 2/3`。
- `MapCameraController.fitCameraToWaypoints()` 最後套用的 bottom padding 是 `persistentBottomInsetPx + (screenHeight * bottomOffsetRatio)`。
- Google Maps logo 會遵守 `GoogleMap.setPadding()`；因此 logo 被推到畫面 1/3，是 bottom padding 過大的直接視覺證據。

### Root Cause

Root cause 是高度保留被重複計算：bottom sheet 實際高度已經透過 `persistentBottomInsetPx` 套進 GoogleMap padding，但 inspect route fit 又額外用完整 `displayMetrics.heightPixels * 2/3` 計算第二段 bottom offset。

在一般手機上這可能只是過度上推；在 Samsung foldable 上，`displayMetrics.heightPixels` 更可能不等於實際 map container 高度，因為折疊狀態、工作列/導覽列、多視窗或 app window bounds 都會影響真實可視區，於是錯誤被放大。

### Secondary Cause

展開順序是次因。`sheet.show(...)` 後第一次 fit 可能早於 bottom sheet 完整量測；接著 `onSheetViewportInsetChanged()` 觸發 refit，但 refit 仍使用同一個「persistent inset + screen ratio」公式，所以它會重算出同樣偏大的 bottom padding。

### Fix Strategy

- inspect/edit route fit 不應同時使用 persistent sheet inset 與 full-screen ratio offset。
- 應改成以實際 map view height 與實際 sheet inset 計算可視區，或直接以明確 top/right/bottom/left padding fit bounds。
- bottom sheet 實際 inset 尚未回報前，不應以 `displayMetrics.heightPixels` 推估 foldable 的可視高度。
- inset 變動後可以 refit，但必須避免將 sheet inset 再加上一段 full-screen `2/3` offset。

## Regression RCA: ISS-DBG-0903-007 Main Map Stays In Inspect Viewport After Return

### Error

多種實機測試發現，檢視側溝後返回主畫面時，主地圖沒有恢復到正常全螢幕可視範圍，而是仍像檢視表單存在時一樣只剩上方約 1/3 高度可用。使用者回到主地圖後滑動地圖，無法用完整螢幕尺寸查看側溝。

### Root Cause

Root cause 是上一個 `ISS-DBG-0903-005` 修正引入的收尾漏項：檢視路線 fit 改成使用 explicit GoogleMap padding 並設定 `resetPaddingAfter = false`，但從 `GutterInspectActivity` 非編輯返回主地圖的 terminal path 沒有清除這個 padding。

因此檢視畫面關閉後，overlay/form 已經不在，但 GoogleMap 仍保留檢視用的 bottom padding。後續主地圖拖曳、縮放與 viewport 查詢都在錯誤的 1/3 可視區中運作。

### Debug Analysis

- `MapWorkspaceFragment.fitInspectRouteAboveSheet()` 會呼叫 `MapCameraController.fitCameraToWaypointsWithBottomPadding(..., resetPaddingAfter = false)`。
- `MapCameraController.fitCameraToWaypointsWithBottomPadding()` 會直接呼叫 `GoogleMap.setPadding(...)` 套用 explicit bottom padding。
- 因為 `resetPaddingAfter = false`，camera 動畫完成後不會自動把 padding 還原。
- `MapWorkspaceFragment.inspectLauncher` 的非編輯返回分支會清除 `isInEditingMode`、`inspectPreviewIntent`、`shouldReturnToInspectPreview`、reference route、preview layer 與 markers，然後呼叫 `loadGuttersByViewport(showFeedback = true)`。
- 但該分支沒有呼叫 `mapCameraController.setPersistentBottomInset(0)`，也沒有清 `currentSheetBottomInsetPx` / `lastInspectRouteRefitInsetPx` / indicator inset。
- 所以主地圖畫面已返回，但 GoogleMap padding 仍停留在檢視 fit 後的狀態。
- legacy `MainActivity.inspectLauncher` 也有相同型態的非編輯返回分支，需一併 audit，避免離線或舊入口保留同一 bug。

### Fix Strategy

- 新增集中 helper，例如 `restoreMainMapViewport()`。
- helper 需清除 `currentSheetBottomInsetPx`、`lastInspectRouteRefitInsetPx`，同步 `mainMapLoadIndicatorController.setBottomInset(0)`，並呼叫 `mapCameraController.setPersistentBottomInset(0)` 釋放 GoogleMap padding。
- 所有離開 inspect/edit/form overlay 回到主地圖的 terminal path 都要呼叫 helper。
- 保留檢視或編輯 overlay 仍可見時的 explicit inspect padding，不要在返回檢視預覽或切入編輯中途過早清掉。
- 一併檢查 inspect launch 失敗、inspect API error、delete/update 成功/失敗後返回主圖的 early exit path。

## RCA: ISS-DBG-0903-008 Layer Disabled But Gutter Inspection Still Opens

### Error

使用者關閉側溝/本次計畫調查相關圖層後，主地圖上仍可點出「檢視側溝」。使用者體感是圖層已關閉，但互動熱區還留著。

### Root Cause

Root cause 是圖層「可見狀態」與 polyline「可點擊狀態」沒有綁在同一個 gate。

`MapWorkspaceFragment.onOverlayTogglesChanged()` 只把 `showPlan` 傳給 `ScopeGutterPolylineController.setVisible(showPlan)`，這會切換已載入 polyline 的 `isVisible`。但 `ScopeGutterPolylineController.drawFeatures()` 建立 polyline 時已設定 `.clickable(true)`，後續 `setVisible(false)` 沒有同步關閉 clickable，也沒有清掉 polyline click listener 的入口判斷。

因此即使視覺圖層關閉，GoogleMap 內既有的 polyline object 仍可能保留互動狀態；`MapWorkspaceFragment.setOnPolylineClickListener` 又只檢查測量模式，沒有檢查目前圖層是否允許檢視，所以仍會呼叫 `openInspectBottomSheet(polyline)`。

### Debug Analysis

- `LayersBottomSheet` 的「本次計畫調查」checkbox 對應 `showPlan`。
- `MapWorkspaceFragment.onOverlayTogglesChanged()` 將 `showPlan` 套到 `scopeGutterPolylineController.setVisible(showPlan)`。
- `ScopeGutterPolylineController.setVisible()` 只同步 `isVisible`。
- `ScopeGutterPolylineController.drawFeatures()` 的 inner polyline 預設 `clickable = true`。
- `MapWorkspaceFragment.setOnPolylineClickListener` 沒有檢查 `mapOverlayController.currentState().showPlan`。
- 所以 toggle off 只關閉視覺，不保證關閉 interaction。

### Fix Strategy

- `MapWorkspaceFragment.setOnPolylineClickListener` 應先檢查同一個 overlay state；圖層關閉時直接 return。
- 若要更完整，`ScopeGutterPolylineController` 的 handle 應擴充 `isClickable`，讓 `setVisible(false)` 可同時關閉 clickable。
- 需先確認產品語意：「本次計畫調查」到底是 `showPlan` 的可檢視 scope polyline，還是 `showPossible` 的 `roadServey` WMS；目前程式中檢視側溝是 `showPlan` 這條路。

## RCA: ISS-DBG-0903-009 No-Ditch WMS Tap Target Too Small

### Error

無側溝點位圖層開啟後，使用者看得到 WMS 上的無側溝點，但實際點擊不容易命中，體感範圍偏小。

### Root Cause

Root cause 是「顯示」與「互動命中」不是同一層，而且互動命中沒有容錯範圍。

無側溝點位視覺上是 `MapOverlayController` 加上的 WMS `TileOverlay`，但 GoogleMap 的 WMS tile 本身不可點擊。程式另用 WFS 載入 marker 作為可點物件，並在一般地圖點擊時用 WMS `GetFeatureInfo` 查單一 I/J 像素作為 fallback。

這代表使用者看到的是 WMS 圖示，但真正可點的是 WFS marker 或 GetFeatureInfo 的單點 pixel。只要 WMS 符號、WFS 座標、使用者手指落點與 server feature pixel 有一點偏差，就容易點不到。

### Debug Analysis

- `MapOverlayController.applyWmsOverlays()` 開啟 `map_no_ditch_points` WMS tile overlay。
- `MapWorkspaceFragment.onOverlayTogglesChanged()` 開啟無側溝點位後呼叫 `loadNoDitchPointsForVisibleArea()`。
- `loadNoDitchPointsForVisibleArea()` 透過 WFS BBOX 建立 GoogleMap markers，但只限目前 visible BBOX 回傳的資料。
- marker icon 使用 `20dp` bitmap，實際視覺與命中可能偏小。
- `handleMainMapTap()` 在非回報模式且 showNoDitchPoints 開啟時，呼叫 `fetchAndShowNoDitchPointNoteAt()`。
- `fetchAndShowNoDitchPointNoteAt()` 以 `projection.toScreenLocation(targetLatLng)` 取得單一點的 `I/J`，對 WMS `GetFeatureInfo` 查詢 `FEATURE_COUNT`，但沒有 radius / buffer / 多點 sampling。
- 在高 DPI 螢幕、fractional zoom、WMS/WFS 坐標有細微差異，或使用者指腹點擊偏移時，單點查詢很容易 miss。

### Fix Strategy

- 優先以已載入的 WFS no-ditch points 做 nearby search，使用 zoom-aware meter radius 當 touch tolerance。
- nearby search 沒命中時，再做 WMS GetFeatureInfo fallback。
- fallback 可改成小範圍 pixel grid sampling，或確認 GeoServer 是否支援 buffer/vendor parameter。
- marker 視覺大小與互動 hit target 可分離處理，避免 WMS 看起來小但手指命中過難。
- 關閉無側溝圖層時，WMS overlay 與 WFS marker lifecycle 必須同步清除。
