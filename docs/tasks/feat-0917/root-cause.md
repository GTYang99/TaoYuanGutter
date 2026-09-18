# Root Cause: ISS-007

## Symptom

After creating and uploading a gutter, opening inspection, and returning to the main map, gutter segments from `/v1/map/scopeSearch` appeared absent.

## Cause

`MapWorkspaceFragment.inspectLauncher` reloads the viewport before reopening the multi-gutter list. However, `showAddGutterList()` immediately and unconditionally hid `scopeGutterPolylineController`. The request and drawing path could therefore complete, but the list transition made the resulting polyline layer invisible.

## Minimum Fix

When the add-gutter list opens, set scope-polyline visibility from `mapOverlayController.currentState().showPlan` rather than forcing it to `false`.

## Scope and Risk

Only the multi-gutter list transition is changed. The normal map layer toggle remains the source of truth; users who manually turn the gutter layer off continue to see it hidden.

# Root Cause: ISS-011 and ISS-012

## ISS-011 — Editable list shows an inspection-only suffix

`WaypointAdapter.buildDisplayLabel()` appends `(銜接點)` for every adapter consumer. Its only production consumer is `AddGutterBottomSheet`, so the editable waypoint list inherits a label that is required only in the inspection screen. The inspection screen does not use this adapter: `GutterInspectPhotosFragment` builds its spinner label independently in `pointLabel()` / `buildPointStatusSuffix()`.

## ISS-012 — Inspection detail order differs from the requested order

`GutterInspectPhotosFragment.renderFields()` creates rows dynamically. The current append sequence is `側溝材質 → 淤積程度 → 連結管 → 溝體結構受損 → 附掛或過路管線 → 補充說明`, while the required sequence is `側溝材質 → 溝體結構受損 → 附掛或過路管線 → 淤積程度 → 連結管 → 補充說明`.

## Minimum Fix Scope

- Remove the tie-in suffix from `WaypointAdapter` without changing inspection spinner labels.
- Reorder only the five dynamic detail-row additions in `GutterInspectPhotosFragment.renderFields()`.
- Add regression coverage for both labels and the exact inspection-field order.

## Confidence

Root-cause confidence: 99%. The observed output maps directly to the only two code paths that build these labels and fields.
