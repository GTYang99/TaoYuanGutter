# Issue Log

## ISS-002

```yaml
issue_id: ISS-002
task_id: feat-0825
phase: implementation
category: implementation_regression
priority: P1
title: Initial location recenter does not reload main map scope layer
status: resolved
impact: Main map can show scope polylines for the default viewport instead of the user's current viewport until the user manually moves the map.
repro_steps:
  - Log in and open the main map.
  - Let the map move from the default Taoyuan camera to the user's current location.
  - Do not manually pan or zoom the map.
expected: The user's current viewport should load scopeSearch data after the location recenter finishes.
actual: The initial location recenter did not own a scope reload, so no reload was triggered after the programmatic camera idle.
evidence:
  - onMapReady initial location callback only updated lastKnownLocation.
  - pendingLocationRecenterReload was only set by the manual my-location button flow.
next_action: debug
owner: developer
```

## ISS-003

```yaml
issue_id: ISS-003
task_id: feat-0825
phase: implementation
category: implementation_regression
priority: P1
title: AddGutterBottomSheet waypoint row cannot reliably open the edit form on device
status: resolved
impact: Users can be blocked from editing waypoint forms from the bottom sheet on real devices.
repro_steps:
  - Open AddGutterBottomSheet in add or edit mode on a real device.
  - Tap a waypoint row.
expected: Tapping a waypoint row opens the waypoint edit form.
actual: The tap may not open the form because loading state can leave the list disabled after preload failure, and row click handling is attached to the outer item instead of the visible foreground row.
evidence:
  - preloadEditWaypointDetails enabled loading and disabled rvWaypoints without a try/finally recovery path.
  - WaypointAdapter attached row click handling to binding.root while ItemTouchHelper manipulates layoutForeground.
next_action: debug
owner: developer
```
