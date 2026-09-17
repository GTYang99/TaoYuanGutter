# Implementation Execution Report

## Implemented

- Added `is_connect_point` and `is_connect_pipe` to read DTOs, form draft data, inspect-to-edit handoff, request mapping, and JSON serialization.
- Added Figma-aligned Connect Point and Connect Pipe controls to the basic-info form. Connect Point is mutually exclusive with Cant Open; Connect Pipe defaults to 無 and is independent.
- Virtual points hide and reset Cant Open, Connect Point, and Connect Pipe; the mapper omits all three payload keys.
- Create requests omit `XY_NUM`; edit requests preserve the existing value. Successful create responses map backend-generated start/intermediate/end `XY_NUM` values back into waypoints.
- Changed closest-node import to call `GET /api/v1/node/closestNodeDetails` without location query parameters and removed the opened-sheet GPS trigger and location UI.
- Added mapper JSON regression tests for create/edit/virtual payload behavior.

## Validation

| Check | Result | Evidence |
|---|---|---|
| Debug APK build | PASS | `./gradlew :app:assembleDebug`; `app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests | PASS | `./gradlew :app:testDebugUnitTest` |
| Full Gradle JVM tests | PASS | `./gradlew test` |
| Android test compilation | PASS | `./gradlew :app:compileDebugAndroidTestKotlin` |
| Mapper targeted tests | PASS | `StoreDitchNodeRequestMapperTest` |
| Physical device AC-001..AC-004 | NOT VERIFIED | No device/API session available in this phase |

Environment evidence: `adb devices` could not start its daemon (`could not install smartsocket listener: Operation not permitted`), and `emulator -list-avds` is unavailable in the workspace shell. No connected-device test was claimed as PASS.

## Limitations

- Existing unused legacy location helper methods remain in `GutterFormActivity`; the sheet-open path no longer invokes them and the visible import UI no longer exposes location controls.
- Independent verification, CI, and physical-device request/response evidence remain pending.
