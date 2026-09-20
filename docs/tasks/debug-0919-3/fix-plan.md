# Minimum Fix Plan

This plan is limited to the reported no-ditch note-panel displacement. It
does not authorize changes to reporting API behavior, map picking, or the
measurement panel.

1. Choose one coordinated IME strategy for `noDitchPanel`: do not add the full
   IME bottom inset as a bottom margin when the activity/window already
   resizes or pans for the focused editor. System-bar spacing and IME spacing
   must be applied at one layer only.
2. Keep the panel bottom anchored and make its content viewport bounded by the
   available height when the IME is visible, so the `NestedScrollView` can
   scroll the note field instead of moving the entire card excessively high.
3. Preserve the existing no-ditch state machine: map pick, coordinate display,
   note validation, reset, submit, and success/error handling.
4. Add focused UI coverage for the reported boundary. The evidence must
   distinguish normal panel bounds, IME-visible panel bounds, editor
   visibility, and restored bounds after IME dismissal. Use a foldable or a
   reproducible compact-height configuration when available.
5. During implementation validation, record the device/OS/keyboard, fold
   posture, window height, system-bar inset, IME inset, and panel top/bottom
   coordinates. Do not report runtime positioning as PASS without this
   evidence.

## Explicitly out of scope

- Replacing the inline panel with a `Dialog` or a new bottom-sheet component.
- Changing `storeNoDitch` request fields, API endpoints, or repository logic.
- Changing map projection, marker placement, or no-ditch point hit testing.
- Changing `MeasureModeUiController` unless a separate regression is proven.
- Broad activity-wide window-insets refactoring.
