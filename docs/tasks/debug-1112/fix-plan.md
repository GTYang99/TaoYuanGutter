# Fix Plan

## Issue

- `ISS-DBG-1112-001` — New D retains C's `XY_NUM` after C is deleted, but the client does not detect that the inheritance source was removed in the same edit.
- `ISS-DBG-1112-002` — `XY_NUM` duplicate validation only covers the current waypoint list and does not cover the deleted source dependency or backend ordering.

## Minimum Fix Scope

1. Before submit, retain enough edit-start information to compare current waypoint `XY_NUM` values with source points removed during this edit.
2. Detect the case `[A, B, C] → [A, B, D(XY_NUM=C, node_id=null)]` and block submission with an actionable message, unless the backend contract explicitly guarantees atomic inherit-before-delete.
3. Do not copy C's `node_id` to D; D must remain a new node. Keep the existing current-list duplicate guard for ordinary duplicate entries.
4. If product requires allowing this operation, define and verify a backend/API transaction that resolves C's data before applying C deletion. Add sanitized request/response evidence; this cannot be proven from the Android repository alone.
5. Add a request-level regression assertion showing D has `node_id=null`, retains the intended `XY_NUM`, and the chosen client/backend policy does not remove both C and D.

## Regression Risk

- Do not copy `_nodeId` from C into new D; the clarified product intent explicitly requires D to be new.
- Blocking the operation may affect users if the backend already supports atomic inherit-before-delete; confirm the API contract before enforcing the client guard.
- Verify ordinary edit of an unchanged existing point, edit with a changed point, import into an existing point, deletion of an existing point, and photo metadata handoff.
- Verify `XY_NUM` remains preserved for normal edit and remains omitted/generated for new-point behavior where required by `feat-0917`.

## Validation Plan for Implementation

- JVM/request test: construct A/B/D after deleting C and assert the dependency guard rejects D's `XY_NUM=C` when the original C is absent.
- JVM/request test: assert a valid new D sends `node_id=null` and the imported `XY_NUM` unchanged.
- Focused instrumentation test: import a fixture with `node_id` and `XY_NUM`, return from the form, delete the original source waypoint, then verify the submit guard or captured `storeDitch` request according to the approved policy.
- Backend smoke test with sanitized request/response evidence, because the server implementation and its inheritance ordering are not in this repository.

## Current Gate

Implementation is intentionally not started. The remaining contract decision is whether the client must reject source deletion or the backend guarantees atomic inherit-before-delete; the identity semantics for D are already resolved.
