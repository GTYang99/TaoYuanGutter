# Plan Review

Task: feat-0923  
Reviewer: Plan Critic Agent  
Review Iteration: 3  
Review Date: 2026-09-23

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The revised plan resolves the previous blocking findings. It now defines `GutterRepository.storeDitch` method entry as the shared submission boundary for add and edit flows, persists the marker before the Retrofit request, specifies missing draft-id/row behavior, and adds named ordering, migration, and pending-list UI tests. The plan is implementation-ready.

No production code was modified during this review. Existing unrelated working-tree changes remain outside this task scope.

---

# Review Scope and Evidence

- Required inputs reviewed: `AGENTS.md`, `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`, `ai/plan-critic-rules.md`.
- Supporting rules/templates reviewed: `ai/planning-rules.md`, `ai/templates/plan-template.md`, `ai/templates/plan-critic-rules-templates.md`, `ai/architecture.md`, `ai/testing-rules.md`, `ai/verification-rules.md`.
- Repository evidence reviewed: current `GutterRepository.storeDitch`, both `AddGutterBottomSheet` submission paths, both `LocationPickerHost` implementations, pending draft persistence/coordinator/database/adapter/layout, existing tests, and current dependency configuration.
- The revised planning evidence was also reviewed in `root-cause.md`.
- Working-tree context: branch `feat/草稿tag`; preserve the existing `gradle/libs.versions.toml` change, `.worktrees/` untracked content, and the unrelated existing `AddGutterBottomSheet.kt` message change.

---

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | The revised plan explicitly defines method entry as the observable submission boundary and records the local/remote atomicity limitation. |
| Acceptance Criteria complete | PASS | AC-001 and AC-002 now have a precise boundary and both add/edit paths; AC-003 to AC-005 have named validation. |
| Repository analysis complete | PASS | The analysis now includes `GutterRepository.kt`, missing-row behavior, existing callbacks, and the current test limitations. |
| Architecture impact reasonable | PASS | Persistence remains in `pending`, the boundary remains in the repository, and UI hosts only provide the marker callback. |
| Affected modules identified | PASS | The revised file list covers persistence, API boundary, both hosts, UI resources, JVM tests, and instrumentation tests. |
| Dependencies identified | PASS | Room versioning, coordinator/repository, callbacks, and existing badge styling are identified. |
| Risks evaluated | PASS | Crash ordering, migration, overwrite, legacy drafts, and layout risks are addressed. |
| Test Plan complete | PASS | Ordering, missing-field serialization, migration fixture, adapter binding, regression, build, and instrumentation targets are named. |
| Regression Plan complete | PASS | Restore, delete, success cleanup, offline/curve drafts, error/timeout, and legacy compatibility are covered. |
| Open Questions documented | PASS | Legacy history remains explicitly false by compatibility default and is separated from the resolved boundary decision. |
| Implementation steps actionable | PASS | Steps specify the method signature, callback order, missing-row policy, UI behavior, and validation commands. |
| Task size appropriate | PASS | The scope remains coherent and does not introduce unrelated product work. |
| Rollback strategy (if applicable) | PASS | Rollback is limited to the task changes and preserves unrelated workspace changes. |

---

# Findings

## Finding 1

Severity:
- [x] Critical
- [ ] Major
- [ ] Minor
- [ ] Suggestion

Category: Submission Boundary / Acceptance Criteria Alignment

Description:

The previous plan marked the draft from Host callbacks before entering `repository.storeDitch(...)`, leaving a crash window that could violate AC-002.

Recommendation:

Use one shared repository method-entry boundary, define the marker callback as the first fallible operation, and specify behavior when the draft id or Room row is unavailable.

Planning Response:

`analysis.md` and `plan.md` now define `GutterRepository.storeDitch(request, token, onRequestEntered)` as the shared boundary. Both add/edit call sites pass the callback; fixed ids use ensure-and-mark, missing rows are created from the session snapshot, local marker failure stops the request, and direct inspect-edit without a pending draft id is an explicit no-op policy. The residual local-write/remote-request atomicity limitation is documented.

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

Category: Test Plan / Migration Evidence

Description:

The previous plan named migration and UI coverage without a reproducible fixture or exact assertions.

Recommendation:

Name the test files and define deterministic migration, ordering, adapter, and regression assertions.

Planning Response:

The revised plan names `GutterRepositoryStoreDitchBoundaryTest`, `GutterDraftSubmissionStateTest`, `GutterDraftDatabaseMigrationTest`, and `PendingDraftAdapterUiTest`. It specifies the fake API ordering assertions, manually created version-3 schema, version-4 migration expectations, tag text/visibility/background/stroke/padding, and click/long-click regression checks.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 3

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [x] Suggestion

Category: Plan Traceability

Description:

The previous plan did not distinguish the two Host implementations from the shared API boundary in its test seam.

Recommendation:

Name the exact owners and test locations.

Planning Response:

The revised affected-file list and traceability table identify `GutterRepository.kt`, `AddGutterBottomSheet.kt`, `MainActivity.kt`, `MapWorkspaceFragment.kt`, and each named test file. Add/edit boundary behavior is separately described while sharing the repository seam.

Status:
- [ ] Open
- [x] Resolved

---

## Finding 4

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [x] Suggestion

Category: Working Tree Safety

Description:

The current worktree contains an unrelated one-line change in `AddGutterBottomSheet.kt`, which is also an affected file in this plan.

Recommendation:

Implementation must preserve that existing line and keep it out of the task change set, alongside the already documented `gradle/libs.versions.toml` and `.worktrees/` content.

Planning Response:

Accepted as an implementation constraint. The current diff was inspected and is independent of the planned submission-boundary regions.

Status:
- [x] Open
- [ ] Resolved

---

# Blocking Issues

None.

---

# Improvement Suggestions

- During implementation, use a focused patch around the two `storeDitch` call sites so the unrelated `AddGutterBottomSheet.kt` message change is not overwritten.
- If the instrumentation environment is unavailable, record the affected checks as `NOT VERIFIED`; do not convert them to PASS from compilation alone.

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

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] `state.yaml` updated

