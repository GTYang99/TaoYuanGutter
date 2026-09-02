# Reusable Assets

## Document Information

```yaml
document: reusable-assets
version: 0.1
status: draft
last_updated: YYYY-MM-DD
```

## Reuse Levels

- `project`: 只在目前專案使用
- `organization`: 可在組織內其他專案使用
- `public`: 可公開發布
- `restricted`: 受授權或合約限制

## Asset Catalog

### AST-001 資產名稱

```yaml
id: AST-001
type: icon
status: draft
reuse_level: project
version: 0.1
source: ""
license: ""
path: ""
```

使用限制：

- 可使用的畫面
- 是否允許改色或修改尺寸
- 是否能用於其他專案

## Reusable Component Catalog

### CMP-001 元件名稱

```yaml
id: CMP-001
type: ui_component
status: draft
reuse_level: organization
version: 0.1
source_module: ""
owner: ""
```

用途：

描述元件解決的共用問題。

輸入：

| Property | Type | Required | Description |
|---|---|---|---|
| state | Enum | Yes | 元件狀態 |

輸出／事件：

| Event | Description |
|---|---|
| onClick | 使用者點擊事件 |

支援狀態：

- Default
- Loading
- Disabled
- Error
- Empty

依賴：

- Theme token
- Android SDK
- 第三方 library

重用限制：

- 不得直接依賴目前專案的 Activity
- 不得寫死 API endpoint 或產品文案
- 必須由外部傳入資料與事件
- 必須有獨立測試

版本紀錄：

| Version | Change | Compatibility |
|---|---|---|
| 0.1 | 初始範本 | Not released |
