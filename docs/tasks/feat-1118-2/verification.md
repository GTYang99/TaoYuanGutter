# Verification Report

## Round and Revision

- Round: verification of the corrected map-host instrumentation, followed by a valid Maps key rerun.
- Commit under test: `d21a49a1d7db0f104e85b2aa06b8c23a69810222` on `codex/feat/feat-1118-2`.
- Tests ran from the fixed `d21a49a1` source/test snapshot `/private/tmp/feat1118-2-verification-r4.d21a49a1`. Production and test source match the committed revision. The checkout's uncommitted `gradle/libs.versions.toml` change (`agp = 9.5.3`) was excluded and preserved. The initial snapshot placeholder was replaced with the valid key from ignored local configuration for this rerun; no key value was recorded.
- Emulator: `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`; stopped after testing.
- No production or test source was modified during Verification. Task artifacts record this round.

## Final Result

**FAIL — map-host instrumentation is unstable.** The valid Maps key resolved ISS-010, and the formerly blocked MainActivity case passed once. A complete four-case map-host run on the same source/test revision then failed one LEGACY snapshot color assertion: expected `#ff156d1d`, observed `#ffa3c2a1`. The single permitted retry of that failed case passed. The intermittent mismatch is classified as an implementation validation failure (ISS-011) and routes to Debug to distinguish overlay readiness from snapshot-test synchronization. AC-010 remains `NOT VERIFIED`; AC-011 fails. Remote CI and several plan-required deterministic scenarios remain unavailable.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused map/state JVM tests plus CI workflow filters | PASS | One combined `:app:testDebugUnitTest` invocation with map and `MainViewModelTest` filters plus the five `.github/workflows/android-ci.yml` filters; 70 tests, 0 failures/errors. Includes `OfflineSourceReadinessTest`. |
| Debug and instrumentation APK builds | PASS | With the valid key, `./gradlew --offline --no-daemon --rerun-tasks :app:assembleDebug :app:assembleDebugAndroidTest` passed; both manifests merged. |
| `OfflinePolygonDataStoreInstrumentedTest` | PASS | 7/7 passed on the current commit's emulator run. |
| `OfflinePolygonMapHostInstrumentedTest` | FAIL | With a valid key, the previously blocked MainActivity case passed 1/1. The complete class then ran 4 cases and failed 1 at the LEGACY snapshot pixel (`#ff156d1d` expected, `#ffa3c2a1` observed); three other cases passed. The one permitted retry of that failed case passed 1/1. This intermittent failure prevents a passing gate. |
| Current worktree manifest merge | PASS locally | `./gradlew --offline --no-daemon --rerun-tasks :app:processDebugMainManifest :app:processDebugAndroidTestManifest` passed with the existing uncommitted `agp = 9.5.3` change. The reported Android Studio error was not reproduced by this command. |
| GPKG host validator | PASS under accepted source-authority decision | `metadata=PASS`, `union_consistency=PASS`; `partition_consistency=FAIL` remains the accepted diagnostic recorded by `knowledge-resolution.md`. |
| APK asset readback | PASS | All 29 manifest GPKGs are present in the Debug APK, with matching size and SHA-256. |
| Latest code-diff whitespace check | PASS | `git diff --check 906c4b5b6edddaa053cd18c35b5a89eac95a47c3..d21a49a1d7db0f104e85b2aa06b8c23a69810222` reports no errors. |
| Remote CI | NOT VERIFIED | GitHub returned no combined statuses and no workflow runs for this commit; CI state remains pending. |

## Acceptance Criteria

| Criterion | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | Host metadata and geometry-union validation pass under the accepted source-authority decision. APK readback confirms all 29 files, sizes and hashes. |
| AC-002 | PASS | Source defaults all 13 districts and three layers on. Corrected emulator host cases pass initial provider visibility; the checkbox synchronization case passes. |
| AC-003 | PASS | `LayersBottomSheet` maps NONE/PARTIAL/ALL to `MaterialCheckBox.setCheckedState`; JVM state tests and emulator all/individual selection interactions pass. |
| AC-004 | PASS | `OfflinePolygonSourceTest` verifies full and per-district source mapping, and the full-source DataStore rendering test passes on emulator. No partial-source visual snapshot was completed. |
| AC-005 | NOT VERIFIED | Empty-selection checks pass, but no test demonstrates multi-district OR results and selected-district filtering across all three rendered layers. |
| AC-006 | NOT VERIFIED | Source review shows candidates remain unattached until source readiness and the latest-request check. The planned controlled out-of-order generation/atomic-overlay test is not present; the map-host screenshot scenario did not finish. |
| AC-007 | NOT VERIFIED | The single-thread readiness regression test and per-district mapping tests pass. No provider-level injected corrupt/missing source case verifies fallback isolation and continued successful sources end to end. |
| AC-008 | NOT VERIFIED | Provider release and DataStore release checks pass, but the approved 20-round manager lifecycle and provider/overlay/cache bounds were not exercised. |
| AC-009 | NOT VERIFIED | Preference serialization tests pass; no Android process/app restart and restore scenario ran. |
| AC-010 | NOT VERIFIED | Existing geometry and full-source tile checks pass, and one map-host snapshot passed. Another run captured the LEGACY interior without its expected fill. The intermittent result and remaining baseline regression checks prevent a reliable conclusion. |
| AC-011 | FAIL | 70 JVM tests and both APK builds pass, but a complete map-host instrumentation run failed 1/4 at the LEGACY snapshot color assertion. Its single retry passed. Emulator performance/ANR evidence and remote CI remain missing. |

## Regression and Coverage Review

- The corrected initial provider waits resolve ISS-009; three map-host cases now pass, and the remaining case proceeds past provider visibility and direct tile assertions.
- Emulator connectivity and Maps SDK authorization are now sufficient for the snapshot test. The intermittent `#ffa3c2a1` pixel may reflect a tile overlay that was not ready when the snapshot was captured; the current test waits for camera idle and map load but has no explicit overlay-render readiness condition. Debug must establish whether the test timing or app rendering is responsible.
- Deterministic out-of-order generation, provider-level source-failure injection, the 20-round lifecycle bound, app restart restoration, and emulator performance/ANR evidence are still missing.
- Physical-device testing is out of scope; validation uses an Android emulator.

## Issues and Next Action

- ISS-009 — initial provider-visibility timing race: closed; the corrected checks passed on this revision.
- ISS-010 — Maps SDK authorization blocker: closed after valid key configuration and a passing focused case.
- ISS-011 — intermittent LEGACY map snapshot color mismatch: open, implementation, P1.
- Required next action: `debug` to establish the cause of the intermittent snapshot mismatch before changing code or tests, then reimplement, validate, commit, and rerun Verification. Complete deterministic AC-005–AC-009, baseline map regression and performance evidence, and remote CI before Release.
