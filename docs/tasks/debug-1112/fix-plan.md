# Fix Plan

## Issue

- `ISS-DBG-1112-001` — Import allows a new D to use an `XY_NUM` already used by another point in the same ditch.
- `ISS-DBG-1112-002` — Submit-time `XY_NUM` duplicate validation is too late to prevent the invalid import-then-delete sequence.

## Minimum Fix Scope

1. At import time, read the imported `XY_NUM` and compare it with all current waypoint `XY_NUM` values in the same ditch.
2. If a match exists, stop the import before updating D and show exactly: `不可匯入本側溝中的同樣點位`.
3. Do not copy C's `node_id` to D; D must remain a new node. Keep the existing submit-time duplicate guard as a secondary defense.
4. Add regression coverage for `[A, B, C]` plus new D importing C: import is rejected, D is not populated with C's data, and the message is shown.
5. Keep the backend ordering issue as a defense-in-depth concern; the client import-time guard prevents the reported sequence from producing the invalid payload.

Recommended implementation boundary: apply the guard in `GutterFormActivity.handleImportedNodeDetails()` before `setImportedWaypointLocked(true)`, `prefillDataFromImport(nodeDetails)`, or photo downloads. Compare against the existing `sessionWaypoints` passed by `GutterFormNavigator`; for an existing target waypoint, decide whether its own current `XY_NUM` is excluded, but never exclude another waypoint such as C when the target is new D.

## Regression Risk

- Do not copy `_nodeId` from C into new D; the clarified product intent explicitly requires D to be new.
- The check must be scoped to the current ditch. It must not compare against unrelated ditches.
- If importing into an existing waypoint is supported, verify whether the current target waypoint itself should be excluded from the comparison; the new-D case must always be blocked when C already exists in the same ditch.
- Verify ordinary edit of an unchanged existing point, edit with a changed point, import into an existing point, deletion of an existing point, and photo metadata handoff.
- Verify `XY_NUM` remains preserved for normal edit and remains omitted/generated for new-point behavior where required by `feat-0917`.

## Validation Plan for Implementation

- JVM/import test: with existing A/B/C and new D, importing C's `XY_NUM` is rejected and the exact message is emitted.
- JVM/import test: importing a `XY_NUM` not present in the current ditch remains allowed; D still has no `node_id`.
- JVM/import test: the guard does not query or reject an equal `XY_NUM` belonging to another ditch; its comparison input is only the current `sessionWaypoints`.
- Regression test: after a rejected import, D's waypoint data and the final `storeDitch` request are unchanged.
- Keep a submit-time test for the existing current-list duplicate guard.
- Backend smoke test with sanitized request/response evidence, because the server implementation and its inheritance ordering are not in this repository.

## Current Gate

Implementation is intentionally not started. The clarified implementation requirement is to reject the duplicate at import time; the identity semantics for D are already resolved.
