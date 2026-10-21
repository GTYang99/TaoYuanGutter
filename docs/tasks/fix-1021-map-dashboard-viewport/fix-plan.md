# Fix Plan

## Goal
- Preserve the map viewport when switching between the main map and Dashboard.

## Minimum Fix
- Keep tab fragments added under stable tags; hide the inactive tab and show the selected tab.
- Use `setMaxLifecycle(STARTED)` for hidden fragments and `RESUMED` for the selected fragment.
- Do not change initial camera fallback, location acquisition, explicit Locate, or map/domain state behavior.

## Acceptance Mapping
- AC-001 / AC-002: preserve map fragment identity across Dashboard round-trips; the retained map view keeps its camera state.
- AC-003: only tab retention changes; first creation still enters the existing map initialization path.

## Regression Risk
- Hidden fragment visibility or lifecycle may be wrong, leaving two tabs visible or the selected tab inactive.

## Validation
- Run the focused `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs` instrumentation case on the approved test emulator only.
- Build the debug and androidTest variants without running test tasks on the host.

## Rollback
- Revert the single fix commit if fragment visibility, lifecycle, or saved-state restoration regresses.
