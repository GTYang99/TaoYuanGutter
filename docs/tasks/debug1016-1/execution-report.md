# Execution Report

## Revision and Scope

- Task: `debug1016-1` (`bugfix`)
- Branch: `codex/debug/debug1016-1`
- Worktree: `/Users/a10362/.codex/worktrees/debug-1016-1/TaoYuanGutter`
- Scope: Center the mini-map of a fresh, online add form on the user's current location when the waypoint has no saved coordinates. Keep location camera-only.

## Implementation

- `AddGutterBottomSheet` now marks only fresh online add sheets as eligible; edit, offline, restored, inspect, and submitted read-only sheets do not set the flag.
- Both host launch paths pass the flag through `GutterFormNavigator` into `GutterFormActivity`.
- The initial-location policy accepts the explicit new/add flag while retaining saved-coordinate, view, offline, and submitted-read-only guards.
- The existing permission, fused-location, retry, manual-pan, unavailable-prompt, and fallback flow remains in use. A valid fresh-add fix uses `moveCamera()` because emulator evidence showed `animateCamera()` left that form map at the fallback even when the callback was valid and the apply guard allowed it. Normal edit-mode animation remains unchanged.
- Instrumentation asserts that current coordinates, `NODE_X`, `NODE_Y`, and the session waypoint coordinates remain empty.

## Validation Environment

- Emulator: `emulator-5554` (`sdk_gphone16k_arm64`, Android 16)
- App package: `com.example.taoyuangutter`
- Instrumentation package: `com.example.taoyuangutter.test`
- The connected physical device was not targeted.
- The debug build required the repository's Maps manifest placeholder. A temporary ignored `local.properties` containing only `MAPS_API_KEY=debug-placeholder-not-a-real-key` was used and removed after the build.
- The test receiver initially lacked the emulator `MOCK_LOCATION` app-op. The positive test uses the emulator-native GPS injection command instead. The test package app-op and secure mock-location setting were restored to their original denied/disabled values after testing.

## Results

| Acceptance criterion | Result | Evidence |
|---|---|---|
| AC-001: Fresh add form centers on usable current location | PASS | `freshNewPointWithEmulatorGpsFixCentersCameraOnly` passed after `adb -s emulator-5554 emu geo fix 121.5 25.03`. |
| AC-002: Location remains camera-only | PASS | The same test asserted `currentLat/currentLng == 0`, blank `NODE_X/NODE_Y`, and null session waypoint latitude/longitude. |
| AC-003: Saved waypoint coordinates retain precedence | PASS | `savedCoordinateEditKeepsSavedCameraTarget` passed on emulator. |
| AC-004: Offline/restored drafts remain excluded | PASS by source review; no dedicated runtime case | The bottom-sheet predicate excludes offline mode and positive restored `draftId`; the policy still rejects offline and read-only forms. The unmarked-form emulator regression passed. |
| AC-005: Unavailable location retains prompt and fallback | NOT VERIFIED at runtime | The failure flow and fallback branch were not changed. A no-fix case was not rerun because the emulator-native GPS fix remained cached. |

### Build

Command:

```text
PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH" ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

Result: PASS (`BUILD SUCCESSFUL`). The built APKs were `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

### Emulator Instrumentation

The app and instrumentation APKs were installed with `adb -s emulator-5554 install -r ...`. Each test was invoked directly on that serial; no Gradle connected-device task was used.

```text
adb -s emulator-5554 emu geo fix 121.5 25.03
adb -s emulator-5554 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#freshNewPointWithEmulatorGpsFixCentersCameraOnly' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner
```

Result: PASS (`OK (1 test)`).

```text
adb -s emulator-5554 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#unmarkedNewPointKeepsTaoyuanFallback' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner
```

Result: PASS (`OK (1 test)`).

```text
adb -s emulator-5554 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#savedCoordinateEditKeepsSavedCameraTarget' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner
```

Result: PASS (`OK (1 test)`).

## Limitations

- The JVM policy test source was updated but not run because the requested validation scope was emulator-only.
- The no-fix prompt case and offline/restored runtime cases were not run.
- Remote CI and independent Verification have not run; these remain downstream gates.
- Emulator-native GPS was set to the test coordinate. Mock-location app-op and secure setting were restored; no physical device was changed.

## Diff Hygiene

- `git diff --check`: PASS
- Temporary diagnostic logging was removed from production code.
- Temporary Maps placeholder file was removed.
