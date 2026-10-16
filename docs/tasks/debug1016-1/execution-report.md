# Execution Report

## Revision and Scope

- Task: `debug1016-1` (`bugfix`)
- Branch: `codex/debug/debug1016-1`
- Implementation commit: `990e60888604da1a5917b72a542effa6649f60f1`
- Source revision used for the post-commit emulator audit: `350f30e369d36f9f9ce81bc80dccd8c5710e31c0` (documentation-only commit after the implementation commit)
- Worktree: `/Users/a10362/.codex/worktrees/debug-1016-1/TaoYuanGutter`
- Scope: Center the mini-map of a fresh, online add form on the user's current location when the waypoint has no saved coordinates. Keep location camera-only.

## Implementation

- `AddGutterBottomSheet` now marks only fresh online add sheets as eligible; edit, offline, restored, inspect, and submitted read-only sheets do not set the flag.
- Both host launch paths pass the flag through `GutterFormNavigator` into `GutterFormActivity`.
- The initial-location policy accepts the explicit new/add flag while retaining saved-coordinate, view, offline, and submitted-read-only guards.
- The existing permission, fused-location, retry, manual-pan, unavailable-prompt, and fallback flow remains in use. A valid fresh-add fix uses `moveCamera()` because emulator evidence showed `animateCamera()` left that form map at the fallback even when the callback was valid and the apply guard allowed it. Normal edit-mode animation remains unchanged.
- Instrumentation asserts that current coordinates, `NODE_X`, `NODE_Y`, and the session waypoint coordinates remain empty.

## Validation Environment

- Emulators: `emulator-5554` (`sdk_gphone16k_arm64`, Android 16) and temporary `emulator-5556` (`sdk_gphone64_arm64`, Android 14)
- App package: `com.example.taoyuangutter`
- Instrumentation package: `com.example.taoyuangutter.test`
- The connected physical device was not targeted.
- The debug build required the repository's Maps manifest placeholder. A temporary ignored `local.properties` containing only `MAPS_API_KEY=debug-placeholder-not-a-real-key` was used and removed after the build.
- The test receiver initially lacked the emulator `MOCK_LOCATION` app-op. The original positive run used emulator-native GPS injection. During the post-commit audit, toggling the emulator location mode cleared its native last fix; the native injection command then returned `OK` but `dumpsys location` showed no last fix. The positive case passed on its one retry using the test APK's mock provider. Mock-location app-op, secure setting, location mode, and animation settings were restored afterward.

## Results

| Acceptance criterion | Result | Evidence |
|---|---|---|
| AC-001: Fresh add form centers on usable current location | PASS | `freshNewPointWithEmulatorGpsFixCentersCameraOnly` passed after `adb -s emulator-5554 emu geo fix 121.5 25.03`. |
| AC-002: Location remains camera-only | PASS | The same test asserted `currentLat/currentLng == 0`, blank `NODE_X/NODE_Y`, and null session waypoint latitude/longitude. |
| AC-003: Saved waypoint coordinates retain precedence | PASS | `savedCoordinateEditKeepsSavedCameraTarget` passed on emulator. |
| AC-004: Offline/restored drafts remain excluded | PASS by source review; no dedicated runtime case | The bottom-sheet predicate excludes offline mode and positive restored `draftId`; the policy still rejects offline and read-only forms. The unmarked-form emulator regression passed. |
| AC-005: Unavailable location retains prompt and fallback | NOT VERIFIED | The no-fix case reached the unavailable prompt, fallback, and coordinate assertions on its first run, but Espresso could not click “Continue” while animations were enabled. Its single retry with animations disabled did not find the expected unavailable title. The separate permission-denial fallback case passed on Android 14, but the no-fix continue interaction remains unverified. |

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

### Post-commit Emulator Audit

The three focused positive/regression cases were rerun against the committed source on `emulator-5554`:

- `freshNewPointWithEmulatorGpsFixCentersCameraOnly`: PASS (`OK (1 test)`) after enabling the test APK mock provider with `lat=25.03`, `lng=121.5`; this retry followed one failed attempt after the emulator location mode had been toggled and its native last fix was absent.
- `unmarkedNewPointKeepsTaoyuanFallback`: PASS (`OK (1 test)`).
- `savedCoordinateEditKeepsSavedCameraTarget`: PASS (`OK (1 test)`).

The no-fix case `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` was run with emulator location disabled. It displayed the unavailable prompt and retained the fallback, and its coordinate assertions passed; Espresso failed to click the continue button with animations enabled. The one permitted retry with animations disabled failed to locate the expected prompt title. AC-005 therefore remains `NOT VERIFIED`; no further retry was made.

A separate attempt to run `returningFromSettingsWithoutPermissionKeepsFallbackUsable` also did not complete: Android 16 killed the app process when the test helper revoked runtime location permissions. Bounded logcat recorded `ActivityManager` killing the target with reason `permissions revoked`, with no application exception stack trace. The test was not retried.

The same permission-denial case then passed on the existing Android 14 AVD, `emulator-5556`, whose baseline location permissions were already denied. Command:

```text
adb -s emulator-5556 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#returningFromSettingsWithoutPermissionKeepsFallbackUsable' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner
```

Result: PASS (`OK (1 test)`). The AVD was shut down after the focused case.

The repository's GitHub Actions workflow runs a JVM unit-test selection and a debug build. No workflow runs or commit statuses exist for this local commit. The GitHub remote was unreachable from the shell during this task, so CI remains `NOT VERIFIED`.

## Limitations

- The JVM policy test source was updated but not run because the requested validation scope was emulator-only.
- Offline/restored runtime cases were not run; their exclusions were reviewed in source and policy assertions.
- Remote CI and independent Verification have not run; these remain downstream gates.
- Emulator `emulator-5554` location mode was restored to `3`; secure mock-location setting was restored to `0`; test package `android:mock_location` app-op was restored to `deny`; animation settings were restored to their original values. Android 14 AVD `emulator-5556` was shut down after testing. No physical device was changed.

## Diff Hygiene

- `git diff --check`: PASS
- Temporary diagnostic logging was removed from production code.
- Temporary Maps placeholder file was removed.
