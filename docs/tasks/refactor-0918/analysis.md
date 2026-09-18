# Repository Analysis

## Current Behavior
- `Wms3857TileProvider` 以 WMS 1.1.0 GetMap 與 EPSG:3857 BBOX 生成背景圖層 tile URL。
- `MapOverlayController`、`MapPointPickerActivity`、`GutterFormActivity` 各自建立三個指定圖層的 WMS provider。
- 其他圖層仍依賴既有 WMS 3857 或 EPSG:3826 provider。

## Expected Behavior
- 三個指定背景圖層以固定 WebMercatorQuad tile 索引發送 WMTS GetTile，並使用後端預設樣式。
- 三個地圖入口共用同一份 WMTS 圖層與請求設定；既有 z-index、開關和失敗時 `null` URL 的降級行為不變。

## Affected Modules
- `map`：新增受測的 WMTS URL builder/provider 與三圖層設定。
- `MapOverlayController`、`gutter/MapPointPickerActivity`、`gutter/GutterFormActivity`：改用共用 WMTS provider。
- JVM unit tests：覆蓋請求參數及圖層設定。

## Dependencies
- Google Maps SDK 的 `UrlTileProvider` 傳入 x/y/zoom tile 索引。
- GeoServer GWC WMTS 端點，支援 0–24 的 WebMercatorQuad matrix identifier。

## Risks
- 端點要求 `STYLE=` 空值；省略參數與傳送空值的後端行為可能不同，故測試將確認 URL 明確包含 `STYLE=`。
- WMTS 與 WMS 的參數名稱及 tile 索引語意不同；以固定 URL builder 減少三個畫面產生不一致請求的風險。
- 無裝置或服務端回應證據時，實機視覺驗收須標記為未驗證。

## Unknowns
- 無
