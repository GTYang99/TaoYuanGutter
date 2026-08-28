# Verification Report

Date: 2026-08-28
Task: feat-0827 儀表板
Result: PASS

## Inputs

- Requirement: `docs/tasks/feat-0827/requirement.md`
- Plan: `docs/tasks/feat-0827/plan.md`
- State: `docs/tasks/feat-0827/state.yaml`
- Verification rules: `ai/verification-rules.md`
- Git diff: current working tree changes for shell, dashboard, and map workspace

## Test Evidence

- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDebugAndroidTestKotlin`
- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest`
- Device: `XQ-AU52 - 12`

## Acceptance Criteria

| AC | Area | Result | Evidence |
|----|------|--------|----------|
| AC-004 | Survey length cards match layout and typography | PASS | `DashboardFragment.kt` now renders the total and group cards with the required stacked layout, 90dp minimum height, and split value/unit styling. |
| AC-008 | Progress group selector requires explicit `完成` | PASS | `showProgressGroupDialog()` now keeps a temporary selection and applies it only from the confirm button. |
| AC-011 | Other tracking items split value and unit styling | PASS | `renderProgressIssues()` now renders the count and `筆` in separate views so the unit can use the secondary color. |
| AC-013 | Map tab preserves original saved state behavior | PASS | `MapWorkspaceFragment.onSaveInstanceState()` now stores the inspect waypoints, reference route, and edit polyline snapshot keys used by the original map flow. |
| AC-014 | Dashboard tab hides map controls and returning to map restores them | PASS | `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs()` now verifies default map, dashboard switch, and return to map; `connectedDebugAndroidTest` passed on the device. |

## Result

The dashboard shell, tab switch flow, and map workspace state restoration are now verified by source inspection plus passing compile and connected Android tests.
