# Root Cause Analysis

## Task

- Task: `debug-0919-3`
- Branch: `fix/debug-0919-3-回報點位難點`
- Issue: `ISS-debug-0919-3-001`
- Production code was not modified during this analysis.

## Root cause

The report UI is an inline bottom-constrained `CardView`, not a dialog. The
normal logged-in app path is `LoginActivity → MainShellActivity →
MapWorkspaceFragment`; `MapWorkspaceFragment` inflates `activity_main.xml`,
so the panel is inside the shell's `shell_container`, which is already
constrained above the shell bottom navigation. The offline path launches
`MainActivity`, but shares the same panel and controller.

When the note field gains focus, `NoDitchModeUiController.setupPanelInsets()`
reads the IME bottom inset and sets:

```text
panel.bottomMargin = originalBottomMargin + max(systemBars.bottom, ime.bottom)
```

The panel is already constrained to the fragment parent bottom, and that
parent is already above the shell bottom navigation in the normal path.
Adding the full IME height as a bottom margin therefore moves the whole panel
upward by the keyboard height on top of its existing shell placement.
`MainShellActivity` and `MainActivity` do not declare an explicit
`windowSoftInputMode`, so the platform's focus/IME behavior may additionally
resize or pan the available window. The manual IME margin is therefore a
confirmed over-offset mechanism; a second platform displacement is plausible
but not proven without runtime inset/window measurements.

The panel's `NestedScrollView` is `wrap_content`, and the note editor has
`minLines="6"`. This is a contributing layout factor: the panel has no bounded
viewport that could absorb the reduced height by scrolling its content. It is
not the primary trigger; the trigger is the explicit IME-height margin on a
bottom-constrained panel.

## Failure mapping

### AC-001 — no-ditch note panel remains usable above the keyboard

Observed failure from the attached debug note: on a foldable phone, entering
text after tapping `btnReportNoDitch` makes the report window move too high.

Code path:

1. The normal logged-in host inflates the map fragment and initializes the
   controller (`MapWorkspaceFragment.kt:329-371`). Its button binding calls
   `openNoDitchReport()` (`MapWorkspaceFragment.kt:637-694`, especially line
   689).
2. `openNoDitchReport()` calls `enterNoDitchMode()`; that method shows the
   inline panel through `NoDitchModeUiController.enter()`
   (`MapWorkspaceFragment.kt:1023-1027`, `1463-1470`).
3. A map tap calls `onNoDitchLatLngPicked()`, which makes the note field
   visible through `setPickedLatLng()` (`MapWorkspaceFragment.kt:1525-1547`,
   `NoDitchModeUiController.kt:113-126`).
4. On IME insets dispatch, `setupPanelInsets()` adds `ime.bottom` to the
   panel bottom margin (`NoDitchModeUiController.kt:26-43`).
5. The panel is constrained to the parent bottom in
   `activity_main.xml`, while `panel_no_ditch_report.xml` keeps a large
   multi-line content height. The top edge consequently jumps upward by more
   than the amount required to keep the editor visible.

AC-001 is therefore an implementation regression in the no-ditch panel's
IME/layout integration, not a requirement ambiguity or a map-coordinate
problem.

## Non-root-cause findings

- `btnReportNoDitch` only starts the mode; it does not create or position a
  `Dialog`.
- `submitNoDitch()` and `GutterRepository.storeNoDitch()` are downstream of
  the visual defect and are not involved before text entry.
- The map click and selected-coordinate state are separate from the keyboard
  transition; no evidence indicates that coordinate picking causes the panel
  to move.
- The root-level inset listener in `MapWorkspaceFragment.applySystemBarInsets()`
  adjusts the add-gutter FAB and location-picker bar and returns the insets
  (`MapWorkspaceFragment.kt:520-548`). `MainShellActivity.applyInsets()` also
  returns the insets after padding the bottom navigation
  (`MainShellActivity.kt:78-88`). Neither listener directly translates the
  no-ditch panel, but both leave the same insets available to the child panel;
  the shell bottom-navigation placement is therefore part of the layout
  boundary.

## Evidence

- `LoginActivity.kt:161-162` — normal login launches `MainShellActivity`.
- `MainShellActivity.kt:63-71` — normal shell creates `MapWorkspaceFragment`.
- `MapWorkspaceFragment.kt:329-371` — fragment inflates
  `ActivityMainBinding` and initializes `NoDitchModeUiController`.
- `MapWorkspaceFragment.kt:689` — normal logged-in no-ditch button binding.
- `MapWorkspaceFragment.kt:1023-1027` — no-ditch mode entry.
- `MapWorkspaceFragment.kt:1463-1470` — panel shown and map-pick mode enabled.
- `NoDitchModeUiController.kt:26-43` — manual system/IME inset converted to
  panel bottom margin.
- `activity_main.xml:170-180` — panel constrained to the parent bottom.
- `panel_no_ditch_report.xml:1-17` — bottom margin and wrap-content panel
  container.
- `panel_no_ditch_report.xml:63-85` — multi-line note field with six minimum
  lines inside a wrap-content scroll view.
- `AndroidManifest.xml:53-63` — `MainShellActivity` and `MainActivity` have no
  explicit `windowSoftInputMode`.
- Commit `0652659` — introduced the current inline panel and its
  `setupPanelInsets()` behavior while replacing the earlier bottom-sheet
  implementation.
- `/Users/a10362/Desktop/markdown file/ty_refactor_0917.md` — records the
  earlier map-tap difficulty that motivated the inline-panel design; it is
  related history, not the current text-input symptom.

## Confidence and remaining verification

- Static over-offset mechanism: high confidence.
- Exact foldable runtime symptom and any additional platform pan/resize:
  unverified. The connected non-foldable device is evidence of available test
  infrastructure only; it is not evidence that the reported foldable case
  reproduces.
