# FEAT-1006 Developer Execution Report

> Historical implementation and emulator evidence for the former two-GPKG scope is retained below. The latest approved FEAT-1006 plan adds the Taoyuan administrative-boundary GPKG and replaces the `regions` WMTS request; these prior results do not verify that additional source or offline region behavior and must not be treated as current acceptance evidence.

## Current Implementation — Offline Taoyuan Administrative Boundary (2026-10-14)

### Changes

- Merged `a86dc3d` into `feat/feat-1006-wmts-offline-data` as merge commit `555dea4`. Kept the approved FEAT-1006 three-GPKG requirement and marked the merge's previous two-source verification as historical.
- Added `桃園市行政區.gpkg` unchanged (1,081,344 bytes; SHA-256 `aeed88ae15bc53956bbea3e7ebd9a3faf1bf29fc45a6bfaa4188b5333458c1bc`).
- Added the region source (`桃園市行政區`, SRS id 100000, MultiPolygon) to the shared offline GeoPackage loader. During background index creation, geographic x/y coordinates are projected with the existing GRS80 TWD97 TM2 zone 121 transform and indexed in EPSG:3826 meters. The normalized geometries are cached in the versioned app-private index; the original packaged source is unchanged.
- Routed the existing region toggle in all four map hosts through the offline tile manager. Region tiles use a transparent background, antialiased `#762E77` outline, no fill, z-index `-0.5f`, and zero transparency. A 2.5 tile-pixel stroke produces measured local-tile medians of 2 px opaque core and 4 px footprint at alpha ≥64. Disable and host teardown release the provider overlay and bounded tile cache. No application call site constructs `Wmts3857TileProvider(REGIONS)`.
- Removed `.gpkg` suffixes from the two thematic layer titles, keeping `桃園行政區` and all other layer-sheet content unchanged.

### Validation

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `:app:testDebugUnitTest` | PASS | Includes the region projection control point and existing outline/geometry tests. |
| `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` | PASS | Instrumentation sources compiled and emulator test APK packaged. |
| `:app:assembleDebug` | PASS | Debug APK packaged at 49,534,430 bytes; the instrumentation asset test verified all three packaged source sizes and SHA-256 hashes. |
| `OfflinePolygonDataStoreInstrumentedTest` on `emulator-5554` | PASS | Android emulator; `OK (6 tests)`. The administrative GPKG loaded, queried the recorded z13 reference extent, and rendered exact purple boundary pixels over transparent background. |
| `OfflinePolygonMapHostInstrumentedTest` on `emulator-5554` | PARTIAL | The MapWorkspaceFragment and combined gutter-form/point-picker methods each passed (`OK (1 test)`). MainActivity verified its three actual layer checkboxes, local region tile, and provider lifecycle before its base-map snapshot timed out waiting for Google's `onMapLoaded` callback. The emulator reported DNS failures for `taipei.srgeo.com.tw`. |
| Current app-process OkHttp capture | NOT VERIFIED for successful network outcomes | During the passing MapWorkspace test, logcat recorded 96 outgoing GET entries: 0 for `regions`, `legacyDitch`, or `roadServey`, and 91 attempted requests for unrelated `map_ditch_nodes_labels`. DNS failures mean server responses were unavailable. Summary: `evidence/AC-001-emulator-offline-region-capture.md`. |
| Physical Android phone testing | NOT RUN | Explicitly excluded by user direction. ADB commands selected `emulator-5554`; no command targeted the attached physical device. |
| WMTS tile-style baseline parity | PASS for generated tile; host snapshot NOT VERIFIED | The local z13 tile has transparent background, exact `#762E77` pixels, 2 px opaque-core median and 4 px alpha≥64 footprint median. MainActivity snapshot over the live basemap could not complete because of emulator DNS failure. |
| Remote CI / independent verification | NOT VERIFIED | No push or CI run was requested for this FEAT-1006 branch; independent Verification has not started. |

The MainActivity test was repeated with the project's ignored local Maps configuration; layer checkbox, local tile, and lifecycle assertions completed, then the live map snapshot timed out. Emulator logcat reported `UnknownHostException` for `taipei.srgeo.com.tw`. The temporary worktree `local.properties` copy was removed after validation.

### Acceptance-Criteria Status

- AC-001: NOT VERIFIED end-to-end. All three packaged hashes pass and the region tile renders locally. The app-process capture observed no `regions`, `legacyDitch`, or `roadServey` GET attempt, but emulator DNS was unavailable so the capture cannot establish successful network behavior.
- AC-002: NOT VERIFIED for geographic alignment over a live base map. The two thematic layers retain their tested colors and rendering path; the projection control point passes.
- AC-003: PARTIAL. Region visibility is routed through the common manager in all four hosts. MainActivity and MapWorkspaceFragment region checkbox toggles were exercised; form and picker rendered the enabled region tile. The MainActivity live map presentation could not be inspected after the base-map callback timeout.
- AC-004: PARTIAL. Focused camera/navigation assertions passed in the three successful host-test paths; MainActivity's full host scenario could not finish after its base map did not report loaded.
- AC-005: PASS. All four host test paths completed provider release/re-enable/deduplication checks before the MainActivity snapshot wait.
- AC-006: PASS. Instrumentation asserts the extension-free thematic titles and existing administrative label; no other layer-sheet text/layout was changed.
- AC-007: PASS. Existing zoom-dependent thematic outline profile tests pass and the renderer behavior is unchanged for those layers.
- AC-008: NOT VERIFIED end-to-end. The source hash, local indexing/query, normalized region geometry, z13 offline tile, alpha behavior, and measured stroke-width parity are verified. The emulator's failed DNS resolution leaves successful request and MainActivity live-map presentation evidence unavailable.

### Handoff

- Branch: `feat/feat-1006-wmts-offline-data`
- Merge commit: `555dea4fd6d5c72c101935ad6020223255687b09`
- Implementation commit: pending
- Application ID: `com.example.taoyuangutter`
- Runtime test environment: Android emulator `emulator-5554` only; no physical-phone test.
- CI: pending; no remote result available. Independent Verification and Release have not started.

## Historical Reimplementation — Two GPKG Overlays, WMTS Regions (2026-10-08; superseded scope)

### Implementation

- Added `20260601_水務局未調查側溝_v5.gpkg` unchanged and removed the superseded possible-layer GeoJSON app asset. The legacy GPKG remains unchanged.
- Configured the possible layer for GeoPackage table `refactored`, EPSG:3826 `MULTIPOLYGON`, 61,435 features; both layers now use GeoPackage staging, metadata validation, cell indexing, and GeoPackage WKB decoding.
- Removed the GeoJSON importer/data source. On first load of the new possible GPKG, the app removes only the old `possible_polygon.sqlite` cache and its SQLite journal/WAL/SHM sidecars.
- Changed the possible-layer title to `可能側溝位置（.gpkg）`; fill colors and all four map-host wiring remain in place. The outline now follows amended AC-007: thicker at broad zoom and thinner at close zoom.
- Added device instrumentation for packaged asset size/hash and absence of the old asset, stale-cache invalidation, real-source query/render at zoom 16 and 20, and exact option labels. Added a MultiPolygon-with-hole GeoPackage decoder unit case.

### Developer Validation — Worktree Re-entry (2026-10-08)

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors in the re-entry changes. |
| `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` | PASS | Latest host test sources, including populated/sparse tiles and map snapshots, compile and package. A temporary ignored `local.properties` contained only a non-real Maps key placeholder and was removed after validation. |
| `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` | PASS | JVM unit tests, debug APK, and unsigned release APK built successfully. |
| Release APK asset readback | PASS | APK size 43,752,006 bytes. Both GPKG source contents matched the source hashes and lengths; exactly two `.gpkg` entries and no `.geojson` entry were present. |
| `OfflinePolygonDataStoreInstrumentedTest` on `emulator-5554` | PASS | Android 14 emulator, `OK (5 tests)`, 2.591 seconds. |
| `OfflinePolygonMapHostInstrumentedTest` on `emulator-5554` | PASS | Android 14 emulator, `OK (3 tests)`, 63.11 seconds in the request-capture rerun. Only the emulator appeared in `adb devices -l`; no physical phone was used. |
| Emulator app-process OkHttp request capture | PASS | During the four-host suite, 374 outgoing GET entries were logged for the app process; 242 requested WMTS `regions`, 128 requested WMS `map_ditch_nodes_labels`, and 0 requested `legacyDitch` or `roadServey`. See `evidence/AC-001-emulator-okhttp-capture.md`. This is application-level request logging, not a raw packet capture. |
| Source route review for feature requests | PASS, static evidence | The four host paths use `OfflinePolygonOverlayManager`; application call sites instantiate `Wmts3857TileProvider` only for `REGIONS`. No app call site uses `LEGACY_DITCH` or `ROAD_SURVEY`. |
| Remote CI / independent verification | NOT VERIFIED | No CI result or independent Verification record is available for this worktree revision. |

### Historical Acceptance-Criteria Status (prior two-source plan only)

- AC-001: PASS on emulator. Both GPKG source hashes match the packaged assets, all four host providers rendered local features, and the app-process OkHttp capture recorded 0 `legacyDitch`/`roadServey` requests while other map requests were logged. Capture details are in `evidence/AC-001-emulator-okhttp-capture.md`.
- AC-002: PASS on emulator. Real GPKG interior coordinates produced their exact opaque layer colors in host provider tiles; MainActivity snapshots at the projected interior points also matched the exact layer colors. Visual evidence is saved under `evidence/AC-002-main-legacy.png` and `evidence/AC-002-main-possible.png`.
- AC-003: PASS on emulator. MainActivity and MapWorkspaceFragment layer-sheet controls hid/restored the corresponding offline providers. Gutter form and point picker honored their passed visibility states and rendered their enabled local providers.
- AC-004: PASS for the focused plan scenarios. All four hosts reached zoom 16/18/20, panned through populated and sparse extents, and generated expected populated/empty local tiles. Camera calls returned under one second; no crash or ANR occurred in the test run.
- AC-005: PASS on emulator. Each host manager released provider/overlay references on hide and teardown, restored one provider per enabled layer, and retained the same provider on repeated enable.
- AC-006: PASS on emulator. Both exact `.gpkg` titles were asserted in instrumentation and observed in MainActivity.
- AC-007: PASS on emulator. Real-source rendering and style checks covered zoom 16/18/20, alternate outline width, unchanged interior fill, and provider cleanup.

### Restarted Validation Details

- Instrumentation command for host scenarios: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — `OK (3 tests)`, 63.11 seconds in the request-capture rerun.
- Instrumentation command for data/style scenarios: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — `OK (5 tests)`, 2.591 seconds.
- Host coverage now includes two launch cycles for all four map contexts, real GPKG tiles at interior points and NO_TILE outside the source extents, layer manager hide/show and duplicate prevention, exact visible map snapshot colors for both sources, and zoom/pan across populated and sparse coordinates.
- Responsiveness observation: synchronous camera changes returned in under one second and the request-capture rerun finished in 63.11 seconds with no crash or ANR. The earlier MainActivity map observation recorded `TOTAL PSS: 164785 KiB`; no new memory profiler trace was collected in this re-entry.
- Feature-request evidence: application call sites use the WMTS provider for `REGIONS`; `LEGACY_DITCH` and `ROAD_SURVEY` occur only in the provider enum/test coverage, not as application provider construction. In the host test run, the app-process OkHttp log recorded 374 outgoing GET entries and zero for either feature layer; other map layer requests appeared in the same log. The capture is application-level logging rather than a raw packet trace.
- Runtime tests used only `emulator-5554` (`sdk_gphone64_arm64`, Android 14). The shared emulator had no active instrumentation before these runs. No physical phone was used.

### Historical Handoff

- Branch: `codex/feat-1006-gpkg-restart` in the dedicated FEAT-1006 worktree.
- Build artifacts: `app/build/outputs/apk/debug/app-debug.apk`; `app/build/outputs/apk/release/app-release-unsigned.apk` (43,752,006 bytes).
- Application ID: `com.example.taoyuangutter`; no test account required for offline map instrumentation.
- Verification precondition: use the committed task branch and Android emulator only. The worktree is in use for this re-entry and must remain unchanged after independent Verification starts.
- Developer validation is complete against the emulator-only plan scope. The feature request capture is app-level OkHttp logging; a raw packet trace was not collected.

## Earlier Planning Re-entry — Zoom-Dependent Outline (2026-10-08; superseded by emulator-only scope and restarted validation above)

- User clarified that the outline should be slightly thicker when viewing a broad area and smaller when zoomed in. The previous constant-width AC-007 did not express that behavior and is superseded by the amended requirement and plan.
- Current status: planning, plan review, and implementation are complete for the AC-007 change. Tunable defaults are 2.5 tile pixels at zoom 16 and below, linearly transitioning to 1.5 at zoom 20 and above. At that planning point, zoom-dependent style appearance had not yet been checked at runtime; the restarted emulator validation above now covers zoom 16, 18, and 20. Physical-phone testing is out of scope per the user.
- Validation at that point: `git diff --check` PASS. `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:compileDebugUnitTestKotlin` PASS. `:app:assembleDebug :app:assembleRelease` PASS. Unit, instrumentation, and runtime tests had not yet been executed; the restarted emulator results are recorded above.
- This was the pre-reentry status before the user narrowed device testing to emulator-only. The emulator-only validation results are recorded above; no physical phone was used.
- Implementation commit: `ec16975` (`feat(FEAT-1006): scale outline width with zoom`). Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Earlier Validation Scope Update — Emulator Only (2026-10-08; superseded by restarted validation above)

- User directed that runtime testing must use an Android emulator only; physical phone testing is out of scope. The requirement and plan now specify emulator-only testing, and plan review approved the update.
- Current ADB inventory contains `emulator-5554` (`sdk_gphone64_arm64`). All runtime/instrumentation commands will target this serial explicitly.
- At the time of this update, current-source emulator validation had not started. Its results are recorded in the restarted validation section above.

## Earlier Emulator Validation — Four Map Hosts (2026-10-08; superseded by restarted validation above)

- Build command: Android Studio bundled JBR running `GradleWrapperMain :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` — PASS. An initial attempt exposed test-local variable scope errors in the new lifecycle assertions; declarations were moved outside the `try/finally` blocks and the same build then passed. No production-code change was needed.
- Device inventory: `adb devices -l` listed only `emulator-5554` (`sdk_gphone64_arm64`); `getprop ro.build.version.release` returned `14`. No physical phone was used.
- Install/run: `adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — PASS. `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — PASS, `OK (3 tests)`, 36.767 seconds.
- Coverage: MainActivity and MapWorkspaceFragment exact layer labels and off/on UI toggles; private provider visibility checked as each toggle changes; provider and overlay references checked cleared on host teardown. Gutter form and point picker each verified the requested initial per-layer visibility, map-ready callback, camera zoom 16/18/20 and pan, and complete provider release on close. Every host was launched twice.
- Responsiveness observation: each synchronous camera move returned in under one second; no crash or ANR appeared during the three tests. This is focused smoke evidence, not a broad performance profile.
- Shared-emulator condition: the other project's app remained installed/foreground after the run, but no competing AndroidJUnitRunner was active when checked; FEAT-1006 tests completed successfully. The separate AVD remains unavailable due to low host disk space and was not needed.
- Remaining evidence: feature-endpoint request capture and visual geographic alignment against the basemap are still `NOT VERIFIED`; form and point-picker interactive hide/show is also not exercised. See the per-AC status above.

## Earlier Emulator Validation — Focused Pass (2026-10-08; superseded by restarted validation above)

- Emulator: `emulator-5554`, `sdk_gphone64_arm64`, Android 14. The app reported `TOTAL PSS: 164785 KiB` during MainActivity map display.
- Command: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — PASS, `OK (5 tests)`, 61.19 seconds.
- Coverage: packaged GPKG names, sizes and SHA-256; old GeoJSON absence; stale cache and sidecar removal; real-source local query and PNG tile rendering at zoom 16, 18, and 20; fill-color preservation; style profile values; provider release; exact two `.gpkg` labels.
- MainActivity emulator UI: launched through the login screen's offline entry. The map rendered over the basemap, both exact `.gpkg` labels appeared, and the legacy and possible layer checkboxes were toggled off/on. No crash was observed in this focused flow. Screenshot was inspected locally at `/private/tmp/feat1006-main-map-zoom-default.png`.
- AC-006 and AC-007: PASS on emulator. AC-001 is partial: packaged hashes and local data queries pass; no feature-endpoint request capture or all-host runtime assertion is recorded yet. AC-002 geometry/color tests pass; visual alignment still needs confirmation. AC-003–AC-005 remain partial/unverified for the other map hosts and repeated lifecycle/pan/zoom scenarios.
- Interference: another project's `com.example.tp_ncolso_android.test` instrumentation became active on the shared emulator during manual UI checks. We did not stop it. A second `Medium_Phone` emulator could not start because the host has 406 MiB free; it reported insufficient disk space. Remaining host tests are paused until the shared emulator is idle.

---

## Historical Evidence — Superseded GPKG/GeoJSON Source Pair

The implementation and results in the following historical sections describe the earlier possible-layer GeoJSON source. They do not verify the amended GPKG source.

## Historical Implementation

- Bundled the two exact user-linked source files in `app/src/main/assets/` and recorded source metadata and checksums in `asset-manifest.md`.
- Added background GeoJSON-to-SQLite indexing and GeoPackage staging/reading with a persistent 512-meter EPSG:3826 cell index built from ordinary SQLite tables. Both sources filter cell candidates by exact bounds without requiring SQLite RTree support. Polygon WKB decoding, projection, and bounded offline tile rendering preserve holes.
- Replaced the `legacyDitch` and `roadServey` feature requests in `MapOverlayController`, `GutterFormActivity`, and `MapPointPickerActivity`; `MainActivity` and `MapWorkspaceFragment` pass their app context to the shared controller. Provider caches and overlays are removed on disable and map-host teardown.
- Changed only the two specified layer-sheet strings to `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`.
- Added unit coverage for projection round-trip, GeoPackage geometry-header decoding, Polygon/MultiPolygon rings and holes, tile bounds, and color constants. Instrumented coverage queries both packaged sources, verifies exact opaque colors in PNG tiles sampled from true polygon interiors, checks provider release, and asserts the two layer labels in the inflated UI layout.
- Added a same-color offline polygon outline controlled by `OfflinePolygonStyle.OFFLINE_POLYGON_STROKE_WIDTH_PX` (default `2.0f` tile pixels). The tile provider accepts an alternate width for focused rendering checks; fill colors and polygon paths remain unchanged.

## Historical Validation

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| `:app:testDebugUnitTest` | PASS | Includes the new `OfflineGeometryTest`. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Instrumented data-store/provider test compiles. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest` | PASS | Android 12 `XQ-AU52`; 3 tests, 0 failures. For both real polygon sources, zoom-16 and zoom-20 tiles rendered with the 2.0 default and injected 5.0 width; width variation changed tile edge pixels while a sampled interior pixel retained the exact layer fill color. Provider release and exact layer labels also passed. |
| `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug` | PASS | Built using Android Studio bundled JBR; unit tests passed, Android instrumentation sources compiled, and the updated renderer packaged in debug APK. |
| `:app:assembleRelease` | PASS | Rebuilt after the RTree fix; package contents inspected below. Build used a temporary compile-only Maps key placeholder. |
| Repository Android CI job commands | PASS locally | Reproduced the workflow's task-scoped JVM tests and `:app:assembleDebug`; command completed successfully. This is local evidence, not a remote CI result. |
| APK asset readback and SHA-256 comparison | PASS | Both original asset byte lengths and SHA-256 values matched. APK is 46,745,127 bytes; GPKG compressed to 7,668,975 bytes and GeoJSON to 25,342,525 bytes. See `asset-manifest.md`. |
| `git diff --check` | PASS | No whitespace errors before commit. |
| Physical Android device / map-host scenarios | NOT VERIFIED | Current `adb devices -l` lists only `emulator-5554` (`sdk_gphone64_arm64`); no physical Android device is attached. AVD instrumentation does not cover host UI toggles, visual alignment against the basemap, repeated host lifecycle, network request capture, or pan/zoom/no-ANR scenarios in all four hosts. |

An initial Gradle invocation could not locate Java through the default shell PATH. Using Android Studio's bundled JBR resolved that environment issue. A subsequent initial manifest-processing attempt lacked `MAPS_API_KEY`; the successful compile/package checks used an ephemeral non-secret placeholder, then removed `local.properties`. The APK is therefore packaging evidence and is not suitable for distribution or runtime map verification.

## Historical Acceptance-Criteria Status

- AC-001: Both exact packaged assets and both local data-store queries are verified on Android 14 AVD. Feature-network request capture and launch through each host are NOT VERIFIED.
- AC-002: Both real-data tile PNGs contain their required exact opaque fill color on Android 14 AVD; geometry-hole preservation has unit evidence. Geographic alignment against the base map is NOT VERIFIED on device.
- AC-003: All four source paths compile; runtime toggle behavior in the four hosts is NOT VERIFIED.
- AC-004: Indexing runs on the dedicated background executor and rendering uses the Maps tile-provider callback. Pan/zoom, crash, and ANR behavior in the four hosts are NOT VERIFIED.
- AC-005: Provider cache is bounded; AVD instrumentation verifies provider release after a tile request. Repeated UI toggles, host teardown, and re-enable behavior are NOT VERIFIED.
- AC-006: PASS on Android 14 AVD: inflated `sheet_layers.xml` displays exactly `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`. Diff review confirms no other layer labels or layout elements changed in the scope.
- AC-007: PASS on Android 12 `XQ-AU52`: both real layers rendered at zoom 16 and 20 with the 2.0 tile-pixel default; injected 5.0 width changed edge pixels while the sampled interior fill pixel remained unchanged. The configured width is independent of zoom and tile output dimensions remain 256×256. Manual broad/close map-host appearance remains part of the existing unverified all-host scenarios.

## Historical Handoff

- Branch: `feature/FEAT-1006-wmts-offline-data`
- Build variant: release (unsigned package-content evidence)
- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Application ID: `com.example.taoyuangutter`
- Test account: none required for offline polygon unit/data-store tests.
- Required precondition for remaining checks: Android 9+ physical device with the app installed and a valid Maps key; make the two feature-data endpoints unavailable during the local-source check.
- Implementation commit: `af49905` (`feat(FEAT-1006): render offline map overlays`).
- Debug-fix commit: `dddd391` (`fix(FEAT-1006): replace SQLite RTree dependency`).
- Tested revision: `0d58ada` (`feat(FEAT-1006): add adjustable polygon outlines`); the Android 12 instrumentation run completed on the same source/test tree before this commit was created.
- CI: NOT RUN / pending; no CI result is available in this worktree. Independent Verification and Release have not started.
- Remote check: GitHub branch search returned no `feature/FEAT-1006-wmts-offline-data` branch, and the feature branch has no configured upstream in this checkout. `git ls-remote --heads origin feature/FEAT-1006-wmts-offline-data` could not resolve `github.com`, so no remote CI run is available for the tested revision.
