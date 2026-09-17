# Root Cause

## Issue

`ISS-FEAT-0916-1-010` caused AC-003 to fail at verification revisions `eef13b8` and `a83f745`.

## Cause

`MapWorkspaceFragment` installs an enabled `measureBackCallback` while measurement is active. `a83f745` moved `MainShellActivity`'s `addCallback(this, ...)` call ahead of `showTab()`, but it retained the Activity lifecycle owner. That API defers insertion into the dispatcher until the Activity emits `ON_START`; the map Fragment's view-lifecycle callback is inserted before that moment. The shell callback is therefore still inserted last and evaluated first, so the map tab calls `finish()` instead of allowing `exitMeasureMode()` to restore the list sheet.

## Failed Acceptance Criteria

- AC-003: list-source measurement entered successfully, but Android Back returned to the Launcher instead of restoring the same list.

## Evidence

- `verification.md`: authenticated list measurement displayed distance, then Android Back closed the task on `emulator-5554`.
- `MapWorkspaceFragment.kt`: `exitMeasureMode()` restores the list through `showAfterMeasure()`.
- `a83f745` re-verification: the same `90 公尺` list measurement and Back-to-launcher failure reproduced after its callback-order change.
- AndroidX lifecycle semantics: `addCallback(this, callback)` waits for the lifecycle owner to start; the activity's callback consequently remains later in dispatcher order than the Fragment's view callback.

## Regression Risk

The fix affects only Back callback registration timing. Measurement calculation, touch routing, sheet state, and layer visibility remain unchanged.
