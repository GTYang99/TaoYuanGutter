# API 34 Emulator Revalidation — AC-001

## Scope and Environment

- Acceptance criterion: AC-001.
- AVD: `Codex_Debug1021_API34`, Android Emulator API 34, serial `emulator-5554`.
- App package: `com.example.taoyuangutter`.
- Build variant and artifacts: `debug`; `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Test class: `com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest`.
- The AVD had no physical-device test involvement. Instrumentation ran via explicit `adb -s emulator-5554` commands.

## Test Results

| Scenario | Result | Evidence |
|---|---|---|
| Required fields missing while all required photo URIs are usable | PASS | After adding the required gutter-type fixture value, the focused test returned `OK (1 test)`; dialog, button label/enabled state, and zero submissions were asserted. |
| Required fields complete while photos are missing | PASS | Passed in the three-case instrumentation run after the app-update prompt was disabled through the existing test hook. |
| Required fields and photos both missing | PASS | Passed in the same three-case instrumentation run. |

All cases assert the “資料/照片不足” dialog, dismiss it with “確定”, assert `btnSubmitGutter` displays “新增側溝” and is enabled, and assert the host submission count remains zero.

## Commands and Outcomes

- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest` — **BUILD SUCCESSFUL**; the focused JVM policy suite reported 6 tests, 0 failures.
- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL** after adding deterministic update-prompt setup to the instrumentation fixture.
- `./gradlew :app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL** after correcting the data-only scenario's missing `SPI_TYP` fixture value.
- App and test APK installation targeted `emulator-5554` explicitly and completed successfully.
- `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` — 3 tests run; the photo-only and combined-incomplete cases passed. The data-only fixture initially stopped at the existing “請先選擇側溝類型後再送出” guard because its test data omitted `SPI_TYP`.
- After adding `SPI_TYP=1` to that fixture, the focused data-only command `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest#missingRequiredFieldsRestoresAddButtonAfterDialogDismissal com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner` returned **`OK (1 test)`** in 7.032 seconds.
- An earlier run on the fresh AVD showed the app-update prompt. The test now uses the existing `AppVersionUpdateTestHooks` seam to return the installed version, preventing live version-check behavior from interfering with this form test.

## Limitations

- The combined and photo-only cases passed in the three-case run before the final data-only fixture-only edit; the data-only case passed in a focused run after that edit. Production code was unchanged between these results.
- CI and independent Verification for the new committed revision remain pending.
- The original implementation predates the plan review; the retrospective review records this workflow deviation without changing its chronology.
