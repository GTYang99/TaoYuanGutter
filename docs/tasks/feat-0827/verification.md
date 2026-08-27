# Verification Report

Date: 2026-08-27
Task: feat-0827 儀表板
Result: FAIL

## Inputs

- Requirement: `docs/tasks/feat-0827/requirement.md`
- Analysis: `docs/tasks/feat-0827/analysis.md`
- Plan: `docs/tasks/feat-0827/plan.md`
- State: `docs/tasks/feat-0827/state.yaml`
- Verification rules: `ai/verification-rules.md`
- Testing rules: `ai/testing-rules.md` is missing from the repository
- Git evidence: dirty working tree with dashboard/map implementation changes and verification docs
- Figma evidence: Copy file `62VD4By6OHYQFyZnu4Una4`, section `2209:24618`, screen node `2209:20119`

## Test Evidence

- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest`
- PASS: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest`
- Connected device: `XQ-AU52 - 12`
- Scope note: connected test currently inflates the shell layout and checks bottom navigation item count only. It does not exercise real tab switching, map workflow restoration, dashboard rendering, filter interaction, or Figma visual conformance.

## Acceptance Criteria

| AC | Area | Result | Evidence |
|----|------|--------|----------|
| AC-001 | Login/main navigation creates map + dashboard tabbar and defaults to map | PASS | Plan requires shell default map tab. Current tests confirm `activity_main_shell` inflates with two bottom nav items in `MainShellActivityTest.kt`. Source inspection confirms the shell exists and map/dashboard fragments are present in the worktree. |
| AC-002 | Dashboard tab icon and title | PASS | Requirement asks dashboard icon and 24px title. Layout title is present and Figma node `2209:20124` confirms 24px dashboard heading. |
| AC-003 | Dashboard API endpoint and query support | PASS | Existing unit tests pass. Dashboard query state includes date/month paths, and `DashboardViewModelTest` covers date/month mutual clearing. |
| AC-004 | Survey length total/group cards match Figma and requirement | FAIL | Figma node `2209:20132` shows total card gradient `#7B4FFF` to `#4726A9`, and nodes `2209:20138` / `2209:20143` show two-up 90px group cards with stacked group/value content and right-aligned unit. Current total card has gradient resource applied in `activity_dashboard.xml`, but group cards are programmatic `wrap_content` with `minimumHeight = 72dp`, horizontal name/value layout, and value text at 20sp instead of the requirement's 16px group value in `DashboardFragment.kt`. |
| AC-005 | Length settings support API group multi-select | PASS | `DashboardFragment.showLengthSettingsDialog()` builds checkbox options from available groups and applies selection to ViewModel. |
| AC-006 | Filter UI is a new bottom option page with date/month exclusive behavior | PASS | `DashboardFilterBottomSheet` is a `BottomSheetDialogFragment`, starts with no selected mode, renders date/month sections exclusively, applies either date query or month query, and is opened by the filter button. |
| AC-007 | Clicking group cards shows account-level distance rows | PASS | `DashboardFragment` selects length detail group on group card click and renders account rows with dividers. |
| AC-008 | Progress group selector includes all groups and applies after completion | FAIL | Requirement says user selects one group then clicks `完成`. Current `showProgressGroupDialog()` uses `setSingleChoiceItems` and dismisses immediately on item click, with no completion action. |
| AC-009 | Progress counts and percentages render correctly | PASS | `bindProgressRow()` now formats one decimal such as `34.3 %`, matching Figma examples, and handles zero totals as `0.0 %`. |
| AC-010 | Pie chart renders progress proportions safely | PASS | Unit and connected tests pass; source keeps a custom dashboard pie chart view wired to the four progress values. |
| AC-011 | Other tracking items match label/count/unit styling | FAIL | Figma node `2209:20243` separates value and `筆`, with unit color `#909399`. Current `renderProgressIssues()` puts `"$value 筆"` into a single black `TextView`, so the unit cannot use the required secondary color. |
| AC-012 | 401 dashboard response follows existing auth handling | PASS | No regression found in source review; dashboard path keeps auth-error handling pattern. |
| AC-013 | Map tab preserves original `MainActivity.kt` map behavior | FAIL | `MapWorkspaceFragment` now ports much of the original map workflow, but it is still not equivalent to `MainActivity.kt`. Its `onSaveInstanceState()` saves current waypoints/session state only, while `restoreStateAfterRecreation()` expects inspect waypoints, reference route state, reference route coordinates, edit polyline flag, and edit coordinate snapshot. Original `MainActivity.onSaveInstanceState()` saves those missing keys. Rotation/recreation can therefore lose inspect/edit/reference-route state in the shell map tab. |
| AC-014 | Dashboard tab hides map controls and returning to map restores them | FAIL | Fragment replacement likely hides map controls while dashboard is active, but current tests only inflate bottom navigation. There is no connected coverage proving tab switch, map controls restore, overlays, bottom sheets, camera, or scope loading after returning to map. Source state-save mismatch in AC-013 also blocks confidence in restoration. |

## Plan Conformance

- PASS: dashboard API/query, pie chart, filter bottom sheet, and two-tab shell are present.
- PARTIAL: map workflow was moved into `MapWorkspaceFragment`, but plan required preserving original map behavior. The port still misses saved-state parity with `MainActivity.kt`.
- FAIL: test plan asked for instrumentation coverage of default map, dashboard tab, return-to-map, and map-control visibility. Current connected test only verifies shell layout inflation and bottom nav item count.

## Regression Review

- Blocking regression risk: map inspect/edit/reference-route state can be lost across recreation in `MapWorkspaceFragment` because save and restore keys do not match.
- Blocking coverage gap: no device test proves actual dashboard tab switching or map restoration.
- UI regression risk: Figma/requirement card layout and other-tracking unit styling are still not fully aligned.
- Non-blocking process gap: required `ai/testing-rules.md` does not exist.

## Failed Acceptance Criteria

- AC-004: length group card layout/height/value typography does not match requirement/Figma.
- AC-008: progress group selector applies immediately and lacks required `完成` action.
- AC-011: other tracking item unit text cannot use required `#909399` color because value and unit are rendered in one TextView.
- AC-013: map tab is not saved-state equivalent to original `MainActivity.kt`.
- AC-014: map/dashboard tab restoration is not sufficiently implemented or tested.

## Root Cause

The implementation fixed several prior blockers, but remaining work is split between UI conformance and map-shell parity. The dashboard UI still has programmatic row/card rendering that does not fully encode the Figma structure. The map tab ports behavior from `MainActivity.kt`, but the saved-state contract was not carried over completely, and test coverage does not exercise the real shell behavior.

## Recommended Action

1. Copy the missing `MainActivity.onSaveInstanceState()` keys into `MapWorkspaceFragment.onSaveInstanceState()` and add recreation-focused coverage or manual verification for inspect/edit/reference-route state.
2. Update length group cards to match Figma: two-up 90dp cards, stacked group/value content, right-aligned unit, required typography.
3. Change progress group selection to a single-select flow with explicit `完成`.
4. Split other-tracking value and `筆` into separate views/spans so the unit can use `#909399`.
5. Add connected coverage for default map tab, dashboard tab switch, return-to-map restoration, and at least one dashboard filter/rendering path.

## Final Result

FAIL
