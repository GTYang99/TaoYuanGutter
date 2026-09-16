# Fix Plan

## Scope

Resolve `ISS-FEAT-0916-1-010` with the minimum change needed for AC-003.

## Approved Fix

1. Register the shell Back callback before creating/showing the selected Fragment.
2. Let the Fragment view-lifecycle callback, when enabled for measurement, take precedence and execute `exitMeasureMode()`.
3. Do not change measurement calculations, working-layer preservation, scope visibility policy, draft behavior, or Sheet lifecycle APIs.
4. Validate compilation and focused emulator tests, then repeat authenticated list-source measurement and Android Back.

## Change Set

- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`

## Exit Criteria

- First tap on the map measurement button enters measurement from the editor Sheet.
- The same behavior is available from the list Sheet.
- Exiting or pressing Back restores the same source Sheet without close confirmation.
- Existing working-layer and scope-layer requirements remain unchanged.
