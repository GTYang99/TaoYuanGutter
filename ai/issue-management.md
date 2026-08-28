# Issue Management

## Objective

Issue Management tracks blockers, regressions, and requirement mismatches without mixing them into the task lifecycle itself.

Task state records the main workflow.
Issue records explain what went wrong, how urgent it is, and which phase should handle it next.

---

## Issue Lifecycle

```
Detected
↓
Triage
↓
Classified
↓
Prioritized
↓
Assigned to Phase
↓
Resolved
↓
Verified
↓
Closed
```

---

## Issue Types

- `requirement_gap`
- `implementation_regression`
- `verification_failure`
- `environment`
- `unknown`
- `enhancement_request`

---

## Priority Rules

- `P0`: core function broken, data risk, or broad regression
- `P1`: major flow blocked, but workaround exists
- `P2`: local defect, edge case, or UI degradation
- `P3`: cleanup, polish, or non-blocking improvement

---

## Phase Routing

- `requirement_gap` -> `planning`
- `implementation_regression` -> `debug`
- `verification_failure` -> `debug`
- `environment` -> `infrastructure`
- `unknown` -> `investigation`
- `enhancement_request` -> backlog or a separate task

---

## Recommended Fields

```yaml
issue_id: ISS-001
task_id: TYG-205
phase: implementation
category: implementation_regression
priority: P0
title: Main button stopped working
status: open
impact: Core workflow blocked
repro_steps:
  - Open main screen
  - Tap primary button
expected: Button should navigate to form
actual: No response
evidence:
  - screenshot
  - logcat
next_action: debug
owner: developer
```

---

## Usage Notes

- Keep `state.yaml` focused on the task's main phase.
- Record issue details in a separate issue log when a problem appears.
- Always classify before changing code.
- Do not treat a requirement mismatch as a code fix.
