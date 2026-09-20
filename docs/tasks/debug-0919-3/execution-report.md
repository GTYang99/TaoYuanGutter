# Execution Report

## Branch
- `fix/debug-0919-3-回報點位難點`
- Production commit under review: `9ed2c7c`

## Implementation
- Changed `NoDitchModeUiController` so the shared no-ditch panel uses only the system bar bottom inset.
- Removed the IME bottom inset from the panel bottom-margin calculation.
- Added `NoDitchPanelInsetPolicy` and focused unit coverage for IME independence, repeated inset updates, and negative inset safety.
- No API, data, permission, map selection, or no-ditch state-machine changes were made.

## Validation

| Check | Result | Evidence / limitation |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors in the working diff. |
| `state.yaml` parse | PASS | Ruby YAML parser accepted the task state. |
| `./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest` | PASS | JDK 21; focused policy tests passed and Kotlin compilation completed. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled successfully with JDK 21. |
| `./gradlew :app:connectedDebugAndroidTest` | FAIL, then classified | Sony XQ-AU52 / Android 12: 37/38 passed; one unrelated imported-waypoint test first hit `NoActivityResumedException`. |
| Focused `MainShellActivityTest` | PASS | 12/12 passed on Sony XQ-AU52 / Android 12. |
| Retry `Debug0919ImportedWaypointUiTest` | PASS | 1/1 passed on the same device; first full-suite failure was intermittent and unrelated to this change. |
| Foldable physical-device AC-001/AC-002 | NOT VERIFIED | No foldable device or approved equivalent window configuration is available. |

## Limitations
- The direct code path, compilation, focused unit test, Debug APK build, and relevant Android instrumentation regression are verified.
- The connected Sony XQ-AU52 Android 12 device is non-foldable and was not used as a substitute for foldable acceptance evidence.
- The first full-suite failure was retried once as allowed by the testing rules and passed; no production change was made for it.
