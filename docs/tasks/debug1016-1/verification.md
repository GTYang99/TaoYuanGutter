# Verification Report

## Revision Under Review

- Implementation commit: `990e60888604da1a5917b72a542effa6649f60f1`
- Source audit HEAD: `4780ae6064e6ef5b4d51aa1cabbf3fccd1a7db87`
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
| AC-005: Without a usable location, the existing prompt and Taoyuan fallback remain usable | NOT VERIFIED | After rebooting `emulator-5554` to clear a stale injected pointer, the rerun displayed the prompt, retained the Taoyuan fallback, passed coordinate non-persistence assertions, and successfully dismissed the prompt. The final manual-pan assertion failed because Google Maps SDK rejected the configured non-real placeholder API key (`INVALID_ARGUMENT` / authorization failure), so map interaction remains unverified. Earlier Android 14 permission-denial evidence does not cover this no-fix interaction. |

## Regression Review

- Fresh add camera centering: PASS on emulator.
- Coordinate non-persistence: PASS in the same instrumentation case.
- Unmarked empty form fallback: PASS on emulator.
- Saved-coordinate precedence: PASS on emulator.
- Offline/restored draft behavior: source guards reviewed; runtime not run.
- Manual-pan behavior: the focused no-fix rerun reached the map-pan assertion, but the emulator Maps SDK rejected the placeholder API key; interaction is NOT VERIFIED.
- No-fix prompt dismissal: PASS on the post-reboot emulator rerun; the test then failed at the manual-pan assertion.
- Permission-denial fallback case: PASS on `emulator-5556` (Android 14). The same case could not complete on Android 16 because its helper revoked runtime permissions while instrumentation was active; logcat showed `ActivityManager` killing `com.example.taoyuangutter` with reason `permissions revoked`, with no application exception stack trace.

## CI and Independent Review

- Local debug and instrumentation APK build: PASS, recorded in `execution-report.md`.
- Focused policy unit test: PASS in this verification run; exact command is recorded below.
- GitHub workflow runs and combined statuses for the implementation commit: none were returned (`workflow_runs: []`, `statuses: []`). The local branch has no upstream; the shell could not resolve `github.com` when checking the remote.
- No remote CI result is available. This bounded pass ran only the single targeted policy unit test requested by the user; the broader CI suite remains unverified.
- Independent Verification was performed in this pass. Its final result is `NOT VERIFIED` because AC-005 remains incomplete.

## Environment Classification

- Emulator: `emulator-5554`, Android 16 (`sdk_gphone16k_arm64`).
- For the no-fix rerun, emulator location mode and animation scales were disabled. The first attempt hit an emulator `InputDispatcher` stale-pointer error (`Invalid DOWN event - pointers already down`). Rebooting cleared this condition; Espresso then clicked Continue and dismissed the prompt successfully.
- The rerun reached the manual-pan assertion, but logcat reported `Google Maps Android API: Error requesting API token. StatusCode=INVALID_ARGUMENT` and `Google Android Maps SDK: Authorization failure` for `debug-placeholder-not-a-real-key`. This deliberately non-real placeholder is not a valid credential; without an authorized test Maps SDK key, emulator map gestures cannot provide AC-005 evidence.
- The permission-denial test's helper revokes runtime permissions while instrumentation is active. Android 16 then kills the app process; logcat records the platform kill rather than an application exception.
- Running the permission-denial case on the available Android 14 AVD with the app's permissions already denied completed successfully.
- Fine and coarse location permissions were granted by the instrumentation helper and were granted after the run; their pre-run values were not captured. Location mode, mock-location settings, mock-location app-op, and animation values were restored to the recorded pre-run values.

## Result

**NOT VERIFIED.** AC-001 through AC-004 have passing runtime or source evidence. AC-005 prompt dismissal passed after reboot, but map-pan usability remains unverified because the Maps SDK rejected the non-real test key. No remote CI result is available. Do not advance to Release; Infrastructure remains blocked pending an authorized test-only Maps SDK key and separate CI evidence.

## Environment Cleanup

After this run, emulator location mode was restored to `3`, secure mock-location setting to `0`, test APK `android:mock_location` app-op to `deny`, transition/window animation scales to `1.0`, and the previously unset animator scale was deleted. Fine/coarse runtime location permissions are currently granted by the instrumentation helper; pre-run values were not recorded. No physical device was changed.

## Bounded Verification Run — 2026-10-16

- Started at 16:57 and completed all checks and emulator cleanup by 17:02 (Asia/Taipei), within the requested 10-minute limit.
- Fixed implementation revision: `990e60888604da1a5917b72a542effa6649f60f1`; task worktree HEAD was `4780ae6064e6ef5b4d51aa1cabbf3fccd1a7db87`. No app source differed from the implementation commit.
- Environment: emulator `emulator-5554`, model `sdk_gphone16k_arm64`, Android 16; package `com.example.taoyuangutter`; instrumentation package `com.example.taoyuangutter.test`. No physical device was listed or targeted.
- Build: `PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH" ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` — PASS. A temporary ignored `local.properties` supplied only `MAPS_API_KEY=debug-placeholder-not-a-real-key` and was removed at command exit.
- Unit test: `PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH" ./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest.initialCameraLocationAllowsEditsAndExplicitFreshAddsOnly' --console=plain` — PASS.
- AC-005 emulator case: initial attempt failed dispatching Continue at line 119 due to a stale emulator pointer. After reboot, the same focused case passed Continue/dismissal and fallback assertions, then failed the map-pan assertion at line 128. Logcat identified Maps SDK authorization failure for the non-real placeholder key; AC-005 remains NOT VERIFIED.

## Infrastructure Follow-up — 2026-10-16

- Originating phase/check: Verification, AC-005 no-fix prompt dismissal and manual-pan usability on `emulator-5554`.
- Remediation: rebooted the test emulator to clear the stale InputDispatcher pointer state, reapplied no-location/zero-animation test conditions, and reran only the blocked AC-005 test. Prompt dismissal now passes.
- Rerun command: `adb -s emulator-5554 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — FAIL at the manual-pan assertion (`Map did not respond to a manual swipe`, test line 128).
- Cause: logcat states the Maps API token request returned `INVALID_ARGUMENT` and the Google Maps Android SDK authorization failed for the non-real `debug-placeholder-not-a-real-key`. No valid test credential was available; none was recorded or substituted.
- Immediately after the rerun, emulator settings were confirmed restored: location mode `3`, transition/window animations `1.0`, animator scale unset, mock-location app-op denied. A later attempt to re-query through ADB could not start the daemon because the sandbox rejected smartsocket binding (`Operation not permitted`); this did not change the already-confirmed settings.
- No app source was modified. The fixed app revision remains `990e60888604da1a5917b72a542effa6649f60f1`; the later task HEAD contains documentation only. CI remains without a remote run/status.
- Result: AC-005 remains NOT VERIFIED and the task stays blocked in Infrastructure until an authorized test-only Maps SDK key is available; then rerun this same focused case and return to Verification.
