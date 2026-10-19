# Execution Report — debug-1019-map-user-location

## Implementation

- Blank editable gutter forms without a usable saved coordinate now request a bounded high-accuracy location, including offline and restored drafts. The request moves only the camera; it does not write form or waypoint coordinates.
- Valid saved point coordinates remain the initial camera target. Null, out-of-range, non-finite, and `(0,0)` targets are rejected by the shared map-coordinate policy.
- Host and point-picker locate flows can prefer a newer usable result even when its accuracy improves by less than 10 m. A usable cached fix remains the fallback when a fresh result is invalid or unavailable.
- Host form handoff skips camera movement if neither the waypoint nor host map has a usable target.
- Point-picker Locate remains viewport-only; Confirm returns the selected camera target.

## Validation

| Command / procedure | Result | Impact / limitation |
|---|---|---|
| Focused `:app:testDebugUnitTest` for `EditMapLocationPolicyTest`, `MapCoordinatePolicyTest`, and `LocationFixQualityPolicyTest` | PASS — 10 tests, 0 failures, confirmed in the three JUnit XML reports | Covers eligibility, coordinate validity, cached-fix freshness, and explicit recenter policy. |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | PASS — `BUILD SUCCESSFUL` | Artifacts: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. The Kotlin incremental compiler reported a temporary-file exception, fell back to non-incremental compilation, and completed successfully. |
| `git diff --check` | PASS | No whitespace errors. |
| `ANDROID_SERIAL=emulator-5554 :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#restoredBlankFormWithoutFreshAddFlagUsesLocationCameraOnly` | FAIL at `EditMapInitialLocationInstrumentedTest.kt:336`; test waited 40.9 s and the camera remained away from the injected test point | After enabling the AVD mock-location provider, Android's location service showed the mock fix, but the fused location callback still did not reach the form. The same case failed again after changing the form request to high accuracy. Classify this as an emulator/GMS environment limitation; AC-002 remains `NOT VERIFIED`. See `ISS-DBG-1019-004`. |
| Remote CI | NOT RUN | No CI run or status is available in this local task context. |
| Physical device | NOT RUN | No physical Android device was attached; Android emulator was API 36, serial `emulator-5554`. |

## Build Details

- Application package: `com.example.taoyuangutter`
- Build variant: `debug`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- A temporary ignored `local.properties` with a non-secret placeholder Maps key was used only to allow local builds; it is not part of the task changes.

## Acceptance-Criteria Evidence Snapshot

- AC-001: `NOT VERIFIED` by emulator; source preserves a usable saved coordinate and the focused unit policy is covered, but no successful camera instrumentation result is retained for the final revision.
- AC-002: `NOT VERIFIED`; the restored blank-form instrumentation case fails because the emulator does not deliver its injected fused fix.
- AC-003: `PASS` for the recenter decision rule in unit tests; host/picker end-to-end movement remains unverified.
- AC-004: `PASS` for fallback and unavailable prompt assertions in the focused instrumentation run before its later manual-gesture assertion; the manual-pan gesture is `NOT VERIFIED` because this AVD has an invalid Maps SDK key.
- AC-005: `PASS` by source review; both host callbacks keep an absent camera target nullable and omit the camera move.
- AC-006: `PASS` by source review; the picker Locate handler requests viewport movement, while only Confirm returns the camera target as a result.

## Handoff

- Independent Verification and remote CI are still required. Do not mark the task done or advance to Release while AC-002 and other device-dependent evidence remain unverified.
