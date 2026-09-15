# Debug Fix Plan — ISS-FEAT-0914-2-009

## Minimum fix

1. Add an explicit handoff state for launching `GutterFormActivity` from an add sheet.
2. Make `AddGutterBottomSheet.onDismiss()` skip cancellation cleanup while that handoff is active; genuine close/cancel continues to invoke the existing cleanup callback.
3. In `gutterFormLauncher` result handling, restore the hidden add sheet and its current session after both successful and canceled form returns. Clear the handoff state only after restoration is complete.
4. Preserve the multi-gutter session ID list and draft ID; do not create a new session or load unrelated persisted multi-gutter drafts.
5. Add an instrumentation regression covering list → add → form → back → same list, then verify the add action remains available.

## Validation

- Compile and unit tests.
- Targeted authenticated/form-return instrumentation or equivalent host-level regression test.
- Full connected instrumentation.
- Re-run verification for AC-002.

