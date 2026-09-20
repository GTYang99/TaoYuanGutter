# Issue Log

## ISS-DBG-0920-001

```yaml
issue_id: ISS-DBG-0920-001
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P1
title: Tie-in point exemption was historically omitted from shared form rules
status: verified
impact: Tie-in points could be treated as ordinary points and require exempt fields/photos.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterCompletionPolicy.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
  - commit 07e3b5a
next_action: release
owner: developer
```

## ISS-DBG-0920-002

```yaml
issue_id: ISS-DBG-0920-002
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P2
title: Existing-waypoint import shows an unwanted missing-photo Toast
status: verified
impact: Import completes with an unexpected instruction to retake photos.
repro_steps:
  - Open the existing-waypoint import flow.
  - Import a point whose one or more photo URLs are unavailable.
expected: The specified missing-photo completion message is not shown.
actual: GutterFormActivity emits the message when a local slot is empty.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt:714-727
next_action: release
owner: developer
```

## ISS-DBG-0920-003

```yaml
issue_id: ISS-DBG-0920-003
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P1
title: Inspection edit entry shows a confirmation dialog
status: verified
impact: Users cannot enter edit directly and receive an unrequested confirmation step.
repro_steps:
  - Open gutter inspection.
  - Tap the edit button for a gutter with no server photos or preload photo issues.
expected: Edit preload proceeds without the 進入編輯確認 dialog.
actual: GutterInspectActivity routes the condition to showEditEntryConfirmation.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt:301-310
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt:323-330
next_action: release
owner: developer
```

## ISS-DBG-0920-004

```yaml
issue_id: ISS-DBG-0920-004
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P2
title: Severe silt is rendered as medium in inspection
status: verified
impact: Inspection shows a product option that should not exist and misrepresents saved data.
repro_steps:
  - Select 淤積程度=嚴重 in the form.
  - Open inspection for the saved gutter.
expected: Display 嚴重.
actual: Form encodes severe as 2; inspection maps 2 to 中度.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:2129-2133
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectPhotosFragment.kt:485-490
next_action: release
owner: developer
```

## ISS-DBG-0920-005

```yaml
issue_id: ISS-DBG-0920-005
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P1
title: Inspection edit treats backend-generated measurement id as user-required
status: verified
impact: Edit flow can show a required marker or block on measurement coordinate number that the user should not enter.
repro_steps:
  - Open a gutter through inspection.
  - Tap edit and enter the gutter form.
  - Observe the measurement coordinate number field/validation.
expected: Backend-provided measurement id is used when available; the user is not blocked on manually entering it.
actual: Edit/view mode enables the required marker and validation for XY_NUM.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:937-939
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:1148-1163
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:734
  - app/src/main/java/com/example/taoyuangutter/gutter/WaypointAdapter.kt:30-67
next_action: release
owner: developer
```

## ISS-DBG-0920-006

```yaml
issue_id: ISS-DBG-0920-006
task_id: debug-0920-1
phase: verification
category: implementation_regression
priority: P1
title: Tie-in point selection skips clear confirmation
status: verified
impact: Selecting 銜接點 could leave incompatible detail fields, photos, and connecting-pipe data in the form without warning.
expected: Show the specified confirmation message; cancel preserves data; confirm clears the shared detail state.
actual: The checkbox listener only applied mutual exclusion and notified the draft.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
  - commit 7c43493
  - GutterBasicInfoUiTest.tieInPointWarnsBeforeClearingAndCancelPreservesData
next_action: release
owner: developer
```

## ISS-DBG-0920-007

```yaml
issue_id: ISS-DBG-0920-007
task_id: debug-0920-1
phase: verification
category: implementation_regression
priority: P1
title: Cant-open clearing omits connecting-pipe selection
status: verified
impact: Selecting 無法開蓋 left the 接管／連接管 radio choice visible and retained in form state.
expected: The connecting-pipe selection is cleared with the other detail fields and restored when the transition is cancelled or reversed.
actual: The clear method, session field list, and restore renderer omitted IS_CONNECTING.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/CantOpenSessionViewModel.kt
  - commit 7c43493
  - GutterCantOpenUiTest.confirmDialogClearsAffectedFields
  - GutterCantOpenUiTest.snapshotSurvivesConfigurationRecreation
next_action: release
owner: developer
```

## ISS-DBG-0920-008

```yaml
issue_id: ISS-DBG-0920-008
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P1
title: Exempt-mode API payload still sends detail defaults and connecting-pipe flag
status: implemented_pending_verification
impact: Tie-in points receive MAT_TYP/NODE_DEP/NODE_WID/COVER_DEP/IS_BROKEN/IS_HANGING/IS_SILT defaults, and cant-open/tie-in nodes still send IS_CONNECTING instead of omitting the parameter.
expected: For IS_CANTOPEN or IS_TIEINPOINT, omit all exempt detail parameters and IS_CONNECTING from storeDitch JSON; retain mode flags.
evidence:
  - app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt:14-60
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:1302-1324
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt:1977-1986
  - app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt:51-79
next_action: verification
owner: developer
```

## ISS-DBG-0920-009

```yaml
issue_id: ISS-DBG-0920-009
task_id: debug-0920-1
phase: debug
category: implementation_regression
priority: P1
title: Inspection renderer does not apply tie-in detail exemption
status: implemented_pending_verification
impact: A nodeDetails response with IS_TIEINPOINT=1 still displays measurements, attributes, and exempt photos in inspection.
expected: Tie-in inspection display matches cant-open detail suppression.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectPhotosFragment.kt:161-212
  - /Users/a10362/Desktop/markdown file/ty_debug_0920-1.md
next_action: verification
owner: developer
```

## ISS-DBG-0920-010

```yaml
issue_id: ISS-DBG-0920-010
task_id: debug-0920-1
phase: implementation_debug
category: implementation_regression
priority: P1
title: Inspection-to-edit converts missing connection attribute to 無
status: implemented_pending_verification
impact: Existing 銜接點／無法開蓋 data with omitted IS_CONNECTING opens the edit form with the 連接管 radio button 無 selected.
expected: Missing IS_CONNECTING remains absent; both 連接管 radio buttons remain unselected and disabled for exempt modes; the storeDitch request omits the parameter.
actual: Inspection preload and form prefill defaulted missing IS_CONNECTING to 0, while the UI mutual-exclusion code only disabled the group.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt
  - app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt
next_action: verification
owner: developer
```
