# Independent Verification — debug-1019-map-user-location

## Inputs and Revision

- Reviewed commit: `0b78cc43c37bc02d7e36420595311c4c9d8b6398` on `codex/fix/map-dashboard-viewport`. The worktree was clean when Verification started.
- Reviewed the approved requirement, plan, plan review, current production sources, focused instrumentation tests, execution report, issue log, state, `AGENTS.md`, `ai/verification-rules.md`, and `ai/testing-rules.md`.
- Developer-reported emulator targets: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 / API 36 for the recent blank-form, `(0,0)`, point-picker, and host-map cases; `DBG1017_API34_Camera`, Android 14 / API 34 for saved, restored, offline, and no-fix form cases. Package: `com.example.taoyuangutter`.
- No physical device was used. Verification did not run tests or builds.

## Acceptance Criteria

| AC | Result | Evidence and evaluation |
|---|---|---|
| AC-001 — A valid saved coordinate remains the form-map center without starting initial location acquisition. | **PASS** | The execution report records `savedCoordinateEditKeepsSavedCameraTarget` passing on the current API 36 emulator and earlier saved-coordinate coverage on API 34. The test asserts the saved camera target and that `editMapLocationFlowActive` is false (`EditMapInitialLocationInstrumentedTest.kt:136-149`). Source chooses a usable saved target first and starts location only in the no-target branch (`GutterFormActivity.kt:1675-1700`). |
| AC-002 — Editable blank online, offline, and restored forms center on a usable fix without writing form/session coordinates. | **PASS** | The execution report records passing ordinary blank, offline blank, restored blank, and fresh-add cases on the approved emulator(s). The tests assert the injected camera target and unchanged `NODE_X`, `NODE_Y`, `currentLat/currentLng`, and waypoint coordinates (`EditMapInitialLocationInstrumentedTest.kt:38-65`, `:276-359`, `:362-395`). The fused result updates the map camera only (`GutterFormActivity.kt:1743-1764`). |
| AC-003 — Host-map and point-picker Locate prefer a newer usable fix even below the 10 m accuracy threshold; stale/invalid results do not replace a usable cached fallback. | **PASS** | The execution report records `MainMapLocateInstrumentedTest#hostMapLocateButtonMovesCameraToFreshFix` and `MapPointPickerLocationInstrumentedTest#explicitLocateMovesCameraToFreshFixWithoutFinishingPicker` passing on API 36 with an injected fix. Focused policy tests cover the sub-10 m preference and freshness rules (`LocationFixQualityPolicyTest.kt:15-51`). Current controller falls back to an accepted cache when a preferred fresh refinement is rejected or fails (`MyLocationController.kt:108-129`); coordinate and age validation are at `LocationFixQualityPolicy.kt:10-22`. |
| AC-004 — With no usable fix, blank-map fallback and location-unavailable behavior remain; unusable coordinates cannot replace them. | **PASS** | The API 34 no-fix instrumentation case records the existing Taoyuan fallback and unavailable prompt. The API 36 injected `(0,0)` case records a usable final camera target and unchanged form/session coordinates (`EditMapInitialLocationInstrumentedTest.kt:96-132`, `:424-457`). Current point-picker source calls the existing unavailable Toast callback when no accepted fix is available (`MapPointPickerActivity.kt:189-209`; `MyLocationController.kt:112-129`). No separate point-picker no-fix emulator run is recorded. |
| AC-005 — A missing waypoint coordinate and unavailable host target do not move the host camera to `(0,0)`. | **PASS** | Both host handoff paths keep the target nullable and filter waypoint/map targets with `MapCoordinatePolicy` (`MainActivity.kt:1608-1624`; `MapWorkspaceFragment.kt:771-788`). `openAddForm()` moves the host camera only inside `latLng?.let`, so a missing target issues no camera move (`MainActivity.kt:1659-1694`; `MapWorkspaceFragment.kt:1236-1271`). |
| AC-006 — Point-picker Locate changes only the viewport; Confirm still returns the selected camera coordinate. | **PASS** | The API 36 instrumentation case records that Locate moved the camera to the injected fix while the picker remained open (`MapPointPickerLocationInstrumentedTest.kt:25-56`). Source assigns the camera target to the result only from Confirm (`MapPointPickerActivity.kt:145-158`); Locate invokes the location controller with an empty coordinate callback and does not finish (`:189-209`). |

## Implementation and Test Review

- `MapCoordinatePolicy.isUsableCoordinate()` rejects null, non-finite, out-of-range, and exact `(0,0)` pairs while leaving stored values untouched (`MapCoordinatePolicy.kt:5-11`). It is applied to saved form targets and provider results (`GutterFormActivity.kt:1675-1684`, `:1743-1752`), host handoff targets, point-picker inputs, and shared location fixes.
- `EditMapLocationPolicy` allows an initial camera request for writable, editable forms without saved coordinates, including offline/restored flows; view-only and submitted-read-only forms are excluded (`EditMapLocationPolicy.kt:5-20`).
- `MyLocationController` tries a fresh high-accuracy refinement for explicit locate flows. It uses a newer valid fix regardless of the prior 10 m threshold and falls back to a usable cached fix when refinement fails or is rejected (`LocationFixQualityPolicy.kt:24-52`, `MyLocationController.kt:108-129`).
- The focused emulator cases cover saved coordinate precedence, blank online/offline/restored camera-only behavior, provider and saved `(0,0)` rejection, both explicit Locate buttons, and the main-map dashboard round trip. The source test for point-picker confirms Locate does not finish the picker; Confirm's result path is confirmed by source review.
- The full Nearby import sheet was not exercised end to end. Its separate policy and `(0,0)` rejection have passing focused unit-test evidence in the execution report, and its source wiring passes review. This remains a validation limitation rather than an identified implementation failure.

## Regression and Behavior Boundary

- **Confirmed:** a valid saved point wins over automatic location; camera-only location leaves form/session coordinates unchanged; `(0,0)` is not accepted as a map target; absent host targets do not synthesize an origin camera move; the explicit Locate buttons use the shared fresh-preferred controller.
- **Expected fallback:** a blank form can still display the Taoyuan fallback if permission is denied or no usable current fix is available. This is explicitly required by FR-005. Verification therefore does not claim the city-hall fallback is impossible in every location/permission state.
- **Not exercised:** point-picker no-fix behavior was source-reviewed but not run on the emulator; full Nearby sheet end-to-end behavior and physical-device behavior remain unverified. Physical-device testing is outside scope.

## CI Gate

- **NOT VERIFIED.** The task state and execution report identify combined-branch CI as pending. GitHub lookup found no PR for this branch and no associated workflow run for reviewed commit `0b78cc43`; the available workflow-run lookup only reports PR-triggered runs, so it cannot rule out every other trigger. The prior successful run recorded for commit `7b1780f3` predates the dashboard-retention fix and the current combined revision, so it is not evidence for this HEAD.
- The checked-in Android CI workflow runs JVM tests on `ubuntu-latest`, outside the explicit emulator-only validation scope. Verification did not trigger that workflow. Its configured JVM-test list also does not include the location-policy tests for this task.

## Issues

- No acceptance criterion failed in source review or the committed emulator evidence.
- Environment/validation gate: current combined-branch CI has no PASS result.

## Validation Limitations

- Emulator outcomes are developer-reported in the committed execution report; Verification independently inspected the test assertions and current source but did not rerun them. Raw current-branch instrumentation output is not committed for every case.
- Nearby sheet end-to-end and point-picker no-fix behavior do not have dedicated current-branch emulator results.
- Current combined-branch CI is pending and must not be represented as PASS.

## Failure Classification

`environment` — current combined-branch CI evidence is unavailable under the approved emulator-only test scope. No implementation failure was found.

## Next Action

`infrastructure` — provide an approved CI path compatible with the emulator-only constraint and record a passing result for the current combined revision. Preserve the noted Nearby sheet and picker no-fix limitations in any release decision.

## Final Result

**NOT VERIFIED**
