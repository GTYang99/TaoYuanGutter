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

---

## Issue
- `ISS-004`

## Classification
- `verification_failure`
- Priority: `P1`

## Failed Behavior
- On a real device, after first login the map may briefly show the default Taoyuan viewport.
- The camera later recenters to the user's current location.
- If the user does not manually pan or zoom after that recenter, nearby main-map gutter polylines may not appear.

## Root Cause
- The current startup flow still has two independent camera/load owners:
  - `onMapReady()` immediately moves to the default Taoyuan viewport at zoom `16f`.
  - `requestBackgroundScopeRefresh()` can run for that default viewport.
- `MyLocationController.enableMyLocationAndMove()` invokes `onLocationUpdated(loc)` before starting `map.animateCamera(...)`.
- `MainActivity.handleMainMapLocationUpdated()` therefore marks `pendingLocationRecenterReload` before the camera has actually reached the user viewport.
- The implementation depends on a future `OnCameraIdleListener` callback to consume that flag. On real devices, that callback can be missed, delayed until the app is not eligible to query, or evaluated against a zoom/load threshold mismatch.
- The latest requested threshold is `18f`, but `MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM` remains `16f`, so startup/background loading and visible layer activation can still disagree with the expected implementation gate.

## Affected Acceptance Criteria
- `AC-003`: scopeSearch/layer drawing should run for the final user-visible viewport once the map is at the required zoom.
- `AC-005`: startup behavior should not require manual map movement to show nearby gutters.

## Minimum Fix
- Make user-location recenter completion an explicit load owner instead of relying only on a pending flag plus generic camera idle.
- Trigger the forced scope reload from the location camera animation completion path, or add a one-shot camera-idle owner that validates the final camera target/zoom before loading.
- Align the scope-search minimum zoom constant with the current `18f` requirement.
- Prevent startup background scope refresh from drawing stale default-viewport gutters after a pending initial location recenter exists.

---

## Issue
- `ISS-005`

## Classification
- `verification_failure`
- Priority: `P1`

## Failed Behavior
- On a real device, tapping a waypoint row in `AddGutterBottomSheet` still may not open `GutterFormActivity`.

## Root Cause
- The previous adapter-level fix only changed which row view receives `OnClickListener`.
- `AddGutterBottomSheet.setupBottomSheetBehavior()` still replaces the dialog window callback and forwards touch events to the Activity whenever `ACTION_DOWN.rawY < design_bottom_sheet.top`.
- That check only compares against the Material `design_bottom_sheet` container top. It does not verify whether the touch is inside the actual sheet content root, `RecyclerView`, or row foreground.
- During real-device layout/animation states, `design_bottom_sheet` top and translation can differ from the actual visible/touchable content. A row tap can be misclassified as outside the sheet and forwarded to the main Activity/map, so RecyclerView never receives the click.
- ItemTouchHelper may also cancel clicks after swipe/drag state, but the primary unhandled risk is the global touch router stealing the event before the adapter can handle it.

## Affected Acceptance Criteria
- `AC-005`: add/edit waypoint form entry from the bottom sheet must remain usable on device.

## Minimum Fix
- Replace the global top-only touch routing with a bounds check against the actual sheet content root.
- Only forward touches to the Activity when the `ACTION_DOWN` is outside the visible sheet content bounds.
- Reset routing state on `ACTION_UP`/`ACTION_CANCEL` to avoid leaking a forwarded gesture into the next tap.
- Keep the existing adapter foreground click handling, but add validation coverage for touch routing as the actual failure point.
