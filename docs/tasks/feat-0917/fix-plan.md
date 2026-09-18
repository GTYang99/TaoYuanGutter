# Fix Plan: ISS-007

1. Replace the unconditional scope-polyline hide in `MapWorkspaceFragment.showAddGutterList()` with the current map layer-toggle state.
2. Compile and run the focused JVM regression suite.
3. Verify on a physical device: add a gutter, upload it, enter inspection, return with “上一頁”, and confirm the scope-search segments remain visible when the gutter layer is enabled.

# Fix Plan: ISS-011 and ISS-012

1. In `WaypointAdapter`, keep the editable point label to the base point name (and any pre-existing editable-list status that remains required); do not append `(銜接點)`. Preserve `GutterInspectPhotosFragment`'s independent inspection spinner suffix logic.
2. In `GutterInspectPhotosFragment.renderFields()`, emit the non-virtual/non-cant-open rows in this exact order: material, structural damage, hanging pipeline, silt, connecting pipe, note.
3. Add focused regression tests for: (a) an add-list waypoint with `IS_TIEINPOINT=1` has no suffix, (b) an inspect spinner still displays `(銜接點)` and `(待架站)` in the required order, and (c) inspect dynamic field labels follow the required sequence.
4. Run focused JVM/UI tests, compile the Debug variant, and then rerun the bounded inspection/add-list emulator flow.
