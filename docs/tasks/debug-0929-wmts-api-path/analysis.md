# Repository Analysis

## Task Classification

- `debug`／configuration fix：根因已由靜態證據確認，使用者已補齊 active host、遷移範圍、inactive DEMO 與憑證暫時處理方式；可進入 Planning 與 Implementation。

## Current Behavior

- `GutterApiClient` 在 `GutterApiService.kt:300-332` 同時宣告內部 `BASE_URL` 與 DEMO `DEMO_URL`，但 Retrofit 實際使用的是 `.baseUrl(BASE_URL)`；`DEMO_URL` 目前未使用。
- `Wmts3857RequestBuilder.BASE_URL` 在 `Wmts3857TileProvider.kt:16` 固定為 `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts`。
- `MapOverlayController`、`MapPointPickerActivity`、`GutterFormActivity` 共使用 `Wmts3857TileProvider`；三個 WMTS layer 的 endpoint 因而由同一個 builder 集中產生，而不是三處各自硬編碼。
- 非 WMTS 的 GeoServer WMS 仍有獨立固定值：`MapOverlayController` 的測量標籤／無側溝點，以及 `Wms3826RequestBuilder` 的 `deleted_area`。
- `network_security_config.xml` 的額外 CA trust domain 只列 `demo.srgeo.com.tw`；切到 Taipei host 時需要納入同一 CA chain。hostname mismatch 不能由 network security config 單獨解決。
- 原工作區的未提交手動變更把部分文件註解、WMTS、WMS builder 與 overlay URL 改成 Taipei，但沒有改變 Retrofit 的 `.baseUrl(BASE_URL)`，也沒有新增 DEMO 的具名 inactive reserve。

## Expected Behavior

- API、GeoServer WMS/WFS/GetFeatureInfo 與 WMTS endpoint 都以 Taipei 為 active，並由集中 endpoint 常數提供。
- 三個 WMTS layer 應維持既有 `SERVICE=WMTS`、`VERSION=1.0.0`、`WebMercatorQuad`、layer format、空白 `STYLE=` 與 z/y/x 對應。
- 原本 DEMO endpoint 以清楚命名的非啟用值保留，不應在網路失敗時自動切換環境。
- 暫時憑證繞過只針對精確 `taipei.srgeo.com.tw` host，所有 build 都可使用；不繞過 CA chain 驗證，正式憑證修復後移除。
- 未列入核准範圍的 WMS/WFS 行為、圖層開關、z-index 與失敗降級不應因 endpoint 整理而改變。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/map/Wmts3857TileProvider.kt`：三個背景 WMTS endpoint 的集中設定。
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`：WMTS 呼叫入口及目前獨立 WMS endpoint。
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt`、`GutterFormActivity.kt`：共用 WMTS provider 的另外兩個地圖入口。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`：Retrofit API base URL 與 GeoServer API interface。
- `app/src/main/java/com/example/taoyuangutter/map/Wms3826RequestBuilder.kt`：EPSG:3826 WMS endpoint。
- `app/src/main/res/xml/network_security_config.xml`：host-specific CA trust 設定。
- `app/src/test/java/com/example/taoyuangutter/map/Wmts3857TileProviderTest.kt` 及相關 request-builder tests：active／legacy endpoint 與回歸驗證。

## Dependencies

- Retrofit 要求 base URL 以 `/` 結尾，且 API relative paths 目前以 `api/...` 開頭。
- Google Maps `UrlTileProvider` 將 x/y/zoom 傳給共用 WMTS builder；三個畫面均依賴此行為。
- Taipei 與 DEMO host 的 DNS、TLS 憑證、CA chain 及 GeoServer capabilities 是外部環境依賴。
- 既有 `refactor-0918` requirement 曾把 DEMO WMTS endpoint 視為固定規格；本次使用者指示優先，但必須先解決 Taipei host 的文字矛盾。

## Risks

- 只改 WMTS builder 會造成 WMTS 與 WMS/API 分屬不同環境；全域替換又可能超出使用者只要求 WMTS 的範圍。
- 只修改 `DEMO_URL` 不會改變 API runtime，容易形成「文件看似已切換、實際請求仍走內部 IP」的假完成狀態。
- Taipei host 未列入額外 trust domain 時，可能出現 TLS／憑證行為差異；release 也只能把 exact-host hostname bypass 作為憑證修復前的臨時措施。
- 照片顯示路徑另有 Glide 預設 loader；若不替換其遠端 URI loader，照片會繼續使用不含 Taipei workaround 的網路堆疊。
- 加入自動 DEMO fallback 會把資料環境混用，可能導致登入、圖資與寫入 API 不一致；在 OQ-004 未確認前禁止加入。

## Unknowns

- Taipei 正式憑證尚未建立，release runtime 連線能力在憑證修復前不應視為已驗證。

## Evidence Collected

- 全專案搜尋 `demo.srgeo.com.tw`、`taipei.srgeo.com.tw`、`WMTS`、`geoserver`：確認 app source、network security、tests 與歷史 task 文件的 endpoint 分布。
- `docs/tasks/refactor-0918/requirement.md`：既有 WMTS AC-001～AC-004 維持三個 layer、WebMercatorQuad 與 DEMO endpoint 的原規格。
- `docs/tasks/refactor-0918/verification.md`：既有三入口共用 provider 的 source review 與 focused WMTS test 證據。
- 原工作區 `git diff`：確認使用者已手動修改四個 production source 檔案，但該 diff 未改 Retrofit active base URL。

## User Decisions

- 正式站所有 API／GeoServer endpoint 改用 `taipei.srgeo.com.tw`。
- 所有 GeoServer WMS/WFS 一起切換；台北市都發局 NLSC WMTS 底圖不變。
- DEMO 只保留 inactive 常數。
- 憑證修復前，release 也暫時允許精確 Taipei host 的 hostname mismatch；正式憑證修復後移除。
