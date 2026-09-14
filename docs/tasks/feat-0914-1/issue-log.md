# Issue Log

```yaml
issue_id: ISS-0914-1-001
task_id: feat-0914-1
phase: knowledge_resolution
category: requirement_gap
priority: P1
title: API response does not define how to identify the logged-in account's mileage
status: resolved
impact: The two required mileage cards cannot be mapped to a response value without risking display of group or all-account totals.
evidence:
  - LoginActivity persists token, name, company, and group_id, but not username.
  - Dashboard response example exposes all/group/account dynamic keys without a current-account field or selection rule.
resolution: Save the login username and resolve the value by matching it against non-total dashboard survey-length account keys.
next_action: planning
owner: product_api
```

```yaml
issue_id: ISS-0914-1-002
task_id: feat-0914-1
phase: verification
category: environment
priority: P1
title: Authenticated dashboard and visual verification evidence unavailable
status: resolved
impact: Previously blocked AC-001, AC-004, and AC-005.
evidence:
  - XQ-AU52 authenticated dashboard entry sent same-day and no-date queries, both returned HTTP 200, and displayed the account-specific values.
  - XQ-AU52 same-day date search returned HTTP 200 and 0.18 km; clear restored empty state.
  - Device screenshots were stored under docs/tasks/feat-0914-1/evidence/.
resolution: Real-device authenticated dashboard and visual smoke test passed.
next_action: verification
owner: verification
```

```yaml
issue_id: ISS-0914-1-003
task_id: feat-0914-1
phase: verification
category: environment
priority: P1
title: CI evidence unavailable for verified revision
status: open
impact: Release cannot proceed despite all acceptance criteria passing.
evidence:
  - Repository contains no CI configuration.
  - No recorded CI build or test result exists for fa7bfdf.
next_action: infrastructure
owner: infrastructure
```
