# Project Documents

## Document Layers

| Layer | File | Purpose |
|---|---|---|
| Product | `docs/product-spec.md` | 固定產品行為與業務規則 |
| Design | `docs/design-spec.md` | 畫面、互動與設計狀態 |
| API | `docs/api-contract.md` | API request、response 與錯誤契約 |
| Assets | `docs/reusable-assets.md` | 圖片、圖示與可重用元件 |
| Tasks | `docs/tasks/[task-id]/` | 單次開發與驗證紀錄 |

## Status Rules

- `fixed`: 已確認，是實作與驗證的規格來源
- `draft`: 可以討論，但不得直接進入實作
- `deprecated`: 不得用於新功能
- `TBD`: 資訊不足，Agent 不得自行推測

## Change Rules

修改 `fixed` 規格時，必須：

- 建立獨立 `spec_change` Task
- 記錄修改原因與受影響規格 ID
- 評估產品、設計、API 與元件影響
- 更新文件或規格項目的版本
- 重新驗證受影響功能

## Task References

Task 的 `requirement.md` 只引用適用的固定規格，不重複整份規格內容：

```yaml
references:
  product:
    - PRD-001
  design:
    - UI-001
  api:
    - API-001
  assets:
    - CMP-001

specification_versions:
  product-spec: "1.0"
  design-spec: "1.0"
  api-contract: "1.0"
  reusable-assets: "1.0"
```

沒有適用規格的層級使用空陣列。Task 不得直接 override `fixed` 規格；需要變更時先完成 `spec_change` Task。
