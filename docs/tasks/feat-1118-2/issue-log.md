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

## ISS-004

```yaml
issue_id: ISS-004
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P2
title: All-district checkbox did not show its indeterminate state
status: closed
impact: Resolved and verified for AC-003 in commit 94bd7cce0801f578d5226b1148aa2c30d654692e.
expected: The all-selection checkbox shows indeterminate when the selected count is between zero and thirteen.
actual: LayersBottomSheet now maps NONE/PARTIAL/ALL into MaterialCheckBox.setCheckedState; the selection-state unit test and emulator selection interaction pass.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt:96-105
resolution: Tri-state mapping is implemented and verified in the current revision.
next_action: closed
owner: developer
```

## ISS-005

```yaml
issue_id: ISS-005
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P1
title: Candidate overlays attach before the replacement is ready
status: open
impact: AC-006 atomic switching is not guaranteed; a candidate can render while the previous complete result remains attached.
expected: Keep the previous complete overlay visible until the latest generation is ready, then atomically replace it.
actual: stageVisibility attaches candidate overlays before whenSourcesReady completes, then removes the previous overlays in a later main-thread callback.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:293-315
next_action: debug
owner: developer
```

## ISS-006

```yaml
issue_id: ISS-006
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P1
title: Per-district fallback query scope was not honored
status: closed
impact: The original query-scope defect is fixed and verified in commit 94bd7cce0801f578d5226b1148aa2c30d654692e.
expected: A district fallback queries only the failed district while successful district sources continue independently.
actual: OfflinePolygonTileProvider now uses each source definition's queryDistricts; OfflinePolygonSourceTest also verifies a single-district fallback mapping.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:56-71
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonSource.kt:31-56
resolution: Query scope is fixed. A separate fallback-readiness deadlock is tracked as ISS-008.
next_action: closed
owner: developer
```

## ISS-007

```yaml
issue_id: ISS-007
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P2
title: Unused source and index databases were not released
status: closed
impact: Source and index handles now close when their last provider reference is released; provider release behavior passed instrumentation.
expected: Release or evict no-longer-used source and index resources when a district or layer is removed.
actual: The source cache is reference-counted, its zero-reference path removes the entry and closes the loaded data source, and provider release clears the tile cache and decrements source references.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt:61-103
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt:316-324
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:117-125
resolution: The direct resource-retention defect is fixed and verified. The planned 20-round manager bound remains NOT VERIFIED under AC-008.
next_action: closed
owner: developer
```

## ISS-008

```yaml
issue_id: ISS-008
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P1
title: Fallback readiness can block the single-thread source executor
status: open
impact: AC-007 fallback may never become ready after a district source fails, preventing the latest selection from committing.
expected: A failed district source falls back to the full source without blocking the executor that loads that fallback.
actual: whenSourcesReady attaches a synchronous future.handle that calls fallbackFuture(...).get(); source and fallback loads use the same single-thread executor, so completion on that executor can wait for its own queued fallback task.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:100-115
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt:58-89
next_action: debug
owner: developer
```
