# Plan Review

Task: feat-0904
Reviewer: Plan Critic Agent
Review Iteration: 2
Review Date: 2026-09-04

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The revised plan is executable. The previous blocking issues were resolved by adding a concrete authenticated 401 call-site matrix, a per-surface draft-save contract, and a test strategy with an instrumentation seam fallback.

Implementation may begin.

---

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | Authenticated API 401 must force logout, save draft first when editing, and exclude login 401. |
| Acceptance Criteria complete | PASS | AC-001 through AC-014 are present and measurable. |
| Repository analysis complete | PASS | Revised analysis includes the concrete authenticated 401 call-site matrix requested in review iteration 1. |
| Architecture impact reasonable | PASS | Shared `AuthExpiredHandler` plus an `ApiResult.Error` helper fits the app's Activity/Fragment/controller structure. |
| Affected modules identified | PASS | Revised plan includes dashboard, main/map workspace, add/edit sheet, form, inspect, import, photo upload, and shared upload coordinator paths. |
| Dependencies identified | PASS | Existing `ApiResult.Error.code`, `AuthNavigator`, draft coordinator, Room draft store, and lifecycle/coroutine boundaries are identified. |
| Risks evaluated | PASS | Duplicate 401 events, login/logout distinction, upload propagation, and draft save timing risks are documented. |
| Test Plan complete | PASS | Revised plan names unit targets and an instrumentation strategy, with a test-only repository seam if needed. |
| Regression Plan complete | PASS | Login 401, logout 401, non-401 API errors, upload failures, draft preservation, and duplicate navigation are covered. |
| Open Questions documented | PASS | No external clarification is needed. |
| Implementation steps actionable | PASS | Steps now include code-aware propagation, handler integration, draft-save callbacks, import/upload coverage, and tests. |
| Task size appropriate | PASS | Large but bounded; implementation should stay scoped to auth-expired handling and required tests. |
| Rollback strategy (if applicable) | PASS | Rollback is documented as reverting shared auth-expired helper and call-site changes. |

---

# Findings

## Finding 1

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category:
Affected Modules / Acceptance Coverage

Description:
Iteration 1 found that the plan did not explicitly include all concrete authenticated 401 call sites.

Recommendation:
Maintain a call-site matrix covering all authenticated API consumers and their expected forced-logout behavior.

Planning Response:
Added `Authenticated 401 Call-Site Matrix` to `analysis.md`, covering `MainActivity`, `MapWorkspaceFragment`, `AddGutterBottomSheet`, `GutterFormActivity`, `GutterInspectActivity`, `ImportExistingWaypointActivity`, `ImportExistingWaypointBottomSheet`, `PhotoUploadManager`, `PhotoSlotUploadCoordinator`, `DashboardFragment`, and `ScopeMapCoordinator`.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 2

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category:
Draft Save Contract

Description:
Iteration 1 found that the optional `saveCurrentDraft` callback was not specific enough for each editable surface.

Recommendation:
Define the serialized data source, draft id rule, save method, and failure behavior per editing/photo surface.

Planning Response:
Added `Draft Save Contract` to `plan.md`, specifying each editable surface's serialized data source, draft id rule, save method, and save-failure behavior. The plan now requires draft save before Dialog/logout and uses existing draft paths where possible.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 3

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category:
Test Plan

Description:
Iteration 1 found that AC-013 and AC-014 were too conditional and did not identify concrete test targets.

Recommendation:
Name specific unit/instrumentation tests and add a test seam if current UI tests cannot inject 401 responses.

Planning Response:
Replaced the broad test plan with concrete test files/targets, including auth helper, once-only handler behavior, logout 401, upload/store 401 classification, draft save/preservation, dashboard 401 exposure, `ScopeMapCoordinator` code-aware propagation, and a required instrumentation seam fallback for AC-014.

Status:
- [ ] Open
- [x] Resolved

---

# Blocking Issues

None.

---

# Improvement Suggestions

- During implementation, verify names in the draft-save contract against existing methods. Some named paths, such as creating a missing `GutterFormActivity` draft id, may require a small implementation seam rather than direct reuse of today's private helpers.
- Keep the shared handler API narrow, for example `handleIfAuthExpired(...)`, so non-401 error flows can remain unchanged and readable.
- Avoid expanding repository responsibilities into UI navigation; keep repository output code-aware and let visible owners handle Dialog, draft save, and navigation.

---

# Decision

## APPROVED

Implementation may begin.

---

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
review:
  result: approved
  iteration: 2
```

---

# Review Notes

No requirement clarification is needed. The implementation agent should keep the change scoped to auth-expired handling, required draft preservation, and tests. Production code should not be modified beyond the approved affected modules unless a newly discovered 401 consumer is directly required for the acceptance criteria.

---

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated
