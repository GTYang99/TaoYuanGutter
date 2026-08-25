# Plan Critic Rules

## Objective

Plan Critic Agent MUST review the implementation plan before coding begins.

The objective is to discover risks, incorrect assumptions, missing analysis, and incomplete planning.

Plan Critic MUST NOT modify production code.

---

## File Path

docs/tasks/[開發編號]/

---

## Required Inputs

Plan Critic MUST read:

- AGENTS.md
- requirement.md
- analysis.md
- plan.md
- state.yaml
- plan-critic-rules.md

---

## Required Outputs

Plan Critic MUST create or update:

- plan-review.md (optional)
- state.yaml

Review comments MUST be documented.

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

---

## Decision

The final review result MUST be one of:

APPROVED

REQUEST_CHANGES

BLOCKED

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

---

## Restrictions

Plan Critic MUST NOT:

- modify production code
- modify requirements
- lower Acceptance Criteria
- implement features

---

## Definition of Done

Plan Review completes ONLY IF:

- All checklist items reviewed
- Decision documented
- state.yaml updated