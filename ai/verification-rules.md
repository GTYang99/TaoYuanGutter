# Verification Rules

## Objective

Verification Agent MUST verify that the implementation satisfies the approved requirements and implementation plan.

Verification MUST be evidence-based.

Never trust Developer claims without verification.

Verification MUST route implementation failures to Debug before any new code changes begin.

Developer MUST NOT directly patch a failed implementation without debug evidence.

---

## File Path

docs/tasks/[開發編號]/

---

## Required Inputs

Verification MUST read:

- AGENTS.md
- requirement.md
- knowledge-resolution.md
- analysis.md
- plan.md
- state.yaml
- verification-rules.md
- Git Diff
- CI Results
- Existing Tests
- Resolved fixed specifications recorded in knowledge-resolution.md

---

## Required Outputs

Verification MUST create or update:

- verification.md
- state.yaml

Verification SHOULD include supporting evidence.

---

## Verification Process

Verification MUST perform the following steps:

Requirement
↓

Acceptance Criteria
↓

Implementation Review
↓

Test Review
↓

Regression Review
↓

Evidence Collection
↓

Final Result

---

## Acceptance Criteria Review

Every Acceptance Criteria MUST be evaluated individually.

Each Acceptance Criteria MUST be marked as:

- PASS
- FAIL
- NOT VERIFIED

Verification MUST explain the reason.

---

## Evidence Rules

Every verification result MUST include evidence.

Evidence may include:

- Unit Test
- UI Test
- CI Result
- Source Code
- Git Diff
- Log
- Screenshot

Verification MUST NOT assume success without evidence.

---

## Verification Rules

Verification MUST:

- verify all Acceptance Criteria
- verify implementation follows plan.md
- verify tests are sufficient
- verify regression risks
- verify CI results
- identify missing test coverage
- identify implementation deviations
- classify any blocking issue with priority and category
- reference or create a related issue record when verification fails
- verify applicable fixed product rules
- verify implementation against constraints in knowledge-resolution.md
- verify referenced design behavior and API contracts
- verify reusable asset and component constraints
- record the failed specification ID when a specification check fails

A missing, unclear, or conflicting specification MUST create or update a `requirement_gap` issue. Verification records failure category `requirement` and routes it to Knowledge Resolution.

An implementation that differs from a fixed specification MUST create or update an `implementation_regression` issue. Verification records failure category `implementation` and routes it to Debug.

For refactor tasks, Verification MUST additionally confirm:

- Existing user-visible behavior and entry points are preserved.
- Existing API requests/responses and persistence behavior are preserved unless explicitly approved in a separate task.
- Regression tests cover the behavior identified in the refactor plan.
- The diff contains no unplanned feature or requirement change.
- Each refactor commit remains logically reviewable and independently reversible where practical.

---

## Regression Rules

Verification MUST verify:

- Existing functionality
- Modified functionality
- Related modules
- Edge cases
- Error handling

Regression MUST be included in verification.md.

---

## Verification Result

The final result MUST be one of:

PASS

FAIL

NOT VERIFIED

If FAIL, Verification MUST:

- classify the failure category
- identify failed acceptance criteria
- provide concrete evidence
- recommend the next action
- determine whether debug is required before re-implementation

---

## Update state.yaml

When verification starts:

```yaml
phase: verification

status: verification_in_progress

next_action: verification
```

When verification passes:

```yaml
phase: verification

status: verification_passed

verification:
  result: pass

next_action: release
```

When verification fails because of implementation:

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: implementation
  failed_acceptance_criteria:
    - AC-003

next_action: debug
```

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: planning

next_action: planning
```

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: requirement

next_action: knowledge_resolution
```

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: environment

next_action: infrastructure
```

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: unknown

next_action: investigation
```

---

## Restrictions

Verification MUST NOT:

- modify production code
- modify requirements
- rewrite implementation plan
- modify test evidence
- ignore failed Acceptance Criteria
- assume implementation is correct

---

## Definition of Done

Verification completes ONLY IF:

- verification.md completed
- Every Acceptance Criteria evaluated
- Evidence attached
- state.yaml updated
- Final verification result generated
