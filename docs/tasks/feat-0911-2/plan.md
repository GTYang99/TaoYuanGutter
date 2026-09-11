# Implementation Plan

## Goal
- 在主地圖加入預設開啟的「0910刪除資料」WMS 覆蓋圖層，並以服務支援的 EPSG:3826 正確產生 tile request。

## Scope
- 僅包含主地圖的 deleted-area WMS provider、覆蓋狀態、圖層選單、生命週期還原與測試；不修改其他地圖畫面或既有 layer 設定。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/map/Wms3826RequestBuilder.kt`（新增）：不依賴 Android／Google Maps 的 Google tile → WGS84 → EPSG:3826 BBOX、query encoding 與 GetMap URL builder。
- `app/src/main/java/com/example/taoyuangutter/map/Wms3826TileProvider.kt`（新增）：僅包裝 request builder 並實作 Google Maps `UrlTileProvider`。
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`：新增 deleted-area overlay handle、預設開關、state 欄位、建立與移除邏輯。
- `app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt`：新增圖層 UI state、argument 與 Host callback 參數。
- `app/src/main/res/layout/sheet_layers.xml`、`app/src/main/res/values/strings.xml`：新增勾選控制項與「0910刪除資料」文案。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、`app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：傳遞新 Boolean、保存 OverlayState。
- `app/src/test/java/com/example/taoyuangutter/map/`：新增 EPSG:3826 request／state 行為測試。

## Implementation Steps
1. 建立不依賴 Android 的 `Wms3826RequestBuilder` 與 `Twd97Tm2Zone121Projection`：以 GRS80（`a=6378137.0`、`1/f=298.257222101`）、central meridian `121°E`、`k0=0.9999`、false easting `250000m`、false northing `0m` 實作 WGS84 經緯度至 EPSG:3826 的 Transverse Mercator forward formula；輸入／輸出單位均為 degree／metre，datum 為 TWD97（GRS80）。
2. 將 Google tile `(x,y,z)` 的 NW、NE、SW、SE 四個角轉為 WGS84（Web Mercator tile inverse），逐一投影至 EPSG:3826，對四個 easting／northing 分別取全域 min/max；只可用所得 `minX,minY,maxX,maxY` 建立 WMS 1.1.0 BBOX，不得只投影對角兩點。
3. 以純 Kotlin request builder 產生 URL，使用 UTF-8 percent encoding（包含中文 `STYLES`）並固定 endpoint、`SERVICE=WMS`、`REQUEST=GetMap`、`VERSION=1.1.0`、`LAYERS=deleted_area`、`STYLES=TY_RSGDBIP_0910刪除資料`、`SRS=EPSG:3826`、`FORMAT=image/png8`、`WIDTH=256`、`HEIGHT=256`、`TRANSPARENT=true`；`Wms3826TileProvider` 只呼叫此 builder。保留 `Wms3857TileProvider` 不變。
4. 在 `MapOverlayController` 新增 `showDeletedArea` state（default true）及專屬 TileOverlay；開啟時以合適 z-index 建立、關閉時 remove 並清空 reference，並在 `applyState()` 重新套用。
5. 擴充 `LayersBottomSheet` 的 layout、字串、arguments、初始化與 callback，使「0910刪除資料」顯示且預設勾選；初始 dispatch 必須使用傳入 state，而非強制預設 true 覆寫使用者已關閉的狀態。
6. 同步更新 MainActivity 和 MapWorkspaceFragment 的 `newInstance` 與 `onOverlayTogglesChanged` 呼叫，確保 ViewModel state 在底圖切換、畫面重建與 sheet 重開時不遺失。
7. 補齊下列兩層驗證：對 request builder 做 local JUnit；對 Google Maps TileOverlay、sheet callback 與重建行為做具體的實機 smoke test。無可用 emulator／Google Maps API runtime 時，這些 smoke cases 必須記為 `NOT VERIFIED`，不得列為自動測試通過。

## Test Plan
- 執行 local JUnit 的 `Wms3826RequestBuilderTest`，不載入 Android `Uri`、`UrlTileProvider` 或 Google Maps：驗證全部 WMS query parameters、256x256、PNG8、透明背景、UTF-8 style encoding、SRS 與 BBOX 軸序。
- 使用固定向量 `x=54849, y=28085, z=16`，斷言四角投影後的 BBOX（metres）為 `279756.220,2754335.172,280312.364,2754888.262`，各邊容許誤差至多 `0.1m`；此斷言必須使用硬編碼期望值，而非呼叫待測 helper 產生 expected 值。
- 執行 local JUnit 的 pure overlay-state mapping／argument contract test：default `showDeletedArea=true`、傳入 false 時保持 false；不以 Google Maps `TileOverlay` mock 假裝已驗證 remove/re-add。
- 執行 `testDebugUnitTest` 和 `assembleDebug`。
- 以 emulator 或實機在桃園範圍進行主地圖 smoke test，逐項記錄結果：初次載入預設可見、關閉後 overlay 被移除、再次開啟後重新顯示、切換底圖、旋轉／Activity recreation、關閉後重開 sheet 仍未勾選且 initial dispatch 不重新加回 overlay；確認 overlay 對位、marker 與既有 layer 均可見。

## Regression Plan
- 確認既有 plan、water-old、possible、region、no-ditch WMS overlays 的預設值與切換行為不變。
- 確認 no-ditch interaction callback、scope gutter polyline visibility、量測標籤與底圖切換不因新增 Boolean 而改變。
- 確認 MapPointPickerActivity 與 GutterFormActivity 未被納入此功能，維持原有疊圖集合。

## Risks
- EPSG:3826 影像放入 Google Maps Web Mercator tile 時的局部投影誤差；以四角全域 bounds 避免 tile 漏覆蓋，並以高縮放桃園實機比對驗證，必要時調整專用 provider 的座標計算。
- WMS capabilities、style 或外部服務暫時不可用；需把服務錯誤與 app regressions 分別記錄。
- MainActivity 與 MapWorkspaceFragment 的平行 host 實作可能漏接新 state。

## Rollback Plan
- 回退本任務單一 commit 可移除新 provider／選項／overlay，還原既有五個覆蓋圖層行為。

## Current Behavior
- 主地圖有五個覆蓋圖層選項；沒有「0910刪除資料」。
- 現有 WMS adapter 固定使用 EPSG:3857，但 `deleted_area` service capabilities 只提供 EPSG:3826。

## Expected Behavior
- 主地圖載入即顯示刪除資料，使用者可從圖層選單關閉與重新開啟。
- 新 WMS request 的 CRS、BBOX、layer、style 與 format 符合服務 capabilities，既有圖層與主地圖行為不變。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 4、5、6 | pure argument-contract JUnit、實機 default-state check |
| AC-002 | 1、2、3、7 | fixed-vector／URL JUnit、實機 WMS overlay smoke test |
| AC-003 | 4、5、6、7 | 實機 remove/re-add smoke test |
| AC-004 | 4、5、6、7 | false-state contract JUnit、rotation／sheet-reopen smoke test |
| AC-005 | 4、6、7 | targeted unit tests、existing-layer and interaction regression smoke test |

## Failure Behavior
- 若 WMS tile 請求失敗，Google Maps 依既有 TileOverlay 行為顯示缺少的 tile；不得中斷主地圖、既有 layer 或互動。驗證時記錄外部服務不可用，而非宣稱功能已通過。

## Security and Privacy
- 僅新增公開 WMS GetMap tile 請求，不傳送使用者、定位、照片或 token 資料；tile URL 不得記錄敏感 session 資訊。

## Open Questions
無。
