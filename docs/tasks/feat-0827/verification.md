# Verification Report

Date: 2026-08-28
Task: feat-0827 儀表板
Result: PASS

## Inputs

- Requirement: `docs/tasks/feat-0827/requirement.md`
- Plan: `docs/tasks/feat-0827/plan.md`
- State: `docs/tasks/feat-0827/state.yaml`
- Verification rules: `ai/verification-rules.md`
- Git diff: current working tree changes for dashboard card styling plus existing shell/map fixes

## Test Evidence

- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDebugAndroidTestKotlin`
- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest`
- Device: `XQ-AU52 - 12`

## Acceptance Criteria

| AC | Area | Result | Evidence |
|----|------|--------|----------|
| AC-004 | Survey length cards match layout and typography | PASS | `DashboardFragment.buildLengthGroupCard()` now keeps the 90dp card height and stacked title/value layout, and the bottom row is aligned as left value plus right unit. The unit now uses `#562ECB` with 14sp so it matches the approved card typography more closely. |
| AC-008 | Progress group selector requires explicit `完成` | PASS | Previously verified; `showProgressGroupDialog()` keeps a temporary selection and applies it only from the confirm button. |
| AC-011 | Other tracking items split value and unit styling | PASS | Previously verified; `renderProgressIssues()` renders the count and `筆` in separate views. |
| AC-013 | Map tab preserves original saved state behavior | PASS | Previously verified; `MapWorkspaceFragment.onSaveInstanceState()` stores the inspect waypoints, reference route, and edit polyline snapshot keys used by the original map flow. |
| AC-014 | Dashboard tab hides map controls and returning to map restores them | PASS | Previously verified; `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs()` passed on the device. |

## Result

`AC-004` is now aligned with the approved group card layout and typography. The Android test suite passed after the change, and the task is back in a verified PASS state.
