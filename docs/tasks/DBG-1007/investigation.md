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
