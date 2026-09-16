# Root Cause

## Issue

`ISS-FEAT-0916-1-010` caused AC-003 to fail at verification revision `eef13b8`.

## Cause

`MapWorkspaceFragment` installs an enabled `measureBackCallback` while measurement is active. However, `MainShellActivity` registered an always-enabled Activity-level Back callback after `showTab()` had created the map Fragment. The dispatcher therefore evaluated the shell callback first; on the map tab it called `finish()` instead of allowing `exitMeasureMode()` to restore the list sheet.

## Failed Acceptance Criteria

- AC-003: list-source measurement entered successfully, but Android Back returned to the Launcher instead of restoring the same list.

## Evidence

- `verification.md`: authenticated list measurement displayed distance, then Android Back closed the task on `emulator-5554`.
- `MapWorkspaceFragment.kt`: `exitMeasureMode()` restores the list through `showAfterMeasure()`.
- `MainShellActivity.kt`: the shell callback finishes the map Activity and was registered after the map tab was shown.

## Regression Risk

The fix affects only Back callback precedence. Measurement calculation, touch routing, sheet state, and layer visibility remain unchanged.
