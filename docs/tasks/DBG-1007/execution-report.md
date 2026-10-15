# Execution Report

## Implementation

- Task: `DBG-1007` — center the form map on the current location only when editing an existing point without saved coordinates.
- Branch: `codex/DBG-1007-minimap-location`
- Implementation commit: `5114269122aa762c28f0093904c145e03ce3bd72`
- Selected mock GPS test harness commit: `130bf200c2874d5ec545cc842372efb1eee3c9cd`
- Developer-validation candidate: `130bf200c2874d5ec545cc842372efb1eee3c9cd`; Android package `com.example.taoyuangutter`; build variant `debug`.
- Emulator test commits: `1ad0ab7`, `6177e09`, `9cbec1f`, and `74ad5a6`.
- Production changes: the form requests fine and coarse foreground location permission together, accepts either grant, offers one retry after recoverable denial, and uses one bounded location reacquisition after a failed first fix. Permanent denial offers app Settings. Returning from Settings with permission granted starts one acquisition attempt.
- Privacy and behavior: a usable fix moves only the form map camera. It does not update `Waypoint`, `currentLat`/`currentLng`, form coordinate fields, drafts, or a My Location marker. A manual map gesture prevents a late callback from moving the camera. The existing Taoyuan fallback remains available.
- New files: `EditMapLocationPolicy.kt` and `EditMapLocationPolicyTest.kt`.

## Developer Validation

| Command / check | Result | Evidence / impact |
|---|---|---|
| `:app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest'` | PASS | Targeted policy tests passed. |
| `:app:assembleDebug` | PASS | Debug APK produced at `app/build/outputs/apk/debug/app-debug.apk`; variant: `debug`; package: `com.example.taoyuangutter`. |
| `git diff --check` | PASS | No whitespace errors. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest` | PASS (earlier run) | Ran on `emulator-5554` (Medium_Phone AVD, Android 14 / API 34); 3 tests passed, 0 failed. Covered missing coordinates with no usable fix (fallback and unavailable prompt, coordinate state unchanged), saved-coordinate camera target, and new-point no-recenter regression. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#savedCoordinateEditWithLocationDeniedDoesNotRequestPermission` | PASS | On `emulator-5554`, revoked both foreground location permissions before launch; saved waypoint remained the camera target, location flow stayed inactive, and the unavailable prompt was absent (AC-002). |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#coarseOnlyPermissionStartsEditLocationWithoutPermissionPrompt` | PASS | On `emulator-5554`, granted coarse location while fine location remained denied; the edit flow began a location attempt without showing the app's permission-retry prompt (AC-010). |
| Emulator permission denial flow (AC-004) | PASS | On `emulator-5554` (Android 14 / API 34), denied the first native prompt using `adb shell uiautomator dump /sdcard/window.xml`, `adb shell cat /sdcard/window.xml`, then `adb shell input tap 540 1748`. The app offered one retry. After selecting retry, used the same commands to deny the second prompt; the app displayed the unavailable-location prompt, and Continue dismissed it. Captured result: `docs/tasks/DBG-1007/emulator-results/AC-004.xml`. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#returningFromSettingsWithPermissionStartsOneLocationAttempt` | PASS | On `emulator-5554`, opened app Settings from the permanent-denial prompt, granted foreground location permission, returned to the form, and observed exactly one acquisition attempt with the Settings-return flag cleared (AC-011). Captured result: `docs/tasks/DBG-1007/emulator-results/AC-011-granted.xml`. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#returningFromSettingsWithoutPermissionKeepsFallbackUsable` | PASS | On `emulator-5554`, cleared permission state, denied both native permission prompts using the fixed AVD's shell tap procedure, opened Settings from the unavailable-location prompt, returned without granting permission, and verified the location flow stopped and the camera remained at the Taoyuan fallback (AC-005 and AC-011). AC-004 separately verified the Continue action after the same two-denial prompt. Captured result: `docs/tasks/DBG-1007/emulator-results/AC-011-denied.xml`. |
| `:app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest' --rerun-tasks` | PASS | Forced execution on the current worktree; policy unit tests and production Kotlin compilation passed. |
| `:app:compileDebugAndroidTestKotlin :app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest' :app:assembleDebug` | PASS | Final test-source state compiled, targeted policy unit test task and Debug APK build completed successfully. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` | PASS | On `emulator-5554`, verified the unavailable prompt, dismissed it, confirmed the camera remained at fallback, and performed an emulator swipe in the visible map area; the camera target changed (AC-006/AC-007). |
| Emulator current-location success path (AC-001, AC-003) | PASS | On commit `130bf200c2874d5ec545cc842372efb1eee3c9cd`, after selecting `com.example.taoyuangutter.test` as the mock-location app, its Java receiver injected a framework GPS test-provider fix at `25.030000,121.500000`. The Fused callback centered the form map within 100 m. The test confirmed `currentLat/currentLng`, node coordinate fields, and waypoint coordinates remained unchanged. Evidence: `emulator-results/AC-001-AC-003-fused-mock.md` and `-logcat.txt`. |
| Permanent-denial permission UI (AC-005) | PASS | AC-004 evidence confirms the Continue action after two denials; the AC-011 denied-return emulator test confirms the same unavailable prompt exposes Settings and successfully opens app Settings. The fixed Android 14 AVD required shell taps for native permission dialogs. |
| Manual-pan callback race (AC-009) | PASS | On commit `130bf200c2874d5ec545cc842372efb1eee3c9cd`, the test started acquisition without a fix, manually panned the map, then injected a GPS test-provider fix through the selected test APK. The late callback left the camera at the manual target and did not show the unavailable prompt. Evidence: `emulator-results/AC-009-delayed-fused-callback.md`, `.xml`, and `-logcat.txt`. |
| Other emulator UI cases | PASS | AC-004 denial/retry, AC-006 no-fix recovery and prompt, AC-007 prompt dismissal/manual map movement, AC-008 new-point gate, AC-010 coarse-only permission, AC-005 permanent-denial actions, and AC-011 both Settings-return outcomes passed. |
| CI | NOT VERIFIED | No CI run was available from this worktree. |

The first Gradle attempt could not locate Java through the shell PATH. Validation then ran with Android Studio's bundled JBR. Emulator UI validation used the configured runtime-capable Maps setup. Successful-location and delayed-callback checks used the selected test APK's framework GPS test provider; no production dependency was added. We did not test a physical device, per the user's instruction.

## Remaining Verification Scope

Developer emulator coverage for AC-001 through AC-011 is complete, including successful Fused callback and delayed-callback evidence. Commit the task evidence, then run independent Verification on the fixed committed implementation revision `130bf200c2874d5ec545cc842372efb1eee3c9cd`. CI remains `NOT VERIFIED`. No physical-device test is in scope.
