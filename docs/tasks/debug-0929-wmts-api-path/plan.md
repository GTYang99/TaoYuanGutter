# Implementation Plan

## Goal
- 將所有正式 API／GeoServer WMS、WFS、GetFeatureInfo 與三個 GeoServer WMTS layer 切到 Taipei，保留 DEMO inactive 常數，並提供精確 Taipei host 的臨時 hostname mismatch workaround。

## Scope
- 集中 active／legacy endpoint 常數與共用 HTTP client。
- 更新 Retrofit、GeoServer WMS/WFS/GetFeatureInfo、WMTS consumers。
- 將 GeoServer tile provider 改由共用 OkHttp 下載，讓 exact-host workaround 覆蓋 WMTS/WMS tile；NLSC WMTS 底圖維持原本 `UrlTileProvider`。
- 不改 layer 名稱、格式、樣式、座標、tile mapping、overlay 開關或 z-index。

## Affected Files
- `common/BackendEndpoints.kt`：Taipei active 與 DEMO inactive endpoint constants。
- `common/BackendHttpClient.kt`：Retrofit、圖片與 GeoServer tile 共用 HTTP client；精確 Taipei host 的暫時 hostname verifier。
- `common/HttpTileProvider.kt`：以共用 HTTP client 下載 GeoServer tile。
- `api/GutterApiService.kt`：Taipei Retrofit base URL、WFS/GetFeatureInfo 文件與 client。
- `api/GutterRepository.kt`：遠端圖片下載改用共用 client。
- `map/Wmts3857TileProvider.kt`、`Wms3857TileProvider.kt`、`Wms3826TileProvider.kt`：改用共用 tile client。
- `map/Wms3826RequestBuilder.kt`、`map/MapOverlayController.kt`：Taipei active WMS endpoint。
- `res/xml/network_security_config.xml`：Taipei domain trust anchors；保留 DEMO domain。
- `test/...`：endpoint constants、WMTS URL、WMS URL 與 tile failure tests。

## Implementation Steps
- 建立集中 endpoint constants，active 值固定為 Taipei，legacy DEMO 值只保留不使用。
- 建立共用 OkHttp client，保留既有 timeout/logging，對精確 Taipei host 暫時放寬 hostname verifier，仍驗證憑證鏈，並以明確常數標記待移除。
- 將 Retrofit 與遠端圖片下載接到共用 client。
- 將 GeoServer WMS/WMTS tile provider 接到共用 HTTP tile provider；維持 NLSC `UrlTileProvider` 不變。
- 將所有 hardcoded GeoServer active URL 改用 Taipei constants，更新 API/WFS/WMS 文件註解。
- 更新 network security config 與 focused unit tests，確認 legacy DEMO 沒有 runtime 使用。

## Test Plan
- `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.common.BackendEndpointsTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --tests com.example.taoyuangutter.map.Wms3857RequestBuilderTest --console=plain`
- `./gradlew testDebugUnitTest --console=plain`
- `./gradlew assembleDebug --console=plain`
- `./gradlew assembleRelease --console=plain`：確認 release 編譯且不帶 debug-only bypass。
- 靜態搜尋確認所有正式 GeoServer source consumers 使用 Taipei，NLSC WMTS URL 未變，DEMO 只剩 inactive constants／歷史文件／測試 fixture。

### Physical Device Test Scope
- Requires physical device: Yes
- Device/environment: Android 9+ device with authenticated test account, Google Maps key, Taipei DNS/network access
- In-scope Acceptance Criteria: AC-004, AC-006, AC-007, AC-008
- Regression risk: API login/WFS, GeoServer WMS/WMTS tile rendering, NLSC basemap preservation, debug-only certificate behavior
- Full regression required: No
- Full regression trigger: Taipei endpoint causes unrelated API or map regression, or release security review finds verifier leakage
- Stop condition: API/WFS、GeoServer WMS/WMTS、NLSC 底圖與 debug/release certificate cases all have a result, or certificate/server evidence blocks the case

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-004 | Debug device | Login, open map, toggle no-ditch/WMS overlays, query no-ditch WFS | Requests target Taipei and complete with debug workaround if server cert is still mismatched | Request log/result |
| AC-006 | Debug + release builds | Open Taipei API/GeoServer flow with current certificate | Both builds may pass hostname mismatch for exact Taipei host only; other hosts retain normal verification | Build/runtime result |
| AC-007 | Device | Open main, point-picker and form maps; inspect NLSC basemap | NLSC basemap remains unchanged and usable | Result |
| AC-008 | Device | Toggle three WMTS layers and relevant WMS overlays | Taipei GeoServer layers render or degrade tile-by-tile without changing controls/z-index | Result; screenshot on failure |

## Regression Plan
- Keep `wmts.nlsc.gov.tw` URL templates unchanged.
- Keep all WMTS layer names, formats, `STYLE=`, matrix set and x/y/z mapping unchanged.
- Keep WMS request parameters, overlay toggles, z-indexes and no-tile degradation unchanged.
- Ensure only the exact Taipei host uses the temporary verifier and there is no DEMO fallback; remove the verifier after certificate repair.

## Risks
- Current Taipei certificate mismatch is intentionally bypassed for the exact host in both builds; this is temporary and must be removed after the server certificate is issued correctly.
- Switching all GeoServer consumers can expose endpoint-specific server differences; runtime requests must be checked separately.
- Custom HTTP tile downloading changes the network boundary from Google Maps URL loading to app OkHttp; tile failures must still return `null` without blocking the map UI.

## Rollback Plan
- Revert the task commit to restore the prior DEMO/internal endpoint constants, UrlTileProvider behavior and network trust configuration.

## Current Behavior
- API uses an internal base URL; WMTS/WMS source URLs are hardcoded in separate files; DEMO constant is unused; Google Maps downloads GeoServer tiles through `UrlTileProvider` without the app's OkHttp verifier.

## Expected Behavior
- All formal API/GeoServer requests use Taipei constants, DEMO remains inactive, and debug-only Taipei hostname mismatch handling is shared by API, image and GeoServer tile requests. NLSC basemap remains unchanged.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Central endpoint constants and WMTS provider | WMTS focused tests and static source scan |
| AC-002 | Legacy constants only | Source scan and endpoint constants test |
| AC-003 | Shared WMTS provider retained | Existing provider call-site review |
| AC-004 | Retrofit and GeoServer consumers use Taipei | Endpoint tests, static scan, device request evidence |
| AC-005 | All GeoServer host values centralized | Source scan and WMS/WFS request tests |
| AC-006 | Exact-host temporary verifier | Debug/release build review and device result |
| AC-007 | NLSC code untouched | Static diff review and device smoke |
| AC-008 | Providers preserve parameters/failure behavior | Unit tests and device overlay check |

## Failure Behavior
- HTTP error, invalid URL, invalid tile coordinates or empty tile body returns no tile; map remains usable and other overlays continue.
- Release build temporarily bypasses hostname mismatch only for the exact Taipei host; the server certificate must still be corrected and the workaround removed before long-term release use.

## Security and Privacy
- No trust-all certificate manager is introduced.
- Workaround is restricted to exact host `taipei.srgeo.com.tw`; normal CA chain validation remains active and the temporary flag is explicit for later removal.
- No credentials or tokens are added to endpoint configuration or logs.

## Open Questions
- 無；使用者已確認全部 endpoint scope、Taipei host、inactive DEMO policy、NLSC exclusion 與 release temporary exact-host certificate workaround。
