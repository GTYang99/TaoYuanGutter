# Verification Report

## Revision Under Review

- Implementation commit: `990e60888604da1a5917b72a542effa6649f60f1`
- Source audit HEAD: `6a179ccb4e2a1f9548840bf3ed7de724a9988b0b`
- The descendants after the implementation commit changed task documentation only; `git diff 990e608..HEAD -- app/src` was empty.
- Branch: `codex/debug/debug1016-1`
- Worktree was clean before the emulator audit. The connected physical device was not targeted.

## Requirement and Implementation Review

- Requirement and plan reviewed: `requirement.md`, `analysis.md`, and `plan.md`.
- The approved scope is a fresh, online, writable add form with no saved coordinates. The acquired fix must move only the initial map camera; saved coordinates retain precedence; offline and restored drafts remain excluded.
- Source review found the fresh-add predicate in `AddGutterBottomSheet.shouldUseCurrentLocationForInitialFormViewport()`, passed through both host launch paths and `GutterFormNavigator`, then consumed by `GutterFormActivity` and `EditMapLocationPolicy`.
- `GutterFormActivity.onMapReady()` keeps the saved-coordinate branch first. For an eligible empty add, it reuses the existing permission/fused-location flow. The success callback uses `moveCamera()` only for the explicit add flag; the standard edit path retains `animateCamera()`.
- The location callback does not write `currentLat/currentLng`, `NODE_X/NODE_Y`, or waypoint/session coordinates.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001: A fresh online empty add form centers on a usable current location | PASS | `freshNewPointWithEmulatorGpsFixCentersCameraOnly` passed on `emulator-5554` with the test location `(25.03, 121.5)`. The post-commit rerun passed using the test APK's mock provider after the emulator-native fix was no longer available. |
| AC-002: Camera centering does not write form or waypoint coordinates | PASS | The same instrumentation case asserted zero current coordinates, blank `NODE_X`/`NODE_Y`, and null waypoint latitude/longitude. |
| AC-003: Saved coordinates remain the target | PASS | `savedCoordinateEditKeepsSavedCameraTarget` passed on `emulator-5554`; source retains the saved-coordinate branch before initial location acquisition. |
| AC-004: Offline and restored drafts do not start the new-add initial-location flow | PASS by source review; runtime not run | The launch predicate excludes offline mode and positive restored `draftId`; `EditMapLocationPolicy` separately rejects offline and read-only forms. `unmarkedNewPointKeepsTaoyuanFallback` passed. No dedicated offline/restored UI case exists in this focused test run. |
| AC-005: Without a usable location, the existing prompt and Taoyuan fallback remain usable | NOT VERIFIED | With emulator location disabled, the no-fix instrumentation reached the unavailable prompt, fallback, and coordinate assertions, then Espresso failed clicking “Continue” while animations were enabled. The single retry with animations disabled did not find the expected unavailable title. No further retry was made. |

## Regression Review

- Fresh add camera centering: PASS on emulator.
- Coordinate non-persistence: PASS in the same instrumentation case.
- Unmarked empty form fallback: PASS on emulator.
- Saved-coordinate precedence: PASS on emulator.
- Offline/restored draft behavior: source guards reviewed; runtime not run.
- Manual-pan behavior: existing policy/source guard is unchanged; not rerun in this focused validation.
- No-fix prompt dismissal: NOT VERIFIED as described for AC-005.

## CI and Independent Review

- Local debug and instrumentation APK build: PASS, recorded in `execution-report.md`.
- GitHub workflow runs and combined statuses for the implementation commit: none were returned (`workflow_runs: []`, `statuses: []`). The local branch has no upstream; the shell could not resolve `github.com` when checking the remote.
- No remote CI result is available. The configured GitHub workflow also runs JVM unit tests, which were outside the user's emulator-only test scope.
- A separate independent verifier has not reviewed this revision.

## Result

**NOT VERIFIED.** AC-001 through AC-004 have passing runtime or source evidence. AC-005 remains incomplete, CI has no result, and independent Verification is outstanding. Do not advance to Release. Keep `next_action: verification` until the missing evidence is available.

## Environment Cleanup

After the emulator runs, location mode was restored to `3`, secure mock-location setting to `0`, test APK `android:mock_location` app-op to `deny`, and animation scales to their original values. No physical device was changed.
