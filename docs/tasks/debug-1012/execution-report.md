# Debug 1012 Execution Report

## Revision and Scope

- Task branch: `codex/debug-1012`
- Base revision reviewed: `ac6c9dd`
- Implementation commit: `5714f53` (`fix(debug-1012): clear loading after edit upload failure`)
- Package: `com.example.taoyuangutter`
- Scope: clear host blocking loading on existing-gutter `storeDitch` failures, preserve the current error dialog and host draft flow, and cover 403/retry/save-draft behavior.
- No API contract, 403 classification, draft persistence behavior, or runtime dependency changed.

## Root Cause and Implementation

`performEditSubmit()` enables the host blocking loading state before `storeDitch`. Before this fix, the edit `ApiResult.Error` branch and generic exception branch did not call `onGutterSubmitFailed()`. The save-draft action can dismiss the bottom sheet without another host callback to clear loading, so `MainBlockingUiController` keeps the overlay and indeterminate spinner active.

Both edit failure branches now call the existing host cleanup callback before showing their existing failure UI. Retry still re-enters `performEditSubmit()` and activates loading again. The issue and full control-flow evidence are in `root-cause.md` and `analysis.md`.

## Developer Validation

| Check | Command / environment | Result |
|---|---|---|
| Source and whitespace review | `git diff --check` | PASS |
| Instrumentation regression, AC-001/002/003 | `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.Debug1012UploadFailureInstrumentedTest`; Medium_Phone AVD, Android 16 / API 36 | PASS, 1 instrumentation test, 0 failures. Fake API returned HTTP 403 twice; dialog actions were present; overlay was `GONE`, controller not busy, and spinner rotation was stopped after each failure; retry entered loading; save-draft dismissed the sheet. |
| Unit tests | Included in the command above (`:app:testDebugUnitTest`) | PASS |
| Debug APK | Included in the command above (`:app:assembleDebug`) | PASS; `app/build/outputs/apk/debug/app-debug.apk` |
| Generic exception path, AC-004 | Source review of `performEditSubmit()` catch branch | PASS; cleanup callback precedes the existing error dialog. |

The first instrumentation harness iteration used Espresso to find the dialog and failed because Espresso reported no resumed activity. That was a harness interaction failure; the test was changed to access and operate the actual captured alert dialog. A separate rerun attempt encountered an offline emulator before test installation. The final connected run above completed and passed on the requested emulator.

No physical device was used. CI and independent verification have not run yet and remain pending workflow phases.

## AC Traceability

- AC-001: instrumented 403 asserts the dialog is showing while blocking loading is stopped.
- AC-002: instrumented save-draft button click dismisses the sheet with loading stopped. Draft persistence is outside this task's approved scope.
- AC-003: instrumented retry issues the second 403 request with loading active at request entry, then stops loading again.
- AC-004: source review confirms a non-cancellation exception clears loading before its failure dialog.

## Artifacts and Preconditions

- Emulator: Medium_Phone AVD, Android 16 / API 36, serial `emulator-5554`.
- Instrumentation creates an in-process fake `GutterApiService`; it does not contact a production endpoint.
- Test authentication value is local to the test and removed in teardown; no account credential is needed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Remaining Workflow

- Independent Verification: pending; must inspect the committed revision.
- CI: not run in this local developer phase.
- Release decision: pending Verification and CI evidence.
