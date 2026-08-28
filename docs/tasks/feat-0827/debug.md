# Debug Stage

Date: 2026-08-28
Task: feat-0827 儀表板
Stage: debug

## Workflow

1. `implementation`
2. `verification`
3. `debug`
4. `re-implementation`
5. `developer validation`
6. `git commit`
7. `verification`

## Current Handoff

- Previous verification result: `FAIL`
- Verification category: `implementation`
- Task state: `verification_failed`
- Next action: `debug`

## Current Blocking Issues

- `AC-004`: survey length total/group cards do not fully match the Figma layout, sizing, and typography.
- `AC-008`: progress group selector still needs explicit `完成` confirmation.
- `AC-011`: other tracking items still need split value/unit styling so `筆` can use the required secondary color.
- `AC-013`: `MapWorkspaceFragment` still needs saved-state parity with the original `MainActivity` map flow.
- `AC-014`: connected coverage still does not prove the real shell tab switch and map restore path.

## Debug Focus

- The immediate runtime blocker is the shell tab connected test.
- `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs` still fails because `ActivityScenario.onActivity()` sees the activity as destroyed during the test flow.
- The app-side shell and fragment code already carry partial fixes, but verification is blocked until the connected test path is stable enough to prove them.

## Evidence To Recheck

- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `docs/tasks/feat-0827/verification.md`
- `docs/tasks/feat-0827/state.yaml`

## Re-Implementation Exit Criteria

- Connected test can run without the activity being destroyed mid-check.
- The shell test proves default map, dashboard switch, and return to map.
- The five failed acceptance criteria can be re-verified after the debug fix lands.

