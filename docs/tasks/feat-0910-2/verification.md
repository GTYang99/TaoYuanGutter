# Verification Report

## Verification Basis

- Task: `feat-0910-2`
- Branch: `uiFix/填寫順序`
- Verified revision: `b897de0`
- Requirement, analysis, approved plan and plan review were read before verification.
- Verification is read-only for production code; only task evidence and state are updated.
- No CI result was available in the repository or task artifacts.

## Response to Verification Questions

- Scope deviation: the `GutterFormActivity.kt` IME/insets change was added directly in response to the user's same-task real-device report that the form did not follow the keyboard. It changes window-insets presentation only; it does not change API fields, persisted data, photo slots, or upload metadata. The change is therefore an explicit follow-up to the task's observed UI defect, not unrelated refactoring. Release review should still acknowledge the expanded affected-file scope.
- Connected-test failure: the available `GutterCantOpenUiTest` report is retained as `NOT VERIFIED`/environment-pending. There is not enough evidence to attribute the Espresso visibility failure to the field-order implementation, and the test cannot be rerun in the current no-JDK environment.
- Manual scope: the user's real-device result verifies the reported field-mapping regression (initial display and after three photos), but does not replace the missing edit/import/draft and full regression evidence.

## Evidence Collected

| Check | Result | Evidence |
|---|---|---|
| Working tree and revision | PASS | Verification started from HEAD `b897de0` with no pre-existing working-tree changes. |
| Git whitespace check | PASS | `git diff --check HEAD~8..HEAD`. |
| Layout XML parse | PASS | `fragment_gutter_basic_info.xml` parsed successfully with Ruby REXML. |
| Android Studio install/manual device check | PASS | Execution report records successful install and manual initial/three-photo form check on Sony XQ-AU52. |
| Task-specific Android UI tests | NOT VERIFIED | `GutterBasicInfoUiTest` has no connected-test result. |
| Gradle compile/test/lint from CLI | NOT VERIFIED | Java Runtime is unavailable: `Unable to locate a Java Runtime`. |
| Existing connected regression report | FAIL / environment classification pending | Emulator report contains 2 `GutterCantOpenUiTest` cases with 1 failure; the task-specific test is absent. |

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `fragment_gutter_basic_info.xml` contains `tvMeasurementStatusTitle`, `btnPendingDeploy`, and `cbCantOpen`; manual device validation reached the form. |
| AC-002 | PASS | `reorderEditableSections()` places the approved title/control/photo groups in order; manual device validation confirmed the initial field mapping and positions. |
| AC-003 | PASS | `prefillData()` applies `rbIsBroken0` and `rbIsSilt0` only in the no-data branch. |
| AC-004 | NOT VERIFIED | Source review preserves existing values in the data branch and import path, but no executable edit/import/draft regression test result is available. |
| AC-005 | PASS | Slot-specific strings and button IDs are present; execution report records manual validation after all three photo cards were captured. |
| AC-006 | NOT VERIFIED | No complete regression run. The available emulator report has one failing `GutterCantOpenUiTest` and the task-specific UI test is absent. |

## Regression

- Android Studio build/install to Sony XQ-AU52: completed without a build/install error.
- Launching the installed app reached `LoginActivity` without a crash.
- User real-device validation exercised the form and reported no issue with the initial order or the three-photo state.
- No `FATAL EXCEPTION` for `com.example.taoyuangutter` was present in the captured logcat.
- Existing connected report `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml` recorded `GutterCantOpenUiTest`: 2 tests, 1 failure. The failure occurred in `cancelDialogKeepsOriginalState` while replacing `etDepth`, because Espresso reported the view had no non-empty global visible rectangle.
- `TEST-XQ-AU52 - 12.xml` recorded zero tests; it is not evidence of a pass.

## Issues

- The original form-entry crash was fixed and the affected field mapping was corrected. The upload failure shown in the attached screenshot remains a backend SQL/API schema issue (`xy_num`) and was not changed by this task.
- The connected-test failure is recorded as `ISS-0910-2-06`; its root cause is not established because the report is affected by device/UI visibility conditions and cannot be reproduced from the unavailable CLI environment.
- The committed diff contains `GutterFormActivity.kt` IME/insets changes added for the user's same-task keyboard defect. This is a documented scope extension requiring release acknowledgement; no API, data, photo-slot, or upload-contract impact was found.

## Validation Limitations

- Gradle CLI remains unavailable because the shell environment has no Java runtime.
- Gradle CLI and executable UI tests remain unavailable because the shell environment has no Java runtime.
- AC-004 and AC-006 require dedicated regression evidence. AC-004 also needs edit/import/draft coverage.
- No CI result was supplied, so CI build/test gates are not verified.

## Failure Classification

- Previous `implementation_regression` — `IllegalStateException: The specified child already has a parent` during form creation. Fixed before the current manual validation.
- Current connected-test observation — `verification_failure`, provisionally classified as `environment` because the available report shows Espresso visibility constraints and the test cannot currently be rerun; route to infrastructure before treating it as an implementation regression.

## Next Action

- Provide a JDK/Android build environment, run `GutterBasicInfoUiTest` plus the affected `GutterCantOpenUiTest`, inspect the existing failure, and collect CI build/test results. Resolve the scope deviation review before Release.

## Final Result

NOT VERIFIED overall; AC-001, AC-002, AC-003 and AC-005 have source/manual evidence, while AC-004 and AC-006 lack sufficient executable regression evidence. Release is blocked until infrastructure/test evidence is available.
