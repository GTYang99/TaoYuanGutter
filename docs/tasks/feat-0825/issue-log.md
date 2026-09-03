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

## ISS-004

```yaml
issue_id: ISS-004
task_id: feat-0825
phase: verification
category: verification_failure
priority: P1
title: Initial login location recenter still does not load user viewport scope layer
status: resolved
impact: After login, the main map can remain without nearby gutter polylines for the user's actual location until the user manually pans or zooms.
repro_steps:
  - Log in and open the main map on a real device.
  - Observe the map first show the default Taoyuan viewport.
  - Let the app recenter to the user's actual location.
  - Do not manually move the map.
expected: After the user-location camera movement settles, the main map should load scopeSearch data for the final visible viewport.
actual: Nearby gutter polylines around the user's final location may not be loaded.
evidence:
  - onMapReady still starts with a default Taoyuan camera at zoom 16f before location is obtained.
  - requestBackgroundScopeRefresh can run against the default viewport during startup.
  - MyLocationController calls onLocationUpdated before animateCamera, so reload ownership is recorded before the camera actually reaches the user viewport.
  - The implementation relies on a later camera idle to consume pendingLocationRecenterReload; if that idle is missed, blocked, or below the intended zoom threshold, no immediate recovery load runs.
  - MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM remains 16f even though the latest stage expects layer/API activation at 18f.
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

## ISS-005

```yaml
issue_id: ISS-005
task_id: feat-0825
phase: verification
category: verification_failure
priority: P1
title: AddGutterBottomSheet waypoint row tap still does not open edit form on device
status: resolved
impact: Users can be blocked from opening waypoint edit forms from the bottom sheet.
repro_steps:
  - Open AddGutterBottomSheet in add or edit mode on a real device.
  - Tap a waypoint row in the list.
expected: Tapping a waypoint row opens GutterFormActivity for that waypoint.
actual: The tap does not open the edit form on real device.
evidence:
  - Row click is now attached to layoutForeground, but the sheet still installs a Window.Callback touch router.
  - The router forwards every gesture whose ACTION_DOWN rawY is above design_bottom_sheet top to requireActivity().dispatchTouchEvent.
  - On real devices, design_bottom_sheet top/translation can be stale during show/hide or half-height layout, causing sheet touches to be misclassified as map touches.
  - The router does not verify whether the touch falls inside the actual binding.root, RecyclerView, or foreground row before forwarding.
  - ItemTouchHelper is attached to the same RecyclerView and may compete with click dispatch after foreground translation or gesture cancellation, so touch routing must be isolated first.
next_action: debug
owner: developer
```
