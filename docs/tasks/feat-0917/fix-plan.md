# Fix Plan: ISS-007

1. Replace the unconditional scope-polyline hide in `MapWorkspaceFragment.showAddGutterList()` with the current map layer-toggle state.
2. Compile and run the focused JVM regression suite.
3. Verify on a physical device: add a gutter, upload it, enter inspection, return with “上一頁”, and confirm the scope-search segments remain visible when the gutter layer is enabled.
