# Implementation Execution Report

## Implemented

- Added `is_connect_point` and `is_connect_pipe` to read DTOs, form draft data, inspect-to-edit handoff, request mapping, and JSON serialization.
- Added Figma-aligned Connect Point and Connect Pipe controls to the basic-info form. Connect Point is mutually exclusive with Cant Open; Connect Pipe defaults to 無 and is independent.
- Virtual points hide and reset Cant Open, Connect Point, and Connect Pipe; the mapper omits all three payload keys.
- Create requests omit `XY_NUM`; edit requests preserve the existing value. Successful create responses map backend-generated start/intermediate/end `XY_NUM` values back into waypoints.
- Changed closest-node import to call `GET /api/v1/node/closestNodeDetails` without location query parameters and removed the opened-sheet GPS trigger and location UI.
- Added mapper JSON regression tests for create/edit/virtual payload behavior.

## Bug Fix Follow-up

- Required-marker updates now keep `XY_NUM` hidden for create mode while preserving it for edit/view.
- Remark preset chips now toggle: tapping an existing preset removes only that preset.
- New-point validation no longer requires `XY_NUM`; edit validation continues to require it.
- Waypoint rows show `已填寫資料` when form data exists even before backend-generated `XY_NUM` is available.
- MainActivity now clears the target-panel visibility state when the inspect/edit sheet closes, restoring the map-side buttons.

## Validation

| Check | Result | Evidence |
|---|---|---|
| Debug APK build | PASS | `./gradlew :app:assembleDebug`; `app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests | PASS | `./gradlew :app:testDebugUnitTest` |
| Full Gradle JVM tests | PASS | Latest revision: `JAVA_HOME=... ./gradlew test --no-daemon`; BUILD SUCCESSFUL in 9s |
| Android test compilation | PASS | `./gradlew :app:compileDebugAndroidTestKotlin` |
| Connected UI test | PASS | `Medium_Phone` Android 14 (`emulator-5554`), `GutterBasicInfoUiTest`, `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.GutterBasicInfoUiTest` |
| Full connected Android test suite | PASS | `Medium_Phone` Android 14 (`emulator-5554`), `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --no-daemon`; BUILD SUCCESSFUL in 4m 36s |
| Connection-control UI regression test | PASS | Added assertions for Connect Point / Cant Open mutual exclusion, Connect Pipe default, and virtual-point hiding; targeted `GutterBasicInfoUiTest` on `Medium_Phone` Android 14; BUILD SUCCESSFUL in 45s |
| Bug-fix UI regression test | PASS | `GutterBasicInfoUiTest` now verifies create-mode `XY_NUM` marker is hidden and remark preset removal; targeted connected test on `Medium_Phone` Android 14; BUILD SUCCESSFUL in 50s |
| Map button restoration fix | PASS | `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed; targeted UI suite passed. Full connected suite had 35/36 tests pass; one unrelated `RootViewWithoutFocusException` occurred in `MainShellActivityTest.addGutterListWiresAddAndCloseConfirmationCallbacks`. |
| Mapper targeted tests | PASS | `StoreDitchNodeRequestMapperTest` |
| Store response contract tests | PASS | `StoreDitchResponseParsingTest` verifies generated start/node/end `XY_NUM` parsing; targeted request/response test command completed successfully |
| Physical/API AC-002..AC-004 | NOT VERIFIED | Connected UI test does not provide authenticated API request/response evidence |

Environment note: an initial workspace-shell `adb` attempt could not start its daemon, but the Android Studio emulator environment was subsequently available. Validation was run against the fixed `Medium_Phone` AVD on `emulator-5554`.

## Limitations

- Existing unused legacy location helper methods remain in `GutterFormActivity`; the sheet-open path no longer invokes them and the visible import UI no longer exposes location controls.
- Independent verification, CI, and authenticated API request/response evidence remain pending.
