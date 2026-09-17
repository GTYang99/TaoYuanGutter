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
phase: verification
category: environment
priority: P1
title: Fixed-revision Gradle verification cannot start without Java Runtime
status: resolved
impact: The shell lacked a system Java Runtime, but the fixed-revision checks were completed using Android Studio's bundled JDK in a commit-only temporary clone.
repro_steps:
  - Create a source snapshot from commit 0595b7c7782e3041b3637ed7505d90d53d8fb3ba.
  - Run ./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon.
expected: Gradle starts and produces validation results.
actual: The environment reports that no Java Runtime can be located.
evidence:
  - docs/tasks/feat-0917/verification.md
  - Fixed commit-only clone validation completed successfully: 66 Gradle tasks, including unit tests, Android-test compilation, and APK assembly.
next_action: infrastructure
owner: infrastructure
```

## ISS-009

```yaml
issue_id: ISS-009
task_id: feat-0917
phase: verification
category: environment
priority: P1
title: Connected Android test run stalls on the Android 14 emulator
status: open
impact: The Sony test device completed all 36 tests, but the overall Gradle task cannot reach a terminal result because the Android 14 emulator produces no result file.
repro_steps:
  - Start app:connectedDebugAndroidTest from Android Studio.
  - Observe execution on Medium_Phone(AVD) - 14 and XQ-AU52 - 12.
expected: Both devices complete and Gradle reports a final exit status.
actual: XQ-AU52 reports 36 tests, 0 failures, 0 errors, 0 skipped; the Gradle task was cancelled after 8m32s while no Android 14 result XML was produced.
evidence:
  - app/build/outputs/androidTest-results/connected/debug/XQ-AU52 - 12/TEST-adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp.xml
  - docs/tasks/feat-0917/verification.md
next_action: infrastructure
owner: infrastructure
```

## ISS-010

```yaml
issue_id: ISS-010
task_id: feat-0917
phase: verification
category: environment
priority: P1
title: First fixed-revision Sony connected run had transient resumed-activity failures
status: resolved
impact: The first fixed-revision Sony run had three NoActivityResumedException failures; the targeted rerun passed 1/1 and the final full suite passed 36/36.
repro_steps:
  - Use commit-only clone at 0595b7c7782e3041b3637ed7505d90d53d8fb3ba.
  - Set ANDROID_SERIAL to adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp.
  - Run ./gradlew :app:connectedDebugAndroidTest --no-daemon.
expected: All Sony connected tests complete successfully.
actual: The first full run reported 33 passed and 3 failed with NoActivityResumedException in existing GutterBasicInfoUiTest cases. The targeted rerun passed 1/1, and the final full run passed 36/36.
evidence:
  - /private/tmp/tyg-feat0917-verification/app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml
  - docs/tasks/feat-0917/verification.md
next_action: verification
owner: infrastructure
```
