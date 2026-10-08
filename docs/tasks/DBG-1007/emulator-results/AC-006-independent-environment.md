# AC-006 Independent Emulator Run

- Revision under test: `9e35233f4b94a67344f61ca88fe7f4c49b7eff42`
- Device: `emulator-5554`, Medium_Phone AVD, Android 14 / API 34
- Preconditions: Rebooted the emulator normally without wiping its data. Before running the case, `dumpsys location` reported `last location=null` for the fused, GPS, and network providers. No mock fix was injected in this run.
- Command: `./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt`
- Expected: With fine permission granted and no usable fix, the form waits for its bounded acquisition/reacquisition attempts, then shows the location-unavailable prompt and retains the Taoyuan fallback without writing coordinates.
- Actual: **PASS**. Instrumentation completed one test with zero failures in 54.298 seconds. The test asserted that location flow ended, the unavailable prompt appeared, map camera remained at fallback, and waypoint/form coordinates remained empty. Source review confirms 25 seconds per attempt and at most two attempts.
- Evidence: `AC-006-independent.xml`; filtered runner lines in `AC-006-independent-logcat.txt`.
