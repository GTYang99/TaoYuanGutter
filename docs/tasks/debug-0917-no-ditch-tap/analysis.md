# Repository Analysis

## Current Behavior

- `NoDitchModeUiController.enter()` makes the no-ditch panel visible and disables other main buttons through `MainBlockingUiController`.
- `MapWorkspaceFragment.handleMainMapTap()` accepts a location only while no-ditch mode is active and no prior location is selected; after selection, reset is required before another map tap is accepted.
- The report panel receives system-bar and IME bottom insets, so its occupied map area can vary by device.

## Expected Behavior

- The map must receive deliberate selection taps consistently in its visible region, and the report panel must not create a device-dependent obstruction or state ambiguity.

## Affected Modules

- `main/NoDitchModeUiController.kt`
- `main/MainBlockingUiController.kt`
- `map/MapWorkspaceFragment.kt`
- `res/layout/panel_no_ditch_report.xml`
- legacy `MainActivity.kt`, if it remains a supported direct entry point

## Dependencies

- Google Maps click dispatch, Android view touch dispatch, layout/inset behavior, and `GutterRepository.storeNoDitch` submission flow.

## Risks

- Changing touch handling without device evidence could break panning, panel controls, or normal no-ditch-point inspection.
- The behavior may vary by window inset, screen size, navigation mode, or an overlay from another workflow.

## Unknowns

- The reported device profile and exact failing tap coordinates are required to distinguish layout obstruction from event/state handling.
