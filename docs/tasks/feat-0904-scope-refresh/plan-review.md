# Plan Review

Review Date: 2026-09-04

Reviewer: Plan Critic Agent

## Decision

APPROVED

The planning gaps from the first review pass have been resolved. The plan now
requires stored visibility to be applied to pending-deploy outline polylines
and defines a concrete controller-level test seam for verifying latest-response
retention and old polyline removal.

## Findings

### Finding 1

Severity: Major

Category: Regression Plan

Description:

`ScopeGutterPolylineController.drawFeatures()` applies `isGlobalVisible` to
the inner polyline only. A pending-deploy outline is created without a
visibility value, so it remains visible when the scope layer was hidden before
a successful replacement. The plan only asks for a visibility regression check;
it does not direct the implementation to apply the stored visibility state to
both polylines. This would violate AC-006.

Recommendation:

Update the implementation steps to apply `isGlobalVisible` to every polyline
created during replacement, including the pending-deploy outline, and add a
focused test or verifiable test seam for the hidden-before-replacement case.

Planning Response:

Resolved in `plan.md` by adding an implementation step requiring every created
polyline, including pending-deploy outlines, to apply the stored scope
visibility state. The test plan now requires hidden-before-replacement coverage
for both inner and outline handles.

Status: Resolved

### Finding 2

Severity: Major

Category: Test Plan

Description:

AC-002 requires proof that the controller retains only the latest response.
The plan defers the controller-test approach as "if Google Maps dependencies
can be isolated cleanly" and otherwise only specifies coordinator callback
tests. Those tests can prove that the latest success invokes a callback, but
cannot prove that `ScopeGutterPolylineController` removes old polyline entries
or retains only the new set.

Recommendation:

Choose and document one concrete test strategy before implementation: introduce
a narrow map/polyline abstraction or controller test seam, or add an Android
test that verifies removal and the controller entry set. The test must cover a
first successful response followed by a second response with different
`spiNum` values.

Planning Response:

Resolved in `plan.md` by choosing a narrow internal renderer/handle seam for
`ScopeGutterPolylineController` and adding a required
`ScopeGutterPolylineControllerTest` that replaces response A with response B,
asserts old handles are removed, and asserts `entries()` retains only response B.

Status: Resolved

## Reviewed Checklist

- Requirements and acceptance criteria: understood; AC-006 and AC-002 are now
  backed by concrete implementation and test instructions.
- Repository and affected modules: correct. Both map hosts delegate through
  `ScopeMapCoordinator` and each owns a `ScopeGutterPolylineController`.
- Architecture and dependencies: correct. `ScopeMapCoordinator` rejects stale
  responses before `onBeforeDraw` and the draw callback, so failure and stale
  paths can preserve the existing scope layer.
- Implementation steps: otherwise actionable; controller-level replacement is
  the appropriate shared behavior boundary.
- Risks, regression coverage, scope, and rollback: appropriate.
- Open questions: none requiring external clarification.

## Decision Notes

Implementation may begin. No production code was changed during this planning
revision.

## Revalidation

Review Date: 2026-09-04

Result: APPROVED

This independent revalidation confirmed that the revised plan resolves both
previous blocking findings. The controller-level replacement boundary is shared
by `MainActivity` and `MapWorkspaceFragment`; `ScopeMapCoordinator` invokes it
only after the latest successful response passes its execution-id guard. The
planned renderer seam provides direct unit-test evidence for AC-002, and the
explicit outline visibility requirement and test cover AC-006.

No new blocking issue was identified. The task may proceed to implementation.

## Revalidation 2

Review Date: 2026-09-04

Result: APPROVED

The latest plan remains feasible. Its replacement call sites match both current
`ScopeMapCoordinator` host bindings, and the coordinator invokes drawing only
for the latest successful execution. The plan keeps submitted-polyline cleanup
separate from scope-layer replacement, preserves non-auth failure behavior,
and provides focused coverage for retention, stale responses, visibility, and
explicit clear workflows.

No new blocking issue was identified. The task remains ready for implementation.
