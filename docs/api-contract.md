# API Contract

## Document Information

```yaml
document: api-contract
version: 0.1
status: draft
last_updated: YYYY-MM-DD
owner: ""
source: ""
```

## Common Rules

```yaml
base_url: ""
authentication: Bearer Token
content_type: application/json
timeout: ""
```

固定規則：

- Token 放置位置
- 日期格式與時區
- 空值表示方式
- 分頁格式
- 共用錯誤格式

## Endpoint

### API-001 API 名稱

```yaml
id: API-001
status: draft
version: 0.1
method: POST
path: /v1/example
product_rules:
  - PRD-001
screens:
  - UI-001
```

用途：

簡短描述 API 的業務用途。

Request：

```json
{
  "field": "value"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| field | String | Yes | 欄位說明 |

Response：

```json
{
  "success": true,
  "data": {}
}
```

| Field | Type | Nullable | Description |
|---|---|---|---|
| success | Boolean | No | 是否成功 |
| data | Object | Yes | 回傳內容 |

錯誤：

| HTTP / Code | Meaning | Client Behavior |
|---|---|---|
| 400 | 請求錯誤 | 顯示欄位錯誤 |
| 401 | 登入失效 | 返回登入頁 |
| 500 | 伺服器錯誤 | 顯示重試 |

相容性限制：

- 欄位是否可以新增或改名
- 舊版本如何處理
- 是否需要資料 migration

## API Open Questions

- 不確定的 request、response 或錯誤處理必須寫在這裡，不得由 Developer 推測。
