# Execution Report

Date: 2026-09-04

## Scope

Re-implementation for verification failures in `docs/tasks/feat-0904/verification.md`.

## Changes

- Routed edit preload `getNodeDetails(...)` 401 failures through `AuthExpiredHandler` after preserving current waypoint state.
- Updated auth-expired Dialog copy to include the exact required phrase: `登入狀態已失效，請重新登入`.
- Added a focused import-sheet callback so parent form state is saved before auth-expired logout.
- Restored `GutterApiClient.ENABLE_GROUP_SIMULATION` to `false`.
- Added pure once-only guard coverage for duplicate auth-expired handling, draft-save failures, reset behavior, and non-401 exclusion.
- Added upload classifier coverage for store-ditch 401 and photo 401 reference-code distinction.
- Added repository auth coverage for logout 401 success and login 401 error distinction.
- Added pending draft serialization coverage proving local photo paths and upload state are preserved.
- Added focused connected UI coverage for the auth-expired edit-flow core: save draft callback, Dialog display, confirm action, and navigation to `LoginActivity`.

## Validation

- `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest`
  - Result: PASS
- `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug`
  - Result: PASS
- `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebugAndroidTest`
  - Result: PASS
- `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AuthExpiredUiFlowTest`
  - Result: PASS
  - Devices: `Medium_Phone(AVD) - 14`, `XQ-AU52 - 12`

## Limitations

- Full connected instrumentation suite was not rerun after focusing `ISS-0904-003`; the focused auth-expired UI flow passed on both connected devices.
