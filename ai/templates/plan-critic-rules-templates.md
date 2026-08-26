# Plan Review

Task: TYG-XXX
Reviewer: Plan Critic Agent
Review Iteration: 1
Review Date: YYYY-MM-DD

---

# Summary

## Review Result

- [ ] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Briefly summarize the review outcome.

Example:

The implementation plan is generally executable.
Two blocking issues were identified and must be resolved before implementation.

---

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS / FAIL | |
| Acceptance Criteria complete | PASS / FAIL | |
| Repository analysis complete | PASS / FAIL | |
| Architecture impact reasonable | PASS / FAIL | |
| Affected modules identified | PASS / FAIL | |
| Dependencies identified | PASS / FAIL | |
| Risks evaluated | PASS / FAIL | |
| Test Plan complete | PASS / FAIL | |
| Regression Plan complete | PASS / FAIL | |
| Open Questions documented | PASS / FAIL | |
| Implementation steps actionable | PASS / FAIL | |
| Task size appropriate | PASS / FAIL | |
| Rollback strategy (if applicable) | PASS / FAIL / N/A | |

---

# Findings

## Finding 1

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [ ] Suggestion

Category:

Description:

Recommendation:

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [ ] Open
- [ ] Resolved

---

## Finding 2

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [ ] Suggestion

Category:

Description:

Recommendation:

Planning Response:

Status:
- [ ] Open
- [ ] Resolved

---

(Add additional findings as needed.)

---

# Blocking Issues

List ONLY issues that prevent implementation.

Example:

- Regression Plan missing.
- API dependency not analyzed.

If none:

None.

---

# Improvement Suggestions

List non-blocking recommendations.

Example:

- Rename Repository methods.
- Simplify ViewModel responsibilities.

If none:

None.

---

# Decision

## APPROVED

Implementation may begin.

OR

## REQUEST_CHANGES

Planning Agent should update the implementation plan and submit for review again.

OR

## BLOCKED

Planning cannot continue until external clarification or human decision is provided.

---

# Next Action

- [ ] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# state.yaml Update

```yaml
phase: plan_review

status:

next_action:

review:
  result:
  iteration:
```

---

# Review Notes

Additional comments or observations.

---

# Definition of Done

Before closing this review, confirm:

- [ ] All checklist items reviewed
- [ ] Findings documented
- [ ] Blocking Issues identified (if any)
- [ ] Improvement Suggestions separated
- [ ] Decision recorded
- [ ] state.yaml updated