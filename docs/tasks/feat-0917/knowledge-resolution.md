# Knowledge Resolution

## Sources Reviewed

- 更新後附件 `ty_feat_0917.md`、兩張 UI 示意圖及使用者逐項決議。
- Figma MCP：檔案 `EaWah6suPwkJ4Jqm4vuSZo`，區段 `1691:12479`、`1701:16618`，元件 `2374:21501`、`2374:21505`。
- 目前分支的 API model、mapper、表單、inspect-to-edit、adapter 與既有測試。

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| Request keys 是 `IS_TIEINPOINT`、`IS_CONNECTING`，非虛擬點固定送 Boolean，包含 false | 使用者決議 | High | AC-002 |
| `nodeDetails` 以 String `"0"`／`"1"` 回傳，缺值為 false | 使用者決議、附件範例 | High | AC-002 |
| 互斥項選取時另一項設 false、灰化且不可選 | 使用者決議、Figma | High | AC-001 |
| UI 文案一律為「連結管」 | 使用者決議 | High | AC-001 |
| 虛擬點清除三值、不送三 key；取消虛擬點後維持 false；檢視不顯示連結管 | 使用者決議 | High | AC-002, AC-003 |
| 草稿採 API 相同的保存、omission 與缺值規則 | 使用者決議 | High | AC-003 |
| 同時為無法開蓋及銜接點時，以無法開蓋優先 | 使用者決議 | High | AC-002 |
| 下拉名稱附 `(銜接點)`；待架站標記最後 | 使用者決議 | High | AC-003 |
| Figma 未繪製下拉標記時，以文字需求為準 | 使用者決議 | High | AC-003 |

## Repository Evidence

- 現行程式與 `StoreDitchNodeRequestMapperTest` 仍使用舊小寫 key、Int request、Boolean read DTO，需整體替換並重建測試。
- `GutterBasicInfoFragment`、`GutterInspectActivity` 與 `WaypointAdapter` 是新資料跨表單、檢視及草稿流程的關鍵邊界。
- 既有 completed implementation／verification 為舊 API 契約所產生，應被保留為歷史紀錄但不可作為新版驗證證據。

## Resolved Conflicts

- 附件中的舊 `0/1` request 描述，由使用者明確決議覆蓋為 Boolean request。
- Figma 未標註銜接點下拉文字，不構成需求缺口；文字需求的標記與順序具有優先權。
- 原有文件的「連接管」文案一律改為「連結管」。

## Safe Planning Assumptions

- 缺失欄位和舊草稿都使用 false fallback，不向後端推送虛擬點的三項 key。
- 非虛擬點不可因資料未變更而省略兩個 Boolean key。

## Question Requiring Approval

- 無。
