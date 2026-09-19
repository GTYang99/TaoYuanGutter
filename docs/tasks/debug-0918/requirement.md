# Requirement

## Background

`ditchDetails` 查詢整條側溝及其節點摘要；`nodeDetails` 查詢單一節點的
表單資料。兩支 API 的資料範圍不同，照片 metadata 不得依 response 順序
或不同側溝資料互相套用。

## Goal

修正 debug-0918 的既有點位匯入與主地圖 UI 行為，保留既有 API contract，
讓未替換照片不重複上傳，並確保兩支 API 的資料責任清楚分離。

## Functional Requirements

- `nodeDetails` 回傳的 URL-only 既有照片，匯入後仍須下載並顯示。
- URL-only 且未被使用者替換的既有照片，不得呼叫 `POST /v1/node/nodeImage`。
- 使用者新增或替換照片時，須依 photo slot 呼叫 `nodeImage`；成功回傳的
  `img_id` 才可進入後續 `storeDitch` 資料。
- `ditchDetails` 的 `data.nodes[].url[].id` 只可用於該次整條側溝 response
  內相同節點的照片資料，不得拿來補另一筆 `nodeDetails` 或另一條側溝。
- 主地圖的「0910刪除資料」圖層預設關閉，使用者仍可手動切換。
- 「既有點位資料」頁面標題須相對完整 header row 水平置中，左右控制仍可操作。

## Non-functional Requirements

- 不改變 `ditchDetails`、`nodeDetails`、`nodeImage`、`storeDitch` endpoint
  與既有 photo slot 定義。
- 不影響虛擬點、無法開蓋、草稿回復、既有編輯與新增側溝流程。
- 不以缺少 `img_id` 為理由猜測或合併不同 API 的節點資料。

## Acceptance Criteria

- AC-001：以 `nodeDetails` 回傳 URL-only 照片匯入時，照片可下載並顯示；
  未替換前不呼叫 `/v1/node/nodeImage`。
- AC-002：使用者替換任一照片後，只有被替換的 photo slot 呼叫
  `/v1/node/nodeImage`，成功回傳的 `img_id` 可被後續資料流程使用。
- AC-003：`ditchDetails` 與 `nodeDetails` 的資料 mapping 以各自 API contract
  為準；不得以不同 response 的順序或不相同節點資料填入照片 ID。
- AC-004：主地圖首次載入時「0910刪除資料」為關閉且不顯示 overlay；
  取消/重新勾選可移除/恢復 overlay，狀態在重建圖層後一致。
- AC-005：「既有點位資料」標題在完整 header row 置中，返回與 optional
  location control 的 48dp 操作區不受影響。
- AC-006：既有的 virtual、cannot-open、draft resume、existing edit 與
  new gutter photo flows 不回歸。

## Constraints

- 本次不新增 `ditch_id → SPI_NUM` 的假設，也不新增未經 API contract 證實
  的 query parameter。
- Verification 必須在固定 production revision 上執行；未執行的 runtime
  case 必須標記 `NOT VERIFIED`。

## Open Questions

無。
