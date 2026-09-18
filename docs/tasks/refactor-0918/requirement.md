# Requirement

## Background
- GeoServer 的三個背景圖層改由 WMTS GetTile 端點提供，取代目前各畫面直接使用的 WMS GetMap 請求。

## Goal
- 將道路調查、既有水務局資料與桃園行政區背景圖層改為文件指定的 WebMercatorQuad WMTS 連結，並維持既有圖層開關與失敗降級行為。

## Functional Requirements
- 僅替換 `roadServey`、`legacyDitch`、`regions` 三個背景圖層。
- 使用 `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts`、`SERVICE=WMTS`、`REQUEST=GetTile`、`VERSION=1.0.0`、`TILEMATRIXSET=WebMercatorQuad`。
- tile 請求以 Google Maps 的 `z`、`y`、`x` 分別提供 `TILEMATRIX`、`TILEROW`、`TILECOL`。
- 三個圖層皆傳送空白 `STYLE=`，由後端選用預設樣式。
- 保留既有格式：道路調查為 `image/png`；其餘兩圖層為 `image/png8`。

## Non-functional Requirements
- 不新增第三方依賴；維持既有 TLS 網路通訊與 Google Maps `UrlTileProvider` 整合方式。

## Acceptance Criteria
- AC-001：三個指定背景圖層的請求皆為 WebMercatorQuad WMTS GetTile，且 URL 參數與文件相符。
- AC-002：三個指定背景圖層皆送出空白 `STYLE=`，不再指定既有中文樣式名稱。
- AC-003：主地圖、點位選擇與表單地圖皆使用相同的 WMTS 圖層設定；未指定的 WMS 圖層不受影響。
- AC-004：WMTS 圖層無法取得 tile 時，該圖層不顯示而其他地圖功能維持可用。

## Constraints
- 不得依 GetCapabilities 自動建立 tile grid，因服務端 WebMercatorQuad 的 `TopLeftCorner` 已知不正確。
- 不得修改 `map_ditch_nodes_labels`、`map_no_ditch_points` 或 `deleted_area` 圖層。

## Open Questions
- 無
