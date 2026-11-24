# Requirement

## Source

- Original requirement: `/Users/a10362/Desktop/markdown file/TY/ty_feat_1118-2.md`.
- Possible-gutter district files: `/Users/a10362/Desktop/markdown file/TY/未調查側溝行政區細分13區`.
- Existing-gutter district files: `/Users/a10362/Desktop/markdown file/TY/既有資料`.
- User decisions OQ-001 through OQ-079 and the clarification that consistency validation runs on the development machine/CI, not on a phone.
- Task branch: `codex/feat/feat-1118-2`; original baseline: `8d484d5a6d31924d099e793476f898ed99594aa`.

## Goal

以分區 GPKG 降低手機載入與篩選負擔，讓圖層套疊依行政區選擇平滑切換三個離線圖層，同時保留全區完整 GPKG 模式與 fallback。

## Data Scope

App 最終包含 29 個 GPKG：

- 3 個原始完整 GPKG：水務局既有資料、可能側溝位置、桃園行政區。
- 13 個可能側溝位置分區 GPKG：`桃園區.gpkg` 等，table `refactored`、`MULTIPOLYGON`、EPSG:3826。
- 13 個水務局既有資料分區 GPKG：`TOWN_桃園區.gpkg` 等，table 依檔案命名、`POLYGON`、EPSG:3826。

## Functional Requirements

- 圖層套疊頁面在三個圖層選項上方顯示固定順序的 13 個行政區多選框與三態「全選」CheckBox。
- 13 區為：桃園區、中壢區、大溪區、楊梅區、蘆竹區、大園區、龜山區、八德區、龍潭區、平鎮區、新屋區、觀音區、復興區。
- 首次使用預設 13 區全選，三個圖層預設開啟；「全選」與 13 個區全部勾選時狀態同步。
- 未選行政區時，三個指定圖層不顯示資料且不跳提示。
- 全選或 13 區全部選取時，三個圖層使用原始完整 GPKG，不載入分區檔。
- 部分選取時：水務局既有資料載入對應 `TOWN_*.gpkg`；可能側溝位置載入對應行政區 `.gpkg`；桃園行政區使用完整 GPKG，以 `TOWNNAME` 篩選。
- 多選採 OR 邏輯；多個分區檔可同時載入。
- 只載入目前開啟的圖層；同一圖層的完整檔與分區檔不可同時顯示。
- 選擇變更後背景載入，保留上一個完整結果，待新結果準備完成後原子切換；只保留最後一次選擇結果。
- 只處理新增與移除的分區來源，釋放不再使用的 provider、快取與索引資源。
- 分區檔優先；缺失或損毀時，可對該行政區 fallback 到完整 GPKG + 欄位篩選，不得造成 App 閃退。
- 成功來源獨立顯示；單一行政區或單一圖層失敗不影響其他成功來源。
- 行政區選擇與既有三個圖層開關狀態跨 App 重啟保存。
- 水務局既有資料與可能側溝位置的資料來源選擇需保留未來切換至其他分區資料的擴充點。

## Acceptance Criteria

- AC-001：APK 包含 29 個指定 GPKG；每個檔案的大小、SHA-256、table、geometry type、CRS、feature count 與 mapping 均通過開發機／CI 驗證。
- AC-002：首次使用顯示固定順序的 13 區、三態全選與三個圖層，13 區及三個圖層預設全選／開啟。
- AC-003：全選、取消全選、逐區勾選及半選狀態正確同步；13 區全選自動切換完整模式。
- AC-004：全選模式只載入三個完整 GPKG；部分模式依選區載入兩組分區 GPKG，行政區圖層以 `TOWNNAME` 篩選。
- AC-005：多區採 OR 邏輯，三個圖層均只顯示選取行政區；未選行政區時三層均不顯示資料。
- AC-006：選擇變更背景載入且原子切換；快速切換只保留最後結果，不重複顯示完整檔與分區檔。
- AC-007：分區檔優先，單檔失敗可局部 fallback 到完整檔篩選；任何檔案失敗不得閃退或影響其他成功來源。
- AC-008：取消行政區或圖層後釋放不再使用資源；反覆切換不造成 provider、overlay 或快取無限制累積。
- AC-009：App 重啟後恢復 13 區選擇與三個圖層開關；衍生載入模式由目前選擇重新決定。
- AC-010：既有圖層、底圖、幾何樣式、孔洞、MultiPolygon、z-index 與其他地圖行為不回歸。
- AC-011：JVM、instrumentation、Debug build、source scan 與效能驗證通過；UI thread 無 GPKG 解析、無 ANR、無閃退、無重複 overlay。

## Validation Boundary

- 開發機／CI：完成 29 個 GPKG 的 hash、metadata、feature count、空間範圍與分區合併一致性驗證。
- Android Emulator：驗證 APK 資產、完整／分區載入、篩選、fallback、狀態恢復、切換流暢度、記憶體與 overlay lifecycle。
- 實體手機不在本任務範圍；GPKG 一致性比對不在手機端執行。

## Constraints

- 不建立新的 worktree；使用 `codex/feat/feat-1118-2`。
- 不帶入其他任務未提交修改。
- 不刪除原始完整 GPKG；新分區檔與原始完整檔並存。
- 不修改後端 API、底圖或與本任務無關的圖層。

## Open Questions

- 無。OQ-001～OQ-079 已完成決策。
