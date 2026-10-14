# AC-009 Delayed Location Callback Evidence

- Result: **PASS** for developer emulator validation.
- Device: `emulator-5554`, Medium_Phone AVD, Android 14 / API 34.
- Mock source: selected instrumentation APK `com.example.taoyuangutter.test`, using its framework GPS test provider.
- Test: `EditMapInitialLocationInstrumentedTest#manualPanBeforeFusedMockCallbackKeepsManualCameraTarget`.
- Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#manualPanBeforeFusedMockCallbackKeepsManualCameraTarget`.
- Result XML: `AC-009-delayed-fused-callback.xml` (1 test, 0 failures, 0 errors; 2026-10-08 10:09 Asia/Taipei).
- Relevant log: `AC-009-delayed-fused-callback-logcat.txt`; the receiver completed the initial no-fix setup, then the injected-location action, then cleanup.

The test started the location attempt without a fix, manually panned the form map, then injected `25.030000, 121.500000`. It asserted the late callback did not move the camera from the manual target, that the manual target remained over 100 meters from the injected location, and that no unavailable-location prompt appeared.
