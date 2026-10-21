# Execution Report — debug-1019-map-user-location

## Revision

- Branch: `codex/debug/debug-1019-map-user-location`
- Earlier source fix: `c26dd3f` (`fix(debug-1019-map-user-location): reject zero-zero provider fixes`)
- Latest source fix: `1e896f1` (`fix(debug-1019-map-user-location): reject invalid nearby fixes`)
- Baseline implementation commit: `3e78cfcca0f55a57f518c46490237e1b97ed5ad0`
- Follow-up: corrected the fresh-add instrumentation fixture and fixed the fused-provider `(0,0)` paths in both initial form centering and Nearby import. The Nearby policy fix is committed after local validation.

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
| Focused `:app:testDebugUnitTest` for `NearbyLocationFixPolicyTest`, `EditMapLocationPolicyTest`, `MapCoordinatePolicyTest`, and `LocationFixQualityPolicyTest` | PASS — `BUILD SUCCESSFUL`; Nearby policy 3 tests, 0 failures | Includes the new fresh-accurate `(0,0)` Nearby fix rejection test; valid fresh coordinates keep the existing timestamp and accuracy behavior. |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | PASS — `BUILD SUCCESSFUL` | Re-run after the Nearby policy change using the CI build-only `MAPS_API_KEY=ci-placeholder`; ignored `local.properties` was removed after validation. |
| GitHub Actions Android CI, Run #19, commit `673671a62998352e90a15e6478c7ea15fd6ede51` | NOT VERIFIED — superseded | The job remained in progress without any recorded steps long after runner startup. It predates both source fixes and cannot validate them. |
| GitHub Actions Android CI, Run #20, commit `6468e8903ba2985a3a85c5732e235e55783380f8` | NOT VERIFIED — superseded | The run was queued behind Run #19 and predates the latest Nearby policy fix, so it cannot validate the final source revision. |
| GitHub Actions Android CI, Run #21, commit `7b1780f3bfab9507c055e74b335090683788c05c` | PASS | `Unit tests and debug build` completed successfully, including task-scoped JVM tests and debug APK assembly. This revision contains both source fixes. |
| Physical device | NOT RUN | No physical Android device was attached. |

## Emulator-only Revalidation on the Current Branch

- Current branch before recording these results: `codex/fix/map-dashboard-viewport`, based on the committed location fixes and the later map-tab retention fix.
- Test target: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 / API 36. No physical device or host/JVM test runner was used in this revalidation.
- `:app:assembleDebugAndroidTest` | **PASS** — `BUILD SUCCESSFUL`; rebuilt the instrumentation APK after correcting a test-only assumption.
- `EditMapInitialLocationInstrumentedTest#savedCoordinateEditKeepsSavedCameraTarget` | **PASS** — `OK (1 test)`; the saved gutter point remained centered with location permission available.
- `EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithFusedMockLocationCentersCameraOnly` | **PASS** — `OK (1 test)`; a blank form centered on the injected usable fix without writing form or waypoint coordinates.
- `EditMapInitialLocationInstrumentedTest#zeroZeroWaypointIsNotUsedAsFormMapTarget` | **PASS** — `OK (1 test)`; a `(0,0)` waypoint did not become the camera target.
- `EditMapInitialLocationInstrumentedTest#zeroZeroFusedLocationDoesNotMoveBlankFormMapToOrigin` (initial, strict fallback assertion) | **FAIL — test assumption**; API 36 returned a usable non-zero camera target `(25.051658, 121.407173)`, so requiring only the Taoyuan fallback was incompatible with the blank-map current-location requirement. Tracked as `ISS-DBG-1019-008`.
- Corrected `EditMapInitialLocationInstrumentedTest#zeroZeroFusedLocationDoesNotMoveBlankFormMapToOrigin` | **PASS** — `OK (1 test)`; asserts the final camera target is usable and cannot be `(0,0)`, while checking form/session coordinates remain unchanged. The separate no-fix test remains responsible for the fallback and unavailable prompt.
- `EditMapInitialLocationInstrumentedTest#offlineBlankFormUsesLocationCameraOnly` | **PASS** — `OK (1 test)`; the legacy/offline blank form centered on the injected usable fix without saving it as form or waypoint coordinates.
- `MapPointPickerLocationInstrumentedTest#explicitLocateMovesCameraToFreshFixWithoutFinishingPicker` | **PASS** — `OK (1 test)`; the picker opened on a saved point, the Locate FAB moved the camera to the injected fresh fix, and the picker remained open so Locate did not return/commit a coordinate.
- `MainMapLocateInstrumentedTest#hostMapLocateButtonMovesCameraToFreshFix` | **PASS** — `OK (1 test)`; tapping the primary map's Locate button moved the production map from a different camera center to the injected fresh location.
- Fine and coarse location permissions were denied again after instrumentation; `dumpsys package` confirmed the emulator returned to its original denied state. The mock GPS test provider is disabled by each test's `finally` cleanup.
- Current combined-branch CI and independent Verification remain pending. The configured CI workflow runs hosted JVM tests, outside the emulator-only validation scope.

## Build Details

- Application package: `com.example.taoyuangutter`
- Build variant: `debug`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- An ignored local `local.properties` was used for local Maps-enabled emulator validation. It is not part of the task changes and was removed after testing.

## Acceptance-Criteria Evidence Snapshot

- AC-001: `PASS` on Android 14 API 34; the saved-coordinate instrumentation case retained its waypoint camera target.
- AC-002: `PASS` for tested fresh-add, ordinary blank-edit, offline, and restored form-map flows on Android 14 API 34; provider `(0,0)` is rejected and camera-only coordinate assertions passed.
- AC-003: `PASS` for the recenter decision policy in the previously recorded unit tests and for primary host-map and point-picker Locate movement in the current API 36 emulator instrumentation; stale/invalid fallback remains covered by policy tests.
- AC-004: `PASS` for initial blank-form location fallback on Android 14 API 34 and for the Nearby provider policy boundary in unit tests; invalid fresh `(0,0)` is rejected. Full Nearby sheet runtime behavior remains `NOT VERIFIED`.
- AC-005: `PASS` by source review; both host callbacks keep an absent camera target nullable and omit the camera move.
- AC-006: `PASS` by source review and picker instrumentation; Locate moves the camera while leaving the picker open, and only Confirm returns the camera target as a result.

## Handoff

- Independent Verification and a completed remote CI result for the current combined branch are still required. Nearby import's full sheet runtime behavior remains `NOT VERIFIED`; physical-device testing is outside the approved plan.
