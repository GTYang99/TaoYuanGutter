# Repository Analysis

## Planning Baseline
- Branch: `feat/表單合併測距功能`
- Base revision: `ac1eacbc236385e131b523ac63418e278768ec20` (`ac1eacb`)
- This analysis was rechecked against the selected base after the worktree was created.

## Current Behavior
- `MapWorkspaceFragment` 綁定 Activity 版面中的 `btnMeasureDistance`，以 `toggleMeasureMode()` 呼叫 `MeasureModeUiController` 與 `DistanceMeasureManager`。
- `AddGutterListBottomSheet` 與 `AddGutterBottomSheet` 都是 child FragmentManager 上的 `BottomSheetDialogFragment`；它們會以獨立 Dialog 視窗覆蓋 Activity 的地圖控制項。
- `GutterMapController.refreshWorkingLayer()` 會同時繪製目前工作側溝的節點與線段；`clearWorkingLayer()` 會清除兩者。清單的當前工作圖層與編輯面板的圖層共用這組 API。

## Expected Behavior
- 在兩個面板使用期間，使用者可觸發既有測距模式，且測距結束後能回到同一份面板狀態。
- 清單來源的測距須隱藏本次工作側溝圖層及所有既有 scope 側溝圖層；編輯來源則保留正在編輯的工作側溝圖層。
- 測距結束時恢復正確圖層與 UI，不觸發草稿刪除、表單 dismiss 或工作階段切換。

## Affected Modules
- `map/MapWorkspaceFragment.kt`：管理測距狀態、工作圖層、清單／編輯面板生命週期與回復路徑。
- `main/MeasureModeUiController.kt`：主按鈕與測距面板的視覺狀態；可能需要支援「有暫時收起面板」的狀態。
- `gutter/AddGutterListBottomSheet.kt`、`gutter/AddGutterBottomSheet.kt`：提供安全的暫時收起／回復回呼或測距入口整合。
- `map/GutterMapController.kt`：驗證既有清除及重繪工作圖層 API 是否足以隔離清單與編輯兩種策略。
- `activity_main.xml` 與相關測試：確認按鈕層級、可點性及測距面板不受回復流程影響。

## Dependencies
- `DistanceMeasureManager` 會覆寫 GoogleMap 的 map-click 和 camera-move listener，並在 exit 清除自己的量測圖層。
- `MainBlockingUiController` 在測距期間會停用其他主地圖控制項，但刻意保留 `btnMeasureDistance` 可用。
- `feat-0914-2` 的 `MultiGutterSessionCoordinator`、`GutterSheetSessionBinder` 與 `AddGutterListBottomSheet` 需維持現有草稿與面板返回流程。
- Android Dialog 與 Activity 視窗層級決定 `btnMeasureDistance` 是否能如需求所述真正位於 BottomSheetDialog 之上。

## Risks
- 直接 dismiss 面板以露出測距按鈕可能觸發 `onDismiss`，導致工作圖層清除、草稿清理或多側溝會話狀態錯誤。
- 測距模式會接管地圖點擊 listener；錯誤回復可能使新增節點、選取 marker 或側溝點擊失效。
- 清單與編輯的圖層策略相反，若只以 `clearWorkingLayer()` 實作，可能誤清除編輯中的側溝圖層。
- Dialog 視窗分層限制使「提升既有 Activity 按鈕」無法保證可行，未確認前不宜鎖定實作方案。

## Unknowns
- OQ-001 已決定採面板內代理入口；OQ-003 已決定系統返回鍵先退出測距並回復原面板。
- OQ-002 已決定清單測距時暫時隱藏全部 scope 側溝，退出時恢復；現況清單會顯示 scope 載入的既有側溝，且不顯示未上傳草稿的工作圖層。
