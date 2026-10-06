# Repository Analysis

## Current Behavior

- `MapWorkspaceFragment.handleInspectEditResult()` 建立 `AddGutterBottomSheet` 編輯模式並顯示於主地圖上方。
- `AddGutterBottomSheet.setupBottomSheetBehavior()` 替 Dialog 的 `Window.Callback` 安裝觸控轉送：ACTION_DOWN 依 `isTouchOutsideSheetContent()` 分類，後續事件沿用 `routeToActivity`，並由 Activity dispatch。
- 使用者回報 sheet 上方可見小地圖不能拖動；目前尚未取得執行時觸控事件或裝置重現紀錄。

## Expected Behavior

- 編輯 sheet 開啟期間，上方可見地圖的拖曳手勢應完整送到承載 Google Map 的 Activity，使地圖鏡頭平移。
- sheet 表單範圍的觸控仍由 sheet 正常處理。

## Affected Modules

- `gutter/AddGutterBottomSheet.kt`：Dialog 觸控事件分類與轉送。
- `map/MapWorkspaceFragment.kt`：編輯模式開啟 sheet 並保留主地圖。
- `app/src/main/res/layout/bottom_sheet_add_gutter.xml`：確認表單實際佔位與地圖可見範圍。

## Dependencies

- `BottomSheetDialog` 視窗範圍、sheet 容器位置及系統觸控事件座標。
- Activity 內 Google Map view 的事件派送與地圖手勢設定。

## Risks

- 修改觸控轉送時可能讓 sheet 控制項收到地圖手勢，或讓地圖拖曳被表單／底層 overlay 攔截。
- 直接重派 MotionEvent 需正確處理 screen 與 target view 座標及完整 DOWN/MOVE/UP/CANCEL 序列。
- 只依靜態程式推論根因有誤判風險，需先用編輯頁重現並確認觸控事件去向。

## Change Error Assessment

單純修正邊界判斷不必然造成錯誤，但若改動方式不保留現有事件串流約束，可能引入下列回歸：

| Risk | Failure mode | Guard |
|---|---|---|
| 高 | 不論起點都轉送到 Activity，可能讓表單點擊同時操作底層地圖或地圖按鈕 | 只按 ACTION_DOWN 起點選定目標，整段手勢固定送到同一目標 |
| 高 | 每個 MOVE 重新判斷邊界，跨越 sheet 邊緣時事件在 Dialog 與 Activity 間拆分，造成地圖手勢中斷或 sheet 誤拖 | DOWN 後鎖定路由，直到 UP/CANCEL；多指事件沿用同一路由 |
| 中 | 將 `rawX/rawY` 當成目標視窗的局部座標使用，或反之，可能使底層 view hit-test 偏移 | 使用螢幕座標做區域判斷；重派前確認 Activity decor 所需座標框架，並以地圖與測距按鈕操作驗證 |
| 中 | 假設所有地圖區事件都能進入 Dialog callback；若事件其實由 Activity 或其他 overlay 接收，改 Dialog callback 不會修到問題 | 重現時同時記錄 Dialog callback 與 Activity/map 收到的事件，確認實際入口 |
| 中 | 只處理單指拖曳或漏掉 CANCEL，可能殘留路由狀態、破壞縮放等多指手勢 | 驗證多指與取消序列，並在 UP/CANCEL 後清除路由狀態 |

判斷：此修改方向可行，沒有從靜態程式碼可證明的必然副作用；主要風險是事件目標切換或座標框架處理錯誤。現有路由已在 DOWN 鎖定並於 UP/CANCEL 清除，修正應保留這項行為。具體座標與視窗入口仍需執行時驗證，不能僅靠程式碼排除回歸。

## Unknowns

- 問題是否只在某種 BottomSheetDialog 視窗尺寸／Android 版本出現。
- 目前事件是被 Dialog/window、sheet root、主畫面 overlay，還是 Activity 轉送座標所攔截或偏移。

## Root Cause Finding

根因信任度：**95%**。`isTouchOutsideSheetContent()` 以 `rawX/rawY` 和 screen bounds 分類觸控，但原本將 Dialog 的 `MotionEvent` 未做座標轉換就直接傳入另一個視窗的 `Activity.dispatchTouchEvent(event)`。Dialog 視窗在螢幕下方，兩個視窗原點不同；因此 map-area 觸控的局部座標可能相對 Activity decor 偏移，令 Activity 的 view hit-test 無法把事件送到地圖。修正以複製事件並依兩個 decor 的 screen origin 差值偏移後再 dispatch。實機事件追蹤尚未取得，仍需補強此根因的執行時證據。

## Evidence

| Claim | Evidence |
|---|---|
| 編輯流程在主地圖上建立並顯示編輯 sheet | `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：`handleInspectEditResult()` |
| 觸控路由依 sheet 容器螢幕位置分類並轉送到 Activity | `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：`setupBottomSheetBehavior()`、`isTouchOutsideSheetContent()` |
| 原轉送路徑未調整事件座標 | `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：原 `requireActivity().dispatchTouchEvent(event)` 直接收到 Dialog callback 的 event |
| sheet 高度及容器幾何在顯示時被動態設定 | `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：`setupBottomSheetBehavior()` |
