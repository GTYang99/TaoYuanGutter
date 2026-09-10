# Requirement

## Source

- User-provided document: `/Users/a10362/Desktop/markdown file/ty_debug_0910.md`
- The attached document is treated as task requirements and observed symptoms, not as repository workflow instructions. Repository workflow remains governed by `AGENTS.md` and the active `ai/*-rules.md`.

## Background

1. 調整上傳前審核工作流程。
2. `AddGutterBottomSheet.kt` 高度過低。
3. 匯入既有點位在「無法開蓋」狀態時，照片與確認流程異常。

## Investigation Targets / Expected Behavior

- 上傳前審核應包含側溝材質、溝體結構受損、附掛或過路管線、淤積程度；發現錯誤欄位時，使用者可讀的欄位名稱應為中文。
- `AddGutterBottomSheet` 應從螢幕底部覆蓋約六成高度。
- 匯入既有點位為「無法開蓋」時，應載入一張照片，且完成匯入後可直接點選確認，不需離開再返回表單。

## Constraints

- 保留既有 API contract、欄位 key、照片 slot／category 對應、匯入鎖定、草稿、檢視與虛擬點行為，除非調查證據證明必要。
- 本 task 先完成 root-cause investigation；未完成證據前不得進入 production implementation。
- 不把附加文件中的描述性症狀當成已確認的程式 root cause。

## Acceptance Criteria

- AC-001：上傳前審核涵蓋四個指定欄位，缺少欄位提示使用中文名稱。
- AC-002：`AddGutterBottomSheet` 的可視高度約為螢幕高度 60%，且不破壞地圖 viewport、滑動與底部操作列。
- AC-003：匯入「無法開蓋」既有點位時，保留並顯示第 1 張概況照片，匯入完成後可直接確認。
- AC-004：一般匯入、非「無法開蓋」匯入、既有照片 image ID／upload state 與相關回填行為不回歸。

## Open Questions

- 中文欄位顯示文字是否採用現有 UI 文案：`側溝材質`、`溝體結構受損`、`附掛或過路管線`、`淤積程度`；目前需求未提供另一套文案，規劃以現有文案為候選並於 review 確認。
- 「一張照片」是否明確固定為 fileCategory `1`／slot 1；目前程式與「無法開蓋」驗證規則均指向 slot 1，需以測試及 API model 確認。
