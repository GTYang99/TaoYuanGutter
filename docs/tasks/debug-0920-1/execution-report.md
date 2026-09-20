# Execution Report

## Scope

- Task: `debug-0920-1`
- Branch: `fix/debug-0920-1-表單問題`
- Implementation scope: AC-002 through AC-005; AC-001 retained the existing shared policy fix from `07e3b5a`.

## Follow-up implementation — AC-006 and AC-007

- `cbConnectPoint` now uses the detail-exemption confirmation flow with the exact requested message. Cancellation preserves the current state; confirmation clears detail fields, measurement photos, and connecting-pipe selection.
- `cbCantOpen` now clears the connecting-pipe selection as part of the same clear path.
- The existing session snapshot now captures and restores `IS_CONNECTING`, including after configuration recreation and after reversing either exemption selection.
- Implementation and regression-test commit: `7c43493`.

## Changes

| Acceptance criterion | Implementation result |
|---|---|
| AC-001 | No new production change. Existing tie-in exemption remains shared across form, photo, and submit rules. |
| AC-002 | Removed only the missing-photo completion Toast after existing-waypoint import. |
| AC-003 | Edit entry now proceeds through preload without the no-photo/photo-issue confirmation dialog. Detail-load failure still opens the retry/block dialog. |
| AC-004 | Inspection maps `IS_SILT=2` to `嚴重`; legacy `3` remains `嚴重`. |
| AC-005 | Existing `XY_NUM` remains populated and preserved, while the field is read-only and excluded from manual completion requirements. |

## Validation

- `git diff --check`: PASS.
- Static source trace against all five acceptance criteria: PASS.
- Focused JVM tests (`GutterCompletionPolicyTest`, `StoreDitchNodeRequestMapperTest`, `InspectionPresentationTest`): PASS.
- Full JVM suite (`./gradlew testDebugUnitTest`): PASS.
- `./gradlew assembleDebug`: PASS.
- Full Android UI suite (`./gradlew connectedDebugAndroidTest`) on `Medium_Phone` Android 14: PASS.
- AC-002 import-fixture validation: PASS via `GutterImportExistingWaypointUiTest` on `Medium_Phone` Android 14.
- AC-003 inspection/preload validation: PASS via `GutterInspectEditEntryUiTest` on `Medium_Phone` Android 14.
- Targeted UI regression: PASS. `GutterBasicInfoUiTest` passed on XQ-AU52 / Android 12, including `tieInPointWarnsBeforeClearingAndCancelPreservesData`.
- Targeted UI regression: PASS. `GutterCantOpenUiTest` passed on XQ-AU52 / Android 12, including connecting-pipe clearing and snapshot restoration.
- Full JVM suite after follow-up: PASS.
- Debug build after follow-up: PASS.
- Full connected Android suite after follow-up: PASS on XQ-AU52 / Android 12.

## Limitations

## Follow-up implementation — API omission and inspection exemption

- `StoreDitchNodeRequestMapper` now treats both `IS_CANTOPEN` and `IS_TIEINPOINT` as detail-exempt.
- Exempt nodes retain the mode flags but omit `IS_CONNECTING`, all listed detail parameters, and photo association IDs for slots 2/3 from the JSON request.
- `GutterInspectPhotosFragment` now hides tie-in measurements, detail rows, and photo slots 2/3 in the same way as cant-open.
- Inspection-to-edit preload skips fallback loading of exempt photo slots 2/3.
- `StoreDitchNodeRequestMapperTest` and `InspectionPresentationTest` now cover the clarified omission behavior.

## Follow-up validation

- `git diff --check`: PASS.
- Targeted JVM tests: PASS, 32 tests completed via `:app:testDebugUnitTest --tests ...`.
- Full JVM suite: PASS, `:app:testDebugUnitTest`.
- Debug build: PASS, `:app:assembleDebug`.
- AndroidTest compilation: PASS, `:app:compileDebugAndroidTestKotlin`.
- Connected Android suite: NOT VERIFIED for this follow-up. `:app:connectedDebugAndroidTest` was started on XQ-AU52 / Android 12 but stalled with `RootViewPicker: No activity currently resumed`; it was stopped without an assertion result.
- Limited connected checks after resetting the device: PASS for `GutterCantOpenUiTest#confirmDialogClearsAffectedFields`, `GutterCantOpenUiTest#cancelDialogKeepsOriginalState`, `GutterBasicInfoUiTest#tieInPointWarnsBeforeClearingAndCancelPreservesData`, and `GutterInspectTieInUiTest#tieInPointInspectionHidesExemptDetailsAndPhotos` on XQ-AU52 / Android 12.
- Per the user's request, no further full connected suite was run; physical-device acceptance remains for the user's own test.
- Remote CI and release records remain outside this implementation-validation session.

## Follow-up implementation — inspection-to-edit empty connection value

- Inspection edit preload now preserves an omitted `IS_CONNECTING` instead of synthesizing `0`.
- Form prefill and import prefill select `連接管` only for an explicit value; an omitted value leaves both radio buttons unselected.
- Exempt-mode draft normalization removes `IS_CONNECTING`, preventing later draft synchronization from recreating a false 「無」 selection.
- Added focused UI coverage for existing 銜接點 and 無法開蓋 data with a missing connection value.

## Follow-up validation — inspection-to-edit empty connection value

- `git diff --check`: PASS.
- `:app:testDebugUnitTest`: PASS.
- `:app:compileDebugAndroidTestKotlin`: PASS.
- `:app:assembleDebug`: PASS.
- Physical-device inspection-to-edit acceptance: NOT VERIFIED; user device testing remains required.
