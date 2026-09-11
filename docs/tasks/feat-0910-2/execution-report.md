# Execution Report

## Implementation

- Branch: `uiFix/填寫順序`
- Commit: pending follow-up fix
- Scope: 基本資料表單順序、測量狀態群組、拍照按鈕文案、新建 RadioGroup 預設值與 UI 測試
- Changed source:
  - `app/src/main/res/layout/fragment_gutter_basic_info.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- Changed tests:
  - `app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`

## Debug Re-implementation

- Issue: `ISS-0910-2-02`
- Fix: detach each ordered View from its actual current `ViewGroup` before adding it to `formContent`.
- Fix commits: `7afc6b2`, `e230f44`, `e94c166`

## Follow-up Field and Keyboard Fixes

- Restore the measurement-coordinate title by moving its outer row instead of the inner `0dp` weighted container.
- Reapply the form-panel IME translation from both window-insets dispatch and animation completion so it returns with the keyboard.
- Request a form hierarchy remeasure after each photo card changes visibility.
- Map field title rows to their individual virtual-section children so each title remains adjacent to its matching control.
- No photo-upload request fields were changed. The reported `xy_num` failure is recorded as a backend schema/API issue.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors reported. |
| XML parse of `fragment_gutter_basic_info.xml` | PASS | Layout is well-formed. |
| `./gradlew compileDebugAndroidTestKotlin --no-daemon` | NOT VERIFIED | Environment has no available Java runtime; Gradle stopped before compilation with `Unable to locate a Java Runtime`. |
| `JAVA_HOME=.../Android Studio.app/.../Home ./gradlew assembleDebug testDebugUnitTest --no-daemon` | PASS | Debug APK assembled and unit tests completed successfully with Android Studio's bundled JDK. |
| Android Studio build/install on Sony XQ-AU52 | PASS | Android Studio reported `Install successfully finished`; installed revision includes the corrected field-row mapping. |
| Real-device form entry and three-photo state after fix | PASS | User confirmed on the installed revision that the initial field mapping and the state after capturing three photos are correct. |
| `connectedDebugAndroidTest` after follow-up fixes | NOT VERIFIED | First run exposed two implementation regressions; follow-up run could not start because devices disconnected. |

## Limitations

- Android unit/UI tests and lint remain `NOT VERIFIED` until a JDK/Android build environment is available.
- Connected UI regression remains pending after the follow-up fixes; a run was blocked by disconnected devices.

## ISS-0910-2-09 debug reruns (2026-09-11)

- Targeted command: `connectedDebugAndroidTest` for `GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults`.
- Same device/revision run 1: PASS, 1/1, about 5 seconds.
- Same device/revision run 2: FAIL, 1/1, 45.5 seconds, `NoActivityResumedException`.
- The result is recorded as flaky lifecycle evidence; it is not a production-fix validation pass.

## Acceptance Criteria Evidence

- AC-001/AC-002: status group and runtime order are implemented; UI test checks labels and relative positions.
- AC-003/AC-004: new-form defaults and existing-value preservation are covered by `GutterBasicInfoUiTest`.
- AC-005: three slot-specific button labels are asserted by UI test.
- AC-006: existing IDs, data keys, photo slots, and mode-control methods are retained; targeted regression execution is pending Verification.
