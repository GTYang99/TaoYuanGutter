# Knowledge Resolution

## Sources Reviewed

- 使用者需求與更新後附件 `ty_feat_0917.md`。
- Figma MCP：檔案 `IfmNbZKhr4wojZ2bF5rYHG`、節點 `61:1950`、`61:1949`、`1153:7633`。
- `GutterApiService.kt`、`GutterRepository.kt`、`GutterApiModels.kt`、`StoreDitchNodeRequestMapper.kt`。
- `GutterBasicInfoFragment.kt`、`GutterFormActivity.kt`、`ImportExistingWaypointBottomSheet.kt` 與對應 layouts、現有 mapper/UI tests。
- `docs/product/`、`docs/design/`、`docs/api/`、`docs/assets/`：僅有範本，無可覆蓋本任務的已核准規格。

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| 銜接點置於無法開蓋後方，兩者互斥；連接管置於淤積程度下方，均以「無／有」呈現 | 需求、Figma | High | AC-001 |
| 新增表單不應要求或送出使用者輸入的 `XY_NUM`；檢視／編輯保留顯示，編輯鎖定 | 需求 | High | AC-003 |
| 新增 request 完全省略 `XY_NUM`；系統生成值走既有 response `XY_NUM` | 使用者確認 | High | AC-003 |
| 檢視／編輯以同名 Boolean key 回填新屬性，缺值均為 false；新增 request 為 0/1 int | 更新後需求 | High | AC-002 |
| 匯入頁以無 query GET 取得最近存檔點位，response 為既有 NodeDetails 陣列 | 更新後需求 | High | AC-004 |
| Figma 對 `XY_NUM` 顯示的可編輯必填狀態不採用 | 需求優先於 Figma | High | AC-003 |
| 虛擬點關閉 Cant Open、Connect Point、Connect Pipe，重置為 false 並在 payload 省略三者 | 使用者確認 | High | AC-001, AC-002 |

## Repository Evidence

- `GutterBasicInfoFragment` 已統一負責欄位 layout 重排、可編輯／檢視／匯入鎖定、`collectData()` 與草稿變更通知；這是兩個新屬性的適當 UI 與暫存入口。
- `StoreDitchNodeRequestMapper` 及 `StoreDitchNodeRequest` 目前只映射既有欄位，且新增與編輯共用 mapper；`xyNum` 目前無條件由 `basicData["XY_NUM"]` 帶入。
- `NodeDetails` 未宣告銜接點或連接管欄位；`GutterInspectActivity` 與 `GutterBasicInfoFragment` 的既有回填均依此模型。
- `getClosestNodeDetails(lng, lat, token)` 在 service 與 repository 都把 `lng`／`lat` 設為不可省略參數。匯入 flow 現在取得定位權限、GPS/主地圖位置、marker 與 map padding，之後才查詢。

## Resolved Conflicts

- 無參數最近存檔點位改用 `GET /v1/node/closestNodeDetails`，response 為既有 `NodeDetails` 陣列；現有 query 與定位流程要移除。
- `is_connect_point`、`is_connect_pipe` 是檢視／編輯 response 的暫定同名 key，型別為 Boolean；缺值以 `false` 安全回填。
- Figma 與文字需求對 `XY_NUM` 的衝突已依優先順序採文字需求。新增 request 完全省略 key；系統名稱走既有 response `XY_NUM`。
- 虛擬點規則已確認：不顯示、不保留、不送出 Cant Open、Connect Point、Connect Pipe；切換為虛擬點時先清除其既有選取值。

## Safe Planning Assumptions

- 既有檢視頁的 `XY_NUM` 顯示、既有編輯 payload、按 XY_NUM 搜尋的另一個匯入分頁，都不在已核准需求中被移除。
- 既有空清單、錯誤與 401 處理可沿用，僅移除其定位前置條件。

## Question Requiring Approval

- 無。
