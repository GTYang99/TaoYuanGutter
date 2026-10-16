# Execution Report

## Implementation

- Restored the three import tabs in the approved order and default selection.
- Kept Recent Access on the existing no-query recent-saved request.
- Restored Nearby GPS lookup and the top-right current-location button using the shared `closestNodeDetails` endpoint with `lng`/`lat`.
- Re-query each tab on entry; Search re-runs only its last submitted XY_NUM value.
- Routed successful results from all tabs to the shared candidate-map callbacks and kept prior rows while refresh overlays are visible.
- Redacted `lng` and `lat` values from HTTP debug log lines; did not restore historical repository logs containing GPS coordinates.
- Added an instrumentation test for tab order/default, nearby entry and location-button refresh, blank search prompt, and search re-query on return. It caught and fixed a duplicate initial recent-saved request.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | PASS | 98 tests completed, no failures |
| `:app:assembleDebug` | PASS | Debug APK built at `app/build/outputs/apk/debug/app-debug.apk` |
| `:app:compileDebugAndroidTestKotlin` | PASS | Android instrumentation test sources compiled |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` | PASS | 1 targeted instrumentation test passed on emulator `emulator-5554` (Android 16). It verifies candidate callbacks in all three modes, radio selection and import enablement, selection clearing on tab switch, and import callback handoff. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportWaypointMapMarkerTest` | PASS | 1 instrumentation test passed on emulator `emulator-5554` (Android 16). It exercises the activity's actual `GoogleMap` marker objects: candidate position/title/tag/z-index, selected marker and visibility highlight, restoring candidates after deselection, and marker cleanup. It does not test physical GPS or visually inspect map rendering. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Updated instrumentation test sources compiled successfully. |
| `git diff --check` | PASS | No whitespace errors |
| Physical GPS permission, current-location marker/camera, and map import handoff | NOT VERIFIED | Emulator UI test uses a fake location callback; live GPS and authenticated backend flow were not exercised |

## Environment and Scope Notes

- Gradle initially could not access the wrapper lock in `~/.gradle` under the workspace sandbox. The planned build/test checks were then run successfully with the Android Studio bundled JDK and approved Gradle cache access.
- At the time of the original feat-1015 validation, no CI configuration or CI run was available. The incoming feat-1006 branch adds `.github/workflows/android-ci.yml`; its recorded CI run does not cover the combined branch. Independent Verification for the earlier committed production/test revision is recorded in `verification.md`; the map-marker test added afterward is additional developer evidence, not an independent verification result.
- Existing user change `gradle/libs.versions.toml` (`agp = "9.4.1"`) was preserved and is excluded from this task's commit.

## Build Artifact

- Feature implementation commit: `231ba769a3bf5bece73422848fbf298119d0901c`
- Test coverage commit: `75d1e3f`
- Variant: `debug`
- Package: `com.example.taoyuangutter`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

## Scoped Callback Coverage

- Updated instrumentation test: `ImportExistingWaypointUiTest.tabsKeepOrderDefaultAndRefreshEachSubmittedModeOnReturn`.
- On emulator `emulator-5554` (Android 16), the test passed after covering candidate callbacks for all three modes, selection and radio highlight, clearing selection on tab changes, and the selected-item import callback.
- Physical XQ-AU52 testing is NOT VERIFIED per the user's instruction not to install or operate the test APK on that device.
- `ImportWaypointMapMarkerTest.candidateMarkersRenderSelectionHighlightsAndCleanupOnMap` passed on `emulator-5554` (Android 16). It checks Google Maps marker objects and selection visibility but does not replace a visual map run or the declined physical GPS test.

## Infrastructure Follow-up (2026-10-15)

- Both follow-up test commands ran at `b0eb11adab6f1e57914877f5081b9b8ade8dc0cc`; the implementation and Android test sources match the committed feature/test revision `399b37d`.
- Created dedicated Pixel 7 AVDs `Feat1015_Test` and `Feat1015_Test_API36`, both Android 16 / API 36 and isolated from the shared `Medium_Phone` AVD. The second uses the installed `google_apis_playstore` image and ran as `emulator-5558`; the first was stopped after its launcher became unresponsive during first boot. The AVD configurations remain available for this task.
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportWaypointMapMarkerTest` — PASS on `Feat1015_Test_API36` (`emulator-5558`, Android 16/API 36). Gradle reported `BUILD SUCCESSFUL`; the targeted map-marker test passed.
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` — NOT VERIFIED on the dedicated AVDs. First invocation on `Feat1015_Test` failed at the first tab-label assertion (`ImportExistingWaypointUiTest.kt:146`) with `RootViewWithoutFocusException`; a screenshot showed a Pixel Launcher ANR dialog. The one permitted retry on `Feat1015_Test_API36` failed at the same assertion with the same Espresso focus exception; the launcher retained focus and no unrelated instrumentation suite was running. The retry limit is exhausted for this evidence cycle.
- Neither dedicated AVD test changes the physical-device scope: GPS permission/current-location behavior and authenticated backend behavior remain NOT VERIFIED per the user's instruction not to use XQ-AU52.
- During the original feat-1015 implementation, the working-tree change in `gradle/libs.versions.toml` was preserved and excluded from those commits. The later, separately approved branch integration includes the source branch's committed AGP `9.4.1` value.

## Branch Integration (2026-10-15)

- Merged `feat/feat-1006-wmts-offline-data` at `0c10708037449e18c98093f07c32216a6610bd4a` into `feat/feat-1015-restore-nearby-waypoints` with merge commit `eb536d4d077f6157a048d69f36d9735fdad237c5`.
- Resolved the `GutterApiService.kt` conflict by retaining both nearby query overloads and the source branch's selectable backend service. Kept location-coordinate redaction active in the shared `BackendHttpClient` logging interceptor.
- Included the source branch's AGP `9.4.1` version as explicitly approved by the user.
- No build or test was run against the combined revision. It remains pending independent verification and CI; the source branch's CI result applies only to its verified revision.

## AC-004 Focused Test (2026-10-16)

- Added `ImportExistingWaypointUiTest.searchWaitsForSubmissionAndRepeatsLastSubmittedXyNumOnReturn`, which covers only AC-004: opening Search without a submitted query keeps the prompt and sends no XY_NUM request; submitting `A001` sends exactly `A001`; returning from Recent to Search submits the last value again.
- The test uses the existing `AppVersionUpdateTestHooks` seam to return the installed app version and avoid the unrelated foreground update dialog. No production source was changed.
- `:app:assembleDebugAndroidTest` — PASS using JDK 21; Android test APK compiled and packaged.
- Direct instrumentation command — PASS, 1/1 in 4.636 seconds on `UI002_API28` / `emulator-5556` (Android 9/API 28):
  `adb -s emulator-5556 shell am instrument -w -r -e class com.example.taoyuangutter.ImportExistingWaypointUiTest#searchWaitsForSubmissionAndRepeatsLastSubmittedXyNumOnReturn com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner`
- The emulator's Wi-Fi was off only for the test and restored to its original enabled state afterward; the temporary AVD was then shut down. The successful instrumentation command explicitly targeted only `emulator-5556`.
- A prior `connectedDebugAndroidTest` invocation was interrupted after Gradle reported that it would use `UI002_API28`, `Medium_Phone`, and physical `XQ-AU52`, despite the injected serial argument. The local XQ-AU52 output directory contains device information only and no test result or test log; no direct ADB command targeted that device. Whether Gradle began installing an APK there cannot be confirmed without contacting the device, so no claim is made about its install state. The emulator test result from that aborted invocation is not used as AC-004 evidence.
- The existing combined UI method also failed before Search at line 156 with an ambiguous `rbSelect` matcher on the API 28 emulator. That result does not contradict the focused AC-004 pass; the focused method avoids candidate selection and map assertions.
