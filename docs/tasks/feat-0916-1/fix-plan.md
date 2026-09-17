# Fix Plan

## Scope

Resolve `ISS-FEAT-0916-1-010` with the minimum change needed for AC-003.

## Approved Fix

1. In `MainShellActivity`, construct the shell Back callback in `onCreate` and add it directly with `onBackPressedDispatcher.addCallback(callback)` rather than the Activity lifecycle-owner overload.
2. Keep that direct registration before `showTab()`, so the map Fragment's view-lifecycle callback is inserted later and, when enabled for measurement, takes precedence to execute `exitMeasureMode()`.
3. Do not change measurement calculations, working-layer preservation, scope visibility policy, draft behavior, or Sheet lifecycle APIs.
4. Validate compilation and focused emulator tests, then repeat authenticated list-source measurement and Android Back.

## Change Set

- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt` (add a focused dispatcher-precedence regression test if the existing harness can expose the callbacks)

## Exit Criteria

- First tap on the map measurement button enters measurement from the editor Sheet.
- The same behavior is available from the list Sheet.
- Exiting or pressing Back restores the same source Sheet without close confirmation.
- Existing working-layer and scope-layer requirements remain unchanged.
