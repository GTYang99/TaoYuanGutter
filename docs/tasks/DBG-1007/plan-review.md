# Plan Review

## Review Scope

Reviewed `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`, `root-cause.md`, `fix-plan.md`, and `ai/plan-critic-rules.md` for the edit-only current-location map behavior.

## Findings

### Finding 1
Severity: Suggestion
Category: Privacy and map behavior
Description: The reusable main-map location controller enables the Google Map My Location layer, which would show a location indicator beyond the approved viewport-only behavior.
Recommendation: Do not use that controller for the form map. Use the form's fused location client and move only the camera.
Planning Response: The plan explicitly avoids `MyLocationController` and calls out no location dot, marker, or persisted coordinate.
Status: Resolved

### Finding 2
Severity: Suggestion
Category: Async camera behavior
Description: A late location callback could override a manual pan while the form is waiting for a location fix.
Recommendation: Track user map movement and ignore later camera updates; test this behavior.
Planning Response: AC-009 and the device regression case cover the guard.
Status: Resolved

### Finding 3
Severity: Suggestion
Category: Permission coverage
Description: Existing code supports both fine and coarse permission checks, while location import requests fine permission only.
Recommendation: Accept either permission for viewport centering and test coarse-only grant behavior.
Planning Response: FR-008, AC-010, and the plan's permission tests cover fine/coarse handling.
Status: Resolved

### Finding 4
Severity: Suggestion
Category: Permission recovery
Description: The Settings action needs a defined return path so granting permission there can complete the original map-centering request.
Recommendation: Recheck permission when the form resumes from Settings; acquire one location fix if permission is now granted, otherwise leave the fallback usable.
Planning Response: FR-009, AC-011, and the implementation plan define that return behavior.
Status: Resolved

## Checklist

- Requirements understood: Yes
- Acceptance criteria complete and traceable: Yes, AC-001 through AC-011 map to implementation and validation.
- Repository analysis complete: Yes; form map, host map, existing location flows, and data boundaries are identified.
- Affected modules and dependencies identified: Yes.
- Risks and rollback reviewed: Yes.
- Test and regression plans cover the changed behavior: Yes; targeted policy tests, app build, and scoped device cases are listed.
- Open questions documented: None remain.
- Scope appropriate: Yes; normal existing-point edit flow only.

## Decision

**APPROVED** — implementation may begin on `codex/DBG-1007-minimap-location` from base commit `b5254c76d22b9eed77805b8d32601e2572358f9d`. The post-review Settings-return criterion AC-011 is covered by the reviewed implementation plan and test scope.

Blocking issues: None.

Non-blocking suggestions: The timeout is aligned with the existing 25-second nearby-location acquisition timeout; no new shared controller abstraction is needed.
