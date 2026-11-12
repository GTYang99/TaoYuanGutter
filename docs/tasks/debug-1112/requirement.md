# Requirement

## Background

編輯既有 `ABC` 側溝時，新增 `D` 點並匯入 `C` 點資料，接著刪除原 `C` 點再上傳，實際結果可能只剩 `AB`。D 是新點，因此不應帶 C 的 `node_id`；D 會以 C 的 `XY_NUM` 讓後端尋找資料並繼承。使用者要求確認匯入、`node_id`、`XY_NUM` 與後端更新／新增邊界的責任。

## Goal

找出 `C`／`D` 點消失的根因，確認目前是否有 `XY_NUM` 同名檢查，並記錄可交給 implementation 的最小修正方向；本回合不修改 production code。

## Functional Requirements

- 追蹤既有點位編輯、匯入、表單回傳、刪除與 `storeDitch` payload 的 identity 流程。
- 明確說明 D 為新點時 `node_id` 為空的預期語意，以及 C 的 `XY_NUM` 在哪個邊界被保存、驗證或失效。
- 將根因分類為 requirement、planning、implementation 或 environment，並留下 evidence。

## Non-functional Requirements

- 不降低既有資料保留與編輯更新的 acceptance criteria。
- 不把 C 的 `node_id` 填入 D；這會把新 D 錯誤地變成 C 的更新，而非新點。

## Acceptance Criteria

- AC-001：文件指出匯入 C 後 D 的 `node_id`／`XY_NUM` 狀態，以及送出前後各資料轉換邊界。
- AC-002：文件明確回答目前 `XY_NUM` 同名檢查的觸發時機、比較範圍與本案例為何可能通過。
- AC-003：文件列出受影響檔案、implementation root cause、回歸風險與最小修正方向。

## Constraints

- 本回合只做 source／test／history evidence review，不修改 production code。
- 後端實作未在本 repository 內提供；後端以 `node_id` 辨識更新／繼承的部分，除使用者回報外標記為待 API／payload 驗證。

## Open Questions

- 後端是否能在同一個請求中，先解析 D 對 C 的 `XY_NUM` 繼承，再刪除 C？若不能，前端需在送出前阻止「刪除來源 C、D 仍引用 C 的 `XY_NUM`」的組合。
