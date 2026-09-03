# Root Cause

## Issue
- `ISS-002`

## Classification
- `implementation_regression`
- Priority: `P1`

## Failed Behavior
- On first login/main-map startup, the map first moves to the default Taoyuan camera.
- The app then obtains the user's current location and animates the map to that location.
- Because that camera movement is programmatic, it is not treated as a user gesture.
- No force reload owner was registered for the initial location recenter, so the scope layer could remain loaded for the default viewport until the user manually moved the map.

## Root Cause
- The location-recenter reload flag was only set for the manual my-location button path.
- The initial location request in `onMapReady()` updated `lastKnownLocation`, but did not mark the following camera idle as needing a scope reload.

## Affected Acceptance Criteria
- `AC-003`: scopeSearch should start only after a valid map interaction owner; initial location recenter is a valid programmatic owner for the final visible viewport.
- `AC-005`: the main map should remain consistent without adding unrelated loading behavior to other modules.

## Minimum Fix
- Track location recenter reload ownership separately from the manual button flag.
- Mark the initial location move as requiring one scope reload after the location animation reaches idle.
- Reuse the same path for manual my-location recenter.

---

## Issue
- `ISS-003`

## Classification
- `implementation_regression`
- Priority: `P1`

## Failed Behavior
- On a real device, tapping a waypoint row in `AddGutterBottomSheet` may not open the waypoint edit form.

## Root Cause
- `preloadEditWaypointDetails()` turned on the blocking loading overlay and disabled `rvWaypoints`, but did not use `try/finally`; an exception could leave the sheet permanently disabled.
- `WaypointAdapter` attached row click handling to the outer root while the foreground row is the interactive visual surface manipulated by `ItemTouchHelper`.
- The click handler used deprecated `adapterPosition`, which can return an invalid position during RecyclerView layout/animation windows.

## Affected Acceptance Criteria
- `AC-005`: other main-map related flows, including add/edit waypoint form entry, must remain usable.

## Minimum Fix
- Guarantee edit preload always restores loading and list enabled state.
- Attach row clicks to `layoutForeground`.
- Use `bindingAdapterPosition` and ignore `NO_POSITION`.
