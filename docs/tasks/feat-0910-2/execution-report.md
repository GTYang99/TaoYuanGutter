# Execution Report

## Implementation

- Branch: `uiFix/填寫順序`
- Commit: `93cacc7`
- Scope: 基本資料表單順序、測量狀態群組、拍照按鈕文案、新建 RadioGroup 預設值與 UI 測試；另修正 `ISS-0910-2-09` 的表單啟動競態
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

## ISS-0910-2-09 Implementation

- Remove the XML-declared `SupportMapFragment` from the form layout so `setContentView()` no longer synchronously creates Maps during Activity startup.
- Keep the existing form pager and controls on their original initialization path to preserve field behavior and avoid a second lifecycle race.
- Create the map fragment after `onPostResume()` on the view queue, with lifecycle/state-saved guards and restored-fragment reuse before calling `getMapAsync()`.
- Guard photo-state synchronization for the short interval before the pager is available.

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
| `JAVA_HOME=.../Android Studio.app/.../Home ./gradlew assembleDebug --no-daemon` | PASS | Debug APK assembled with Android Studio's bundled JDK. |
| `JAVA_HOME=.../Android Studio.app/.../Home ./gradlew testDebugUnitTest --no-daemon` | NOT VERIFIED | 49 tests ran; one pre-existing unrelated failure remains in `MainMapLoadIndicatorStateMachineTest.minimumZoomMatchesGutterLayerRequirement`. |
| Android Studio build/install on Sony XQ-AU52 | PASS | Android Studio reported `Install successfully finished`; installed revision includes the corrected field-row mapping. |
| Real-device form entry and three-photo state after fix | PASS | User confirmed on the installed revision that the initial field mapping and the state after capturing three photos are correct. |
| Targeted `GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults` on XQ-AU52 | PASS (1 run) | Same fixed revision completed the new-form assertions successfully. Two earlier runs with the broader deferral exposed regressions and were corrected before this pass. |
| `GutterBasicInfoUiTest` class on XQ-AU52 | NOT VERIFIED | The device disconnected during APK reinstall; Gradle reported `adb: device offline`. |

## Limitations

- The unrelated unit-test failure remains open and must not be attributed to this fix.
- Full connected regression remains pending because XQ-AU52 disconnected during the class run; the targeted new-form test has one post-fix PASS.

## ISS-0910-2-09 debug reruns (2026-09-11)

- Targeted command: `connectedDebugAndroidTest` for `GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults`.
- Earlier broad-deferral runs: FAIL with `NoActivityResumedException` and an uninitialized pager; both regressions were removed by restoring the original pager initialization path.
- Final narrowed fix run: PASS, 1/1, on XQ-AU52.
- The class-level rerun was blocked by `adb: device offline`; full verification remains pending.

## ISS-0910-2-11 Implementation and Validation

- Restored `applyVirtualModeUi(isVirtual)` state-dependent behavior:
  - virtual on: hide the page switch bar and disable ViewPager interaction;
  - virtual off: restore the page switch bar and enable ViewPager interaction.
- Added `GutterBasicInfoUiTest.turningVirtualPointOffRestoresNormalFormInteraction`, covering virtual on → off, checkbox state, page switch visibility, and ViewPager interaction.
- `JAVA_HOME=.../Android Studio.app/.../Home ./gradlew assembleDebug compileDebugAndroidTestKotlin --no-daemon`: PASS.
- `git diff --check`: PASS.
- Connected execution: NOT VERIFIED; `adb devices` currently reports no connected device.
- Virtual-mode field correction: hide `cbCantOpen` while virtual mode is active, leaving only the measurement-status 「待架站」 option; restore it when virtual mode is turned off.
- The focused UI regression now verifies `cbCantOpen` hidden in virtual mode and visible after turning virtual mode off.

## Acceptance Criteria Evidence

- AC-001/AC-002: status group and runtime order are implemented; UI test checks labels and relative positions.
- AC-003/AC-004: new-form defaults and existing-value preservation are covered by `GutterBasicInfoUiTest`.
- AC-005: three slot-specific button labels are asserted by UI test.
- AC-006: existing IDs, data keys, photo slots, and mode-control methods are retained; targeted regression execution is pending Verification.
