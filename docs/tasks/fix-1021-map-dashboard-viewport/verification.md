# Verification — fix-1021-map-dashboard-viewport

## Inputs and Revision

- Reviewed commit: `49e934bcb6cc9a14b4acf3a630ebaae19fd2b00a` (`codex/fix/map-dashboard-viewport`). The worktree was clean when verification started.
- The implementation and focused tests are from `f279e2b` and `80726c0`. The only changes from `80726c0` to the reviewed `HEAD` are task documentation (`execution-report.md` and `state.yaml`); the reviewed production and instrumentation-test files are byte-for-byte unchanged.
- Physical device: not used. The developer execution report records the approved test target as `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 / API 36, package `com.example.taoyuangutter`.
- This verification reviewed source, test code, task evidence, and CI availability. No test or build was run during Verification.

## Acceptance Criteria

| AC | Steps | Result | Actual Result | Evidence | Retry Count |
|---|---|---|---|---|---:|
| AC-001 | On the production map, set a known center and zoom, switch Map → Dashboard → Map, and inspect the camera. | **PASS** | The recorded emulator instrumentation case set center `(25.12345, 121.54321)` and zoom `13.5`, then asserted the same values after returning to Map. The production map and nested `SupportMapFragment` identities were also unchanged. | `execution-report.md` records `productionMapAndNestedMapFragmentsSurviveDashboardSwitches` as `OK (1 test)` and gives the exact emulator command. Assertions are in `MainShellActivityTest.kt:334-404`, especially `:397-399`. | 0 |
| AC-002 | Switch between tabs repeatedly; inspect fragment identity, visibility, and lifecycle. | **PASS** | The recorded emulator shell test confirms the original Map and Dashboard test-fragment instances are reused, the inactive tab is hidden and `STARTED`, and the selected tab is shown and `RESUMED`. The production-map case independently confirms that the real map fragment and its nested map fragment are retained. | `execution-report.md` records `shellSwitchesBetweenMapAndDashboardTabs` as `OK (1 test)`. Assertions are in `MainShellActivityTest.kt:287-326` and `:371-396`. Production switching is implemented in `MainShellActivity.kt:64-87`. | 0 |
| AC-003 | Inspect first-load initialization and explicit Locate behavior, then verify tab return does not recreate the map. | **PASS** | The task diff does not modify `MapWorkspaceFragment`; its first-load fallback and location request remain in `onMapReady()`, and the explicit Locate click handler remains in place. The production-map instrumentation test observes the same map and nested map fragment after switching back, so the retained map does not re-enter its initialization path on tab return. The Locate button was not separately tapped in the recorded test. | `git diff 6ad03bce..49e934b -- app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` is empty. Current source shows first-load behavior at `MapWorkspaceFragment.kt:555-576` and explicit Locate at `:682-692`. The runtime identity/camera evidence is in `MainShellActivityTest.kt:334-404` and the execution report. | 0 |

## Implementation and Test Review

- `MainShellActivity.showTab()` looks up tabs by stable tag, hides other fragments, caps inactive fragments at `STARTED`, adds a tab only once, and resumes the selected fragment (`MainShellActivity.kt:64-87`). This matches the approved show/hide plan and keeps tab visibility exclusive.
- The focused production-map test uses a real `MapWorkspaceFragment`, `SupportMapFragment`, and `GoogleMap`; it checks exact latitude, longitude, and zoom after navigation. The shell test separately covers repeated navigation, mutual visibility, identity, and lifecycle.
- The tests are proportionate to the regression. The production-map test directly exercises AC-001; the shell test covers AC-002. AC-003 is supported by the unchanged initialization and Locate source plus retained map identity. A direct tap of the Locate button and an activity-recreation scenario are not covered by these focused cases.
- Reviewed regression risks: both tab view hierarchies remain alive while the shell is open, as documented in the plan. Inactive fragments are capped at `STARTED`; the focused shell test checks the selected/inactive lifecycle states. Activity recreation is handled by FragmentManager restoration and the saved selected-tab ID in `MainShellActivity.kt:39-44`, but that path was not exercised in this task's focused tests.
- No production implementation deviation or implementation failure was found.

## CI

- **NOT VERIFIED.** The repository state records CI build/test as pending. The configured `.github/workflows/android-ci.yml` runs JVM tests on a hosted Ubuntu runner, which is outside this task's approved emulator-only test scope; Verification did not trigger it.
- A GitHub PR search for `codex/fix/map-dashboard-viewport` returned no PR, and the available commit workflow-run lookup returned no runs for reviewed commit `49e934b`. That lookup is limited to PR-triggered runs, so it cannot establish that no other kind of run exists. No CI PASS evidence is available here.

## Regression

- **Confirmed:** first-load map setup and explicit Locate source are unchanged; selected/inactive tab visibility and lifecycle are covered by the focused shell instrumentation case; the actual map camera center and zoom survive the tab round trip in the recorded emulator case.
- **Not exercised:** physical-device behavior (prohibited by the requirement), the explicit Locate interaction itself, activity recreation, and broader regression. No broader regression was required by the approved plan.

## Issues

- No implementation issue found. CI evidence is unavailable under the approved test scope; this is an environment / validation gate, not a failed acceptance criterion.

## Validation Limitations

- No tests were run by Verification. Developer-reported emulator results are recorded in `execution-report.md`; the repository does not contain a retained raw instrumentation result file for these two methods.
- CI has no verified result. This prevents Verification from advancing the task to Release under `AGENTS.md` even though the three acceptance criteria pass on the available code and test evidence.

## Failure Classification

`environment` — required CI status is not available without running a hosted JVM-test workflow that is outside the approved test scope.

## Next Action

`infrastructure` — obtain an approved CI validation path compatible with the emulator-only testing constraint, then update the CI evidence and rerun the verification gate on the resulting committed revision.

## Final Result

**NOT VERIFIED**
