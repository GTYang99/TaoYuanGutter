# Execution Report

## Implementation

- Task: `DBG-1007` — center the form map on the current location only when editing an existing point without saved coordinates.
- Branch: `codex/DBG-1007-minimap-location`
- Implementation commit: `5114269122aa762c28f0093904c145e03ce3bd72`
- Emulator test and evidence commit: `1ad0ab7` (`test(DBG-1007): verify edit map location on emulator`).
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
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#savedCoordinateEditWithLocationDeniedDoesNotRequestPermission` | PASS | On `emulator-5554`, revoked both foreground location permissions before launch; saved waypoint remained the camera target, location flow stayed inactive, and the unavailable prompt was absent (AC-002). Result XML: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml`. |
| Emulator current-location success path (AC-001, AC-003) | NOT VERIFIED | `adb emu geo fix 121.5000 25.0300` returned `OK`, and `dumpsys location` reported a mock fix in the fused provider. The instrumented camera assertion still observed the Taoyuan fallback (`24.992900,121.301100`), about 20.5 km from the injected point. The full class run and one isolated retry both failed this assertion. The evidence does not distinguish emulator/GMS mock delivery from an app-side issue; investigation is open. Automatic review rejected another retry of the same acceptance case, so testing stopped. |
| Remaining emulator UI cases | NOT VERIFIED | Permission retry/permanent denial and Settings return (AC-004, AC-005, AC-011), coarse-only runtime permission (AC-010), manual-pan callback race (AC-009), and prompt-dismiss/manual interaction (AC-007) were not exercised. AC-006's no-fix fallback/prompt and AC-008's new-point gate passed. |
| CI | NOT VERIFIED | No CI run was available from this worktree. |

The first Gradle attempt could not locate Java through the shell PATH. Validation then ran with Android Studio's bundled JBR. Emulator UI validation used the configured runtime-capable Maps setup. We did not test a physical device, per the user's instruction.

## Remaining Verification Scope

Use a fixed committed revision and the emulator to cover the remaining cases in `plan.md`. AC-001/003 need evidence from a location fix the app's Fused Location client can consume; the injected emulator mock did not move the camera. See `investigation.md` for the unresolved classification. No physical-device test is in scope.
