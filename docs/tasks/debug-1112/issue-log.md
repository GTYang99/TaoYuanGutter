# Issue Log

## ISS-DBG-1112-001

```yaml
issue_id: ISS-DBG-1112-001
task_id: debug-1112
phase: debug
category: implementation_regression
priority: P0
title: New D references XY_NUM of source C that is deleted in the same edit
status: open
impact: An edited gutter can submit D with C's XY_NUM while C is also omitted for deletion; if backend inheritance resolves after deletion, both C and D disappear.
expected: New D remains without C's node_id, and the client either rejects a deleted inheritance source or the backend atomically resolves C before deletion.
actual: D retains C's XY_NUM and has no node_id as intended, but the client has no check against the edit-start source list after C is deleted.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt:654-661
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:1437-1444
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1351-1374
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:2546-2554
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:872-895
next_action: implementation_debug
owner: developer
```

## ISS-DBG-1112-002

```yaml
issue_id: ISS-DBG-1112-002
task_id: debug-1112
phase: debug
category: implementation_regression
priority: P1
title: XY_NUM duplicate guard does not see deleted source points
status: open
impact: Deleting C before submit removes the second occurrence from the local list, so D can submit with C's XY_NUM without a duplicate warning or source-dependency error.
expected: Validation accounts for source points removed during this edit, or the backend contract explicitly guarantees atomic inheritance.
actual: validateWaypointPhotosAndFieldsOrAlert groups only nonblank current waypoint basicData[XY_NUM] values at submit time.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1935-1958
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:872-895
next_action: implementation_debug
owner: developer
```
