# Issue Log

## ISS-FEAT-0916-1-001
- **Task:** feat-0916-1
- **Phase:** planning
- **Category:** requirement_gap
- **Priority:** P1
- **Status:** resolved
- **Title:** 主地圖測距按鈕在 Dialog 型底部面板上方的呈現方式未定義
- **Impact:** 需求指定既有 Activity `btnMeasureDistance` 位於最上層，但目前兩個 BottomSheetDialogFragment 是獨立 Dialog 視窗。未確認是否容許代理入口或須改變面板承載架構前，無法安全選擇實作方式或驗收標準。
- **Evidence:** `AddGutterListBottomSheet` 與 `AddGutterBottomSheet` 均繼承 `BottomSheetDialogFragment`；`btnMeasureDistance` 位於 `activity_main.xml`。
- **Resolution:** 2026-09-16 產品確認允許在兩個面板中放置同一測距功能的代理入口。
- **Next action:** 按此決策完成實作規劃。
- **Owner:** product / planning

## ISS-FEAT-0916-1-002
- **Task:** feat-0916-1
- **Phase:** planning
- **Category:** requirement_gap
- **Priority:** P2
- **Status:** resolved
- **Title:** 清單測距期間「任何側溝線段」的圖層範圍未定義
- **Impact:** 目前工作側溝圖層與主地圖 scope 側溝圖層由不同控制器管理；隱藏範圍影響使用者可見地圖內容及回復邏輯。
- **Evidence:** `GutterMapController` 管理工作圖層，`ScopeGutterPolylineController` 管理既有 scope 側溝線段。
- **Resolution:** 2026-09-16 產品確認清單測距期間隱藏所有既有 scope 側溝線段，測距退出並恢復清單時重新顯示。
- **Next action:** 按此決策完成實作規劃。
- **Owner:** product / planning

## ISS-FEAT-0916-1-003
- **Task:** feat-0916-1
- **Phase:** planning
- **Category:** requirement_gap
- **Priority:** P2
- **Status:** resolved
- **Title:** 測距期間系統返回鍵的優先行為未定義
- **Impact:** 需避免返回鍵直接關閉或破壞正在編輯的面板／草稿。
- **Evidence:** 需求僅指定「關閉測距模式後」恢復面板，未定義系統返回鍵。
- **Resolution:** 2026-09-16 產品確認測距期間的 Android 返回鍵先退出測距並回復原面板。
- **Next action:** 按此決策完成實作規劃。
- **Owner:** product / planning

## ISS-FEAT-0916-1-004
- **Task:** feat-0916-1
- **Phase:** plan_review
- **Category:** planning_gap
- **Priority:** P1
- **Status:** resolved
- **Title:** 測距退出後未規劃還原主地圖點擊 listener
- **Impact:** `DistanceMeasureManager.exit()` 將 `GoogleMap` 的 map-click 與 camera-move listener 設為 `null`；主地圖原本的 map-click listener 用於 `handleMainMapTap`。若未明確重綁，測距退出後主地圖點擊流程可能失效，違反 AC-004 與 AC-005。
- **Evidence:** `DistanceMeasureManager.kt` 的 `exit()`；`MapWorkspaceFragment.kt` 的地圖初始化設定 `setOnMapClickListener { handleMainMapTap(latLng) }`。
- **Planning response:** Plan revision defines `MapWorkspaceFragment` as listener owner and orders exit as measurement cleanup, normal map-click reinstallation, then UI restoration; focused verification is required.
- **Next action:** plan_review
- **Owner:** planning

## ISS-FEAT-0916-1-005
- **Task:** feat-0916-1
- **Phase:** plan_review
- **Category:** planning_gap
- **Priority:** P1
- **Status:** resolved
- **Title:** scope 側溝圖層回復策略未保留使用者原始顯示偏好
- **Impact:** 計畫僅稱量測結束後「恢復 scope 側溝可見性」。使用者可能原本已關閉 `showPlan`；一律重新顯示會改變原本圖層設定，且量測期間圖層設定變更也沒有定義。
- **Evidence:** `ScopeGutterPolylineController.setVisible()` 會直接改變 polyline 可見性；`MapWorkspaceFragment.onOverlayTogglesChanged()` 以 `showPlan` 控制該可見性。
- **Planning response:** Plan revision retains the overlay preference during list measurement, forces only rendered scope polylines hidden, and reconciles with current `showPlan` only on exit; focused regression coverage is required.
- **Next action:** plan_review
- **Owner:** planning

## ISS-FEAT-0916-1-006
- **Task:** feat-0916-1
- **Phase:** plan_review
- **Category:** planning_gap
- **Priority:** P1
- **Status:** resolved
- **Title:** Android 返回鍵的攔截生命週期與回復順序未具體規劃
- **Impact:** OQ-003 要求返回鍵優先退出測距並回復面板；目前 `MapWorkspaceFragment` 未註冊返回鍵 callback，而面板的既有取消／dismiss 路徑會觸發關閉確認或 `onDismiss` 副作用。計畫未指明 callback 的擁有者、啟停時機與事件消費規則，無法保證資料不被意外關閉。
- **Evidence:** `MapWorkspaceFragment.kt` 無 `OnBackPressedCallback`；`AddGutterListBottomSheet.onCancel()` 會呼叫 `confirmClose()`；`AddGutterBottomSheet.onDismiss()` 會回呼 `onWaypointsChanged(null)`。
- **Planning response:** Plan revision defines a view-lifecycle-bound, source-measurement-only Back callback that invokes the common exit path and is disabled after restoration; focused UI/integration coverage is required.
- **Next action:** plan_review
- **Owner:** planning

## ISS-FEAT-0916-1-007
- **Task:** feat-0916-1
- **Phase:** plan_review
- **Category:** planning_gap
- **Priority:** P1
- **Status:** resolved
- **Title:** 清單面板的可逆暫時隱藏合約被誤當成既有能力
- **Impact:** 計畫步驟 3 寫為清單與編輯面板均使用既有 `hideSelf()`，但目前只有 `AddGutterBottomSheet` 實作 `hideSelf()`／`showSelf()`；`AddGutterListBottomSheet` 沒有等效方法。若開發者改以 `dismiss()` 填補，會違反草稿與清單狀態保留需求。
- **Evidence:** `AddGutterBottomSheet.kt` 定義 `hideSelf()` 與 `showSelf()`；`AddGutterListBottomSheet.kt` 僅有 `onStart()`、`onCancel()` 與 `onDestroyView()`，沒有可逆隱藏 API。
- **Planning response:** Plan step 3 now explicitly requires `AddGutterListBottomSheet.hideForMeasure()` and `showAfterMeasure()` using a reversible Dialog decor visibility/translation implementation. Step 4 requires `MapWorkspaceFragment` to retain and restore that same list fragment instance without a Fragment transaction. The test plan verifies restore does not trigger list close confirmation, cancellation, host close callback, or a new fragment.
- **Next action:** plan_review
- **Owner:** planning
