# Verification Report

## Round and Revision

- Round: independent verification after the Debug fixes.
- Commit under test: `94bd7cce0801f578d5226b1148aa2c30d654692e` on `codex/feat/feat-1118-2`.
- Tests ran from immutable `git archive` snapshot `/private/tmp/feat1118-2-verification-r2.A15NyG`; its source version matched the committed revision. The checkout's uncommitted `gradle/libs.versions.toml` change (`agp = 9.5.3`) was excluded and preserved. A temporary `MAPS_API_KEY=ci-placeholder` was used only in the snapshot.
- Device: Android Emulator `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`.
- The emulator was stopped after the scoped tests. No production source or test file was modified during Verification.

## Final Result

**FAIL — implementation.** AC-006 and AC-007 have source-level defects that contradict the approved atomic-switch and fallback requirements. Route to Debug before any re-implementation. AC-005 and AC-008–AC-011 remain `NOT VERIFIED`; they are not counted as passes.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused JVM tests | PASS | `./gradlew --offline --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.*' --tests 'com.example.taoyuangutter.main.MainViewModelTest'`; 42 tests, 0 failures/errors. Includes source mapping, selection-state helper, map state and persistence tests. |
| Debug build | PASS | `./gradlew --offline --no-daemon :app:assembleDebug`; `BUILD SUCCESSFUL` on the immutable snapshot. |
| `OfflinePolygonDataStoreInstrumentedTest` | PASS | `:app:connectedDebugAndroidTest` with the class filter; 7/7 passed on the emulator, including packaged-source checks, full-source tile rendering, empty-selection filtering, and provider release behavior. |
| `OfflinePolygonMapHostInstrumentedTest` | FAIL / limited evidence | Initial run and the one permitted retry both reported 3 failures and 1 pass. The three failures are immediate provider-field assertions at lines 617–619, called as soon as the Google map reports ready; the new manager assigns active providers only after source readiness. The test does not wait for that asynchronous commit, so these failures do not establish the eventual rendered result. The passing case exercises all-selection and individual district checkbox interactions, but does not assert the indeterminate visual state. |
| GPKG host validator | PASS under the resolved source-authority decision | `python3 docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py --manifest docs/tasks/feat-1118-2/evidence/asset-manifest.json`: `metadata=PASS`, `union_consistency=PASS`; per-district `partition_consistency=FAIL` remains the accepted diagnostic recorded in `knowledge-resolution.md`. |
| APK asset readback | PASS | The Debug APK contains 29 GPKGs. Every manifest filename was present and all 29 packaged sizes and SHA-256 hashes matched `asset-manifest.json`. |
| Remote CI | NOT VERIFIED | State remains pending; GitHub workflow lookup returned no runs for this commit. |
| Diff whitespace check | FAIL, documentation-only | `git diff --check 8d484d5a6d31924d099e793476f898ed99594aa..94bd7cce0801f578d5226b1148aa2c30d654692e` reports trailing whitespace in `docs/tasks/feat-1118-2/plan-review.md` lines 3–6 and 176. No production file is implicated. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | Host metadata and geometry-union validation pass under the accepted district-source decision. APK readback confirms all 29 manifest files and verifies each packaged size and SHA-256. |
| AC-002 | PASS | `LayersBottomSheet` defaults to the fixed 13-district set and enables all three layers; the emulator checkbox case confirms initial all-selection and all/individual toggle synchronization. |
| AC-003 | PASS | `LayersBottomSheet` maps NONE/PARTIAL/ALL to `MaterialCheckBox.setCheckedState`; `DistrictSelectionStateTest` passes all count boundaries, and the emulator interaction test passes all-selection and per-district toggles. The UI test does not independently assert the drawn indeterminate icon. |
| AC-004 | PASS | `OfflinePolygonSourceCatalog` and `OfflinePolygonSourceTest` verify full-source selection and per-district GPKG names, schema and district fallback mapping; the region source remains the full GPKG with its district field. Full-source loading/rendering passed on emulator. |
| AC-005 | NOT VERIFIED | Empty-selection filtering passed for all three layers. No completed emulator or query-level test in this round demonstrates OR results across multiple selected districts for all three layers. |
| AC-006 | FAIL | `OfflinePolygonOverlayManager.stageVisibility` attaches each candidate `TileOverlay` immediately (lines 293–298), before `whenSourcesReady()` completes; it removes the old overlays only later on the main thread (lines 299–315). A candidate can therefore start returning tiles while the old result is still attached, so the required non-overlapping atomic swap is not guaranteed. No deterministic out-of-order generation test exists. See ISS-005. |
| AC-007 | FAIL | Per-district fallback query scope is now limited by `definition.queryDistricts` (provider lines 56–71), but readiness handling has a deadlock path: `whenSourcesReady()` uses `future.handle` and synchronously calls `fallbackFuture(...).get()` (lines 100–108). Source loads and fallback loads share the single-thread `OfflinePolygonDataStore` executor (store lines 58–60, 79–89). If a district load fails while its completion handler runs on that executor, the handler waits for fallback work queued to the same occupied executor. The fallback cannot complete and the selected result cannot become ready. See ISS-008. |
| AC-008 | NOT VERIFIED | Provider release now evicts tile cache and decrements source references; the last reference closes source and index databases. Candidate cleanup removes overlays and releases providers, and the provider-release instrumentation passed. The plan-required fixed 20-round manager switching scenario and post-release provider/overlay/cache bounds were not run. |
| AC-009 | NOT VERIFIED | `MainViewModelTest` verifies preference serialization, including selected districts and explicitly empty selection. It does not exercise an Android process/app restart and restore. |
| AC-010 | NOT VERIFIED | Existing geometry unit tests and the offline full-source render passed, but map-host tests failed before their rendering/lifecycle assertions. Basemap, layer overlap, holes, MultiPolygon and z-index regression coverage was not completed. |
| AC-011 | NOT VERIFIED | Focused JVM tests, Debug build, source validator and asset hash readback passed. The map-host instrumentation class still fails three assertions after one retry; emulator performance/ANR measurements and remote CI are unavailable. |

## Regression and Coverage Review

- The prior verification at `0f9125cd2714b6121480437b70b0d0fef44c3f82` found missing tri-state display, district-wide fallback scope, early removal of active overlays and missing source cleanup. This revision addresses the tri-state mapping, per-district query scope and resource-close paths. AC-006 still has a candidate-overlay overlap window, and AC-007 still has the fallback executor wait cycle described above.
- No committed deterministic tests cover out-of-order generations, failing-source fallback, or the planned 20-round manager lifecycle bound. The host class's immediate provider assertions need to wait for async source readiness before they can provide rendering evidence.
- No physical-device testing was performed; this task's validation boundary calls for emulator testing only.

## Issues and Next Action

- ISS-005 — AC-006 candidate overlays attach before candidate readiness; open, P1.
- ISS-008 — AC-007 fallback readiness can block the single-thread source executor; open, P1.
- ISS-004, ISS-006 (the prior query-scope defect), and ISS-007 are verified as fixed in this revision.
- Required next action: `debug`. After the approved fixes, add deterministic atomic-generation and fallback-failure evidence, complete the planned lifecycle scenario, obtain CI results, and verify a new fixed commit.
