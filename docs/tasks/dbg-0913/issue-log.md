# Issue Log

## ISS-DBG-0913-002
- Category: implementation_regression
- Priority: P2
- Status: verified
- Title: New form defaults trigger the cant-open clear confirmation
- Evidence: A new form can preselect `rbIsBroken0` (否) and `rbIsSilt0` (無); when the default gutter type is 明溝, its UI also forces cover thickness to `0`. The first implementation treated these UI-derived defaults as clearable user content, so the first cant-open selection showed an Alert despite no user input.
- Expected: The initial default selections do not count as user-filled content; the first cant-open selection proceeds without an Alert.
- Fix: Treat only non-default broken/silt selections and non-system-forced cover thickness as confirmation-worthy content, and test the real initial form state without clearing its views.
- Verification: PASS — 2026-09-13 Android 14 emulator `GutterCantOpenUiTest` completed all 5 cases with no failures; the real new-form no-data case returned `OK (1 test)` when rerun independently.
- Next action: release

## ISS-DBG-0913-003
- Category: implementation_regression
- Priority: P1
- Status: resolved_pending_validation
- Title: Rechecking cant-open bypasses the clear confirmation
- Evidence: After a no-data selection, `confirmCantOpenSelection()` replaced the full checked-change listener with `onCantOpenCheckedChanged()`, which only applies UI state. A subsequent recheck after entering depth or photos skipped `hasCantOpenContentToClear()`.
- Expected: Every user-initiated check evaluates the current clearable content.
- Fix: Reattach the full toggle handler and add a UI test for empty select → uncheck → enter depth → recheck.
- Next action: implementation

## ISS-DBG-0913-004
- Category: implementation_regression
- Priority: P1
- Status: in_progress
- Title: Exit validation omits required point identity and cover thickness fields
- Evidence: `validateRequiredFields()` does not inspect `NODE_TYP`, `NODE_X`, `NODE_Y`, `XY_NUM`, or `COVER_DEP`; it also returns early for both 明溝 and 無法開蓋. `handleNavigateBack()` delegates directly to this validator for the incomplete-form Alert.
- Expected: All modes require side-gutter form, location and measurement coordinate number. General gutters require cover thickness. 明溝 exempts only cover thickness; 無法開蓋 retains its existing detail and photo 2/3 exemptions.
- Fix: Validate point identity fields before the cant-open exemption; skip only cover thickness for 明溝; retain the cant-open early return after point identity validation.
- Verification: PASS — Android 14 `GutterFormExitUiTest` 7/7 passed, including each formerly omitted field, 明溝 depth, and 無法開蓋 coordinate number. Build and unit tests also passed.
- Next action: release
