# Issue Log

## ISS-001

```yaml
issue_id: ISS-001
task_id: feat-0917
phase: knowledge_resolution
category: requirement_gap
priority: P1
title: Required API contracts for no-parameter import and point-property readback are absent
status: resolved
impact: Resolved by the updated requirement and the user's explicit XY_NUM decision.
evidence:
  - GutterApiService.getClosestNodeDetails requires lng and lat.
  - NodeDetails has no is_connect_point or is_connect_pipe mapping.
  - Updated requirement: no-query GET returns NodeDetails list; same-name Boolean fields default false.
  - User decision: new storeDitch omits XY_NUM and consumes system-generated XY_NUM response.
next_action: planning
owner: planning
```

## ISS-002

```yaml
issue_id: ISS-002
task_id: feat-0917
phase: knowledge_resolution
category: requirement_gap
priority: P2
title: Virtual-point applicability of connect point and connect pipe is unspecified
status: resolved
impact: Resolved by explicit virtual-point reset and payload-omission rules.
evidence:
  - GutterBasicInfoFragment.setVirtualMode hides Cant Open and all detailed fields, retaining only Pending Deploy.
  - Figma node 1693:15034 shows the virtual-point variant without the new controls.
  - User decision: virtual points turn off and reset Cant Open/Connect Point, turn off Connect Pipe, and omit all three payload keys.
next_action: planning
owner: planning
```

## ISS-003

```yaml
issue_id: ISS-003
task_id: feat-0917
phase: plan_review
category: planning_gap
priority: P1
title: Inspect-to-edit response mapping for new point attributes is absent from the plan
status: resolved
impact: Resolved by adding the inspect-to-edit owner, explicit basicData mapping and no-op edit preservation test to the plan.
evidence:
  - GutterInspectActivity.kt maps NodeDetails into editable waypoint basicData.
  - Revised plan identifies GutterInspectActivity.kt and requires nullable Boolean mapping with false fallback before edit form construction.
  - AC-001 and AC-002 require readback and edit persistence for non-virtual points.
next_action: plan_review
owner: planning
```

## ISS-004

```yaml
issue_id: ISS-004
task_id: feat-0917
phase: plan_review
category: planning_gap
priority: P1
title: New attributes are not planned across the Fragment argument and prefill boundary
status: resolved
impact: Resolved by explicitly extending the Fragment argument contract, whitelist and prefill path, with an end-to-end inspect-to-edit regression scenario.
evidence:
  - GutterInspectActivity.kt writes the editable waypoint basicData.
  - GutterBasicInfoFragment.newInstance() copies only explicit keys to arguments.
  - Revised plan requires constants, newInstance() whitelist and prefillData() mappings for both keys with false fallback.
next_action: plan_review
owner: planning
```

## ISS-005

```yaml
issue_id: ISS-005
task_id: feat-0917
phase: plan_review
category: planning_gap
priority: P1
title: Plan expands Connect Point into Cant Open-equivalent data and photo restrictions
status: resolved
impact: Resolved by limiting Connect Point to mutual exclusion and explicitly preserving normal details, validation and photo slots.
evidence:
  - requirement.md requires mutual exclusion only.
  - Cant Open currently clears detailed fields and suppresses photo slots 2 and 3.
  - Revised plan confines Cant Open restrictions to Cant Open and adds Connect Point preservation regression coverage.
next_action: plan_review
owner: planning
```

## ISS-006

```yaml
issue_id: ISS-006
task_id: feat-0917
phase: debug
category: implementation_regression
priority: P1
title: Successfully uploaded gutter remains in Add Gutter List
status: resolved
impact: A completed multi-gutter session item can be shown again and selected as if it were pending.
evidence:
  - MapWorkspaceFragment.finalizePhotoUploadFlow defers multi-session draft cleanup by assigning pendingSuccessfulMultiDraftCleanup when SPI_NUM is present.
  - The actual deleteDraftAndLocalPhotos and multiGutterSessionCoordinator.remove calls occur only in inspectLauncher return handling.
  - AddGutterListBottomSheet receives a one-time drafts snapshot and exposes no successful-item removal or refresh operation.
  - Fixed in commit 75758b8: successful upload now immediately removes the draft before opening inspection.
next_action: verification
owner: developer
```

## ISS-007

```yaml
issue_id: ISS-007
task_id: feat-0917
phase: debug
category: implementation_regression
priority: P1
title: Scope-search gutter segments are hidden after returning from inspection
status: resolved
impact: After add, upload, inspect, and back navigation, the map can appear to have no gutter segments even when scopeSearch has reloaded them.
evidence:
  - inspectLauncher reloads the viewport through loadGuttersByViewport before reopening the multi-gutter list.
  - showAddGutterList then unconditionally called scopeGutterPolylineController.setVisible(false).
  - Fixed in commit d92885a: list opening now follows the main map layer toggle (showPlan) instead of hiding the scope layer.
next_action: verification
owner: developer
```

## ISS-008

```yaml
issue_id: ISS-008
task_id: feat-0917
phase: planning
category: requirement_gap
priority: P1
title: Approved API and UI contract changed after prior implementation and verification
status: resolved
impact: Prior implementation, tests, execution report, and verification evidence use obsolete lowercase keys, integer request values, Boolean responses, and outdated UI wording/behavior.
evidence:
  - Current source and tests use is_connect_point/is_connect_pipe with Int request and Boolean read DTO fields.
  - Updated decisions require IS_TIEINPOINT/IS_CONNECTING Boolean request fields and String read fields.
  - User resolved virtual-point, draft, mutual-exclusion, anomalous-response, label-order, and wording rules.
next_action: plan_review
owner: planning
```

## ISS-009

```yaml
issue_id: ISS-009
task_id: feat-0917
phase: plan_review
category: planning_gap
priority: P1
title: Pending-deploy label test uses the unrelated IS_HANGING field
status: resolved
impact: Resolved by using `IS_PENDING_DEPLOY`/`node.isPendingDeploy` for the pending-deploy fixture and retaining `IS_HANGING` as independent hanging-pipeline coverage.
evidence:
  - requirement.md specifies the label when the point is pending deploy.
  - Revised plan step 5 and test-data.md R-02 use IS_PENDING_DEPLOY/node.isPendingDeploy and assert the exact label order.
  - IS_HANGING remains the hanging-pipeline field.
next_action: implementation
owner: planning
```

## ISS-010

```yaml
issue_id: ISS-010
task_id: feat-0917
phase: plan_review
category: planning_gap
priority: P1
title: Failed node-details preload can overwrite new server values during inspect-to-edit
status: resolved
impact: Resolved by blocking a submittable edit form whenever any node-details preload fails, preventing a DitchNode-only fallback from submitting false values.
evidence:
  - GutterInspectActivity permits continuation with ditchToWaypoints after node-details preload failure.
  - The fallback has no node-details-only IS_TIEINPOINT or IS_CONNECTING values.
  - Revised plan step 3, Failure Behavior, test-data.md F-01/F-02, and the Test Plan require retry/cancel for detail failures and preserve the photos-only warning path when details are complete.
next_action: implementation
owner: planning
```
