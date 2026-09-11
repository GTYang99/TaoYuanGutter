# Requirement

## Background
- 既有側溝的點位在進入編輯流程時，會下載既有照片供表單顯示；儲存既有點位時不應再觸發既有照片的上傳，避免使用者不必要的等待。

## Goal
- 既有點位更新時不攜帶既有照片的 `captured_at` 與 `img_ids`，並維持已下載照片的顯示與既有照片略過上傳行為。

## Functional Requirements
- 既有點位（有後端 `node_id`）組裝 `POST /api/v1/ditch/storeDitch` 更新請求時，`captured_at` 與 `img_ids` 必須省略。
- 進入既有點位編輯時，既有照片仍可依目前流程下載並顯示於表單。
- 儲存未替換既有照片的點位時，不得呼叫 `POST /api/v1/node/nodeImage` 重傳這些照片。
- 使用者新拍攝或替換照片時，仍須依既有 slot（1 概況、2 寬度、3 深度）上傳新照片。

## Non-functional Requirements
- 不變更新增側溝／新增點位的照片 metadata 行為、既有 API endpoint、照片 slot、照片顯示、草稿、無法開蓋與虛擬點規則。
- Planning 不修改 production code；不降低 acceptance criteria；不略過測試。

## Acceptance Criteria
- AC-001：既有點位的 `storeDitch` 節點 JSON 不含 `captured_at` 與 `img_ids`。
- AC-002：既有點位的已下載照片仍顯示，且未被使用者替換時不會呼叫 `/v1/node/nodeImage`。
- AC-003：既有點位替換任一照片後，只有新照片依原 slot 送往 `/v1/node/nodeImage`。
- AC-004：新增側溝／新增點位仍依既有行為攜帶新照片的 `captured_at` 與已成功上傳照片的 `img_ids`。
- AC-005：無法開蓋、虛擬點、草稿回復與編輯後重新開啟的既有照片行為不回歸。

## Constraints
- 僅處理「既有點位不傳照片」；附檔所述「0910刪除資料」WMS 圖層為獨立功能，不納入此任務。
- 「既有點位」以更新請求中有後端 `node_id` 判定；本次不變更後端 contract 以外的欄位。

## Assumptions
- 附檔所稱「既有點位參數改為不帶 `captured_at`、`img_ids`」是指 `storeDitch` 的節點 JSON；`nodeImage` multipart API 本身不使用這兩個欄位。
- 僅略過從後端載入且未由使用者替換的照片；使用者的新照片仍是應上傳的資料。

## Open Questions
無。
