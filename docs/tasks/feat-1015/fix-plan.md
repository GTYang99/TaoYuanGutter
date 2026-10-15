# Fix Plan — ISS-feat-1015-001

## Failed Acceptance Criterion

- AC-005 test evidence is incomplete because the instrumentation test callback collector throws on the required null-selection event during tab changes.

## Minimum Fix

1. Replace `CopyOnWriteArrayList<NodeDetails?>` with a null-safe selection record: store non-null selections and count null clears separately.
2. Keep the candidate callback and selected/import item assertions.
3. Recompile Android test sources and rerun the focused UI test on `emulator-5554`.

For follow-up issue `ISS-feat-1015-002`, change tab interactions to match the entire `TabView` by its accessibility description, then assert the expected `selectedTabPosition` after each switch.

For follow-up issue `ISS-feat-1015-003`, move ActivityScenario to `RESUMED` before `sheet.show()` and assert `supportFragmentManager.isStateSaved == false` in the same main-thread action.

## Regression Risk

Low. This changes only test bookkeeping; production files and behavior remain untouched. If the focused test passes, update the execution and verification evidence and close the issue after review.
