# Issue Log

| Issue ID | Category | Priority | Status | Summary |
|---|---|---:|---|---|
| ISS-debug-0919-3-001 | implementation_regression | P2 | resolved | Fixed revision removes the duplicate IME bottom-margin contribution; isolated foldable AVD confirms usable panel bounds. |
| ISS-debug-0919-3-002 | environment | P2 | resolved | Isolated foldable AVD provides runtime inset and before/after panel-bound evidence; physical-device coverage remains unavailable. |
| ISS-debug-0919-3-003 | environment | P2 | resolved | The default Java command is unavailable, but Android Studio JDK 21 enabled Gradle validation. |
| ISS-debug-0919-3-004 | environment | P2 | resolved | Independent emulator-5556 was used; reserved emulator-5554 was not touched. |
| ISS-debug-0919-3-005 | planning_gap | P2 | open | The approved plan names a focused NoDitchModeUiController instrumentation test, but the fixed revision contains only a pure inset-policy unit test. |
| ISS-debug-0919-3-006 | environment | P2 | resolved | Authenticated BASE_URL session verified successful no-ditch submit with backend success response. |

## ISS-debug-0919-3-001

```yaml
issue_id: ISS-debug-0919-3-001
task_id: debug-0919-3
phase: debug
category: implementation_regression
priority: P2
title: No-ditch note panel is over-shifted when the IME opens
status: resolved
impact: The fixed revision removes the duplicate IME bottom-margin contribution; runtime verification on the isolated foldable AVD confirmed the panel and controls remain usable while the IME is visible.
repro_steps:
  - Open the main map.
  - Tap btnReportNoDitch.
  - Pick a map coordinate.
  - Focus the note field and type text on a foldable device.
expected: The panel remains usable immediately above the keyboard.
actual: The panel moves too far upward.
evidence:
  - app/src/main/java/com/example/taoyuangutter/main/NoDitchModeUiController.kt
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt
  - app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt
  - app/src/main/res/layout/activity_main.xml
  - app/src/main/res/layout/panel_no_ditch_report.xml
  - /Users/a10362/Desktop/markdown file/ty_debug_0919-3.md
  - docs/tasks/debug-0919-3/verification.md (AC-001, AC-002 runtime bounds)
next_action: verification
owner: verifier
```

## ISS-debug-0919-3-002

```yaml
issue_id: ISS-debug-0919-3-002
task_id: debug-0919-3
phase: debug
category: environment
priority: P2
title: Runtime foldable IME evidence is unavailable
status: resolved
impact: Runtime displacement and corrected panel bounds are now independently verified on the isolated foldable AVD; physical-device coverage remains outside the available environment.
evidence:
  - docs/tasks/debug-0919-3/analysis.md
  - docs/tasks/debug-0919-3/root-cause.md
  - device: emulator-5556 / CodexDebug0919_3_Fold / Android 14 API 34 / CLOSED posture
  - evidence: docs/tasks/debug-0919-3/verification.md (AC-001, AC-002 bounds)
next_action: verification
owner: verifier
```

## ISS-debug-0919-3-003

```yaml
issue_id: ISS-debug-0919-3-003
task_id: debug-0919-3
phase: implementation
category: environment
priority: P2
title: Gradle validation environment has no Java runtime
status: resolved
impact: The default Java command was unavailable, but Android Studio JDK 21 enabled build and automated test validation.
evidence:
  - command: ./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest
    result: PASS with JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home
next_action: verification
owner: verifier
```

## ISS-debug-0919-3-004

```yaml
issue_id: ISS-debug-0919-3-004
task_id: debug-0919-3
phase: verification
category: environment
priority: P2
title: No Android device is available for runtime verification
status: resolved
impact: An isolated foldable AVD is available for independent AC-001, AC-002, and partial AC-003 UI-flow evidence; emulator-5554 remained reserved by another agent.
evidence:
  - command: adb devices -l
    result: emulator-5556 available; emulator-5554 intentionally not used
  - device: CodexDebug0919_3_Fold / Android 14 API 34 / CLOSED posture
  - evidence: docs/tasks/debug-0919-3/verification.md (runtime environment and AC results)
next_action: verification
owner: verifier
```

## ISS-debug-0919-3-005

```yaml
issue_id: ISS-debug-0919-3-005
task_id: debug-0919-3
phase: verification
category: planning_gap
priority: P2
title: Planned no-ditch controller instrumentation coverage is absent
status: open
impact: Runtime panel positioning and no-ditch state-flow coverage is weaker than the approved plan requires.
evidence:
  - docs/tasks/debug-0919-3/plan.md
  - app/src/test/java/com/example/taoyuangutter/main/NoDitchPanelInsetPolicyTest.kt
  - missing: app/src/androidTest/java/com/example/taoyuangutter/NoDitchModeUiControllerTest.kt
next_action: planning
owner: developer
```

## ISS-debug-0919-3-006

```yaml
issue_id: ISS-debug-0919-3-006
task_id: debug-0919-3
phase: verification
category: environment
priority: P2
title: No authenticated session is available for no-ditch submit verification
status: resolved
impact: The user-authenticated BASE_URL run verified point selection, note input, successful submit, reset, and exit on the isolated foldable AVD.
evidence:
  - device: emulator-5556 / CodexDebug0919_3_Fold / Android 14 API 34
  - endpoint: http://192.168.10.84/TY_RSGDBIP/api/v1/map/storeNoDitch
  - request_note: authenticated_ac003_base_url
  - response: HTTP 200; success=true; id=18; message="新增成功"
  - follow_up: reset returned to point-selection state; return exited to main shell
next_action: verification
owner: verifier
```
