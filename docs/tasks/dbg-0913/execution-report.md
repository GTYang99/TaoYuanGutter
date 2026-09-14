# Execution Report

## Implemented
- Added `GutterFormExitRules` to make the no-data cant-open confirmation and exit-warning decisions explicit and unit-testable.
- `GutterBasicInfoFragment` now opens the existing cant-open confirmation only when its actual clear targets contain data: cover thickness, depth, top width, material, broken, hanging, silt, or photo slot 2/3.
- `GutterFormActivity` now checks the existing basic-field and photo validators before a non-view-mode Activity exit. The single 「確認」 button retains the existing draft-sync/result/finish path.
- The inspect-edit-preview early-return path remains before the exit check.

## Validation
- PASS — `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterFormExitRulesTest`
- PASS — `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`
- PASS — Android 14 emulator: `GutterCantOpenUiTest` (5 tests), including no-data confirmation suppression and existing confirm/cancel/snapshot regressions.
- PASS — Android 14 emulator: `GutterFormExitUiTest` (4 tests), covering toolbar back, system back, completed virtual form without warning, and edit-to-preview return.
- PASS — `git diff --check`.

## Limitations
- Remote CI is not configured or available in this repository; local build and emulator evidence are recorded instead.

## Follow-up Fix: ISS-DBG-0913-002
- Fixed the regression where initial 「否／無」 selections and the 明溝 system-forced cover thickness `0` were treated as user-entered clearable content.
- PASS — real new-form Android 14 emulator `GutterCantOpenUiTest` (5 tests, 0 failures), including the first selection with no manual test-field cleanup.
- PASS — full `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`.

## Follow-up Fix: ISS-DBG-0913-004
- `validateRequiredFields()` 現在會在非虛擬點流程先驗證側溝形式、側溝位置（經緯度）與測量座標編號。
- 一般側溝會驗證溝蓋板厚度；明溝只略過厚度，仍驗證材質、深度、頂寬與狀態欄位；無法開蓋維持既有細節欄位免填。
- 新增 `GutterFormExitUiTest` 回歸案例，覆蓋上述五個新增審核欄位、明溝的深度必填，以及無法開蓋的測量座標編號必填。

### Validation
- PASS — `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`。
- PASS — Android 14 emulator (`emulator-5554`) `GutterFormExitUiTest`: 7 tests, 0 failures；connected-test XML 已記錄結果。
- PASS — Android 14 emulator (`emulator-5554`) `GutterCantOpenUiTest#snapshotSurvivesConfigurationRecreation`: `OK (1 test)`。
- PASS — Android 14 emulator (`emulator-5554`) `GutterCantOpenUiTest#emptyClearableFieldsDoNotShowCantOpenDialog`: `OK (1 test)`。
- PASS — `git diff --check`。
