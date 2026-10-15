# Root Cause

## Classification

- 根因是 **endpoint 選擇分散且 active／legacy 語意不清**；使用者已確認 Taipei 為 active、DEMO 僅 inactive，分類為 implementation configuration gap，route 為 planning → implementation。
- 追加根因是 **遠端照片使用 Glide 預設網路載入器**，未經過 API／GeoServer 共用 OkHttp，因此先前的 Taipei hostname workaround 沒有涵蓋照片畫面。

## Why It Occurs

1. API client 以 `BASE_URL` 作為 Retrofit active base URL；`DEMO_URL` 只是未使用的私有常數。修改 `DEMO_URL` 本身不會改變 API 請求目的地。
2. 三個 GeoServer WMTS layer 雖共用 `Wmts3857RequestBuilder`，但 builder 直接固定 DEMO endpoint，沒有 active Taipei 值與 legacy DEMO 值的明確設定界線。
3. 其他 GeoServer WMS endpoint 又散落在 `MapOverlayController` 與 `Wms3826RequestBuilder`，因此「切 WMTS」與「切全部 GeoServer」是兩個不同 scope，不能從單一手動 diff 推定。
4. TLS 額外 trust 設定只明確涵蓋 DEMO host；切換到 Taipei host 的憑證行為未被程式或測試描述。
5. `GutterPhotosFragment`、`GutterBasicInfoFragment`、`GutterInspectPhotosFragment` 與 `ImageDetailDialogFragment` 直接將遠端 URI 交給 Glide；預設 loader 不會使用 `BackendHttpClient` 的 hostname verifier。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`
- `app/src/main/java/com/example/taoyuangutter/map/Wmts3857TileProvider.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`
- `app/src/main/java/com/example/taoyuangutter/map/Wms3826RequestBuilder.kt`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/common/BackendGlideModule.kt`

## Regression Risk

- API read/write requests may continue to hit the internal server while map tiles hit Taipei, causing authentication or data mismatch.
- WMTS may render successfully while independent WMS overlays still point at DEMO, making the map appear partially migrated.
- A Taipei certificate failure could be misclassified as a WMTS implementation failure if network trust is not verified separately.
- Automatic fallback to DEMO would make environment selection non-deterministic and is unsafe for write APIs.

## Minimum Safe Direction

- Introduce explicit active Taipei and legacy DEMO endpoint values with one active selection; retain legacy values without runtime fallback.
- Update every GeoServer API/WMS/WFS/WMTS consumer approved by the user, while leaving NLSC WMTS basemap URLs unchanged.
- Use a shared OkHttp client for Retrofit, image downloads and GeoServer tile providers so the temporary exact-host hostname workaround has one controlled boundary across builds.
- Register a Glide remote-URI loader backed by the same client so displayed and preloaded remote photos receive the same temporary handling; retain normal local URI loaders.
- Add focused tests for active endpoint selection, legacy non-use, all three WMTS layers, WMS URLs and debug/release verifier behavior where testable.

## Evidence Boundary

- Production code changes are now authorized by the resolved user decisions and will be made only after plan review.
- Runtime Taipei connectivity remains `NOT VERIFIED`; release now has the explicitly approved temporary exact-host workaround, which must be removed when the server certificate is corrected.
