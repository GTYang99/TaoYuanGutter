# Fix Plan

Date: 2026-09-03
Task: DBG-0903
Phase: debug

## Goal

修正登入後主地圖第一次不自動載入側溝，以及 `rvWaypoints` 點選無法穩定跳轉到 `GutterFormActivity` 的問題。

## Scope

- 只改登入後主地圖載入時機、底部列表點擊路由與必要的驗證測試。
- 不改需求內容，不調整側溝資料結構，不做無關重構。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/res/layout/bottom_sheet_add_gutter.xml`
- `app/src/main/res/layout/activity_gutter_form.xml`

## Implementation Steps

1. 先在主地圖登入後流程補齊一次明確的 scope reload 觸發點，讓定位完成後不必等使用者手動拖動地圖。
2. 檢查 `MainActivity` 與 `MapWorkspaceFragment` 的 reload 條件是否被 camera idle、busy 狀態或遮罩狀態擋住，必要時把首次載入與手動拖曳載入拆開。
3. 把 `rvWaypoints` 的點擊處理改成列表項目本身的明確 click callback，避免只靠 `GestureDetector` 攔截整個 RecyclerView。
4. 確認每個 waypoint 點擊後都穩定進到宿主回呼，再導向 `GutterFormActivity`。
5. 補上實機驗證重點，確認兩個問題都不再需要額外操作才會出現預期畫面。

## Test Plan

- 登入後觀察主地圖是否在首次定位完成後自動載入側溝，不需要再拖動地圖。
- 實機點選 `AddGutterBottomSheet` 的 `rvWaypoints` 每一列，確認都會打開對應的表單頁。
- 重新進出側溝流程一次，確認沒有影響原本的新增 / 編輯 / 檢視切換。

## Regression Plan

- 確認手動拖曳地圖時，原本的 viewport reload 仍然正常。
- 確認底部列表的拖曳排序、刪除按鈕與虛擬點操作沒有被新的點擊處理干擾。
- 確認 `GutterFormActivity` 的既有資料回填與返回流程沒有改壞。

## Risks

- 若首次載入與手動拖曳共用同一條 reload 路徑，容易產生重複 API 呼叫。
- 點擊事件改成 item click 後，可能需要保留拖曳把手的獨立觸控區，避免與排序功能衝突。
- 若主地圖目前的 busy 狀態判斷過嚴，首次定位後的 reload 仍可能被擋住。

## Rollback Plan

- 若驗證失敗，先回退點擊路由與首次 reload 的變更，只保留既有載入流程。

## Current Behavior

- 登入後地圖先顯示預設鏡頭，再跳到使用者位置。
- 側溝清單不會在那一刻穩定載入，常要等使用者再移動地圖才會觸發。
- `rvWaypoints` 在實機點選時沒有穩定導到 `GutterFormActivity`。

## Expected Behavior

- 登入完成並定位後，主地圖應自動載入目前視野內的側溝。
- 使用者不需要額外拖動畫面，側溝就會出現。
- 點選 `rvWaypoints` 任一列都能穩定進入表單頁。

## Open Questions

- 目前首次定位後，真正擋住 reload 的條件是 camera idle、busy 狀態，還是 location callback 的順序。
- `rvWaypoints` 的 tap 被吃掉，是 RecyclerView 事件分派問題，還是 item 內部子元件攔截了點擊。

## Re-marked Debug Cause

根因已確認為 BottomSheet 宿主解析錯誤：`AddGutterBottomSheet` 實際由 `MapWorkspaceFragment.childFragmentManager` 管理，但原程式只從 `requireActivity()` 取得 `LocationPickerHost`，導致 `MainShellActivity` cast 失敗並在 `openWaypointAt()` 提前 return。修正優先從 `parentFragment` 解析 callback host，並保留 Activity fallback。

## Follow-up Fix Scope: Submit Long-Press Simulation

- 將 `triggerNetworkTimeoutTest()` 的 host 解析改為共用 `locationPickerHost()`。
- 將 `triggerStoreDitchConflictTest()` 的 host 解析改為共用 `locationPickerHost()`。
- 保留 `ENABLE_GROUP_SIMULATION` 開關與現有測試選單文字，不調整送出 API 主流程。
- 本次只修長按測試 Alert 後續動作，不做其他 callback host 重構。

## Follow-up Fix Scope: Four-Issue Regression

- 將主地圖側溝圖層載入門檻統一為 `MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM = 16f`。
- `MapWorkspaceFragment` 的 user interaction / background / force scope load 不再硬寫或繞過門檻，改共用主地圖指示器的門檻常數。
- `AddGutterBottomSheet` 中所有正式 callback 皆透過 `locationPickerHost()` 解析 `MapWorkspaceFragment` host，保留 Activity fallback。
- 長按送出測試 Alert 只顯示測試 dialog，不再呼叫正式 `onStoreDitchNetworkClosed()` 復原流程。
- `storeDitch` 網路錯誤改分辨真正 timeout 與一般連線失敗，只有 timeout 顯示「網路連線逾時」。
- 409 照片認領衝突改顯示「資料上傳狀態待確認」，避免誤導成等待逾時。
- 補單元測試鎖定 16 級門檻與連線失敗/逾時分類。

## Follow-up Fix Scope: Inspect Route Fit Regression

### Goal

恢復檢視側溝時的自動縮放行為：進入檢視、從節點表單返回檢視、從檢視切到編輯時，主地圖都要能在表單上方看見整條側溝。

### Minimum Implementation Steps

1. 在 `MapWorkspaceFragment` 新增集中 helper，例如 `fitInspectRouteAboveSheet(waypoints)`，統一呼叫 `mapCameraController.fitCameraToWaypointsWithViewportFraction(..., viewportHeightFraction = 1.0 / 3.0, resetPaddingAfter = false)`。
2. 將 `openInspectBottomSheet()` 的檢視進入 fit 改用該 helper。
3. 將 `handleInspectSheetActivityResult()` 的檢視返回 fit 改用該 helper。
4. 將 `handleInspectEditResult()` 的編輯進入 fit 改用該 helper。
5. 在 `onSheetViewportInsetChanged()` 收到 bottom sheet 實際高度後，如果目前是 inspect/edit 狀態且有路線資料，安排一次延後 refit，避免一開始 `sheet.show(...)` 尚未量測完成就 fit。

### Validation

- 實機檢視一條長側溝，確認檢視表單上方能看到整條側溝。
- 從檢視頁點任一節點進入表單再返回，確認整條側溝仍保持在表單上方。
- 從檢視切到編輯 bottom sheet，確認 bottom sheet 展開後路線不被覆蓋。
- 回歸確認新增側溝、16 級載入門檻、長按送出測試 Alert、刪除側溝確認仍正常。

### Regression Risk

- 若 inset 回報時每次都 refit，可能干擾使用者手動拖曳地圖；implementation 需避免在非 inspect/edit 狀態或無路線資料時重複觸發。
- 若使用 `resetPaddingAfter = false` 後沒有在離開表單時清掉 persistent inset，可能影響回主地圖視野；離開/關閉流程需維持既有 `setPersistentBottomInset(0)`。
