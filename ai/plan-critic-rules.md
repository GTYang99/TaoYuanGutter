# Plan Critic Rules

## Objective

Plan Critic Agent MUST review the implementation plan before coding begins.

The objective is to discover risks, incorrect assumptions, missing analysis, and incomplete planning.

Plan Critic MUST NOT modify production code.

The goal is to determine implementation readiness.

---

## File Path

docs/tasks/[開發編號]/

---

## Review Principle

The objective is to determine whether implementation can safely begin.

The objective is NOT to produce the perfect implementation plan.

Plan Critic MUST distinguish:

Blocking Issues

from

Improvement Suggestions.

---

## Required Inputs

Plan Critic MUST read:

- AGENTS.md
- requirement.md
- knowledge-resolution.md
- analysis.md
- plan.md
- state.yaml
- plan-critic-rules.md
- docs/index.md
- Referenced fixed specifications

---

## Required Outputs

Plan Critic MUST create or update:

- plan-review.md
- state.yaml

Review comments MUST be documented.

---
## Review comments

### Finding 1
Severity: Major
Category: Regression Plan
Description:
...
Recommendation:
...

### Finding 2
Severity: Suggestion
Category: Naming
Description:
...
Recommendation:
...

## Finding 3

Severity:
Major

Category:
Regression Plan

Description:
Regression tests are incomplete.

Recommendation:
Add regression tests for upload retry.

Planning Response:
Added regression tests in plan.md Section 6.

Status:
Resolved

---

## Review Process

Requirement Review
↓

Acceptance Criteria Review
↓

Repository Analysis Review
↓

Architecture Review
↓

Implementation Plan Review
↓

Risk Review
↓

Decision

---

## Review Checklist

Plan Critic MUST verify:

- Every referenced specification ID exists and is `fixed`
- Knowledge Resolution decision is `READY_FOR_PLANNING`
- Planning constraints match knowledge-resolution.md
- Specification versions are recorded
- Implementation steps trace to a requirement or specification ID
- No fixed specification is overridden
- No unresolved `draft` or `TBD` content enters Implementation
- Requirements are fully understood
- Acceptance Criteria are complete
- Repository analysis is complete
- Affected modules are correct
- Dependencies are identified
- Risks are evaluated
- Test Plan is complete
- Regression Plan is complete
- Open Questions are documented
- Implementation steps are actionable
- Scope is appropriate
- Task size is appropriate
- Rollback strategy exists (if applicable)

---

## Decision

The final review result MUST be one of:

APPROVED

REQUEST_CHANGES

BLOCKED

---

### REQUEST_CHANGES

Planning Agent can reasonably resolve the identified Blocking Issues.

### BLOCKED

Planning Agent cannot resolve the identified Blocking Issues without external clarification or decision.

---

## Update state.yaml

When review starts:

```yaml
phase: plan_review

status: review_in_progress

next_action: plan_review
```

When approved:

```yaml
phase: plan_review

status: approved

next_action: implementation
```

When changes are required:

```yaml
phase: plan_review

status: changes_requested

next_action: planning
```

When Knowledge Resolution is missing, stale, or invalid:

```yaml
phase: plan_review
status: blocked
reason: knowledge_resolution_invalid
next_action: knowledge_resolution
```

```yaml
When blocked:

phase: plan_review

status: blocked

reason: requirement_unclear

next_action: requirement_clarification
```

---

## Restrictions

Plan Critic MUST NOT:

- modify production code
- modify requirements
- lower Acceptance Criteria
- implement features
- resolve missing specifications on behalf of Knowledge Resolution

---

## Definition of Done

Blocking Issues documented.

Suggestions documented separately.

Plan Review completes ONLY IF:

- All checklist items reviewed
- Decision documented
- state.yaml updated

---

## Issue Severity

Every finding MUST be classified as one of:

Critical

Major

Minor

Suggestion

---

## Review Flow

Planning
    │
    ▼
Plan Review
    │
 ┌──┼─────────────┐
 │  │             │
 │  │             │
 ▼  ▼             ▼
APPROVED    REQUEST_CHANGES   BLOCKED
 │              │               │
 ▼              ▼               ▼
Developer     Planning      Requirement
                               Clarification
                            or Human Review
