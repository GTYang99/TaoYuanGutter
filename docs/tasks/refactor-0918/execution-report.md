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
| `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain` | NOT VERIFIED | Could not start: environment reports `Unable to locate a Java Runtime`. |
| `./gradlew assembleDebug` | NOT VERIFIED | Not attempted after the same missing Java Runtime limitation; it cannot execute in this environment. |
| Physical device test | NOT VERIFIED | No Android device and no reachable test environment were supplied. |

## Validation Limitation
- The local environment has no Java Runtime, so Gradle cannot compile or execute the focused unit test. This is an environment limitation, not a passing test result.
- The next required evidence is the focused JVM test and debug assembly on a Java-configured environment, followed by the defined physical-device scope.

## Handoff
- Package: `com.example.taoyuangutter`
- Build variant: `debug` (APK path is unavailable until `assembleDebug` succeeds)
- Preconditions: device can reach the specified HTTPS GeoServer WMTS endpoint; open the main map, point picker, and gutter form map.
