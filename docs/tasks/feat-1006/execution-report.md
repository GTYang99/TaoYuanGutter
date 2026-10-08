# FEAT-1006 Developer Execution Report

> This report records the restarted implementation against the amended two-GPKG plan. Earlier GeoJSON-pair evidence is retained below as historical only.

## Current Reimplementation — Confirmed GPKG Pair (2026-10-08)

### Implementation

- Added `20260601_水務局未調查側溝_v5.gpkg` unchanged and removed the superseded possible-layer GeoJSON app asset. The legacy GPKG remains unchanged.
- Configured the possible layer for GeoPackage table `refactored`, EPSG:3826 `MULTIPOLYGON`, 61,435 features; both layers now use GeoPackage staging, metadata validation, cell indexing, and GeoPackage WKB decoding.
- Removed the GeoJSON importer/data source. On first load of the new possible GPKG, the app removes only the old `possible_polygon.sqlite` cache and its SQLite journal/WAL/SHM sidecars.
- Changed the possible-layer title to `可能側溝位置（.gpkg）`; fill colors and all four map-host wiring remain in place. The outline now follows amended AC-007: thicker at broad zoom and thinner at close zoom.
- Added device instrumentation for packaged asset size/hash and absence of the old asset, stale-cache invalidation, real-source query/render at zoom 16 and 20, and exact option labels. Added a MultiPolygon-with-hole GeoPackage decoder unit case.

### Developer Validation

| Command/check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:compileDebugUnitTestKotlin :app:assembleDebug :app:assembleRelease` | PASS | All main and test sources compile; debug and release APKs build in the FEAT-1006 worktree. No app was launched by this command. |
| Release APK contents, size and SHA-256 readback | PASS | APK is 43,752,030 bytes. Both GPKG asset entries match their manifest hashes and byte lengths; the obsolete GeoJSON entry is absent. See `asset-manifest.md`. |
| Android instrumentation on the designated emulator | PASS | `emulator-5554`, model `sdk_gphone64_arm64`, Android 14. Data-store suite: 5 tests passed. Four-host map suite: 3 tests passed in 36.767 seconds. No physical phone was used. |
| Host JVM unit-test execution | NOT RUN | Unit-test source compiled successfully. Actual GPKG/MultiPolygon/render checks ran in the emulator instrumentation suite. |
| Remote CI / independent verification | NOT VERIFIED | No result is available for this new worktree branch. |

### Acceptance-Criteria Status

- AC-001: APK packaging, source hashes, and real local GPKG queries PASS. Runtime tests exercise all four hosts, but feature-endpoint request capture (`legacyDitch`/`roadServey`) remains NOT VERIFIED.
- AC-002: Real GPKG geometry and exact fill-color tile checks PASS, including the possible-layer MultiPolygon source. Geographic alignment against the basemap in populated extents remains NOT VERIFIED.
- AC-003: MainActivity and MapWorkspaceFragment UI toggles PASS, with provider presence/removal asserted. Gutter form and point picker PASS their requested initial visibility states; interactive toggles there remain NOT VERIFIED.
- AC-004: All four hosts passed map-ready, zoom 16/18/20, and pan smoke checks in two launch cycles; camera operations returned under one second and no crash/ANR was observed. Broader populated/sparse-area responsiveness and UI-blocking observation remain NOT VERIFIED.
- AC-005: MainActivity and MapWorkspaceFragment hide/show provider removal/restoration and teardown cleanup PASS. All four hosts passed two-cycle teardown/reopen checks; interactive disable/re-enable coverage in form and point picker remains NOT VERIFIED.
- AC-006: PASS on emulator: exact two `.gpkg` titles asserted in instrumentation and observed in MainActivity UI.
- AC-007: PASS on emulator: the data-store suite rendered real sources and checked the zoom 16/18/20 profile, alternate profile, fill-color preservation, and provider cleanup.

### Handoff

- Branch: `codex/feat-1006-gpkg-restart` in the dedicated FEAT-1006 worktree.
- Production implementation commits: `5bb2b32` (`feat(FEAT-1006): use GeoPackage for possible layer`) and `ec16975` (`feat(FEAT-1006): scale outline width with zoom`). Emulator host lifecycle test commit: `31f6819` (`test(FEAT-1006): assert offline host layer lifecycle`).
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (43,752,030 bytes); debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Application ID: `com.example.taoyuangutter`.
- Device precondition: connect the designated Android 9+ test device; capture its serial, model, Android version, and available-memory observation before testing. No emulator may be used.
- Test scope: AC-001–AC-007 as mapped in `plan.md`, covering the four map hosts, both layers, toggles, outline/fill/holes, pan/zoom, and lifecycle; block the two feature-data endpoints for AC-001.
- Implementation is compiled and packaged; device validation remains the next action.

## Planning Re-entry — Zoom-Dependent Outline (2026-10-08)

- User clarified that the outline should be slightly thicker when viewing a broad area and smaller when zoomed in. The previous constant-width AC-007 did not express that behavior and is superseded by the amended requirement and plan.
- Current status: planning, plan review, and implementation are complete for the AC-007 change. Tunable defaults are 2.5 tile pixels at zoom 16 and below, linearly transitioning to 1.5 at zoom 20 and above. The style values remain subject to physical test-device visual validation.
- Validation: `git diff --check` PASS. `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:compileDebugUnitTestKotlin` PASS. `:app:assembleDebug :app:assembleRelease` PASS. No unit, instrumentation, emulator, or device tests were executed.
- ADB inventory was successfully read with elevated local access and listed only `emulator-5554` (`sdk_gphone64_arm64`). Per user instruction, no tests were run on the emulator. Physical-device visual/runtime validation remains NOT VERIFIED.
- Implementation commit: `ec16975` (`feat(FEAT-1006): scale outline width with zoom`). Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Validation Scope Update — Emulator Only (2026-10-08)

- User directed that runtime testing must use an Android emulator only; physical phone testing is out of scope. The requirement and plan now specify emulator-only testing, and plan review approved the update.
- Current ADB inventory contains `emulator-5554` (`sdk_gphone64_arm64`). All runtime/instrumentation commands will target this serial explicitly.
- Previous `NOT VERIFIED` entries describe validation before this scope update. Current-source emulator validation is now the next action; no emulator test result is claimed yet.

## Emulator Validation — Four Map Hosts (2026-10-08)

- Build command: Android Studio bundled JBR running `GradleWrapperMain :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` — PASS. An initial attempt exposed test-local variable scope errors in the new lifecycle assertions; declarations were moved outside the `try/finally` blocks and the same build then passed. No production-code change was needed.
- Device inventory: `adb devices -l` listed only `emulator-5554` (`sdk_gphone64_arm64`); `getprop ro.build.version.release` returned `14`. No physical phone was used.
- Install/run: `adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — PASS. `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — PASS, `OK (3 tests)`, 36.767 seconds.
- Coverage: MainActivity and MapWorkspaceFragment exact layer labels and off/on UI toggles; private provider visibility checked as each toggle changes; provider and overlay references checked cleared on host teardown. Gutter form and point picker each verified the requested initial per-layer visibility, map-ready callback, camera zoom 16/18/20 and pan, and complete provider release on close. Every host was launched twice.
- Responsiveness observation: each synchronous camera move returned in under one second; no crash or ANR appeared during the three tests. This is focused smoke evidence, not a broad performance profile.
- Shared-emulator condition: the other project's app remained installed/foreground after the run, but no competing AndroidJUnitRunner was active when checked; FEAT-1006 tests completed successfully. The separate AVD remains unavailable due to low host disk space and was not needed.
- Remaining evidence: feature-endpoint request capture and visual geographic alignment against the basemap are still `NOT VERIFIED`; form and point-picker interactive hide/show is also not exercised. See the per-AC status above.

## Emulator Validation — Focused Pass (2026-10-08)

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
