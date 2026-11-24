# Repository Analysis

## Current Behavior

- 目前分支已有三個完整 GPKG 的離線 renderer 與行政區選擇／狀態保存實作。
- 目前 GPKG query 仍以完整資料來源和既有索引為主，App assets 尚未包含 13 個可能側溝與 13 個水務局既有資料分區檔。
- 目前尚未具備 29 個來源的 source manifest、分區優先/fallback 選擇、原子切換與差異化資源釋放。

## Expected Behavior

- 全選使用三個完整 GPKG；部分選取使用對應的兩組分區檔及行政區完整檔欄位篩選。
- 多來源在背景受控載入，完整結果準備後原子切換，舊 generation 結果丟棄。
- 來源、索引與 provider 具版本化、hash 隔離、fallback 與 bounded lifecycle。

## Affected Modules

- `app/src/main/assets/` — 保留 3 個完整檔並新增 26 個分區檔。
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt` — 29 個來源 manifest、分區 schema、屬性篩選、索引與 fallback。
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` — 多來源查詢、generation、provider release 與 renderer 共用。
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt` — 完整／分區模式切換、差異化來源管理、原子更新。
- `app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt`、`sheet_layers.xml` — 三態全選與行政區選擇狀態。
- `app/src/main/java/com/example/taoyuangutter/main/MainViewModel.kt`、`MainActivity.kt`、`MapWorkspaceFragment.kt` — 跨重啟保存與 map host 狀態傳遞。
- `app/src/test/java/com/example/taoyuangutter/map/`、`app/src/test/java/com/example/taoyuangutter/main/` — source manifest、state、切換、fallback 與 generation 測試。
- `app/src/androidTest/java/com/example/taoyuangutter/map/` — 29 資產、載入、顯示、lifecycle、重啟與效能觀察。
- `docs/tasks/feat-1118-2/evidence/` — host-side manifest、一致性、效能與 emulator evidence。

## Dependencies

- 外部資料夾的 26 個新 GPKG 與既有 3 個完整 GPKG。
- 既有 GPKG staging、SQLite index、geometry codec、EPSG:3826 renderer。
- 本地偏好設定、Google Maps tile overlay lifecycle 與既有 map hosts。
- 開發機／CI 的 SQLite/GPKG metadata 與 hash 驗證工具。

## Risks

- 29 個資產增加 APK 大小、首次 staging、索引建置與 storage 使用量。
- 同時載入多個分區 provider 可能造成 CPU、I/O、記憶體或 tile callback 壓力。
- 完整檔與分區檔資料不一致時，錯誤的 source priority 會造成地圖差異。
- 非同步切換若缺少 generation／atomic commit，可能出現舊圖層覆蓋新選擇或重複 overlay。
- `POLYGON` 與 `MULTIPOLYGON` schema 不同，source mapping 或 decoder 錯誤會造成空圖層。

## Unknowns

- 29 個檔案的最終 hash、feature count、extent、總大小與完整／分區一致性需在開發機資產盤點時產生 manifest；這不阻塞產品決策，但必須在 Implementation 前完成。
