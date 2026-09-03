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
