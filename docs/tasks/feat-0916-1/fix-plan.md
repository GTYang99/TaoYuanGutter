# Fix Plan

## Scope

Resolve `ISS-FEAT-0916-1-008` with the minimum change needed for AC-002 and AC-004, while applying the same correction to the list source required by AC-003.

## Approved Fix

1. In both BottomSheet classes, calculate external-touch bounds from the actual Material `design_bottom_sheet` container and its screen coordinates.
2. Preserve the existing direct main-button click routing and the original Dialog callback for touches inside the sheet.
3. Do not change measurement calculations, working-layer preservation, scope visibility policy, draft behavior, or Sheet lifecycle APIs.
4. Validate compilation plus the focused `MainShellActivityTest` and `GutterFormExitUiTest` on `emulator-5554`; then repeat AC-002 and AC-004 manual smoke and execute AC-003 when the authenticated list path is available.

## Change Set

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterListBottomSheet.kt`

## Exit Criteria

- First tap on the map measurement button enters measurement from the editor Sheet.
- The same behavior is available from the list Sheet.
- Exiting or pressing Back restores the same source Sheet without close confirmation.
- Existing working-layer and scope-layer requirements remain unchanged.
