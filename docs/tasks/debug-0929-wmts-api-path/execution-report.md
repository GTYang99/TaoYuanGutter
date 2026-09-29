# Execution Report

## Implementation

- Added centralized Taipei active and DEMO inactive endpoint constants.
- Switched Retrofit API, WFS/GetFeatureInfo, all hardcoded GeoServer WMS and all three GeoServer WMTS layers to Taipei.
- Kept `https://wmts.nlsc.gov.tw/...` NLSC basemap URLs unchanged.
- Reused one OkHttp client for API, remote image downloads and GeoServer tiles.
- Added a debug-only hostname verifier exception for the exact `taipei.srgeo.com.tw` host. CA-chain validation remains enabled; release builds use the normal verifier.
- Converted GeoServer tile providers to use the shared HTTP tile client so the debug workaround covers WMS/WMTS tile requests as well as API/WFS.

## Validation

| Check | Result | Evidence |
|---|---|---|
| Focused JVM tests | PASS | `testDebugUnitTest --tests BackendEndpointsTest --tests Wmts3857TileProviderTest --tests Wms3857RequestBuilderTest` |
| Full JVM tests | PASS | `testDebugUnitTest` |
| Debug build | PASS | `assembleDebug`; APK at `app/build/outputs/apk/debug/app-debug.apk` |
| Release build | PASS | `assembleRelease`; unsigned APK at `app/build/outputs/apk/release/app-release-unsigned.apk` |
| Whitespace check | PASS | `git diff --check` |
| Production endpoint scan | PASS | Active source consumers resolve to Taipei constants; only inactive DEMO constants and retained test/historical references remain |

## Limitations

- Physical-device/API/WMS/WMTS runtime verification is `NOT VERIFIED`.
- The server certificate still does not include `taipei.srgeo.com.tw`; debug workaround is temporary and release deployment must wait for infrastructure certificate repair.
- CI result is `NOT VERIFIED`; no CI workflow/result is available in this repository.
- The temporary `local.properties` contained only a verification placeholder and was removed after builds.
