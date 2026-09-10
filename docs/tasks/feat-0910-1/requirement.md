# Requirement

## Background
- 勾選「無法開蓋」會清除部分欄位與第 2、3 張測量照片，目前沒有確認提示，也無法在同次編輯恢復。

## Goal
- 新增確認提示，並在同一次表單編輯階段暫存／回填被清除資料；不改既有上傳 request 結構。

## Functional Requirements
- 新增／編輯階段勾選「無法開蓋」先顯示 Alert，取消則維持原狀。
- 確認後清除既有不適用欄位與第 2、3 張照片；第 1 張概況照保留。
- 暫存包含欄位與照片完整 metadata，只存在本次表單 session。
- 取消勾選時依 dirty merge 回填未被後續操作修改的資料。
- 暫存不得進入 `currentFormData`、`Waypoint.basicData`、Room 或 API payload。

## Non-functional Requirements
- 不修改後端 API contract、`storeDitch`／照片上傳 request 與 image ID 結構。
- 保留既有驗證、草稿與上傳行為。

## Acceptance Criteria
- AC-001：勾選時顯示清除提示 Alert。
- AC-002：取消 Alert 時 Checkbox、欄位與照片不變。
- AC-003：確認後清除既有欄位與第 2、3 張照片，第 1 張保留。
- AC-004：暫存只存在表單 session，不寫入草稿或 API payload。
- AC-005：同次編輯取消勾選時，未被修改的欄位與第 2、3 張照片完整回填。
- AC-006：後續被替換、刪除、重新拍攝或明確清空的值不得被舊快照覆蓋。
- AC-007：configuration change 保留同一 session；process death／真正離開表單不恢復舊暫存。
- AC-008：既有 validation、`storeDitch` 與照片 upload metadata 結構不變。
- AC-009：檢視、匯入鎖定、虛擬點與明溝模式不誤觸發。

## Constraints
- Planning 不修改 production code；不降低 acceptance criteria；不略過測試。

## Open Questions
無
