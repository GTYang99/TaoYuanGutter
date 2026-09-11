# Repository Analysis

## Task Classification
- `feature`：新增使用者可見的主地圖 WMS 覆蓋圖層與預設選取狀態；需要完整 Plan Review。

## Current Behavior
- `LayersBottomSheet` 提供五個覆蓋圖層 Checkbox；`OverlayState`、BottomSheet arguments、Host callback 和兩個主地圖 host 都以相同的五個 Boolean 傳遞狀態。
- `MapOverlayController` 為既有 WMS layer 各自保有 TileOverlay，依開關新增／移除，並在 `MainViewModel.overlayState` 還原時重新套用。
- `Wms3857TileProvider` 將 Google Maps tile 轉成固定 `EPSG:3857` GetMap request。`deleted_area` 的 capabilities 只宣告 `EPSG:3826`，無法安全重用該 provider。

## Expected Behavior
- 主地圖首次載入即顯示刪除資料，圖層 sheet 內的「0910刪除資料」同步呈勾選。
- 開關變更時只新增或移除新的 deleted-area TileOverlay；主地圖生命週期重建後仍以 OverlayState 正確還原。
- WMS request 使用 `EPSG:3826` BBOX，並維持其他既有 layer 的 EPSG:3857 行為。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/map/Wms3826TileProvider.kt`（新增）：將 Google Maps tile 角點投影為 TWD97 / TM2 EPSG:3826，產生 `deleted_area` GetMap URL。
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`：加入 deleted-area TileOverlay、default state、apply/remove 邏輯與 OverlayState 欄位。
- `app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt`：加入 state argument、Checkbox 初始化及 callback Boolean。
- `app/src/main/res/layout/sheet_layers.xml`、`app/src/main/res/values/strings.xml`：加入「0910刪除資料」選項及文字資源。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：依擴充後的 callback／newInstance 參數傳遞 state 並保存至 ViewModel。
- `app/src/test/java/com/example/taoyuangutter/map/`：新增 tile URL、EPSG:3826 BBOX、OverlayState default／toggle 的單元測試。

## Dependencies
- Google Maps `UrlTileProvider`／`TileOverlay` 的 256px Web Mercator tile index。
- GeoServer WMS 1.1.0 `deleted_area` capabilities：`SRS=EPSG:3826`、style `TY_RSGDBIP_0910刪除資料`。
- TWD97 / TM2 zone 121 forward projection（WGS84 longitude/latitude 至 EPSG:3826）：GRS80 `a=6378137.0`、`1/f=298.257222101`、central meridian `121°E`、`k0=0.9999`、false easting `250000m`、false northing `0m`；必須以可測試的純計算實作。
- `MainViewModel.overlayState` 的 configuration change 還原，以及 MainActivity／MapWorkspaceFragment 的 duplicate host contract。

## Risks
- Google Maps tile 是 Web Mercator，而 WMS 服務只支援 EPSG:3826；provider 必須正確投影 tile 四角並使用 BBOX 軸序，否則圖層會偏移或失真。
- 擴充 `OverlayState` 或 Host callback 若漏改任一 host，可能造成編譯失敗、狀態遺失或開關不同步。
- 新 layer 的 z-index 若不當，可能遮住 marker、量測標籤或其他疊圖。
- 服務可用性與圖資覆蓋範圍是外部依賴；需在實機驗證中分開記錄環境問題與 app 實作問題。

## Unknowns
- 無阻擋規劃的未知項目。實作時需以 capabilities 及桃園實際視圖驗證投影精度和 z-index；若影像對位不足，應記錄 implementation issue 後調整，不可改回不支援的 EPSG:3857。Google Maps／TileOverlay 生命週期不具現成的 local mock runtime，因此該部分只能以 instrumentation 或明確標記結果的實機 smoke test 驗證。
