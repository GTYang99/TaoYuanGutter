# Independent Emulator Verification — AC-001

- Commit under test: `2cfa7c801f75b06a8932dd7aa6459d9aa8f8dc53`
- Device: `DBG1017_API34_Camera` AVD, `emulator-5554`, `sdk_gphone64_arm64`, Android 14 / API 34
- Build source: clean `git archive HEAD` at the commit above, extracted under `/private/tmp/debug-1021-verify-2cfa7c8`; the working tree's version properties and release artifact were excluded.
- Preconditions: required start/end coordinates and gutter type supplied; required data/photo intentionally incomplete.
- Test: `Debug1021IncompleteSubmitUiTest.incompleteRequiredDataRestoresAddButtonAfterDialogDismissal`
- Command: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner`
- Expected: show the “資料/照片不足” dialog; after “確定”, `btnSubmitGutter` reads “新增側溝”, is enabled, and no submission is made.
- Actual: `OK (1 test)` in 5.624 seconds. Espresso assertions confirmed the dialog, restored label and enabled state, and zero submission count.
- Raw instrumentation output: [AC-001-verification-2cfa7c8.txt](AC-001-verification-2cfa7c8.txt)

The Gradle connected-test task compiled and packaged both APKs but discovered the AVD and a connected physical device despite the requested serial, so it was stopped before accepting its multi-device run. The test APKs were then installed explicitly with `adb -s emulator-5554`, and instrumentation was invoked only on that AVD. No physical-device result is counted.
