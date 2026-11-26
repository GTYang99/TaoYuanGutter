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
impact: Resolved for planning readiness; the revised plan defines reproducible full-versus-district content comparison and failure evidence.
evidence:
  - requirement.md AC-001 requires host/CI validation of hashes, metadata, counts, and mapping.
  - knowledge-resolution.md states that full-versus-district consistency must be generated in the host-side manifest.
  - The initial plan.md lacked the comparison algorithm and failure rules.
  - Revised plan.md at 4635d6a defines a host-side validator, row signatures excluding district-local fid, exact signature-multiset comparison, and missing/extra/duplicate/misassigned failure cases.
resolution: Plan Critic re-review confirmed the plan is specific enough to begin implementation; the validator remains planned work and has not yet been run.
next_action: implementation
owner: planning
```

## ISS-003

```yaml
issue_id: ISS-003
task_id: feat-1118-2
phase: implementation
category: requirement_gap
priority: P1
title: Possible district GPKGs are not exact partitions of the packaged full source
status: resolved
impact: Per-district label differences exist, but the user accepted the supplied district files as authoritative for partial selection.
evidence:
  - docs/tasks/feat-1118-2/evidence/asset-manifest.json reports metadata PASS and partition_consistency FAIL.
  - Possible split-only rows: 桃園區 2, 大溪區 1, 楊梅區 35, 蘆竹區 2, 龜山區 12, 八德區 4, 龍潭區 30, 復興區 13.
  - Existing-gutter partitions match their full source after filtering TOWN and normalizing fractional-zero datetime formatting.
resolution: User approved trusting the supplied source files. The validator now gates on metadata and full/district geometry union consistency; per-district label differences remain diagnostic evidence.
next_action: verification
owner: developer
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
impact: Resolved for planning readiness; the revised plan defines deterministic latest-generation and bounded-lifecycle assertions.
evidence:
  - requirement.md AC-006 requires atomic latest-selection switching and forbids simultaneous full/district display.
  - requirement.md AC-008 requires releasing unused resources and avoiding unbounded provider, overlay, or cache accumulation.
  - The initial plan.md described rapid switching and memory/overlay observations without controlled completion ordering or explicit resource bounds.
  - Revised plan.md at 4635d6a defines out-of-order generation completion, a fixed 20-round switching sequence, active provider/overlay bounds, a 48-entry-per-provider cache bound, and zero resources after release.
resolution: Plan Critic re-review confirmed these assertions make AC-006 and AC-008 implementation-ready.
next_action: implementation
owner: planning
```
