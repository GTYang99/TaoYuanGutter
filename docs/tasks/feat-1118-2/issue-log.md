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
title: All-district checkbox does not show its indeterminate state
status: open
impact: AC-003's required three-state all-selection control is not represented when only some districts are selected.
expected: The all-selection checkbox shows indeterminate when the selected count is between zero and thirteen.
actual: LayersBottomSheet only updates isChecked to whether all districts are selected; it never sets isIndeterminate.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt:96-105
next_action: debug
owner: developer
```

## ISS-005

```yaml
issue_id: ISS-005
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P1
title: District changes remove the visible result before the replacement is ready
status: open
impact: AC-006's retain-old-result and atomic-swap behavior is not met; the layer can disappear while new sources are staged/indexed.
expected: Keep the previous complete overlay visible until the latest generation is ready, then atomically replace it.
actual: OfflinePolygonOverlayManager clears all overlays immediately when the selected districts or generation changes.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:205-209
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
title: Per-district fallback queries every selected district
status: open
impact: AC-007 fallback for one failed district can duplicate data from other district files and violate source isolation.
expected: A district fallback queries only the failed district while successful district sources continue independently.
actual: OfflinePolygonTileProvider passes the complete selected-district set to the fallback data source and ignores the fallback definition's queryDistricts.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:52-61
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonSource.kt:29-38
next_action: debug
owner: developer
```

## ISS-007

```yaml
issue_id: ISS-007
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P2
title: Unused source and index databases are not released
status: open
impact: AC-008 requires removed sources, providers, caches, and indexes to release resources; source/index database handles remain retained after provider release.
expected: Release or evict no-longer-used source and index resources when a district or layer is removed.
actual: The static source-future cache has no eviction/close path, and tile-provider release only clears its tile cache.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt:59-77
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt:291-295
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:90-94
next_action: debug
owner: developer
```
