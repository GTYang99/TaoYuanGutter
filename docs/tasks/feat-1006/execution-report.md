# FEAT-1006 Developer Execution Report

## Implementation

- Bundled the two exact user-linked source files in `app/src/main/assets/` and recorded source metadata and checksums in `asset-manifest.md`.
- Added background GeoJSON-to-SQLite indexing and GeoPackage staging/reading with a persistent 512-meter EPSG:3826 cell index built from ordinary SQLite tables. Both sources filter cell candidates by exact bounds without requiring SQLite RTree support. Polygon WKB decoding, projection, and bounded offline tile rendering preserve holes.
- Replaced the `legacyDitch` and `roadServey` feature requests in `MapOverlayController`, `GutterFormActivity`, and `MapPointPickerActivity`; `MainActivity` and `MapWorkspaceFragment` pass their app context to the shared controller. Provider caches and overlays are removed on disable and map-host teardown.
- Changed only the two specified layer-sheet strings to `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`.
- Added unit coverage for projection round-trip, GeoPackage geometry-header decoding, Polygon/MultiPolygon rings and holes, tile bounds, and exact opaque colors. Added instrumented coverage for packaged-source spatial queries and provider release.

## Validation

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| `:app:testDebugUnitTest` | PASS | Includes the new `OfflineGeometryTest`. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Instrumented data-store/provider test compiles. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest` | PASS | Android 14 `Medium_Phone(AVD) - 14`; 2 tests, 0 failures. Both packaged sources returned polygon geometry for the Taoyuan extent; a populated legacy tile was returned before release and `NO_TILE` afterward. |
| `:app:assembleRelease` | PASS | Rebuilt after the RTree fix; package contents inspected below. Build used a temporary compile-only Maps key placeholder. |
| Repository Android CI job commands | PASS locally | Reproduced the workflow's task-scoped JVM tests and `:app:assembleDebug`; command completed successfully. This is local evidence, not a remote CI result. |
| APK asset readback and SHA-256 comparison | PASS | Both original asset byte lengths and SHA-256 values matched. APK is 46,745,127 bytes; GPKG compressed to 7,668,975 bytes and GeoJSON to 25,342,525 bytes. See `asset-manifest.md`. |
| `git diff --check` | PASS | No whitespace errors before commit. |
| Physical Android device / map-host scenarios | NOT VERIFIED | No physical Android device is attached. AVD instrumentation does not cover host UI toggles, visual alignment/colors, repeated host lifecycle, network request capture, or pan/zoom/no-ANR scenarios in all four hosts. |

An initial Gradle invocation could not locate Java through the default shell PATH. Using Android Studio's bundled JBR resolved that environment issue. A subsequent initial manifest-processing attempt lacked `MAPS_API_KEY`; the successful compile/package checks used an ephemeral non-secret placeholder, then removed `local.properties`. The APK is therefore packaging evidence and is not suitable for distribution or runtime map verification.

## Acceptance-Criteria Status

- AC-001: Both exact packaged assets and both local data-store queries are verified on Android 14 AVD. Network request capture with feature endpoints unavailable and launch through each host are NOT VERIFIED.
- AC-002: Required color constants and geometry-hole preservation have unit evidence. Visual fill and geographic alignment are NOT VERIFIED on device.
- AC-003: All four source paths compile; runtime toggle behavior in the four hosts is NOT VERIFIED.
- AC-004: Indexing runs on the dedicated background executor and rendering uses the Maps tile-provider callback. Pan/zoom, crash, and ANR behavior in the four hosts are NOT VERIFIED.
- AC-005: Provider cache is bounded; AVD instrumentation verifies provider release after a tile request. Repeated UI toggles, host teardown, and re-enable behavior are NOT VERIFIED.
- AC-006: Exact two strings are present in resources; the layer sheet and unchanged surrounding UI were not inspected on device.

## Handoff

- Branch: `feature/FEAT-1006-wmts-offline-data`
- Build variant: release (unsigned package-content evidence)
- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Application ID: `com.example.taoyuangutter`
- Test account: none required for offline polygon unit/data-store tests.
- Required precondition for remaining checks: Android 9+ physical device with the app installed and a valid Maps key; make the two feature-data endpoints unavailable during the local-source check.
- Implementation commit: `af49905` (`feat(FEAT-1006): render offline map overlays`).
- Debug-fix commit: `dddd391` (`fix(FEAT-1006): replace SQLite RTree dependency`).
- CI: NOT RUN / pending; no CI result is available in this worktree. Independent Verification and Release have not started.
- Remote check: the feature branch has no configured upstream in this checkout. `git ls-remote --heads origin feature/FEAT-1006-wmts-offline-data` could not resolve `github.com` in the current environment, so remote CI status and push availability could not be verified.
