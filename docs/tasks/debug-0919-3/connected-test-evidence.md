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

## Manual IME Smoke Measurement on Non-foldable Device
- Entry: installed `app/build/outputs/apk/debug/app-debug.apk`, opened `LoginActivity` → `離線填寫表單`, entered the no-ditch mode, and tapped a map coordinate.
- Before focus: `noDitchPanel` bounds were `[0,1498][1080,2394]`; note field and reset/submit controls were visible.
- After focusing `etNoDitchNote` and entering `test`: the IME reduced the app-visible root to height 1572 and `noDitchPanel` bounds were `[0,676][1080,1572]`; note field bounds were `[53,1016][1027,1408]`, and reset/submit controls were `[53,1445][1027,1530]`.
- Observed result: the panel bottom aligned to the visible IME boundary and controls remained visible; no submit action was performed.
- Limitation: this is ordinary Android 12 runtime evidence only. It does not verify fold posture or the keyboard-dismissed restored position, so AC-001/AC-002 remain `NOT VERIFIED`.
