# Repository Analysis

## Task Classification
- Task ID: debug1016-1
- Type: bugfix
- Source revision reviewed: 11b71195b1ffa1f5bb68ef94cd0b08d5961bfd1b
- Task branch: codex/debug/debug1016-1
- Scope: use the user's current location for an empty, normal online new/add form's initial mini-map viewport.
- The user explicitly authorizes this behavior and requests minimal implementation with emulator-only validation.

## Current Behavior
- AddGutterBottomSheet delegates a tapped waypoint to its host. A fresh add sheet reports isEditMode=false; a restored draft has a positive restored draft ID, and offline mode is separately tracked.
- The host builds a GutterFormActivity intent with missing coordinates preserved as null in the session snapshot and passes the host's last-known location separately.
- GutterFormActivity displays the Taoyuan fallback when there is no saved coordinate. EditMapLocationPolicy starts the existing location flow only for normal edit mode. The host last-known location is used by nearby-point import, not initial map setup.
- The existing EditMapInitialLocationInstrumentedTest already exercises fused mock location, permission behavior, fallback, manual pan, and coordinate non-persistence. It also asserts the obsolete behavior that a new point never recenters.

## Expected Behavior
- For a fresh online new/add sheet with no restored draft and no waypoint coordinates, the host explicitly marks the launched form as eligible for the existing camera-only current-location flow.
- Saved coordinates retain precedence. Location acquisition remains viewport-only.
- Offline and restored-draft flows remain unchanged. Permission denial or failed acquisition keeps the existing unavailable prompt and Taoyuan fallback.

## Affected Modules
- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt — exposes whether the sheet is an eligible fresh add form or an edit/restored flow.
- app/src/main/java/com/example/taoyuangutter/MainActivity.kt and app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt — mark eligible launches; the fragment is the normal callback host.
- app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt — passes the explicit launch flag into the form intent.
- app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt and EditMapLocationPolicy.kt — allow the existing location flow for the explicit new/add case while preserving existing guards.
- app/src/test/java/com/example/taoyuangutter/gutter/EditMapLocationPolicyTest.kt and app/src/androidTest/java/com/example/taoyuangutter/gutter/EditMapInitialLocationInstrumentedTest.kt — cover the policy and emulator behavior.

## Dependencies
- AddGutterBottomSheet mode and restored-draft state.
- GutterFormNavigator intent creation.
- GutterFormActivity's existing permission launcher, fused location request, bounded retry, user-pan guard, unavailable prompt, and fallback.
- Android emulator with a native GPS fix injected through the emulator console for the positive centering case; the existing mock-location receiver remains available for other location-flow cases.

## Risks
- A location permission prompt can now occur on a fresh empty add form. Reusing the edit flow keeps its established retry and failure behavior.
- A late location callback must continue to respect a manual pan.
- The mini-map camera target must not be saved as the waypoint's coordinates.
- Running the Gradle connected-device task without serial filtering could target a connected physical phone; validation must explicitly select the emulator.

## Unknowns
- None for the approved scope. Runtime evidence must come from the emulator only.
