# Requirement

## Background
- 勾選「無法開蓋」會清除部分欄位與第 2、3 張測量照片，目前沒有確認提示，也無法在同次編輯恢復。

## Goal
- 新增確認提示；確認清除後，即使取消勾選「無法開蓋」也不回填已清除資料；不改既有上傳 request 結構。

## Functional Requirements
- 新增／編輯階段勾選「無法開蓋」先顯示 Alert，取消則維持原狀。
- 確認後清除既有不適用欄位與第 2、3 張照片；第 1 張概況照保留。
- 取消勾選「無法開蓋」後維持已清除資料，不建立、保存或回填任何 session snapshot。

## Non-functional Requirements
- 不修改後端 API contract、`storeDitch`／照片上傳 request 與 image ID 結構。
- 保留既有驗證、草稿與上傳行為。

## Acceptance Criteria
- AC-001：勾選時顯示清除提示 Alert。
- AC-002：取消 Alert 時 Checkbox、欄位與照片不變。
- AC-003：確認後清除既有欄位與第 2、3 張照片，第 1 張保留。
- AC-004：取消勾選後，已清除的欄位與第 2、3 張照片維持清除狀態，不回填舊資料。
- AC-005：既有 validation、`storeDitch` 與照片 upload metadata 結構不變。
- AC-006：檢視、匯入鎖定、虛擬點與明溝模式不誤觸發。

## Constraints
- Planning 不修改 production code；不降低 acceptance criteria；不略過測試。

## Open Questions
無
