# Repository Analysis

## Task Classification

- Task ID: `debug-1019-map-user-location`
- Type: `bugfix` (the relevant behavior gap is source-confirmed and the user clarified the intended behavior)
- Source revision: `9377f41aa1d030d5fbe3d456c4886ebb5cbd8462`
- Worktree branch: `codex/debug/debug-1019-map-user-location`
- User decision: preserve the selected/saved waypoint as the map center; use the device's current location only for a map without a waypoint location or after an explicit locate action.
- This is a source investigation. No incident-specific reproduction steps, screenshot, or location trace were supplied for this task.

## Current Behavior

- The normal online route launches `MainShellActivity`, which opens `MapWorkspaceFragment`. Its empty host map first moves to the fixed Taoyuan fallback `(24.9929, 121.3011)`, then requests location through `MyLocationController`. If no usable fix is applied, it stays at the fallback.
- The offline route launches legacy `MainActivity`. Its host map has the same fallback-then-location pattern. `AddGutterBottomSheet` is an overlay and does not own a separate map; location picking reuses the host map.
- `GutterFormActivity` gives any non-null waypoint snapshot coordinate precedence, including `(0,0)`. With no snapshot coordinate it uses the Taoyuan fallback and requests location only when `EditMapLocationPolicy` permits it. A fresh online add is explicitly eligible; the policy excludes offline forms and submitted read-only forms. Restored drafts do not receive the fresh-add eligibility flag.
- The separate `MapPointPickerActivity` starts at the supplied coordinate when it is non-zero. With no supplied coordinate it starts at the Taoyuan fallback and automatically requests location. Its locate FAB uses `MyLocationController`.
- The form's Nearby import sheet reuses the form map. Its host coordinate is shown first, then a separate nearby-specific policy accepts a fresh fix within 30 m accuracy and updates the marker, camera, and query.
- The shared `MyLocationController` accepts a cached fix up to five minutes old. It replaces that fix only with a newer fix whose accuracy improves by at least 10 m; the policy does not compare coordinate displacement.

## Expected Behavior

- A saved/selected usable waypoint coordinate remains the camera center; automatic location must not replace it. `(0,0)` and invalid coordinate pairs are not usable viewport targets.
- An editable empty map attempts to center on the device's current location. This applies to empty form maps, including restored/offline flows, subject to location permission and an available usable fix.
- An explicit locate action moves the camera to the device's current location. A camera-only locate action must not write `NODE_X`/`NODE_Y`; the point-picker Confirm action remains the explicit coordinate-selection action.
- Camera-only location behavior must not rewrite existing draft/session coordinate data.
- If permission or a usable location fix is unavailable, retain the existing fallback and user-facing failure behavior for now. The user has not asked to remove the fallback.
- Newly confirmed presentation defect: `GutterFormActivity.onMapReady()` applies the Taoyuan fallback before the asynchronous location request. A valid host fix is already attached to the intent but not used for initial map centering. On a blank form, this can expose the city-hall location for one frame or longer before the current fix arrives.
- Expected presentation: use a fresh usable host fix synchronously; if there is no usable handoff fix, keep the map surface hidden until the current-location result or the existing failure decision selects the visible center. The fallback remains available after failure.

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt` and `map/MapWorkspaceFragment.kt` — normal online host map and location requests.
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` — offline host map and the shared map used by `AddGutterBottomSheet`.
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt` — overlay lifecycle and fresh-add/draft mode signal; no independent map viewport.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`, `GutterFormNavigator.kt`, and `EditMapLocationPolicy.kt` — form-map center selection and blank-map location eligibility.
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt` — saved-point initial center and explicit locate action.
- `app/src/main/java/com/example/taoyuangutter/map/MyLocationController.kt` and `LocationFixQualityPolicy.kt` — shared cached/fresh fix acceptance used by host maps and the picker.
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` and `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` — host-map location actions and form handoff camera target.
- `app/src/main/java/com/example/taoyuangutter/gutter/NearbyLocationFixPolicy.kt` — independent Nearby refresh behavior.

## Dependencies

- Android foreground location permission and the fused location provider.
- Saved waypoint/session coordinates and the host's last-known location.
- Location timestamp/accuracy acceptance policies.
- Camera lifecycle and user-pan guards.

## Risks

- Moving a map with a saved point to the device would violate the user's decision; saved point coordinates must keep precedence.
- A stale cached fix can differ from the device's current position even while passing the five-minute cache-age check.
- Applying a delayed fix after the user manually pans can undo the user's map position.
- Requesting location for offline/restored blank forms can introduce a permission prompt; the request must remain camera-only.

## Unknowns

- No current-task runtime trace establishes which screen or permission state produced the reported concern; implementation is based on confirmed source paths and the user's clarified behavior.
- The exact no-fix UI after a new location attempt has not been requested to change; this analysis assumes existing fallback/error behavior remains.
