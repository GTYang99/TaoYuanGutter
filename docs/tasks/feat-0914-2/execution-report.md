# Implementation Execution Report

## Progress

- Added immutable `createdAt` and `workflowOwnership` to the draft model and Room entity.
- Added Room migration 2→3; existing rows backfill `createdAt` from `savedAt` and use `LEGACY_SINGLE` ownership.
- Added a repository-backed monotonic draft ID allocator.
- Preserved `createdAt` and ownership on updates.
- Restricted SPI_NUM deduplication and batch deletion to legacy drafts.
- Added `MultiGutterSessionCoordinator`, list bottom sheet, adapter, and Figma-aligned XML rows.
- Changed the live `MapWorkspaceFragment` add-gutter entry to open the list first and route each selected draft by ID.
- Added the required close confirmation and kept list item creation order/time/node count visible.
- Reordered the list toolbar to close/delete on the left and add on the right, matching the revised requirement and Figma hierarchy.
- Deferred successful multi-gutter draft/photo cleanup until the inspect activity closes, then returns to the same list and removes only the completed item.
- Routed multi-gutter upload failure confirmation back to the same list while preserving the failed item; legacy flow keeps its existing dismiss behavior.

## Figma evidence

Figma MCP context was read from file `IfmNbZKhr4wojZ2bF5rYHG`, node `2374:26810`.

- Reference width: 402 px.
- Top corner radius: 24 px.
- Toolbar height: 70 px.
- Toolbar content row: 44 px with 32 px horizontal padding.
- Grabber: 36 × 5 px, with 16 px wrapper height.
- Title: SF Pro Bold, 18 px, line height 1.25.
- Add action: SF Pro Bold, 16 px; add icon 18 × 18 px.
- Close and chevron icons: 24 × 24 px.
- List rows: 402 px wide, 32 px horizontal padding, 12 px vertical content padding, bottom divider.
- Row title/detail typography: 16 px / 14 px, 4 px detail gap.
- Bottom sheet shadow: offset (0, -4), radius 20, color `#B7B7C2`.

Requirement text remains authoritative where it differs from the Figma example: close/delete is left, add is right, and timestamps include seconds.

## Validation

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --no-daemon`: PASS.
- Existing compiler deprecation warnings remain; no new compilation errors observed.
- After list UI integration, the same compile and unit-test command: PASS.
- After callback-routing, revised success/failure list flow, and single-item cleanup fixes, compile and unit tests: PASS.
- Targeted `MainShellActivityTest` instrumentation: PASS on `XQ-AU52 - 12` and `Medium_Phone(AVD)`.
- Full `connectedDebugAndroidTest`: PASS on both devices after disabling system animations for the Espresso dialog test.
- Close-confirm flow now finalizes retainable multi-gutter drafts before dismissing the list, preventing drafts from being lost after confirmation.
- Regression test `closingMultiGutterListFinalizesDraftsForLaterUse`: PASS on both devices.
- `git diff --check`: PASS.

## Limitations

- Form-result routing and successful-upload cleanup still need an end-to-end audit for multi-gutter ownership.
- Targeted and full instrumentation execute on available devices; formal authenticated MapWorkspace smoke remains unavailable.
- Formal MapWorkspace smoke remains `NOT VERIFIED` because the installed launcher requires an authenticated session.
- Corrected child-fragment callback routing and process-recreation restoration to use only `MULTI_GUTTER` rows.
- Multi-gutter successful upload now removes only the selected draft after inspect-page close and returns to the list.
- Formal authenticated MapWorkspace smoke is still not executed because no login session/test credentials are available.
