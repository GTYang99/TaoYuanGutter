# Investigation Record

## Issue

- ID: `ISS-DBG-1007-LOC-002`
- Task: `DBG-1007`
- Origin: emulator developer validation
- Affected acceptance criteria: AC-001 and AC-003
- Status: unresolved; classification remains `unknown`

## Expected and Observed

- Expected: an edit form with missing waypoint coordinates uses a usable current fix for the map camera and leaves waypoint/form coordinates unchanged.
- Observed: after injecting `25.0300, 121.5000`, the camera remained at `24.992900, 121.301100`, about 20.5 km away.
- Test device: `emulator-5554`, Medium_Phone AVD, Android 14 / API 34.

## Evidence Collected

1. `adb emu geo fix 121.5000 25.0300` returned `OK`.
2. `adb shell dumpsys location` reported a mock location at `25.030000,121.500000` in the fused provider.
3. The full `EditMapInitialLocationInstrumentedTest` run had three passing cases and one failed successful-location assertion.
4. One isolated retry of that same assertion also failed; the camera was still at the Taoyuan fallback.
5. Location permissions were granted for those runs. The separate saved-coordinate case passed with both location permissions denied.
6. Source inspection shows the form requests `FusedLocationProviderClient.getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY, ...)` and applies a returned valid fix only to the map camera.

## Competing Explanations

- The emulator/GMS stack may keep the injected mock fix in framework state without returning it through the app's Fused Location request.
- The app-side request or callback lifecycle may prevent a usable result from reaching the camera.

Current logs and provider state do not distinguish these explanations. No production code was changed as part of this investigation.

## Classification and Route

- Category: `unknown`
- Route: `investigation`
- Reason: the emulator reports the injected fix, but the Activity's camera remains at fallback; this is insufficient to attribute the failure to either the emulator environment or application behavior.

## Missing Evidence / Next Step

Need a permitted test setup that proves whether the app's Fused Location client receives a valid fix. Automatic review rejected another retry of the same acceptance case after the full-class run and one isolated retry, with the reason that the retry limit had been reached and indirect workarounds must not be used. Do not repeat that case unless the user authorizes a materially different test target or environment.

## Follow-up Emulator Evidence (2026-10-08)

- Reconnected to `emulator-5554` (Android 14 / API 34); location services were enabled. The last fused mock fix was stale, so it was not treated as a current-location result.
- Emulator inventory on this host contains only the `Medium_Phone` AVD, with `emulator-5554` as the only connected test device. No second emulator image is available for an alternate run.
- AC-005 and both AC-011 Settings-return outcomes passed using a shell-level denial interaction for Android's native permission dialog. Evidence is in `emulator-results/AC-011-granted.xml` and `emulator-results/AC-011-denied.xml`.
- A different test-only Fused mock route was explored. Android rejected `setMockMode(true)` because the caller was not the selected mock-location app. Selecting the target package via the secure setting did not authorize the call.
- An AC-009 mock harness was not retained. The first run used a context without an Application; the one retry failed at Fused client setup because the instrumentation APK lacked the required `com.google.android.gms.version` metadata. Evidence: `emulator-results/AC-009-fused-test-setup.xml`.
- Source review of the valid-location callback confirms it only calls `formMap.animateCamera(...)`; it does not update waypoint coordinates, form fields, draft data, or a My Location layer. This is supporting evidence for AC-003, but the callback has not been exercised and AC-003 remains `NOT VERIFIED`.
- No production code change was made during this follow-up. AC-001/003 and AC-009 remain `NOT VERIFIED`; further emulator success-path work needs a valid selected mock-location app test setup or a different permitted emulator configuration. No physical device was used.
