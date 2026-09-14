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
