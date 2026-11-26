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
status: closed
impact: The early-attachment defect is fixed in commit 705017e8; AC-006's out-of-order behavior remains NOT VERIFIED because its planned deterministic test is absent.
expected: Keep the previous complete overlay visible until the latest generation is ready, then atomically replace it.
actual: stageVisibility keeps candidate overlays unattached until source readiness completes, checks that the request is still latest, then clears and attaches on the main thread.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:288-317
resolution: Candidate attachment now occurs only inside the latest-ready commit. The required deferred out-of-order manager test is still missing and is recorded as NOT VERIFIED under AC-006.
next_action: closed
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
status: closed
impact: The blocking fallback readiness path is replaced by future composition; the focused regression test passes on a single-thread executor.
expected: A failed district source falls back to the full source without blocking the executor that loads that fallback.
actual: whenSourcesReady delegates to sourceReadiness, which handles primary failure by composing the fallback future without blocking.
evidence:
  - app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt:100-112
  - app/src/main/java/com/example/taoyuangutter/map/OfflineSourceReadiness.kt:5-17
  - app/src/test/java/com/example/taoyuangutter/map/OfflineSourceReadinessTest.kt:9-26
resolution: The previous deadlock defect is fixed and its regression test passes. Full provider fault-injection coverage remains NOT VERIFIED under AC-007.
next_action: closed
owner: developer
```

## ISS-009

```yaml
issue_id: ISS-009
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P2
title: Initial map-host instrumentation checks providers before async commit
status: closed
impact: The initial provider assertion race is fixed and verified on the current commit; all four host cases reached their post-commit checks, with three passing and one later rendering step blocked.
expected: Wait for the initial source-ready generation to commit before asserting active provider visibility, then verify rendering and lifecycle.
actual: All provider-visibility assertions now use a bounded wait and main-thread reads. The current emulator run passed the initial visibility checks in all four cases; the remaining failure occurs later at the GoogleMap camera-idle wait in the MainActivity screenshot case.
evidence:
  - app/src/androidTest/java/com/example/taoyuangutter/map/OfflinePolygonMapHostInstrumentedTest.kt:88-107
  - app/src/androidTest/java/com/example/taoyuangutter/map/OfflinePolygonMapHostInstrumentedTest.kt:576-612
resolution: Initial provider-visibility race fixed and verified; camera-idle environment issue tracked separately as ISS-010.
next_action: closed
owner: developer
```

## ISS-010

```yaml
issue_id: ISS-010
task_id: feat-1118-2
phase: infrastructure
category: environment
priority: P2
title: Emulator Maps SDK lacks a valid API key for map-render evidence
status: closed
impact: The Maps SDK authorization blocker is resolved; the map snapshot assertion now executes.
expected: The emulator's GoogleMap camera settles and delivers the camera-idle callback so the offline polygon overlay snapshot can be checked.
actual: The first attempt was blocked by an AVD with Wi-Fi disabled and no active default network. Enabling Wi-Fi restored connectivity. The required rerun then failed at the same camera-idle assertion; its log reports `StatusCode=INVALID_ARGUMENT` and a Google Maps SDK authorization failure for the temporary `ci-placeholder` key. At that time the workspace had no valid local Maps key.
evidence:
  - /private/tmp/feat1118-2-verification-r4.d21a49a1/app/src/androidTest/java/com/example/taoyuangutter/map/OfflinePolygonMapHostInstrumentedTest.kt:425-437
  - /private/tmp/feat1118-2-verification-r4.d21a49a1/app/build/outputs/androidTest-results/connected/debug/Medium_Phone(AVD) - 16/logcat-com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest-mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers.txt
  - /private/tmp/feat1118-2-verification-r4.d21a49a1/app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 16.xml
resolution: A valid key was provided in ignored local.properties and copied only to the isolated test snapshot. Debug and AndroidTest APKs rebuilt successfully, and the previously blocked focused case passed 1/1. The later intermittent snapshot mismatch is tracked separately as ISS-011. No credential was added to task artifacts.
next_action: closed
owner: infrastructure
```

## ISS-011

```yaml
issue_id: ISS-011
task_id: feat-1118-2
phase: verification
category: implementation_regression
priority: P1
title: Map-host snapshot intermittently lacks the expected offline polygon fill
status: closed
impact: The intermittent snapshot assertion was repaired and the affected map-host instrumentation passed on the fixed revision; broader AC-010/AC-011 evidence remains incomplete.
expected: With the LEGACY provider active and the camera centered on a known polygon interior, the map snapshot pixel matches the opaque LEGACY fill.
actual: On the fixed revision with a valid Maps key, the focused case passed once. The four-case map-host class then failed 1/4 at the LEGACY snapshot pixel, expected #ff156d1d but observed #ffa3c2a1. The one permitted retry of that failed case passed. Direct provider tile checks and the other three map-host cases passed.
evidence:
  - app/src/androidTest/java/com/example/taoyuangutter/map/OfflinePolygonMapHostInstrumentedTest.kt:425-478
  - The four-case instrumentation output reports the LEGACY snapshot assertion failure at line 462; focused runs before and after it report OK (1 test).
resolution: The test-side fix in commit 94f5ba0a uses bounded snapshot polling while preserving the exact color assertion. Independent Verification on Android 16 Medium_Phone(AVD) passed the focused MainActivity case 1/1 and the complete map-host class 4/4. Other acceptance-criteria coverage gaps remain in verification.md.
next_action: closed
owner: developer
```
