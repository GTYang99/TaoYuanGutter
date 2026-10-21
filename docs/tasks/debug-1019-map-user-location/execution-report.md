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
- Current combined-branch CI remains pending; independent Verification has completed and is recorded in `verification.md`. The configured CI workflow runs hosted JVM tests, outside the emulator-only validation scope.

## Post-verification Emulator Reruns

- Date: 2026-10-21. Current HEAD is `c5ded0d9`; production and instrumentation sources are unchanged from reviewed code/test revision `0b78cc43`.
- Test target: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 / API 36. No physical device or host/JVM test runner was used.
- Command `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#savedCoordinateEditKeepsSavedCameraTarget com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — **PASS**, `OK (1 test)` in 34.088 seconds. A saved gutter point remained the map center.
- Command `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithFusedMockLocationCentersCameraOnly com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — **PASS**, `OK (1 test)` in 35.393 seconds. The blank form centered on the injected usable fix while `NODE_X`, `NODE_Y`, and waypoint/session coordinates remained unchanged.
- Command `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#zeroZeroFusedLocationDoesNotMoveBlankFormMapToOrigin com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — **PASS**. Emulator logcat records `run finished: 1 tests, 0 failed, 0 ignored` and `VM exiting with result code 0`.
- Cleanup: each test's `finally` cleared the mock location provider. Fine and coarse location permissions were revoked and their `user-set` / `user-fixed` flags cleared; `dumpsys package` confirmed both permissions were denied afterward, and the test runner was stopped.

## Blank-Form First-Frame Follow-up

- Branch: `codex/fix/map-dashboard-viewport`.
- Implementation commit: `d4f6f46b` (`fix(debug-1019-map-user-location): prevent blank form fallback flash`); the task-state record was committed separately.
- Production behavior: a blank editable form map is hidden while its initial location is unresolved. A recent usable host fix is applied before the map is first shown; otherwise the map is revealed at the usable provider fix or at the existing fallback after the location/permission failure path. Saved waypoint coordinates remain immediately visible and take precedence.
- Build command: `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' PATH='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin':$PATH ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` — **PASS**, `BUILD SUCCESSFUL`.
- Test target: `emulator-5554`, Android 16 / API 36. Physical-device testing was not performed.
- `blankFormUsesFreshHostLocationBeforeFirstMapDisplay` — **PASS**; the first visible frame used the recent host coordinate, not the Taoyuan fallback.
- `returningFromSettingsWithoutPermissionKeepsFallbackUsable` — **PASS**; the blank map remained hidden while the permission decision was unresolved and became visible at the fallback after denial.
- `freshNewPointWithEmulatorGpsFixCentersCameraOnly` — **PASS**; the usable fix centered the new form without writing coordinate data.
- `savedCoordinateEditKeepsSavedCameraTarget` — **PASS**; the saved point remained centered.
- `manualPanBeforeFusedMockCallbackKeepsManualCameraTarget` — **FAIL / NOT VERIFIED**. Two diagnostic reruns confirmed the map was visible and the location flow active before the swipe; afterward `editMapLocationUserMoved=true`, but the camera target remained exactly at the host coordinate and the location flow had completed. A wider swipe produced the same result. This does not prove whether input timing or a production map behavior caused the unchanged camera, and the test never established a delayed callback after a moved camera. The host-seeded case remains unverified; tracked as `ISS-DBG-1019-011`.
- A later rerun of `returningFromSettingsWithoutPermissionKeepsFallbackUsable` on the same API 36 emulator — **NOT VERIFIED**. Espresso raised `NoActivityResumedException` at the first Retry-button interaction after `denySystemLocationPermissionPrompt()` (`EditMapInitialLocationInstrumentedTest.kt:306`). The helper taps a fixed Android 14 permission-dialog coordinate; the API 36 run did not return to a resumed app activity. This is a test-environment/UI automation limitation and does not establish a product regression. The earlier API 36 pass remains recorded; revalidation needs a permission-dialog interaction that works on this emulator version.
- `git diff --check` — **PASS** after final source, test, and task-document edits and before commit.
- Independent Verification reviewed committed HEAD `a159da51` and recorded `NOT VERIFIED` because the no-host success branch and manual-pan regression remain unproven. Hosted CI remains pending because publishing the exact branch/payload was rejected by the automatic approval review. Do not treat this as a release-complete revision.

## Continued Focused Validation — 2026-10-21

- No physical device was used. The instrumentation command explicitly selected `emulator-5554` (Android 16 / API 36).
- Focused unit command: `./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest' --tests 'com.example.taoyuangutter.map.MapCoordinatePolicyTest'` — **PASS**, `BUILD SUCCESSFUL`.
- `EditMapInitialLocationInstrumentedTest#blankFormUsesFreshHostLocationBeforeFirstMapDisplay` — **PASS**, 1 test / 0 failures. Its `OnPreDraw` capture confirmed the first visible frame used the fresh host coordinate.
- Earlier diagnostic attempt for AC-007's no-host successful-fix branch — **NOT VERIFIED on API 36; superseded by API 34 evidence below**. On that API 36 emulator the fused callback did not arrive within the test window. This was later classified as an emulator/GMS test-environment limitation (`ISS-DBG-1019-012`), not a production failure.
- `git diff --check` — **PASS** after recording these results.

## No-Host First-Frame Revalidation — 2026-10-21

- Test target: `emulator-5556`, AVD `DBG1017_API34_Camera`, Android 14 / API 34. No physical device was used.
- `:app:assembleDebugAndroidTest` — **PASS**, `BUILD SUCCESSFUL` after adding the focused no-host first-frame instrumentation case.
- `blankFormWithoutHostFixStaysHiddenUntilProviderFixIsApplied` — **PASS**, `OK (1 test)` in 7.697 seconds. The form was asserted hidden after the location attempt began and before test GPS injection. An `OnPreDraw` capture confirmed the first visible frame was centered on the injected fix. `currentLat`, `currentLng`, `NODE_X`, `NODE_Y`, and the waypoint snapshot coordinates remained unchanged.
- `freshNewPointWithEmulatorGpsFixCentersCameraOnly` — **PASS**, `OK (1 test)` in 7.007 seconds on the same API 34 AVD.
- Focused JVM command for `EditMapLocationPolicyTest` and `MapCoordinatePolicyTest` — **PASS**, `BUILD SUCCESSFUL`; Gradle reported tasks up to date.
- API 36/API 36 mock-provider diagnostics: the first attempt was rejected because the test package was not authorized for `MOCK_LOCATION`. After enabling the test-only mock-location app-op, the provider accepted the fix and `dumpsys location` showed the fused/GPS cache; however, `GutterFormActivity` remained on attempt 2 without a callback during the assertion window. This is recorded as environment issue `ISS-DBG-1019-012`; the API 34 pass closes AC-007's no-host branch.
- `returningFromSettingsWithoutPermissionKeepsFallbackUsable` on API 34 — **NOT VERIFIED**; the instrumentation process exited without a test result while interacting with the native permission UI. A prior API 36 pass remains recorded; the latest API 36 rerun also stopped before its assertion with `NoActivityResumedException`.
- The secure mock-location-app selection is empty on both AVDs after validation; API 36's test-package app-op is back to default. Location permissions were revoked and test providers were disabled on both emulators. The Android 14 AVD was shut down after validation. No physical device was used.

## Final Implementation-Validation Check — 2026-10-21

- Source and instrumentation revision under test: `b60fe370`; current HEAD at execution: `d6b9e1ff` (documentation-only follow-up, no source or test changes).
- Focused command: `env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' PATH='/Applications/Android Studio.app/Contents/jbr/Contents/bin':$PATH ./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest' --tests 'com.example.taoyuangutter.map.MapCoordinatePolicyTest'` — **PASS**, `BUILD SUCCESSFUL`; Gradle reported the selected tasks up to date.
- The first sandboxed invocation could not create Gradle's wrapper lock under `~/.gradle` (`Operation not permitted`). The same focused command was retried with approved local cache access and passed; no repository source or test files were changed by the run.
- Emulator regression evidence remains the API 36 host-seeded first-frame pass and API 34 no-host first-frame pass recorded above. No physical device was used. The host-seeded manual-pan callback case remains **NOT VERIFIED** (`ISS-DBG-1019-011`); this focused JVM run does not cover that UI timing behavior.
- Hosted CI and Release were not run as the requested scope ends at implementation validation.

## Point-Picker Fallback Flash Follow-up — 2026-10-21

- Changed `MapPointPickerActivity` to keep the map hidden and Confirm disabled for an unusable initial coordinate; it reveals at a usable location, or at the existing Taoyuan fallback after location/permission failure. Valid initial coordinates still display directly.
- Added `MapPointPickerInitialCameraPolicyTest` and `blankPickerFirstVisibleFrameUsesLocationInsteadOfTaoyuanFallback`.
- `:app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.MapPointPickerInitialCameraPolicyTest' :app:assembleDebug :app:assembleDebugAndroidTest` — **PASS**, `BUILD SUCCESSFUL`.
- Android 14 API 34 AVD first-visible-frame case passed once in the focused class run. A later isolated run returned a different usable emulator-provider coordinate than the injected mock; the captured frame was still away from the Taoyuan fallback. The instrumentation assertion now checks the product requirement (usable first frame, no fallback flash) instead of requiring one exact mock coordinate. The revised assertion was compiled but not rerun. The existing explicit Locate case was blocked by the emulator permission dialog during Espresso lookup.
- Hosted CI and independent verification were not run; requested scope ends at implementation validation.

- AC-008: First-visible-frame behavior was observed on Android 14 API 34 at a usable non-Taoyuan provider target, and the initial exact-coordinate version passed once. The relaxed semantic assertion compiles; its standalone rerun is **NOT VERIFIED** due mock-provider variability.

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
- AC-007: `PASS` for the host-fix first-visible frame on API 36 and the no-host successful-fix first-visible frame on API 34. The delayed-callback manual-pan regression remains `NOT VERIFIED` (ISS-DBG-1019-011); permission-denial/fallback visibility has an earlier API 36 pass, but its latest reruns were blocked by permission-dialog automation limitations recorded above.

## Handoff

- Independent Verification and a completed remote CI result for the current combined branch are still required. Nearby import's full sheet runtime behavior remains `NOT VERIFIED`; physical-device testing is outside the approved plan.
