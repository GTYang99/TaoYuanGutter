# Debug Fix Plan

## Minimum fix

1. Remove repository-wide `MULTI_GUTTER` loading from `MultiGutterSessionCoordinator` construction.
2. Add an explicit coordinator restore API that accepts the active session's saved item IDs/order and reads only those IDs from Room.
3. Save the coordinator's current item IDs in `MapWorkspaceFragment.onSaveInstanceState()`.
4. Restore those IDs only when `savedInstanceState` contains an active multi-gutter session marker. A fresh/cold start must initialize an empty add-list; pending drafts remain available through the existing pending-drafts flow.
5. Preserve the current close behavior: final-upsert valid drafts, clear the coordinator session, and leave the persisted drafts available outside the add-list.
6. Replace the conflicting test that expects repository-wide reload with tests for:
   - fresh coordinator has no items despite unrelated persisted multi-gutter drafts;
   - explicit active-ID restore recovers only the saved IDs and order;
   - foreign persisted drafts do not participate in successful-item cleanup.

## Validation

- Compile and unit tests.
- Targeted `MainShellActivityTest` instrumentation.
- Full connected instrumentation if available.
- Re-run verification for AC-005 and AC-006 on the new committed revision.

## Scope boundary

No changes to API behavior, legacy single-gutter ownership, pending-drafts listing, or upload contracts.

