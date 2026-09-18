# Execution Report

## Implemented
- Added `BackgroundWmtsLayer`, `Wmts3857RequestBuilder`, and `Wmts3857TileProvider` for the three approved background layers.
- Replaced the WMS provider only for `roadServey`, `legacyDitch`, and `regions` in `MapOverlayController`, `MapPointPickerActivity`, and `GutterFormActivity`.
- Preserved the existing WMS providers for measurement labels, no-ditch points, and deleted-area overlays.
- Added focused JVM coverage for each WMTS layer's endpoint, request parameters, empty `STYLE=`, format, and WebMercatorQuad tile indices; added invalid-zoom no-tile coverage.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors reported. |
| Source-scope inspection | PASS | Only the approved three layers instantiate `Wmts3857TileProvider`; unapproved WMS layers remain on `Wms3857TileProvider`. |
| `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain` | PASS | Passed with Android Studio OpenJDK 25 and a temporary ignored `MAPS_API_KEY=test` placeholder. |
| `./gradlew assembleDebug --console=plain` | PASS | Debug APK assembled successfully with the same temporary ignored placeholder. |
| Physical device test | NOT VERIFIED | Sony XQ-AU52 (Android 12) is connected, but a valid non-test Google Maps API key is unavailable for a functional map build. |

## Validation Limitation
- The system Java path is unset, but Android Studio's bundled OpenJDK 25 successfully ran the focused test and assembled the debug APK.
- A temporary ignored `local.properties` containing only `MAPS_API_KEY=test` was created for manifest substitution and removed after validation; no real key was used or committed.
- The remaining required evidence is the defined physical-device WMTS failure-path scope, using a local non-test Google Maps API key that is authorized for this debug build.

## Handoff
- Package: `com.example.taoyuangutter`
- Build variant: `debug` (`app/build/outputs/apk/debug/app-debug.apk`)
- Preconditions: device can reach the specified HTTPS GeoServer WMTS endpoint; open the main map, point picker, and gutter form map.
