# Issue Log

## ISS-DBG-1112-001

```yaml
issue_id: ISS-DBG-1112-001
task_id: debug-1112
phase: debug
category: implementation_regression
priority: P0
title: Import allows XY_NUM already used by another point in the same ditch
status: open
impact: An edited gutter can import C into new D, retain C's XY_NUM, then delete C and submit an invalid inheritance dependency.
expected: Import is rejected when the imported XY_NUM already exists in the current ditch, with the message 不可匯入本側溝中的同樣點位.
actual: Import does not compare the imported XY_NUM with current ditch waypoints; D is populated with C's data and the later submit-time check can be bypassed by deleting C.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt:654-661
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:1437-1444
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt:55-77
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1351-1374
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1935-1958
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
title: Submit-time XY_NUM guard is too late for the import flow
status: open
impact: Deleting C before submit removes the second occurrence from the local list, so D can submit with C's XY_NUM without a duplicate warning; the primary prevention must happen during import.
expected: Import-time validation rejects an imported XY_NUM already present in the current ditch; submit-time validation remains a secondary defense.
actual: validateWaypointPhotosAndFieldsOrAlert groups only nonblank current waypoint basicData[XY_NUM] values at submit time.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1935-1958
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:872-895
next_action: implementation_debug
owner: developer
```
