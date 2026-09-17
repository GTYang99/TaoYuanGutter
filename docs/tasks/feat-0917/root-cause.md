# Root Cause: ISS-007

## Symptom

After creating and uploading a gutter, opening inspection, and returning to the main map, gutter segments from `/v1/map/scopeSearch` appeared absent.

## Cause

`MapWorkspaceFragment.inspectLauncher` reloads the viewport before reopening the multi-gutter list. However, `showAddGutterList()` immediately and unconditionally hid `scopeGutterPolylineController`. The request and drawing path could therefore complete, but the list transition made the resulting polyline layer invisible.

## Minimum Fix

When the add-gutter list opens, set scope-polyline visibility from `mapOverlayController.currentState().showPlan` rather than forcing it to `false`.

## Scope and Risk

Only the multi-gutter list transition is changed. The normal map layer toggle remains the source of truth; users who manually turn the gutter layer off continue to see it hidden.
