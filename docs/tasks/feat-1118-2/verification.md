# Verification Report

## Round and Revision

- Round: independent verification after round-two fixes.
- Commit under test: `906c4b5b6edddaa053cd18c35b5a89eac95a47c3` on `codex/feat/feat-1118-2`.
- Verification used immutable `git archive HEAD` snapshot `/private/tmp/feat1118-2-verification-r3.906c4b5`. Its production and test sources match the commit. The checkout's unrelated uncommitted `gradle/libs.versions.toml` change (`agp = 9.5.3`) was excluded and preserved; a temporary `MAPS_API_KEY=ci-placeholder` was used only in the snapshot.
- Emulator: serial `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`. It was stopped after scoped tests.
- No production or test source was changed during this verification. Task artifacts were updated to record this round.

## Final Result

**FAIL — implementation validation.** AC-011 fails because the committed map-host instrumentation class fails three host cases, including after one retry. The remaining assertion gap is in the test synchronization: the initial checks run immediately after Google Maps readiness while overlay providers are committed asynchronously. AC-005–AC-010 remain `NOT VERIFIED` as listed below. This result does not claim that the eventual map render is broken; the failing cases do not wait for the initial commit.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused map and state JVM tests | PASS | `./gradlew --offline --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*' --tests 'com.example.taoyuangutter.main.MainViewModelTest'`; 43 tests, 0 failures/errors. Includes `OfflineSourceReadinessTest.fallbackReadinessDoesNotBlockTheSingleSourceExecutor`. |
| Repository CI workflow-equivalent tests/build | PASS locally | The five JVM test filters from `.github/workflows/android-ci.yml`, followed by `:app:assembleDebug`; `BUILD SUCCESSFUL`. |
| Debug APK and instrumentation APK | PASS | `:app:assembleDebug :app:assembleDebugAndroidTest`; `BUILD SUCCESSFUL`. |
| `OfflinePolygonDataStoreInstrumentedTest` | PASS | 7/7 passed on the emulator, including full-source rendering, empty-selection filtering, packaged assets and released-provider behavior. |
| `OfflinePolygonMapHostInstrumentedTest` | FAIL | First run: 3/4 cases failed; the one permitted retry still failed. Failures occur at immediate provider checks in `mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers`, `mapWorkspaceFragmentShowsAndTogglesBothGeoPackageLayers`, and `gutterFormAndPointPickerMapHostsBecomeReady`. The checkbox synchronization case passed. The new wait helper is used for later visibility changes, but initial assertions at test lines 88–93, 143–150, and 205–210 still run immediately after map readiness. |
| GPKG host validator | PASS under accepted source-authority decision | `python3 docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py --manifest docs/tasks/feat-1118-2/evidence/asset-manifest.json`: `metadata=PASS`, `union_consistency=PASS`; `partition_consistency=FAIL` remains an accepted diagnostic per `knowledge-resolution.md`. |
| APK asset readback | PASS | The Debug APK contains all 29 manifest GPKGs; all packaged file sizes and SHA-256 hashes match. |
| Latest code-diff whitespace check | PASS | `git diff --check 94bd7cce0801f578d5226b1148aa2c30d654692e..906c4b5b6edddaa053cd18c35b5a89eac95a47c3` reports no whitespace errors in the new round's committed delta. |
| Remote CI | NOT VERIFIED | Combined commit status returned no checks, and the workflow-run lookup returned no runs for `906c4b5b`. Task state remains pending for remote CI. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | Host metadata and geometry-union validation pass under the approved district-source decision. APK readback verifies all 29 packaged filenames, sizes and SHA-256 hashes. |
| AC-002 | PASS | Source defaults select all 13 districts and enable all three layers; the emulator checkbox case confirms the initial all-selected state and all/individual toggle behavior. |
| AC-003 | PASS | `LayersBottomSheet` maps NONE/PARTIAL/ALL into `MaterialCheckBox.setCheckedState`; JVM boundary tests and the emulator selection interaction pass. The emulator test does not independently inspect the indeterminate icon rendering. |
| AC-004 | PASS | `OfflinePolygonSourceTest` covers full-source selection and district asset/schema/fallback mapping; source review confirms the full region GPKG and district-field filter. The full-source render passed on emulator. |
| AC-005 | NOT VERIFIED | Empty-selection queries return no geometry for all three layers. No completed test demonstrates multi-district OR results and district filtering across all three rendered layers. |
| AC-006 | NOT VERIFIED | The latest code keeps candidate overlays unattached until source readiness, checks the request id, then commits on the main thread (`OfflinePolygonTileProvider.kt:288–317`). The approved controlled out-of-order generation test is absent, and map-host tests fail before reaching render assertions. |
| AC-007 | NOT VERIFIED | The prior blocking path is replaced by asynchronous `sourceReadiness` composition; the single-thread executor regression test and district fallback mapping test pass. No provider-level injected source failure test demonstrates district isolation, continued successful sources, and no crash end to end. |
| AC-008 | NOT VERIFIED | Ref-counted source release and provider cleanup remain present, and released-provider instrumentation passes. The plan-required 20-round manager scenario and active provider/overlay/cache bounds were not run. |
| AC-009 | NOT VERIFIED | `MainViewModelTest` covers preference serialization, but no Android process/app restart and restore test was completed. |
| AC-010 | NOT VERIFIED | Geometry unit tests and the offline full-source render pass, but the map-host cases fail before the rendering/lifecycle checks. Basemap, overlap, holes, MultiPolygon and z-index regressions were not fully exercised. |
| AC-011 | FAIL | Focused JVM tests, CI-equivalent local JVM filters, both APK builds, validator and APK readback pass. However, the connected map-host class fails 3/4 cases and still fails on retry; the test's initial provider assertions do not wait for asynchronous source readiness. Remote CI and emulator performance/ANR measurements are also unavailable. See ISS-009. |

## Regression and Coverage Review

- Round-two code review confirms candidate overlays are attached only after all required sources report ready and the request is still current. Fallback readiness composes futures without blocking the single-thread store executor. The corresponding direct defects ISS-005 and ISS-008 are fixed in this revision.
- The required controlled out-of-order manager test, end-to-end fallback fault injection, and fixed 20-round lifecycle bounds remain absent. These omissions keep AC-006–AC-008 unverified.
- The map-host test synchronization fix is incomplete for initial startup checks. It fails before the map render, layer toggle or lifecycle assertions in three host cases. Update those checks to await the initial committed overlay state; do not treat the current failures as proof of an eventual render defect.
- Physical-device testing is out of scope; the required Android environment is the emulator.

## Issues and Next Action

- ISS-009 — AC-011 map-host instrumentation asserts providers before asynchronous commit; open, P2.
- ISS-005 and ISS-008 — previous premature-attachment and fallback-readiness defects are fixed and closed.
- Required next action: rerun the corrected instrumentation class. Complete the missing deterministic AC-006–AC-008 coverage and obtain remote CI before Release.

## Follow-up to ISS-009

The immediate provider assertions in the map-host instrumentation helper were corrected in commit `906c4b5b`: all host visibility checks now use a bounded wait and main-thread read after the asynchronous overlay commit. `assembleDebugAndroidTest` passes for the corrected test source. `connectedDebugAndroidTest` remains `NOT VERIFIED` because the current environment reports no connected devices; the prior 3/4 failure belongs to the pre-correction test behavior and must not be reused as a result for the corrected revision.
