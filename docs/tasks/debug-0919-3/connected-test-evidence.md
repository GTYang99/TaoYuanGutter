# Connected Test Evidence

## Device
- Serial: `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`
- Model: Sony XQ-AU52 (`XQ_AU52`)
- Android: 12 / API 31
- Physical size: 1080x2520
- Density: 420
- Available IME: Gboard Latin IME and system voice IMEs
- Package under test: `com.example.taoyuangutter`
- Foldable: No

## In-scope regression test
- Production revision: `9ed2c7c`
- Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`
- Result: PASS, 12/12
- Scope: MainShell activity lifecycle, map/dashboard tab switching, bottom navigation layout, and existing shell controls.

## Full-suite observation
- Command: `./gradlew :app:connectedDebugAndroidTest`
- First run: 37/38 passed. `Debug0919ImportedWaypointUiTest.importedVirtualWaypointKeepsLocationAndVirtualToggleLockedAcrossEditAndRecreation` failed with `androidx.test.espresso.NoActivityResumedException` while asserting imported controls.
- Classification: unrelated environment/test sequencing issue; no changed file is in the failing test's flow.
- Permitted retry: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.Debug0919ImportedWaypointUiTest`
- Retry result: PASS, 1/1.

## Foldable acceptance
- AC-001 and AC-002 were not executed because this device is not foldable and no approved compact-height equivalent was available.
