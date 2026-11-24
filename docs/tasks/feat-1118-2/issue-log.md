# Issue Log

## ISS-001

```yaml
issue_id: ISS-001
task_id: feat-1118-2
phase: plan_review
category: planning_gap
priority: P1
title: Full and district GPKG consistency has no reproducible pass-fail method
status: resolved
impact: AC-001 data-integrity evidence cannot demonstrate that the district sources faithfully map to the complete datasets.
evidence:
  - requirement.md AC-001 requires host/CI validation of hashes, metadata, counts, and mapping.
  - knowledge-resolution.md states that full-versus-district consistency must be generated in the host-side manifest.
  - plan.md says to compare the district collections with the full sources but does not specify the comparison algorithm or failure rules.
next_action: plan_review
owner: planning
```

## ISS-002

```yaml
issue_id: ISS-002
task_id: feat-1118-2
phase: plan_review
category: planning_gap
priority: P1
title: Latest-generation switching and resource bounds lack deterministic test assertions
status: resolved
impact: AC-006 and AC-008 cannot be reproducibly verified from the planned concurrent-switch and lifecycle observations.
evidence:
  - requirement.md AC-006 requires atomic latest-selection switching and forbids simultaneous full/district display.
  - requirement.md AC-008 requires releasing unused resources and avoiding unbounded provider, overlay, or cache accumulation.
  - plan.md describes rapid switching and memory/overlay observations without a controlled completion-order test or explicit resource bound.
next_action: plan_review
owner: planning
```
