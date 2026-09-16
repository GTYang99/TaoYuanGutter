# Requirement

## Background
- 主地圖已有 `@+id/btnMeasureDistance` 測距模式；新增側溝清單與側溝編輯皆以 BottomSheetDialogFragment 呈現，開啟時會遮住主地圖操作層。
- `feat-0914-2` 已導入 `AddGutterListBottomSheet` 多側溝工作階段；本工項要讓該清單及既有 `AddGutterBottomSheet` 編輯流程可暫時使用主地圖測距功能。

## Goal
- 使用者在新增側溝清單或側溝編輯面板時，能啟動既有主地圖測距模式；關閉測距後回到原本正在使用的面板與資料狀態。

## Functional Requirements
- 在 `AddGutterListBottomSheet.kt` 與 `AddGutterBottomSheet.kt` 顯示期間，主地圖的 `@+id/btnMeasureDistance` 必須位於可見且可點選的最上層。
- 從新增側溝清單啟動測距時，清單面板向下收起，地圖進入既有測距模式，且不顯示目前工作中的側溝線段或節點圖示，也暫時隱藏所有既有 scope 側溝線段。
- 從側溝編輯面板啟動測距時，編輯面板向下收起，地圖進入既有測距模式，且保留目前編輯側溝的線段及／或節點圖示。
- 關閉測距模式後，恢復啟動前的清單或編輯面板，並保留原本草稿與編輯資料。
- 測距模式的既有起點、虛線、距離顯示、重設及關閉行為維持不變。

## Non-functional Requirements
- 沿用既有 Android Kotlin、ViewBinding、BottomSheetDialogFragment、Google Maps 與 `DistanceMeasureManager`；不得引入新的 UI 框架。
- 不得影響 `feat-0914-2` 的多草稿切換、立即保存、關閉確認、上傳成功／失敗回到清單等流程。

## Acceptance Criteria
- AC-001：`AddGutterListBottomSheet` 與 `AddGutterBottomSheet` 開啟時，主地圖 `btnMeasureDistance` 可見、可點選且位於可操作的最上層。
- AC-002：由新增側溝清單啟動測距時，清單向下收起、測距模式可正常量測，且地圖不顯示目前工作側溝的線段或節點，並暫時隱藏所有既有 scope 側溝線段；關閉測距後回到相同清單並恢復 scope 側溝線段。
- AC-003：由側溝編輯面板啟動測距時，編輯面板向下收起、測距模式可正常量測，且保留目前編輯側溝的線段或節點；關閉測距後回到相同編輯面板與資料。
- AC-004：測距的既有重設、關閉、距離顯示與地圖點擊行為不回歸；退出後不殘留測距起點或虛線。
- AC-005：新增側溝清單／單筆編輯、草稿保存、地圖工作圖層及主地圖其他控制項不回歸。

## Constraints
- 需求文字指定使用既有主地圖 `@+id/btnMeasureDistance`，不得未確認即以新建或複製的測距按鈕取代。
- 清單模式與編輯模式對側溝工作圖層的可見性要求不同，必須以啟動來源區分，不能以單一清理規則處理。

## Open Questions
- OQ-001：**已決定（2026-09-16）**：允許在兩個面板中放置同一測距功能的代理入口；不要求唯一的 Activity FAB 實體浮在 Dialog 視窗上方。
- OQ-002：**已決定（2026-09-16）**：清單測距期間暫時隱藏所有既有 scope 側溝線段，關閉測距並恢復清單時重新顯示。
- OQ-003：**已決定（2026-09-16）**：測距期間按 Android 系統返回鍵時，先退出測距並恢復原面板。
