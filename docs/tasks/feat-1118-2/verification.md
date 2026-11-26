# Verification Report

## Round and Revision

- Round: verification of the corrected map-host instrumentation.
- Commit under test: `d21a49a1d7db0f104e85b2aa06b8c23a69810222` on `codex/feat/feat-1118-2`.
- Tests ran from immutable `git archive HEAD` snapshot `/private/tmp/feat1118-2-verification-r4.d21a49a1`. Production and test source match the committed revision. The checkout's uncommitted `gradle/libs.versions.toml` change (`agp = 9.5.3`) was excluded and preserved. A temporary `MAPS_API_KEY=ci-placeholder` was used only in the snapshot.
- Emulator: `emulator-5554`, `Medium_Phone(AVD)`, Android 16, package `com.example.taoyuangutter`; stopped after testing.
- No production or test source was modified during Verification. Task artifacts record this round.

## Final Result

**NOT VERIFIED — required Maps environment is unavailable.** The initial provider timing issue is resolved, and the corrected map-host cases pass their asynchronous visibility checks. The first MainActivity screenshot attempt ran while the AVD had no active network; Wi-Fi was enabled and the blocked case was rerun on the same committed revision. The rerun still timed out waiting for the camera-idle callback, and its scoped log reports `StatusCode=INVALID_ARGUMENT` and Google Maps SDK authorization failure for the temporary `ci-placeholder` key. The workspace has no valid local Maps key, so the screenshot/color assertion was not reached. This is an environment limitation, not evidence of an implementation defect. Remote CI and several plan-required deterministic scenarios also remain unavailable.

## Checks and Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused map/state JVM tests plus CI workflow filters | PASS | One combined `:app:testDebugUnitTest` invocation with map and `MainViewModelTest` filters plus the five `.github/workflows/android-ci.yml` filters; 70 tests, 0 failures/errors. Includes `OfflineSourceReadinessTest`. |
| Debug and instrumentation APK builds | PASS | `:app:assembleDebug :app:assembleDebugAndroidTest`; `BUILD SUCCESSFUL`. |
| `OfflinePolygonDataStoreInstrumentedTest` | PASS | 7/7 passed on the current commit's emulator run. |
| `OfflinePolygonMapHostInstrumentedTest` | PARTIAL / NOT VERIFIED | Initial run: 3/4 cases passed; the remaining case passed provider visibility and tile-provider checks, then failed at `cameraIdle.await(30s)`. Infrastructure rerun after enabling AVD Wi-Fi: the same case again failed at that callback (JUnit time 86.401s); log reports `StatusCode=INVALID_ARGUMENT` and Google Maps SDK authorization failure for `ci-placeholder`. The workspace has no valid `local.properties` Maps key. Screenshot/color assertion was not reached. |
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
| AC-010 | NOT VERIFIED | Existing geometry unit checks and full-source offline tile rendering pass. The map-host snapshot/color check stopped before the snapshot because camera-idle was not delivered; baseline map/overlay visual regression is therefore unresolved. |
| AC-011 | NOT VERIFIED | 70 JVM tests and both APK builds pass. Emulator map-host instrumentation is incomplete after one camera-idle timeout and retry; emulator performance/ANR measurements and remote CI are also missing. |

## Regression and Coverage Review

- The corrected initial provider waits resolve ISS-009; three map-host cases now pass, and the remaining case proceeds past provider visibility and direct tile assertions.
- The infrastructure rerun confirms emulator network connectivity is restored, but Google Maps SDK authorization still fails because the isolated build uses the CI placeholder key. This does not establish whether the overlay would render correctly; keep the affected map results `NOT VERIFIED` pending a valid Maps SDK key or authenticated emulator configuration.
- Deterministic out-of-order generation, provider-level source-failure injection, the 20-round lifecycle bound, app restart restoration, and emulator performance/ANR evidence are still missing.
- Physical-device testing is out of scope; validation uses an Android emulator.

## Issues and Next Action

- ISS-009 — initial provider-visibility timing race: closed; the corrected checks passed on this revision.
- ISS-010 — emulator camera-idle evidence unavailable because the Maps SDK key is unauthorized: open, environment, P2. AVD Wi-Fi was enabled and connectivity confirmed; the focused rerun then logged `INVALID_ARGUMENT` / authorization failure for `ci-placeholder`.
- Required next action: `infrastructure` to make a valid Maps SDK key available through ignored local configuration or an authenticated test environment, rerun the blocked map-host case, and return to verification. Complete the remaining deterministic AC-005–AC-009 and performance evidence before Release.
