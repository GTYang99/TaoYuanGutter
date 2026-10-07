# FEAT-1006 Developer Execution Report

## Implementation

- Bundled the two exact user-linked source files in `app/src/main/assets/` and recorded source metadata and checksums in `asset-manifest.md`.
- Added background GeoJSON-to-SQLite/RTree indexing, GeoPackage staging/reading through its existing RTree, EPSG:3826 projection, polygon WKB decoding, and bounded offline tile rendering with preserved holes.
- Replaced the `legacyDitch` and `roadServey` feature requests in `MapOverlayController`, `GutterFormActivity`, and `MapPointPickerActivity`; `MainActivity` and `MapWorkspaceFragment` pass their app context to the shared controller. Provider caches and overlays are removed on disable and map-host teardown.
- Changed only the two specified layer-sheet strings to `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`.
- Added unit coverage for projection round-trip, GeoPackage geometry-header decoding, Polygon/MultiPolygon rings and holes, tile bounds, and exact opaque colors. Added instrumented coverage for packaged-source spatial queries and provider release.

## Validation

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| `:app:testDebugUnitTest` | PASS | Includes the new `OfflineGeometryTest`. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Instrumented data-store/provider test compiles. |
| `:app:assembleRelease` | PASS | Package contents inspected; see `asset-manifest.md`. Build used a temporary compile-only Maps key placeholder. |
| APK asset readback and SHA-256 comparison | PASS | Both exact UTF-8 asset paths were present and both uncompressed bytes matched the original files. |
| `adb devices -l`; `emulator -list-avds` | NOT VERIFIED | No connected Android device or configured AVD was available. Instrumented runtime, map rendering/alignment, layer interaction in all four hosts, and pan/zoom/no-ANR behavior remain unverified. |

An initial Gradle invocation could not locate Java through the default shell PATH. Using Android Studio's bundled JBR resolved that environment issue. A subsequent initial manifest-processing attempt lacked `MAPS_API_KEY`; the successful compile/package checks used an ephemeral non-secret placeholder, then removed `local.properties`. The APK is therefore packaging evidence and is not suitable for distribution or runtime map verification.

## Acceptance-Criteria Status

- AC-001: Asset inclusion and exact packaged bytes verified; local loading without feature-data requests is NOT VERIFIED on Android.
- AC-002: Required opaque color constants and geometry-hole preservation are covered by unit evidence; visual geographic alignment and rendered fill are NOT VERIFIED on Android.
- AC-003: All four source paths are wired and compile; runtime toggle behavior is NOT VERIFIED on Android.
- AC-004: Parsing/indexing is scheduled on a dedicated background executor and rendering uses the Maps tile-provider callback; pan/zoom, crash, and ANR behavior is NOT VERIFIED on Android.
- AC-005: Provider cache is bounded and release clears it; Android lifecycle/toggle behavior is NOT VERIFIED on Android.
- AC-006: Exact two strings are present; the layer sheet and unchanged surrounding UI were not inspected on device.

## Handoff

- Branch: `feature/FEAT-1006-wmts-offline-data`
- Build variant: release (unsigned package-content evidence)
- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Application ID: `com.example.taoyuangutter`
- Test account: none required for offline polygon unit/data-store tests.
- Required precondition for remaining checks: Android 9+ device with the app installed and a valid Maps key; make the two feature-data endpoints unavailable during the local-source check.
- Implementation commit: `af49905` (`feat(FEAT-1006): render offline map overlays`).
