# Design Specification

## Document Information

```yaml
document: design-spec
version: 0.1
status: draft
last_updated: YYYY-MM-DD
design_source: ""
design_version: ""
```

## Design Sources

| Source ID | Type | URL / Path | Version | Status |
|---|---|---|---|---|
| DS-001 | Figma | URL | version | draft |

## Screen Specification

### UI-001 畫面名稱

```yaml
id: UI-001
product_rules:
  - PRD-001
status: draft
version: 0.1
```

入口：

- 從哪一個畫面進入
- 需要什麼權限或資料

固定內容：

- 標題、按鈕與欄位名稱
- 顯示順序
- 必須存在的區塊

互動：

| Element ID | 元件 | 使用者動作 | 預期結果 |
|---|---|---|---|
| UI-001-ACT-001 | 主要按鈕 | 點擊 | 進入指定流程 |

畫面狀態：

| State | 顯示內容 | 可執行動作 |
|---|---|---|
| Loading | 載入提示 | 不可重複送出 |
| Content | 正常資料 | 可操作 |
| Empty | 空資料提示 | 可重新整理 |
| Error | 錯誤訊息 | 可重試 |
| Disabled | 停用樣式 | 不可操作 |

固定設計值：

| Token / Element | Value | Status |
|---|---|---|
| Primary color | `#000000` | draft |
| Button height | `48dp` | draft |

## Accessibility

- 點擊區域最低尺寸
- 文字縮放需求
- Content description
- 色彩對比要求

## Design Open Questions

- 只放尚未確認的設計決策；沒有就寫 `無`。
