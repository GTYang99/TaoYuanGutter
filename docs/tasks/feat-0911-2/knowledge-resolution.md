# Knowledge Resolution

## Sources Reviewed
| Source | Evidence | Authority / confidence |
|---|---|---|
| 使用者附檔 `ty_feat_0911-1.md` | 要求新增預設開啟的「0910刪除資料」WMS 圖層，提供 endpoint、layer、style、GetMap 範例 | 高 |
| `GetCapabilities`，2026-09-11 | `deleted_area` 宣告 `SRS=EPSG:3826`，style 為 `TY_RSGDBIP_0910刪除資料` | 高；服務執行時事實 |
| `Wms3857TileProvider.kt` | 現有 provider 固定產生 `EPSG:3857` BBOX | 高；現況 |
| `MapOverlayController.kt`、`LayersBottomSheet.kt`、`MainActivity.kt`、`MapWorkspaceFragment.kt` | 主地圖覆蓋 state、UI、生命週期還原和兩個 host 的既有整合方式 | 高；現況 |

## Resolved Decisions
| Decision | Rationale | Affected AC |
|---|---|---|
| 新圖層只在主地圖提供 | 附檔 UI 明確指定「主地圖」；點位選擇器與表單不在 scope。 | AC-001 至 AC-005 |
| 使用獨立的 EPSG:3826 tile provider，不修改既有 `Wms3857TileProvider` 的行為 | capabilities 僅對 `deleted_area` 宣告 EPSG:3826；既有 provider 固定 EPSG:3857，直接重用會送出不受此 layer 支援的 CRS。 | AC-002、AC-005 |
| 將 Google Maps tile 邊界轉為 TWD97 / TM2 zone 121（EPSG:3826）後，以 WMS 1.1.0 `SRS` 與 minX,minY,maxX,maxY 請求 | 保持與 Google Maps TileOverlay 介面相容，同時符合服務 capabilities 和需求 BBOX 規格。 | AC-002 |
| 新選項的預設值為 true，納入 `OverlayState` 與 `LayersBottomSheet` arguments | 主地圖初次載入須預設開啟，且畫面重建／bottom sheet 重開必須一致。 | AC-001、AC-004 |

## Resolved Conflict
- 附檔列出 EPSG:3826 與 EPSG:4326，但 capabilities 對 `deleted_area` 僅宣告 EPSG:3826；採用 capabilities 的 EPSG:3826 作為實作 CRS。
- 現有其他圖層使用 EPSG:3857；此設定不可套用到 `deleted_area`，因其 capabilities 未宣告該 CRS。

## Assumptions Safe for Planning
- WMS 1.1.0 `SRS=EPSG:3826` 的 BBOX 軸序為 easting,northing，即 minX,minY,maxX,maxY；這與需求示例相同。
- 新 tile provider 只需供 `deleted_area` 使用，避免影響已運作的 EPSG:3857 layers。
- `image/png8`、256px tiles、透明背景符合既有 overlay 的資料量與疊圖模式。

## Questions Requiring Approval
無。座標系選擇以服務 capabilities 與使用者提供的 WMS 規格可直接判定。
