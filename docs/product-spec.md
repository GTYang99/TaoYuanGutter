# Product Specification

## Document Information

```yaml
document: product-spec
version: 0.1
status: draft
last_updated: YYYY-MM-DD
owner: ""
source: ""
```

範本內容未填完前保持 `draft`。確認所有 fixed rules 後，再更新為 `fixed` 與正式版本。

## Product Goal

簡短描述這個產品要解決的問題。

## User Roles

| Role ID | Role | Permissions | Status |
|---|---|---|---|
| ROLE-001 | 一般使用者 | 查看與填寫資料 | draft |

## Product Rules

### PRD-001 功能名稱

```yaml
id: PRD-001
status: draft
version: 0.1
```

規則：

- 使用者可以執行什麼
- 系統必須產生什麼結果
- 哪些情況不得執行
- 失敗時如何處理

驗收基準：

- 給定某個條件
- 使用者執行某個動作
- 系統應產生指定結果

變更限制：

- `fixed` 規則不得由單一開發 Task 自行修改
- 修改時必須建立 `spec_change` Task

## Business Rules

| Rule ID | Rule | Status |
|---|---|---|
| BR-001 | 固定業務規則 | draft |

## Non-functional Requirements

| ID | Category | Requirement | Status |
|---|---|---|---|
| NFR-001 | Compatibility | 支援 Android 9+ | fixed |
| NFR-002 | Security | 敏感資料不得寫入 log | fixed |

## Open Questions

- 只放尚未確認的產品決策；沒有就寫 `無`。
