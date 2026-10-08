# Investigation Record

## Issue

- ID: `ISS-DBG-1007-LOC-002`
- Task: `DBG-1007`
- Origin: emulator developer validation
- Affected acceptance criteria: AC-001, AC-003, and AC-009
- Status: resolved; category `environment`

## Initial Failure

On `emulator-5554` (Medium_Phone AVD, Android 14 / API 34), `adb emu geo fix 121.5000 25.0300` returned `OK` and `dumpsys location` showed a fused mock fix, while the form camera remained at `24.992900,121.301100`. This input did not prove that the Activity's current-location callback received the fix.

The first test-only `FusedLocationProviderClient.setMockMode(true)` setup was rejected because the caller had not been selected as the mock-location app. Setting a secure setting directly did not grant the mock-location app-op. After the user explicitly authorized selecting the test APK, the first instrumentation-process client call failed in Google Play Services with `Unknown calling package name 'com.example.taoyuangutter.test'`. The instrumentation runner was executing in the target app process while identifying itself with the test package. This points to a caller identity mismatch; it is an inference from the binder error and the two package identities.

## Resolution and Runtime Evidence

1. Installed `com.example.taoyuangutter.test` and selected it in Android Developer Options > Select mock location app. Android then reported `MOCK_LOCATION: allow` for that package.
2. Added a test-only Java `BroadcastReceiver` to the instrumentation APK. A shell broadcast starts it under the selected test APK's own UID; it registers and controls the framework GPS test provider using Android APIs. The helper does not depend on Google Play Services classes or the target APK classpath.
3. Ran `missingCoordinateEditWithFusedMockLocationCentersCameraOnly` with a usable fix at `25.030000,121.500000`. The test passed, the camera reached the fix, and it verified `currentLat`/`currentLng`, `NODE_X`/`NODE_Y`, and the waypoint's nullable coordinates remained unchanged. Evidence: `emulator-results/AC-001-AC-003-fused-mock.md`.
4. Ran `manualPanBeforeFusedMockCallbackKeepsManualCameraTarget`. The test started location acquisition without a fix, panned the form map, then injected the fix. It passed with the manual camera target retained and no unavailable prompt. Evidence: `emulator-results/AC-009-delayed-fused-callback.md`, `.xml`, and `-logcat.txt`.

The test APK is removed by the Gradle connected-test task after execution, which removes its mock-location app-op. Reinstalling it requires selecting it again before another run. This is an emulator setup detail, not a production-app defect.

## Classification and Route

- Category: `environment`
- Resolution: closed
- Reason: The initial mock input and instrumentation caller identity did not exercise the application's successful Fused callback path. The separately selected test APK's framework GPS provider did; both the successful-location and late-callback behaviors passed on the emulator.
- Next workflow action: commit the test harness and evidence, then independent verification. CI remains pending.

No production source changes were made during this investigation. No physical device was used.
