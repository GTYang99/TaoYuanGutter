# Requirement

## Background

在 AddGutterBottomSheet 新增側溝時，使用者按下送出後若必要資料或照片不足，系統會顯示「資料/照片不足」Dialog；目前關閉 Dialog 後，送出按鈕仍停留在「上傳中…」且 disabled。

## Goal

驗證失敗並關閉提示後，表單應恢復可操作，送出按鈕顯示「新增側溝」且可再次點擊。

## Functional Requirements

- 缺少必要欄位或必要照片時，保留既有提示 Dialog。
- 關閉提示後，新增模式的送出按鈕恢復為可點擊的「新增側溝」。

## Non-functional Requirements

無

## Acceptance Criteria

- AC-001：新增模式送出時若必要資料或照片不足，顯示「資料/照片不足」Dialog；按「確定」關閉後，btnSubmitGutter 顯示「新增側溝」並恢復 enabled。

## Constraints

- 保留既有必填資料與照片驗證規則。
- 不因驗證失敗送出側溝建立 API 請求。

## Open Questions

無

