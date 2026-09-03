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
