# Verification Report

## Round and Revision

- Round: independent verification after the ISS-011 snapshot-readiness fix.
- Commit under test: `94f5ba0a0aee62f2a803b63f0bae5b408d3c4c89` on `codex/feat/feat-1118-2`.
- Tests ran from `git archive 94f5ba0a` at `/private/tmp/feat1118-2-verification-r5.94f5ba0a`. Source and test files match the committed revision. The checkout's unrelated uncommitted `gradle/libs.versions.toml` change (`agp = 9.5.3`) was excluded and preserved. An ignored local Maps key was copied into the snapshot without printing or recording its value.
- Emulator: `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`; stopped after testing.
- No production or test source was modified during Verification. Task artifacts record this round.

## Final Result

**NOT VERIFIED — ISS-011 passed, but required coverage and CI remain incomplete.** The formerly intermittent MainActivity snapshot case passed 1/1, and the complete map-host class passed 4/4 on the emulator. The fix polls snapshots for at most 30 seconds while preserving the exact interior-color check; no production code changed in this commit. ISS-011 can close. AC-005–AC-011 remain `NOT VERIFIED` for the separate coverage gaps listed below, including remote CI and performance evidence; Release cannot proceed.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused map/state JVM tests plus CI workflow filters | PASS | On `94f5ba0a`, one `:app:testDebugUnitTest` invocation with map, `MainViewModelTest`, and the five workflow filters passed: 70 tests, 0 failures/errors/skips across 19 suites. |
| Debug and instrumentation APK builds | PASS | `./gradlew --offline --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest` passed on the isolated revision. |
| `OfflinePolygonDataStoreInstrumentedTest` | PASS carried forward | 7/7 passed on prior source revision `d21a49a1`; the new commit changes only map-host instrumentation source and task artifacts, so DataStore app/test code is unchanged. |
| `OfflinePolygonMapHostInstrumentedTest` | PASS | On Android 16 `Medium_Phone(AVD)`, focused MainActivity case passed 1/1 (`23.377s`) and the complete class passed 4/4 (`57.511s`) using the valid Maps key. |
| GPKG host validator | PASS carried forward | Prior independent host validation returned `metadata=PASS`, `union_consistency=PASS`; `partition_consistency=FAIL` remains the accepted diagnostic. The new commit changes no GPKG or validator input. |
| APK asset readback | PASS carried forward | Prior independent readback found all 29 GPKGs with matching size and SHA-256; the new commit changes no assets or packaging configuration. |
| Latest code-diff whitespace check | PASS | `git diff --check c1ffd327..94f5ba0a` reports no errors; the only changed executable file is the map-host instrumentation test. |
| Remote CI | NOT VERIFIED | GitHub returned zero combined statuses and zero workflow runs for `94f5ba0a`; no remote branch contains this local commit. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | Host metadata and geometry-union validation pass under the accepted source-authority decision. APK readback confirms all 29 files, sizes and hashes. |
| AC-002 | PASS | Source defaults all 13 districts and three layers on. Corrected emulator host cases pass initial provider visibility; the checkbox synchronization case passes. |
| AC-003 | PASS | `LayersBottomSheet` maps NONE/PARTIAL/ALL to `MaterialCheckBox.setCheckedState`; JVM state tests and emulator all/individual selection interactions pass. |
| AC-004 | PASS | `OfflinePolygonSourceTest` verifies full and per-district source mapping, and the full-source DataStore rendering test passes on emulator. No partial-source visual snapshot was completed. |
| AC-005 | NOT VERIFIED | Empty-selection checks pass, but no test demonstrates multi-district OR results and selected-district filtering across all three rendered layers. |
| AC-006 | NOT VERIFIED | Map-host rendering now completes, and source review shows candidates remain unattached until readiness/latest-request checks. The planned controlled out-of-order generation/atomic-overlay test is still absent. |
| AC-007 | NOT VERIFIED | The single-thread readiness regression test and per-district mapping tests pass. No provider-level injected corrupt/missing source case verifies fallback isolation and continued successful sources end to end. |
| AC-008 | NOT VERIFIED | Provider release and DataStore release checks pass, but the approved 20-round manager lifecycle and provider/overlay/cache bounds were not exercised. |
| AC-009 | NOT VERIFIED | Preference serialization tests pass; no Android process/app restart and restore scenario ran. |
| AC-010 | NOT VERIFIED | Geometry and full-source tile checks pass, and the corrected map-host snapshot class passes 4/4. The approved baseline checks for basemap, other overlays, holes, MultiPolygon and z-index are not all evidenced. |
| AC-011 | NOT VERIFIED | 70 JVM tests, both APK builds and 4/4 map-host cases pass. The approved emulator performance/ANR scenario and remote CI result are still missing. |

## Regression and Coverage Review

- The new helper retains provider and camera/map readiness checks, then retries `GoogleMap.snapshot()` within a 30-second bound until the same expected opaque interior color is observed. A non-matching final color still fails. The focused and complete host cases pass on the fixed revision, resolving the observed ISS-011 timing failure.
- Deterministic out-of-order generation, provider-level source-failure injection, the 20-round lifecycle bound, app restart restoration, and emulator performance/ANR evidence are still missing.
- Physical-device testing is out of scope; validation uses an Android emulator.

## Issues and Next Action

- ISS-009 — initial provider-visibility timing race: closed; the corrected checks passed on this revision.
- ISS-010 — Maps SDK authorization blocker: closed after valid key configuration and a passing focused case.
- ISS-011 — intermittent LEGACY map snapshot color mismatch: closed after the focused 1/1 and full class 4/4 passed on `94f5ba0a`.
- Required next action: `verification` to gather the still-missing AC-005–AC-011 deterministic, regression, restart and performance evidence plus remote CI. Release remains blocked while any criterion is `NOT VERIFIED`.
