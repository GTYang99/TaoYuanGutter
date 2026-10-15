# AC-001 / AC-003 Fused Location Success Evidence

- Result: **PASS** for developer emulator validation.
- Device: `emulator-5554`, Medium_Phone AVD, Android 14 / API 34.
- Mock source: `com.example.taoyuangutter.test`, selected through Developer Options > Select mock location app. Android reported `MOCK_LOCATION: allow`.
- Injection: the selected test APK's Java `BroadcastReceiver` added a framework GPS test provider and injected `25.030000, 121.500000`. Logcat recorded `Mock provider action completed: enable`.
- Test: `EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithFusedMockLocationCentersCameraOnly`.
- Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithFusedMockLocationCentersCameraOnly`.
- Recorded result: Gradle `BUILD SUCCESSFUL`; instrumentation XML reported 1 test, 0 failures, 0 errors; elapsed 24.250 seconds (2026-10-08 10:07 Asia/Taipei).
- Concise start/completion and provider-action log: `AC-001-AC-003-fused-mock-logcat.txt`.

The test waited for the form-map camera to reach the injected fix within 100 meters. It also asserted `currentLat` and `currentLng` remained zero, `NODE_X` and `NODE_Y` remained blank, the session waypoint retained null latitude/longitude, and the unavailable-location prompt was absent. This executes the successful callback and verifies viewport-only use for AC-001 and AC-003.

The Gradle connected-test task removed the instrumentation APK after execution. Its mock-app selection therefore must be repeated after reinstalling the test APK before another connected-test run.
