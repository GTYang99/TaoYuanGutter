# Verification Report

## Round and Revision

- Round: remaining planned verification on commit `215e59fe51cba50bb458ad2ecfd60e749a028911` (`codex/feat/feat-1118-2`).
- Tests ran from immutable `git archive 215e59fe` at `/private/tmp/feat1118-2-verification-r6.215e59fe`; the ignored local Maps key was configured in that snapshot without printing or recording its value.
- Emulator: `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`; stopped after testing.
- The worktree's unrelated `gradle/libs.versions.toml` edit was excluded from this fixed revision and preserved.

## Final Result

**NOT VERIFIED — AC-010 still lacks one planned basemap and existing-overlay interaction check.** AC-001–AC-009 and AC-011 pass on the fixed revision with host validation, JVM and emulator evidence, performance observations, source review, and successful hosted CI. All 17 map data/host/restart instrumentation cases passed together. AC-010's geometry, fill, hole/MultiPolygon decoding, camera behavior, and offline layer interactions are covered, but this round did not exercise switching the basemap while other WMS/WMTS overlays remain active. Keep AC-010 `NOT VERIFIED` and do not advance to Release until that regression step is evidenced.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| JVM suite | PASS | On the isolated exact commit, `:app:testDebugUnitTest`: 187 tests across 58 suites, 0 failures/errors/skips. |
| Debug and instrumentation APK builds | PASS | `:app:assembleDebug :app:assembleDebugAndroidTest`; `BUILD SUCCESSFUL`. |
| Map data/host/restart instrumentation | PASS | Android 16 `Medium_Phone(AVD)`: `OfflinePolygonDataStoreInstrumentedTest`, `OfflinePolygonMapHostInstrumentedTest`, `OfflinePolygonRestartInstrumentedTest`; 17 tests, 0 failures (`101.707s`). |
| AC-008 lifecycle/performance | PASS | 20 fixed switch rounds passed; no crash/ANR; max observed switch `1453ms`, max PSS `331516KB`; overlay/provider counts and tile-cache bounds passed, and no newly acquired source remained after release. |
| GPKG host validator | PASS carried forward | Prior independent host validation returned `metadata=PASS`, `union_consistency=PASS`; `partition_consistency=FAIL` remains the accepted diagnostic. The new commit changes no GPKG or validator input. |
| APK asset readback | PASS carried forward | Prior independent readback found all 29 GPKGs with matching size and SHA-256; the new commit changes no assets or packaging configuration. |
| Latest code-diff whitespace check | PASS | `git diff --check c1ffd327..94f5ba0a` reports no errors; the only changed executable file is the map-host instrumentation test. |
| Remote CI | PASS | [GitHub Actions run #34](https://github.com/GTYang99/TaoYuanGutter/actions/runs/45784229614) for exact commit `215e59fe51cba50bb458ad2ecfd60e749a028911`: task-scoped JVM tests and Debug build both succeeded. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | Host metadata and geometry-union validation pass under the accepted source-authority decision. APK readback confirms all 29 files, sizes and hashes. |
| AC-002 | PASS | Source defaults all 13 districts and three layers on. Corrected emulator host cases pass initial provider visibility; the checkbox synchronization case passes. |
| AC-003 | PASS | `LayersBottomSheet` maps NONE/PARTIAL/ALL to `MaterialCheckBox.setCheckedState`; JVM state tests and emulator all/individual selection interactions pass. |
| AC-004 | PASS | `OfflinePolygonSourceTest` verifies full and per-district source mapping, and the full-source DataStore rendering test passes on emulator. No partial-source visual snapshot was completed. |
| AC-005 | PASS | New two-district instrumentation compares OR-result geometry signatures against each selected district for all three full sources, asserts empty results, and verifies district-asset isolation for both gutter layers. |
| AC-006 | PASS | Deferred loader instrumentation proves old overlays remain until readiness, generation 2 commits first, late generation 1 is discarded, and release counts match. |
| AC-007 | PASS | Injected failed 桃園區 source falls back to the full GPKG while 中壢區 continues independently; both return rendered tiles. |
| AC-008 | PASS | 20-round full/single/two/empty/full lifecycle passed with per-generation provider/overlay/cache/source bounds and zero newly acquired source refs after release. |
| AC-009 | PASS | Two-phase test saved 桃園區/中壢區 and disabled POSSIBLE; after `adb shell am force-stop com.example.taoyuangutter`, the fresh process restored districts and layer switches correctly. |
| AC-010 | NOT VERIFIED | Geometry unit cases cover holes and MultiPolygon decoding; packaged tile instrumentation checks existing colors, interiors, stroke widths and region boundary; host screenshots and camera interactions pass. The planned basemap switch while existing WMS/WMTS overlays remain active was not executed. |
| AC-011 | PASS | 187 JVM tests, both APK builds, 17 connected map/restart tests, source review of async GPKG loading, 20-round performance/no-ANR evidence, and hosted CI all passed. |

## Regression and Coverage Review

- The snapshot helper retains provider and camera/map readiness checks, then retries `GoogleMap.snapshot()` within a 30-second bound until the expected interior color is observed; a non-matching final color still fails. The complete map-host class passes on the fixed revision.
- Source review confirms GPKG staging/index construction uses `CompletableFuture.supplyAsync` on the dedicated `offline-polygon-index` executor. Tile queries run through the `TileProvider` path. The new repeated-switch case records latency, PSS, cache/resource bounds, and absence of crash/ANR.
- ISS-011, the intermittent snapshot readiness issue, is closed on this revision.
- Physical-device testing is out of scope; validation uses an Android emulator.

## Issues and Next Action

- ISS-009 — initial provider-visibility timing race: closed; the corrected checks passed on this revision.
- ISS-010 — Maps SDK authorization blocker: closed after valid key configuration and a passing focused case.
- ISS-011 — intermittent LEGACY map snapshot color mismatch: closed after the focused 1/1 and full class 4/4 passed on `94f5ba0a`.
- Required next action: `verification` to execute the single missing AC-010 check: switch basemap with existing WMS/WMTS overlays active and confirm they remain visible and ordered. Release remains blocked until AC-010 has evidence.
