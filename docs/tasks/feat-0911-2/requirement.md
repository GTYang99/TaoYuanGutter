# Requirement

## Background
- 主地圖需要新增「0910刪除資料」WMS 覆蓋圖層，讓使用者辨識刪除區域。

## Goal
- 在主地圖圖層選單提供「0910刪除資料」選項，並於主地圖初次載入時預設顯示其 WMS 覆蓋圖層。

## Functional Requirements
- 在圖層套疊選單新增可勾選的「0910刪除資料」選項，預設為開啟。
- 選項開啟時，在主地圖加上 GeoServer WMS `deleted_area` 圖層；關閉時移除該 TileOverlay。
- WMS 使用 `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/wms`、`SERVICE=WMS`、`REQUEST=GetMap`、`VERSION=1.1.0`、`LAYERS=deleted_area`、`STYLES=TY_RSGDBIP_0910刪除資料`、`FORMAT=image/png8`、`WIDTH=256`、`HEIGHT=256`。
- 服務實際 capabilities 對 `deleted_area` 宣告 `SRS=EPSG:3826`；請求 BBOX 使用此 CRS 的 `minX,minY,maxX,maxY` 順序。

## Non-functional Requirements
- 不變更既有底圖、既有 WMS 覆蓋圖層、側溝線段、無側溝點互動、定位與表單地圖行為。
- Planning 不修改 production code；不降低 acceptance criteria；不略過測試。

## Acceptance Criteria
- AC-001：主地圖圖層選單顯示「0910刪除資料」選項，第一次開啟時為勾選。
- AC-002：主地圖初次載入後會顯示 `deleted_area` 覆蓋圖層，且請求的 WMS 參數與服務 capabilities 相符。
- AC-003：取消勾選時立即移除刪除資料覆蓋；再次勾選時可重新顯示。
- AC-004：切換底圖、重新建立主地圖畫面或再次開啟圖層選單後，刪除資料選取狀態與畫面覆蓋一致。
- AC-005：既有計畫調查、水務局舊資料、可能側溝位置、行政區、無側溝點、量測標籤與側溝線段行為不回歸。

## Constraints
- 僅限主地圖；不將新圖層擴充至點位選擇器或表單內嵌地圖。
- GeoServer capabilities 是座標系支援的執行時依據；不得對 `deleted_area` 沿用既有的 `EPSG:3857` provider。

## Open Questions
無。
