# Repository Analysis

## Planning Baseline
- Branch: `feat/表單合併測距功能`.
- 目前分支含先前的面板代理按鈕實作；該方案不符合本次修訂需求，不能作為本輪驗收結果。

## Current Behavior
- `btnMeasureDistance` 是右側控制列第五個按鈕，會落在清單（80% 高）及編輯（70% 高）面板的覆蓋範圍。
- 目前兩個面板各有代理測距按鈕，必須移除。
- 表單流程多處呼叫 `MainBlockingUiController.setMainButtonsEnabled(false)`，會停用主地圖測距按鈕；現有 controller 也不支援「面板時只顯示測距」的 visibility 規則。
- 編輯面板已將面板外觸控轉送 Activity；清單面板尚未有等效機制。

## Expected Behavior
- 兩個目標面板正常顯示時，主地圖只顯示／啟用右上最前位置的 `btnMeasureDistance`，其餘主地圖控制項隱藏。
- 主按鈕由 host 判斷清單或可編輯表單來源，沿用各自量測收起、圖層與回復策略。
- 面板關閉後恢復正常控制項；blocking 狀態優先，維持測距鎖定。

## Affected Modules
- `activity_main.xml`、`MainBlockingUiController.kt`、`MapWorkspaceFragment.kt`。
- `AddGutterListBottomSheet.kt`、`AddGutterBottomSheet.kt` 與兩個 bottom-sheet layout。
- 聚焦 unit/UI/instrumentation 測試。

## Risks
- Dialog 外部觸控未轉送會使按鈕可見但不可點。
- 分散設定 visibility 容易讓 sheet dismiss 或 loading 結束後留下錯誤控制項狀態；必須由 controller 集中重算。
- 既有代理按鈕若與主按鈕並存，會違反唯一入口要求。

## Unknowns
- 無。
