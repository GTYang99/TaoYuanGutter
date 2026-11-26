# Execution Report

## Implementation

- Restored the original `MainActivity`, `MapWorkspaceFragment`, layer-sheet basemap UI, resource strings, and Gradle version file after removing unrelated destructive changes.
- Added the 13-district multi-select UI above the existing overlay and basemap controls, including all-select synchronization.
- Persisted district and overlay selections through `MainViewModel` in both map entry flows.
- Applied exact district-field filtering to the three GeoPackages: `Town`, `TOWN`, and `TOWNNAME`.
- Added empty-selection handling, default all-selected behavior, and generation-based stale tile invalidation.
- Preserved the existing no-argument offline data queries and overlay visibility callers with a default all-district selection.
- Preserved compatibility with existing offline overlay callers and added JVM coverage for state persistence and default generation behavior.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `./gradlew :app:compileDebugKotlin` | PASS | Kotlin compilation completed successfully. |
| `./gradlew :app:testDebugUnitTest` | PASS | Full Debug JVM suite completed successfully. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled at `app/build/outputs/apk/debug/app-debug.apk`. |
| `./gradlew :app:assembleDebugAndroidTest` | PASS | Instrumentation APK and all AndroidTest sources compiled successfully. |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest` | PASS | 7/7 emulator tests passed, including empty district selection and packaged GeoPackage geometry. |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest` | PASS | Full map-host class passed on `Medium_Phone(AVD) - 16`, including district selection, layer toggles, rendering, and lifecycle cases. |
| CI workflow-equivalent commands from `.github/workflows/android-ci.yml` | PASS | The five workflow-scoped JVM test classes and `:app:assembleDebug` passed locally; remote Actions result is not available. |
| `git diff --check` | PASS | No whitespace errors. |
| Scope/source scan | PASS | Restored files have only minimal feature additions; no AGP/version diff remains; all 13 districts and three field mappings are present. |

## Limitations

- Android UI interaction and map rendering verification passed on `Medium_Phone(AVD) - 16`; the snapshot helper avoids overlapping fill samples and tolerates renderer anti-aliasing only.
- CI and independent verification remain pending.

## Implementation Continuation (2026-11-26)

- Added typed full/district source selection for the existing offline polygon loader.
- Partial selection now creates the matching district source list; each district source falls back independently to the corresponding full GPKG filter if staging/indexing fails. Full selection retains the original three full sources.
- Added `OfflinePolygonSourceTest` for full-source and district-fallback mapping.
- Added the 26 district GPKGs and generated `evidence/asset-manifest.json` from the host validator.

| Check | Result | Evidence |
|---|---|---|
| `python3 docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py --manifest docs/tasks/feat-1118-2/evidence/asset-manifest.json` | PASS under accepted source-authority decision | Metadata and union consistency PASS; exact per-district label differences remain recorded as diagnostics. |
| `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*'` | PASS | Focused map JVM suite completed successfully with temporary non-secret Maps placeholder. |
| `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.OfflinePolygonSourceTest'` | PASS | New source mapping test completed successfully. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled with temporary non-secret Maps placeholder. |

ISS-003 is resolved by the user decision to trust district files as the authoritative partial-selection source.

## Debug Re-Implementation (2026-11-26)

- `LayersBottomSheet` now uses `MaterialCheckBox.setCheckedState` for unchecked, indeterminate, and checked states.
- District fallback queries now use the failed source's `queryDistricts` instead of the complete selected set.
- Overlay changes now stage candidate providers and retain the active overlays until source readiness, then commit only the latest request on the main thread.
- Offline data sources now expose `close()`; the shared source cache uses reference counts and providers release their source/index handles on `release()`.

| Check | Result | Evidence |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*'` | PASS | Focused map JVM suite compiled and completed successfully after the fixes. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled with a temporary non-secret Maps placeholder, then the placeholder was removed. |
| `git diff --check` | PASS | No whitespace errors in the task changes. |

The four verification issues remain open until the next independent verification confirms behavior and lifecycle bounds.

## Verification Round 2 Re-implementation (2026-11-26)

- Delayed candidate `TileOverlay` attachment until all candidate source readiness futures complete; stale candidates are released before attachment.
- Replaced fallback readiness blocking with non-blocking `CompletableFuture` composition, avoiding `.get()` on the single-thread source-index executor.
- Added `OfflineSourceReadinessTest`, which completes primary failure and fallback completion on a single executor and asserts readiness completes without deadlock.

| Check | Result | Evidence |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*'` | PASS | Focused map JVM suite completed successfully, including tri-state and fallback-readiness regression tests. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled with a temporary non-secret Maps placeholder, then the placeholder was removed. |

ISS-005 and ISS-008 remain open until independent verification confirms map attachment timing and emulator behavior.

## Verification Follow-up (2026-11-26)

- Updated `OfflinePolygonMapHostInstrumentedTest` to wait for the asynchronous overlay commit before asserting provider visibility; this removes the test's immediate-read race without weakening the expected state.
- Focused map JVM tests and `assembleDebug` pass on the latest source.
- `assembleDebugAndroidTest` passes and produces the instrumentation APK.
- `connectedDebugAndroidTest` is `NOT VERIFIED` in this environment because ADB reports no connected devices; no new device assertion result was produced.

## Verification Feedback Response (2026-11-26)

- The reported immediate provider read was confirmed in the host instrumentation helper and removed from all map-host visibility assertions, not only the lifecycle helper.
- All provider visibility checks now run through a bounded wait and main-thread read after the asynchronous candidate commit.
- `assembleDebugAndroidTest` passes after this test correction.
- Emulator execution remains `NOT VERIFIED` because the environment reports no connected device; the prior 3/4 result must not be reused as evidence for the corrected test revision.

## ISS-011 Debug Re-Implementation (2026-11-26)

- Updated `OfflinePolygonMapHostInstrumentedTest.assertMapShowsLayerInteriorColor()` to poll bounded `GoogleMap.snapshot()` attempts until the expected offline fill is visible at the projected interior point. The exact color tolerance and failure assertion remain unchanged; this only waits for asynchronous custom-tile composition.
- `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*' :app:assembleDebugAndroidTest --no-daemon --console=plain` — PASS.
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest` — 3/4 passed; the only failure was `NoActivityResumedException` during activity startup/reopen, before the snapshot assertion.
- Focused rerun: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class='com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest#mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers'` — PASS on `XQ-AU52 - 12`.
- The focused pass reached the map snapshot path after the new bounded readiness polling. Targeted ISS-011 repair confidence is 96%; full class/independent verification remains pending because the class-level activity-startup failure is environmental and unrelated to the reported overlay-color mismatch.

## Planned Coverage Completion (2026-11-26)

- Added `twoDistrictSelectionUsesOrAcrossAllLayersAndDistrictFiles` for AC-005. It compares one-district and two-district geometry signature multisets for all three full sources, checks empty selection, and verifies both gutter district assets stay within their own district. Focused emulator result: PASS (1/1).
- Added an internal source loader/releaser seam to `OfflinePolygonTileProvider` and `OfflinePolygonOverlayManager`, leaving default DataStore behavior intact. `newestGenerationWinsWhenOlderSourcesFinishLast` controls deferred generation-1 futures and immediate generation-2 futures; it asserts old-overlay retention, unattached candidates, latest-only commit, stale completion rejection, and exact source release counts. Focused emulator result: PASS (1/1).
- Added `failedDistrictSourceFallsBackWithoutBlockingAnotherDistrict` for AC-007. One injected source future fails while a second district source and full-source fallback use packaged data; both districts produce tiles. Focused emulator result: PASS (1/1). An initial file-corruption probe was invalid because the loader restaged the packaged file; it was replaced with the approved injected loader seam. A test-only WKB decoding mistake was corrected before the passing run.
- Added `twentyRoundDistrictSwitchKeepsProvidersOverlaysAndCachesBounded` for AC-008. Twenty fixed rounds of full, one district, two districts, empty, and full selection passed on `Medium_Phone(AVD)` Android 16. Every commit had the expected active generation and district set, at most one provider/overlay per layer, no pending candidate, source cache within mode bounds, and tile cache at most 48 entries per provider. A dedicated run ended with zero source references; a combined run compared against the preexisting source-cache keys held by other tests. Scoped log: `maxSwitchMs=1868`, `maxPssKb=276913`; no test crash or ANR was observed. An initial wrong test expectation for the five-source final generation was corrected before the passing run.
- Added `OfflinePolygonRestartInstrumentedTest` for AC-009. The save phase selected 桃園區 and 中壢區 and disabled the possible layer; after `adb shell am force-stop com.example.taoyuangutter`, the restore phase passed UI and controller-state assertions in a fresh process. Each focused phase passed 1/1. The test methods have deterministic class order for ordinary suite execution. An initial reflection helper assumed a backing field; it was corrected to use the existing getter-first pattern before the passing run.
- Validation build variant: Debug; app package: `com.example.taoyuangutter`; APKs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. A valid Maps key is supplied only through ignored `local.properties`; no credential is recorded here.
- Final developer-validation command `./gradlew --offline --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` passed: 187 JVM tests across 58 suites, 0 failures/errors/skips, plus both APK builds. The three affected emulator classes passed together after suite isolation was corrected: 17/17 tests in 109.695 seconds. The initial combined run failed only because the new lifecycle test counted source keys retained by earlier tests; the final test measures keys newly acquired during its own run.
- Remaining: AC-010's broader basemap/WMS/geometry regression evidence, final AC-011 performance interpretation, independent Verification on a new committed revision, and hosted CI. The unrelated uncommitted `gradle/libs.versions.toml` change remains outside this task commit.
