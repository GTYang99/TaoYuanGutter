# Root Cause Analysis — ISS-FEAT-0914-2-009

## Failure

On the authenticated MapWorkspace flow, opening a new gutter form from the add-gutter list and returning with the form back action leaves the user on the main map instead of the same add-gutter list. This fails AC-002.

## Root cause

The add-list session owns an `AddGutterBottomSheet` (`activeSheet`) while the waypoint form is launched as a separate Activity. The sheet is hidden with `hideSelf()`, but its lifecycle still has an unconditional cleanup path:

```kotlin
override fun onDismiss(...) {
    onWaypointsChanged?.invoke(null)
}
```

`MapWorkspaceFragment` interprets that null callback as cancellation of the active add session. It clears `activeSheet`, restores the main-map UI, and only conditionally attempts to open a new list. This callback is not distinguished from a genuine user cancellation of the add sheet or an external-form transition. Consequently, the external Activity return can enter the cancellation cleanup path before `gutterFormLauncher` handles the form result. The active list reference/lifecycle is then lost, and the form return path cannot restore the original list.

The return handler only handles a result when `activeSheet != null` and `pendingWaypointFormIndex >= 0`; it has no recovery path for a sheet that was cleared by the dismiss callback. `showAddGutterList()` creates a new sheet immediately from the coordinator, so it cannot restore the hidden in-progress form sheet's UI state if the callback has already cleared it.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterSheetSessionBinder.kt`

## Failed acceptance criterion

- AC-002: after creating/editing a gutter from the list, returning from the form must restore the same list so the user can add or switch to another gutter.

## Regression risk

Suppressing cleanup during an external form transition must not leave stale map UI after a real sheet close. The fix must explicitly distinguish form handoff from user cancellation and preserve the active multi-gutter session ID and current draft while the form Activity is open.

