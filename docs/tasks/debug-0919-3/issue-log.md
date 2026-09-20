# Issue Log

| Issue ID | Category | Priority | Status | Summary |
|---|---|---:|---|---|
| ISS-debug-0919-3-001 | implementation_regression | P2 | open | Focusing the no-ditch note field applies the IME height as an additional bottom offset, moving the shared panel too far upward on the reported foldable path. |
| ISS-debug-0919-3-002 | environment | P2 | open | Foldable runtime inset values, screen bounds, and before/after screenshots are not available for independent confirmation. |
| ISS-debug-0919-3-003 | environment | P2 | open | Local Gradle validation cannot start because no Java runtime is installed or discoverable in the workspace environment. |

## ISS-debug-0919-3-001

```yaml
issue_id: ISS-debug-0919-3-001
task_id: debug-0919-3
phase: debug
category: implementation_regression
priority: P2
title: No-ditch note panel is over-shifted when the IME opens
status: open
impact: The no-ditch reporting input flow has a device-specific layout defect on foldable phones; the note panel may move unnecessarily far above the keyboard.
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
next_action: implementation_debug
owner: developer
```

## ISS-debug-0919-3-002

```yaml
issue_id: ISS-debug-0919-3-002
task_id: debug-0919-3
phase: debug
category: environment
priority: P2
title: Runtime foldable IME evidence is unavailable
status: open
impact: The static over-offset mechanism is clear, but exact runtime displacement and the corrected panel bounds cannot yet be independently verified.
evidence:
  - docs/tasks/debug-0919-3/analysis.md
  - docs/tasks/debug-0919-3/root-cause.md
next_action: infrastructure
owner: developer
```

## ISS-debug-0919-3-003

```yaml
issue_id: ISS-debug-0919-3-003
task_id: debug-0919-3
phase: implementation
category: environment
priority: P2
title: Gradle validation environment has no Java runtime
status: open
impact: Build and automated tests cannot be executed locally in this environment; implementation evidence is limited to static checks until a JDK is available.
evidence:
  - command: ./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest
    result: Unable to locate a Java Runtime
next_action: infrastructure
owner: developer
```
