# Knowledge Resolution

## Sources Reviewed
- 使用者目前要求：開始規劃並建立 `feat-0914-2` 相關文件。
- 任務需求來源：`/Users/a10362/Desktop/markdown file/ty_feat_0914-2.md`。
- Figma MCP：設計檔 `IfmNbZKhr4wojZ2bF5rYHG`、section `2374:25786`、清單面板 `2374:26810`。
- 現行實作：`MainActivity`、`AddGutterBottomSheet`、`GutterFormActivity`、`GutterSessionRepository`、`GutterDraftCoordinator`、既有 pending drafts UI。
- 現行 API：`GutterRepository.uploadNodeImage()` 的 multipart body、`PhotoCapturedAtResolver`、`StoreDitchNodeRequestMapper`。
- 架構與流程規則：`AGENTS.md`、`ai/architecture.md`、`ai/knowledge-resolution-rules.md`、`ai/planning-rules.md`。

## Resolved Decisions

| Decision | Source and authority | Confidence | Affected AC |
|---|---|---:|---|
| 清單列的一筆側溝必須有獨立的完整草稿，而非把多條側溝合併到同一筆草稿。 | requirement 的「每條側溝單獨存到草稿列表」高於現行模型。 | High | AC-002, AC-004, AC-005, AC-006 |
| 未上傳資料須在有效修改後立即持久化；關閉確認只決定是否結束清單，不是唯一的保存點。 | requirement 的「程式閃退也要存」；現行 Room repository 可支援 upsert。 | High | AC-004, AC-005 |
| Toolbar 行為採需求文字：左側「＋新增」、右側關閉 x；建立時間到秒；Alert 採指定內容與確定／取消。 | requirement 高於 Figma 的示意排版。 | High | AC-003, AC-004 |
| Figma 的清單面板視覺可重用：地圖上的底部面板、24px 上圓角、70px toolbar、置中標題、88px list row、32px 內距與 chevron。 | Figma node `2374:26810`；與需求不衝突的設計資訊。 | High | AC-003 |
| 空白且沒有任何有效資料的剛建立側溝不產生草稿。 | 延用 `GutterDraftCoordinator` 的有效內容清理規則；不影響使用者已輸入資料。 | Medium | AC-004, AC-005 |
| Toolbar 採左側關閉／刪除、右側新增；無未上傳項目時關閉直接結束。 | 2026-09-15 使用者明確決定，高於既有 requirement 與 Figma 示意。 | High | AC-004 |
| 成功送出後必須先進檢視頁，關閉檢視頁再回到同一新增清單並移除成功項目；失敗 Alert 確認後回清單並保留失敗項目。 | 2026-09-15 使用者明確指定流程；不含後端 delete。 | High | AC-006, AC-008 |
| 每張 `nodeImage` 照片在各自 multipart request 帶 `captured_at`；原始時間無法解析時以裝置目前時間補入。 | 2026-09-15 使用者明確決定；現行 resolver 的格式為 `yyyy-MM-dd HH:mm:ss`。 | High | AC-009 |
| `storeDitch` 的任一 node 均不帶 `captured_at`。 | 2026-09-15 使用者明確決定；現行 mapper 對新 node 組入此欄位，須移除。 | High | AC-010 |

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
- Figma 把 close 放在左側、add 放在右側，且範例建立時間只到分鐘；目前需求也指定 close 左、add 右，但時間仍以需求的秒級格式為準。
- Figma 未提供關閉確認 Alert。Alert 的行為與文案以 requirement 為準；實作時沿用 app 既有 Material Alert 樣式。

## Assumptions Safe for Planning
- 清單本身是 `BottomSheetDialogFragment`，疊在既有主地圖上，不取代 `MainActivity`、地圖或 tab host。
- 每個新增工作項目在首次進入表單前即有穩定的草稿 ID；首次有效變更時寫入 Room，以支援程序重建。
- 清單關閉時只針對仍未上傳且有效的工作項目進行 final upsert；成功上傳的項目應刪除其草稿。
- 成功後的清單項目移除只能發生在使用者關閉既有檢視頁並回到清單時；失敗確認後不得移除該 item 或其草稿。
- 照片時間只在實際單張 `nodeImage` 上傳時附加；重用既有 resolver，空值改以裝置目前時間補入，且不改變 `PhotoUploadCandidateResolver` 的重傳判定。

## Questions Requiring Approval
- 無。
