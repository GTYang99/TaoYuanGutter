# Requirement

## Background

側溝編輯頁由 `AddGutterBottomSheet` 覆蓋於主地圖上方。編輯期間仍應能透過上方可見地圖調整地圖範圍。

## Goal

修正編輯側溝時上方小地圖無法以手指拖動平移的問題。

## Functional Requirements

- 開啟 `AddGutterBottomSheet` 編輯側溝時，使用者可在 sheet 上方可見地圖區拖動並平移主地圖。
- 地圖拖動不得觸發側溝表單控制項，也不得改變現有 waypoint 編輯行為。

## Non-functional Requirements

- 不新增地圖、資料或 API 行為。

## Acceptance Criteria

- AC-001：編輯側溝時，在 sheet 上方可見地圖區拖動，主地圖鏡頭依手勢平移且 sheet 保持開啟。
- AC-002：在 sheet 表單區操作時，表單控制項與 waypoint 編輯行為維持正常。

## Constraints

- 修正範圍限於編輯 sheet 與主地圖之間的觸控路由；不改變編輯流程或資料契約。

## Open Questions

- 根因仍需在執行中的編輯頁重現確認；靜態檢視只能指出目前的觸控路由實作與視窗邊界判斷，不能證明實際事件序列。
