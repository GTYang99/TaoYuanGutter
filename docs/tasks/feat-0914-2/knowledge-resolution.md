# Knowledge Resolution

## Sources Reviewed
- 使用者目前要求：開始規劃並建立 `feat-0914-2` 相關文件。
- 任務需求來源：`/Users/a10362/Desktop/markdown file/ty_feat_0914-2.md`。
- Figma MCP：設計檔 `IfmNbZKhr4wojZ2bF5rYHG`、section `2374:25786`、清單面板 `2374:26810`。
- 現行實作：`MainActivity`、`AddGutterBottomSheet`、`GutterFormActivity`、`GutterSessionRepository`、`GutterDraftCoordinator`、既有 pending drafts UI。
- 架構與流程規則：`AGENTS.md`、`ai/architecture.md`、`ai/knowledge-resolution-rules.md`、`ai/planning-rules.md`。

## Resolved Decisions

| Decision | Source and authority | Confidence | Affected AC |
|---|---|---:|---|
| 清單列的一筆側溝必須有獨立的完整草稿，而非把多條側溝合併到同一筆草稿。 | requirement 的「每條側溝單獨存到草稿列表」高於現行模型。 | High | AC-002, AC-004, AC-005, AC-006 |
| 未上傳資料須在有效修改後立即持久化；關閉確認只決定是否結束清單，不是唯一的保存點。 | requirement 的「程式閃退也要存」；現行 Room repository 可支援 upsert。 | High | AC-004, AC-005 |
| Toolbar 行為採需求文字：左側「＋新增」、右側關閉 x；建立時間到秒；Alert 採指定內容與確定／取消。 | requirement 高於 Figma 的示意排版。 | High | AC-003, AC-004 |
| Figma 的清單面板視覺可重用：地圖上的底部面板、24px 上圓角、70px toolbar、置中標題、88px list row、32px 內距與 chevron。 | Figma node `2374:26810`；與需求不衝突的設計資訊。 | High | AC-003 |
| 空白且沒有任何有效資料的剛建立側溝不產生草稿。 | 延用 `GutterDraftCoordinator` 的有效內容清理規則；不影響使用者已輸入資料。 | Medium | AC-004, AC-005 |

## Figma Component Relationships

```text
Main map shell (map + statusBar + tabBar + map controls)
└─ Add-gutter list bottom sheet (2374:26810)
   ├─ Toolbar (2374:26811)
   │  ├─ close control (2374:26816)
   │  ├─ centered title (2374:26820)
   │  └─ add control (2374:26817–26819)
   └─ Content area (2374:26821)
      └─ repeatable list-row (2374:26823–26825)
         ├─ title + two detail lines
         └─ trailing chevron → selected gutter editor
```

## Unresolved Conflicts
- Figma 把 close 放在左側、add 放在右側，且範例建立時間只到分鐘；requirement 指定相反位置與秒級時間。已依需求文字處理，非阻擋項。
- Figma 未提供關閉確認 Alert。Alert 的行為與文案以 requirement 為準；實作時沿用 app 既有 Material Alert 樣式。

## Assumptions Safe for Planning
- 清單本身是 `BottomSheetDialogFragment`，疊在既有主地圖上，不取代 `MainActivity`、地圖或 tab host。
- 每個新增工作項目在首次進入表單前即有穩定的草稿 ID；首次有效變更時寫入 Room，以支援程序重建。
- 清單關閉時只針對仍未上傳且有效的工作項目進行 final upsert；成功上傳的項目應刪除其草稿。

## Questions Requiring Approval
- 無。
