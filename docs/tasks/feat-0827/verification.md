# Verification Report

Date: 2026-08-28
Task: feat-0827 儀表板
Result: PASS

## Inputs

- Requirement: `docs/tasks/feat-0827/requirement.md`
- Plan: `docs/tasks/feat-0827/plan.md`
- State: `docs/tasks/feat-0827/state.yaml`
- Debug: `docs/tasks/feat-0827/debug.md`
- Root cause: `docs/tasks/feat-0827/root-cause.md`
- Fix plan: `docs/tasks/feat-0827/fix-plan.md`
- Issue log: `docs/tasks/feat-0827/issue-log.md`
- Verification rules: `ai/verification-rules.md`
- Git diff: latest implementation commit `d74a5db fix(feat-0827): refine dashboard cards and filter sheet`

## Test Evidence

- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest`
- Devices: `Medium_Phone(AVD) - 14`, `XQ-AU52 - 12`

## Acceptance Criteria

| AC | Area | Result | Evidence |
|----|------|--------|----------|
| AC-004 | Survey length cards match layout and typography | PASS | `activity_dashboard.xml` moves `bg_dashboard_total_card` onto the inner content container so the gradient is visible. `DashboardFragment.buildLengthGroupCard()` keeps the 90dp card height, stacked title/value layout, left value plus right unit row, and the unit now uses `R.color.dashboard_group_value` (`#562ECB`) at 14sp. |
| AC-008 | Progress group selector requires explicit `完成` | PASS | Previously verified; `showProgressGroupDialog()` keeps a temporary selection and applies it only from the confirm button. |
| AC-011 | Other tracking items split value and unit styling | PASS | Previously verified; `renderProgressIssues()` renders the count and `筆` in separate views. |
| AC-013 | Map tab preserves original saved state behavior | PASS | Previously verified; `MapWorkspaceFragment.onSaveInstanceState()` stores the inspect waypoints, reference route, and edit polyline snapshot keys used by the original map flow. |
| AC-014 | Dashboard tab hides map controls and returning to map restores them | PASS | Previously verified; `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs()` passed on the device. |

## Issue Review

| Issue | Result | Evidence |
|-------|--------|----------|
| ISS-0828-001 | PASS | Total length gradient is applied to the inner container that fills the card, avoiding the previous `MaterialCardView` background layering issue. |
| ISS-0828-002 | PASS | `DashboardViewModel.handleSuccess()` and `selectLengthGroups()` no longer auto-select the first length detail group. |
| ISS-0828-003 | PASS | `DashboardFilterBottomSheet` uses match-parent sheet height, close icon, vertical date/month sections, year/month picker, and bottom action buttons. |

## Result

The debug implementation is verified. The previously failed acceptance criteria and the recorded implementation-regression issues are now supported by code/diff inspection and passing unit + connected Android tests.
