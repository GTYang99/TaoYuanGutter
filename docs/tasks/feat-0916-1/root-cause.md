# Root Cause

## Issue

`ISS-FEAT-0916-1-008` caused AC-002 and AC-004 to fail at verification revision `6838499`.

## Cause

Both BottomSheet Dialogs used their binding root to decide whether a touch was outside the sheet. That root can occupy the Dialog Window rather than the visible `design_bottom_sheet` bounds. A touch on the Activity's map measurement button was therefore classified as sheet content, so the Dialog consumed the first tap and the Activity never received the measurement action.

The same boundary error existed in both `AddGutterBottomSheet.kt` and `AddGutterListBottomSheet.kt`, so the list source had the same regression risk even though it was not reachable during the failed smoke run.

## Failed Acceptance Criteria

- AC-002: the main map measurement button did not receive the first tap.
- AC-004: editor-source measurement could not start, so hide/restore and Back behavior could not execute.

## Evidence

- `verification.md`: tap at `(975, 368)` failed twice on `emulator-5554`.
- `AddGutterBottomSheet.kt` and `AddGutterListBottomSheet.kt`: external-touch routing used binding-root geometry.
- `00c2943`: changed routing to use the actual `design_bottom_sheet` bounds.

## Regression Risk

The fix affects only Dialog-vs-Activity touch classification. Sheet content handling remains delegated to the original Dialog callback; map controls outside the visible sheet become routable. Both source paths require focused emulator validation after the fix.
