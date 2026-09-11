# Fix Plan: ISS-0910-2-09

## Current status

Debug is resolved for `ISS-0910-2-09`: Android Test `EmptyActivity` cross-scenario task handoff, aggravated by an asleep XQ-AU52 device, caused the observed top-resumed failure. The test launcher now isolates each form scenario with `FLAG_ACTIVITY_CLEAR_TASK`; no production window/theme change is required.

## Minimum next checks

1. Keep the existing empty map host container and deferred `SupportMapFragment` startup. (Implemented.)
2. Compare ActivityScenario launch with the production MainShell launch and capture ActivityTaskManager/focus ownership. (Superseded by direct isolated-run evidence.)
3. Preserve the form's full-screen map, panel layout, overlays, markers, camera, and data behavior. (Required.)
4. Re-run `GutterBasicInfoUiTest` repeatedly on XQ-AU52, then run `GutterCantOpenUiTest` and the full connected suite. (Targeted classes complete: 3/3 and 4/4; full suite pending.)

## Guardrails

- Do not remove the map, change the form window theme, or alter activity finish behavior until the launch-path comparison identifies the responsible layer.
- Do not mark the affected acceptance criteria PASS based only on the user's manual field-order result.
- Keep the offline existing-data fix and photo lifecycle fix separate from this investigation.

## ISS-0910-2-11: Virtual-point off-state

### Current status

Debug complete. The virtual checkbox listener still exists, but the Activity-level virtual UI method was made unconditional by the earlier UI redesign, so turning virtual mode off cannot restore the original normal interaction state.

### Minimum implementation

1. Restore state-dependent handling in `applyVirtualModeUi(isVirtual)` for the controls that belong to the virtual-point toggle.
2. Hide `cbCantOpen` while virtual mode is active so measurement status retains only 「待架站」.
3. Preserve the existing `GutterBasicInfoFragment.setVirtualMode(false)` field restoration and persisted `is_virtual=0` behavior.
4. Add a focused UI regression test for virtual on → off, including the restored control state.
5. Validate the virtual toggle together with the existing form-order and cant-open regressions.
