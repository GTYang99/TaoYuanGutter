# Repository Analysis

## Current Behavior

- `MainShellActivity` 預設開啟地圖 tab；正式首頁工作區由 `MapWorkspaceFragment` 顯示。
- `GutterApiClient.ENABLE_GROUP_SIMULATION` 在 `GutterApiService.kt` 由 `BuildConfig.DEBUG` 計算。Debug build 為 true，release build 為 false。
- `BackendEndpoints` 目前提供 Taipei API URL 與 inactive DEMO API URL；沒有 `base` API target。`ACTIVE_API_BASE_URL` 一行目前註解且指向 Taipei。
- 使用者已確認 `base` 為 `http://192.168.10.84/TY_RSGDBIP/`；同一網址也有既有驗證記錄。
- `GutterApiClient.instance` 是 lazy singleton，建立 Retrofit 時固定採用 `BackendEndpoints.ACTIVE_API_TAPIEI_URL`。目前沒有 runtime endpoint selector。
- `GutterRepository` 在建立時取得 `GutterApiClient.instance`，故即使只替換 singleton accessor，已建立的 repository 仍可能持有舊 service。
- WMS、WMTS 各自使用 `BackendEndpoints` 的固定 Taipei URL。只改 API Retrofit base URL 不會切換 GeoServer。
- 初版曾將環境切換入口放在地圖首頁；使用者明確更正入口應位於登入頁，故修訂需求與 implementation scope。

## Expected Behavior

- Debug/test build 可從登入頁隱藏入口選擇一個明確環境。
- 所選環境須對 `GutterApiClient` 後續 request 生效；release 與 gate=false 時不出現控制項。
- 變更不應產生隱式跨環境 fallback。

## Evidence

| Claim | Evidence |
|---|---|
| 首頁預設為地圖工作區 | `MainShellActivity.kt:17,38-40,63-68` |
| gate 在 debug build 為 true、release 為 false | `GutterApiService.kt:284-288` |
| 現行 API 只明確列 Taipei 與 DEMO，沒有 base | `BackendEndpoints.kt:9-13` |
| Retrofit base URL 固定為 Taipei | `GutterApiService.kt:302-309` |
| API service 在 Repository 建立時被捕獲 | `GutterRepository.kt:70` |
| GeoServer endpoints 分開固定 | `BackendEndpoints.kt:15-28`、`Wmts3857TileProvider.kt:16`、`MapOverlayController.kt:105,168` |
| 歷史 base URL | 舊版 `GutterApiService.kt` 曾宣告 `BASE_URL = http://192.168.10.84/TY_RSGDBIP/`；目前 source 未定義此 endpoint |

## Root-Cause Classification

目前沒有證據顯示此切換功能曾存在後來失效，因此不分類為 implementation regression。這是新增測試能力的 `enhancement_request`。直接技術原因是 endpoint 僅以常數指定，Retrofit service 透過 lazy singleton 固定建構，repository 又會保存建立當下的 service；系統沒有環境選擇狀態或登入頁控制項。

## Affected Modules

- `common/BackendEndpoints.kt`: define the approved target URLs in a single typed inventory.
- `api/GutterApiClient`: replace the fixed lazy service with an endpoint-aware access boundary that downstream callers can actually observe.
- `api/GutterRepository` and any direct `GutterApiClient.instance` consumers: confirm how service changes propagate.
- Login UI: add a gated, hidden tester entry and selection UI in `activity_login.xml` / `LoginActivity.kt`.
- GeoServer URLs remain outside scope per the resolved API-only requirement.

## Risks

- A wrong or stale `base` URL can route API reads and writes to an unintended backend.
- Updating a global endpoint while an upload or request is in flight can create inconsistent environment usage unless the selected service is captured per operation.
- A switch that updates Retrofit but not already-created repositories will appear to work in UI while requests continue to the previous environment.
- API and map tile data can diverge if GeoServer remains fixed while API changes.
- Assumption: selection lasts for the current App process and resets to the existing default at next launch; no persistent preference is introduced.

## Validation Evidence Needed

- Unit coverage for target-to-base-URL mapping and the gate.
- A client/repository test proving a selection affects subsequent requests, including already-created repository lifecycle.
- UI coverage for visible/hidden entry in debug/release configurations.
- No live writes should be used solely to prove endpoint switching; use a controlled request target or test server.
