# Debug Fix Plan

## Issue

Resolve ISS-0910-2-02 and the `IllegalStateException` raised while entering `GutterFormActivity`.

## Minimum Fix

Make the existing runtime reordering parent-safe by detaching each ordered View from its actual current `ViewGroup` before adding it to `formContent`. Preserve all IDs, listeners, field bindings, photo-slot mappings, virtual-mode visibility, and view-mode behavior. Keep the change limited to the failing reordering logic and update the UI test/documentation evidence.

## Validation

- `git diff --check`
- XML parsing for the modified layout
- Android Studio build/install on Sony XQ-AU52
- Re-enter the form on the real device and confirm no `FATAL EXCEPTION`
- Run available targeted UI tests; record unavailable CLI checks as `NOT VERIFIED` if Java remains unavailable

## Re-entry Gate

Root cause is identified and the failed acceptance criteria are mapped. Re-implementation may begin after this plan is recorded.
