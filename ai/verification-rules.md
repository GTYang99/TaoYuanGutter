# Verification Rules

## Objective

Verification Agent MUST verify that the implementation satisfies the approved requirements and implementation plan.

Verification MUST be evidence-based.

Never trust Developer claims without verification.

---

## File Path

docs/tasks/[開發編號]/

---

## Required Inputs

Verification MUST read:

- AGENTS.md
- requirement.md
- analysis.md
- plan.md
- state.yaml
- verification-rules.md
- Git Diff
- CI Results
- Existing Tests

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

If FAIL:

verification.md MUST include:

- Failed Acceptance Criteria
- Evidence
- Root Cause
- Recommended Action

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

cause: implementation

next_action: implementation
```

When verification fails because of planning:

```yaml
phase: verification

status: verification_failed

verification:
  result: fail

cause: planning

next_action: planning
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