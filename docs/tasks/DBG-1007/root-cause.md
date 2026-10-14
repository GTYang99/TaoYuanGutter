# Root Cause Analysis

## Finding

Issue: `ISS-DBG-1007-LOC-001`.

There are two map instances in this flow. `AddGutterBottomSheet` is over the shared main map; the point editing screen (`GutterFormActivity`) creates a separate full-screen form map. The Taoyuan fallback in the editing form is caused by the latter's explicit initialization, not by `AddGutterBottomSheet`.

## End-to-End Evidence

1. `AddGutterBottomSheet.openWaypointAt()` delegates normal point editing to `LocationPickerHost.openWaypointForEdit()` (`AddGutterBottomSheet.kt:948-955`).
2. `MapWorkspaceFragment.openWaypointForEdit()` calculates a fallback from the shared main map camera and passes it to `openAddForm()` (`MapWorkspaceFragment.kt:761-780`). `openAddForm()` uses it to move the shared main map, then builds a separate form intent (`MapWorkspaceFragment.kt:1228-1255`).
3. `GutterFormNavigator.buildAddIntent()` serializes absent waypoint coordinates as `0.0` in the legacy arrays, but also writes the normal edit session snapshot with nullable latitude/longitude (`GutterFormNavigator.kt:65-103`). `GutterFormActivity.restoreSessionWaypoints()` prefers that JSON snapshot, preserving null coordinates instead of substituting the legacy zero values (`GutterFormActivity.kt:2387-2417`).
4. The Activity restores session waypoints synchronously in `onCreate()` before it sets up the form and queues map initialization (`GutterFormActivity.kt:1278-1280, 1361-1365, 1404-1419`). Thus the later map callback reads the restored null-coordinate snapshot, rather than racing an empty waypoint list.
5. `GutterFormActivity.onMapReady()` checks the restored current waypoint. With absent coordinates, `currentTarget` is null and the fallback branch centers the form map at `LatLng(24.9929, 121.3011)` (`GutterFormActivity.kt:1607-1617`). The adjacent source comment explicitly describes this as the Taoyuan initial camera and says the form map does not jump to location.
6. The host's last location is passed in the intent, but `GutterFormActivity` stores it as `hostLastLatLng` for nearby-point import. It is used by `tryLoadNearbyFromHost()` after that flow starts, not by initial map setup (`GutterFormActivity.kt:1219-1226, 842-853`).

Therefore, for a normal edit with missing coordinates, the form map's initial center is the fixed Taoyuan coordinate even if the shared main map has already moved to the device location. The main-map fallback calculation is a separate behavior and does not feed the form map's camera target.

Separately, the shared main map starts at `LatLng(24.9929, 121.3011)` and asynchronously tries to move to the device location. If that lookup is denied or unavailable, its camera can remain at the fixed location; if panned, it can be elsewhere. When `MapWorkspaceFragment.openWaypointForEdit()` falls back to the main camera target, that affects the shared map presentation but does not override the form map's separate initialization.

## Affected Code

- `GutterFormNavigator.kt:65-68`: serializes absent waypoint coordinates as `0.0` in the form intent.
- `GutterFormNavigator.kt:68-82`: separately serializes the normal edit waypoint snapshot with nullable coordinates; this is what prevents the legacy `0.0` values from becoming the form's current waypoint coordinates.
- `GutterFormActivity.kt:2387-2417`: restores the session JSON before using the legacy arrays.
- `GutterFormActivity.kt:1278-1280, 1361-1365, 1404-1419`: restores waypoint state before queuing form-map creation, ruling out an initialization-order race on the normal edit path.
- `GutterFormActivity.kt:1265-1268`: reads the intent coordinates as `currentLat`/`currentLng`.
- `GutterFormActivity.kt:1607-1617`: centers the form map on saved coordinates, or hard-codes `LatLng(24.9929, 121.3011)` when missing.
- `activity_gutter_form.xml:4-17`: confirms the form owns a separate map container.
- `MapWorkspaceFragment.kt:566`: initializes the separate main map to a fixed Taoyuan-area coordinate.
- `MapWorkspaceFragment.kt:568-574`: starts an asynchronous location permission/location request for the main map.
- `MapWorkspaceFragment.kt:281-294`: on permission grant, retries location; on denial, leaves the current camera unchanged.
- `MyLocationController.kt:30-36, 51-56, 82-95`: requires permission and moves the map only for an accepted cached or refined fix.
- `MapWorkspaceFragment.kt:761-780`: falls back from missing waypoint coordinates to the main map camera target for the shared map handoff.
- `MapWorkspaceFragment.kt:1228-1255` and `GutterFormNavigator.kt:107-117`: pass the last host location to the form; the form only uses it for nearby search.
- `AddGutterBottomSheet.kt`: delegates point editing through `LocationPickerHost`; it is not the source of the form map's default coordinate.

## Classification and Confidence

- Form map path: confirmed by source inspection; its missing-coordinate default is hard-coded to the Taoyuan-area location.
- Main map path: confirmed separately; its camera can later recenter from a device location.
- Classification: the observed fallback is existing behavior; the user has since approved a new edit-only current-location initial viewport requirement, recorded in `requirement.md`.
- Confidence: **95/100 for the source-code root cause**. The normal edit path, nullable snapshot restoration, null-coordinate branch, and fixed camera destination are all directly connected in source at the current worktree revision.
- Residual uncertainty: no device reproduction or screenshot from the user's installed build was available. This score is for the current source-code explanation, not proof that every reported “other location” came from this exact code revision. A valid saved coordinate or a later user-triggered nearby-point import can center the form map elsewhere.

## Regression Risk

Changing the form map to use device location could affect permission expectations, form startup timing, and whether a provisional map center is interpreted as persisted node data. The main-map location behavior and waypoint selection should remain unchanged unless separately required. No production code was changed in this investigation.
