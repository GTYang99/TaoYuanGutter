# Knowledge Resolution

## Resolution Summary
- 本任務的明確決策是移除「銜接點」／`IS_TIEINPOINT`；既有 `feat-1118` 移除「連接管」／`IS_CONNECTING` 並保留銜接點的內容不適用於本任務。
- 舊資料相容性優先：舊欄位可被 Gson／解析層忽略，不能造成啟動、草稿列表或流程閃退。
- 舊銜接點資料載入後靜默視為一般點；若同時是虛擬點，以虛擬點規則優先。
- request 輸出採唯一規則：所有 `storeDitch` JSON 完全省略 `IS_TIEINPOINT`，不送 `null`。
- 舊已上傳唯讀銜接點草稿是唯一可解鎖修改的唯讀草稿例外；其他唯讀草稿維持不可修改。

## Sources and Decisions
| Source | Finding | Decision |
|---|---|---|
| `ty_feat_1118-3.md` | UI 移除「銜接點」，API 不上傳 `IS_TIEINPOINT` | 採用 |
| User decisions in this task | 舊資料忽略、按一般點處理；虛擬點優先；升級不可閃退 | 採用，轉為 AC-004～AC-008 |
| `ai/architecture.md` | API 使用 Gson；draft 使用 Room；表單與檢視位於 `gutter` | 作為 affected modules 與驗證依據 |
| `feat-1118` historical artifacts | 內容保留 `IS_TIEINPOINT`，方向與本任務相反 | 不採用，僅作回歸風險參考 |

## Resolved Conflicts
- 「全部移除」與「client 可不用解析」：執行中的 UI、local write-back 與 request 必須移除；為支援舊版本資料，解析層可保留相容欄位或由 Gson 忽略未知欄位。
- `null` 或省略：需求允許 API 接受兩者，但本任務明確選擇 request JSON 完全省略。
- 唯讀限制與資料修復：只有已上傳且標記唯讀的舊銜接點草稿可解鎖修復；其他已上傳唯讀草稿不變。

## Remaining Unknowns
- 無
