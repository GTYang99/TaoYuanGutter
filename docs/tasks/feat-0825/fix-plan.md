# Fix Plan

## Issue
- `ISS-002`

## Goal
- Ensure the first automatic location recenter loads the scope layer for the user's final viewport, even if the user does not manually move the map.

## Scope
- `MainActivity`
- A small tracker for location recenter reload ownership
- Unit tests for the tracker

## Steps
- Add a focused tracker that records whether a location move should trigger a reload after location update.
- In `onMapReady()`, mark the initial location move as reload-owned before requesting location.
- In the manual my-location path, reuse the same tracker.
- In permission denial, cancel pending location reload ownership.
- When location is updated, set `pendingLocationRecenterReload` once so the next camera idle uses the existing `requestForceScopeReload("location recenter")` path.
- Validate with `testDebugUnitTest`.

---

## Issue
- `ISS-003`

## Goal
- Make waypoint row taps in `AddGutterBottomSheet` reliably open the waypoint edit form on real devices.

## Scope
- `AddGutterBottomSheet`
- `WaypointAdapter`

## Steps
- Wrap edit preload work with `try/finally` so loading state and `rvWaypoints.isEnabled` are always restored.
- Preserve partial-failure toast behavior after preload cleanup.
- Move row click handling from the item root to `layoutForeground`.
- Resolve click positions with `bindingAdapterPosition`, ignoring `RecyclerView.NO_POSITION`.
- Validate with `testDebugUnitTest`.

---

## Issue
- `ISS-004`

## Goal
- Ensure first-login user-location recenter always loads the main-map scope layer for the final user viewport without requiring manual map movement.

## Scope
- `MainActivity`
- `MyLocationController`
- `MainMapLoadIndicatorStateMachine`
- Unit tests for the recenter/load threshold behavior where feasible

## Steps
- Change the minimum scope-search/layer activation threshold to `18f`.
- Extend the location recenter flow so the caller can run logic after the location camera animation actually finishes.
- For initial login and manual my-location recenter, trigger one forced scope reload after the animation completion or after the one-shot camera idle that represents the final user viewport.
- While an initial location recenter is pending, avoid drawing stale startup/default-viewport scope results.
- Keep manual gesture loading behavior unchanged.
- Validate with `testDebugUnitTest`.

---

## Issue
- `ISS-005`

## Goal
- Make AddGutterBottomSheet waypoint row taps reliably reach RecyclerView and open the waypoint edit form on real devices.

## Scope
- `AddGutterBottomSheet`
- `WaypointAdapter` only if additional click-state cleanup is needed
- Unit tests for extracted touch-routing decision logic if feasible

## Steps
- Replace `rawY < design_bottom_sheet.top` routing with a helper that checks whether `ACTION_DOWN` is outside the actual sheet content root bounds.
- Forward events to the Activity only for touches outside the visible sheet content.
- Reset `routeToActivity` on `ACTION_UP` and `ACTION_CANCEL`.
- Keep row click handling on `layoutForeground` with `bindingAdapterPosition`.
- If ItemTouchHelper still interferes after routing is fixed, reset foreground translation/clickable state during bind and clearView without changing swipe behavior.
- Validate with `testDebugUnitTest`.
