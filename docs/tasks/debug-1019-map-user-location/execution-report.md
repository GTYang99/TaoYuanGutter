# Execution Report — debug-1019-map-user-location

## Revision

- Branch: `codex/debug/debug-1019-map-user-location`
- Implementation commit: `c26dd3f` (`fix(debug-1019-map-user-location): reject zero-zero provider fixes`)
- Baseline implementation commit: `3e78cfcca0f55a57f518c46490237e1b97ed5ad0`
- Follow-up: corrected the fresh-add instrumentation fixture and fixed the remaining fused-provider `(0,0)` acceptance path. The latest source revision is being committed after local validation.

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
| Focused `EditMapInitialLocationInstrumentedTest` cases on `DBG1017_API34_Camera`, Android 14 API 34, with the configured Maps key | PASS — saved coordinate, ordinary blank edit, offline blank, restored blank, `(0,0)` rejection, and corrected fresh-add cases each passed individually | Fresh-add test initially failed because it expected Taipei without setting the mock location. The failure was classified as a fixture defect (ISS-DBG-1019-005); after adding setup/cleanup, the targeted rerun passed. Earlier Android 16 test runs used an invalid Maps key and are historical environment failures (ISS-DBG-1019-004). |
| `ANDROID_SERIAL=emulator-5554 :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#freshNewPointWithEmulatorGpsFixCentersCameraOnly` | PASS — 1 test, 0 failures; Android 14 API 34 | Result: `app/build/outputs/androidTest-results/connected/debug/TEST-DBG1017_API34_Camera(AVD) - 14.xml`. Confirms camera-only centering on the injected point for fresh-add flow. |
| `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` | PASS — `OK (1 test)` on `DBG1017_API34_Camera`, Android 14 API 34 | With system location and mock-location disabled, the blank form kept the Taoyuan fallback, left node/session coordinates unchanged, showed the unavailable prompt, and allowed manual pan. Emulator settings were restored and the emulator stopped after the case. |
| `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#zeroZeroFusedLocationDoesNotMoveBlankFormMap com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` | PASS — `OK (1 test)` on `DBG1017_API34_Camera`, Android 14 API 34 | Injected fused `(0,0)` did not move the camera from fallback, showed the unavailable prompt, and left form/session coordinates unchanged. |
| `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#freshNewPointWithEmulatorGpsFixCentersCameraOnly com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` | PASS — `OK (1 test)` on `DBG1017_API34_Camera`, Android 14 API 34 | Reconfirmed after the fused-coordinate policy change that a valid injected fix still centers a fresh form map without writing the form/session coordinates. |
| GitHub Actions Android CI, Run #19, commit `673671a62998352e90a15e6478c7ea15fd6ede51` | NOT VERIFIED — superseded | The push-triggered `Unit tests and debug build` job remained at “Starting job…” after 16 minutes. This run predates the final source fix and cannot validate it. A new run must be checked for the final committed revision. |
| Physical device | NOT RUN | No physical Android device was attached. |

## Build Details

- Application package: `com.example.taoyuangutter`
- Build variant: `debug`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- An ignored local `local.properties` was used for local Maps-enabled emulator validation. It is not part of the task changes and was removed after testing.

## Acceptance-Criteria Evidence Snapshot

- AC-001: `PASS` on Android 14 API 34; the saved-coordinate instrumentation case retained its waypoint camera target.
- AC-002: `PASS` for tested fresh-add, ordinary blank-edit, offline, and restored form-map flows on Android 14 API 34; provider `(0,0)` is rejected and camera-only coordinate assertions passed.
- AC-003: `PASS` for the recenter decision policy in unit tests; host-map and point-picker end-to-end movement remains `NOT VERIFIED`.
- AC-004: `PASS` on Android 14 API 34 with system location disabled; fallback, unavailable prompt, coordinate preservation, and manual pan assertions passed.
- AC-005: `PASS` by source review; both host callbacks keep an absent camera target nullable and omit the camera move.
- AC-006: `PASS` by source review; the picker Locate handler requests viewport movement, while only Confirm returns the camera target as a result.

## Handoff

- Independent Verification and a completed remote CI result for the final commit are still required. Host-map and point-picker Locate end-to-end behavior remains unverified; physical-device testing is outside the approved plan.
