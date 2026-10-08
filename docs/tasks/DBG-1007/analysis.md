# Repository Analysis

## Current Behavior
- `AddGutterBottomSheet` delegates point editing to `MapWorkspaceFragment`; that host moves the shared main map using the point coordinate or current camera target, then launches `GutterFormActivity` with a separate form map.
- In a normal edit, a waypoint without coordinates is included in the session JSON with null latitude/longitude. `GutterFormActivity` restores this JSON and its `onMapReady()` explicitly centers the form map at `LatLng(24.9929, 121.3011)`. The source comment says the form map does not jump to device location.
- The main map starts at the same fixed Taoyuan-area coordinate, then asynchronously attempts to move to a device location. When opening a waypoint, `MapWorkspaceFragment` also uses the waypoint coordinate or its current camera target for the main map. This does not change the separate form map's fallback.
- The form receives the main screen's last known location, but current code uses that extra only when the user starts nearby-waypoint lookup; initial form map setup does not read it.

## Expected Behavior
- For the edit flow under analysis, use the user's location only to center the form map's initial viewport; do not write it into the waypoint or coordinate fields.
- If permission is denied or location cannot be obtained, retry the permission flow; if the user still does not grant permission or location remains unavailable, show a prompt that the app cannot obtain the user's location.
- A device-location default is technically feasible using the form's existing fused-location client and permission flow. Retry and fallback behavior are recorded under Confirmed Decisions below.

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MyLocationController.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt` (initiates the host callback; it does not choose the fallback coordinate)

## Dependencies
- Android fine/coarse location permission
- Google fused location provider and `LocationFixQualityPolicy`
- Host-to-form intent data and the form's independent map initialization
- Existing `GutterFormActivity` nearby-point permission/retry flow, which provides a 25-second location acquisition timeout and Settings route for non-retryable denial

## Risks
- Waiting for a fresh fix before centering the form map could delay the final initial center; retain the Taoyuan fallback while permission/location resolves.
- The map center must remain separate from waypoint/form data; the approved behavior is viewport-only and must not persist a coordinate.
- A location permission prompt may surprise users who previously could open the form without a usable GPS fix.
- A late location callback could override a manual pan; the implementation must honor map gestures that happen while a request is pending.
- The reusable main-map location controller enables a visible My Location layer, so the form flow will use a camera-only request instead.
- Existing automated tests cover location-quality policy and other form behavior, but no test found in this worktree asserts the missing-coordinate initial camera path. No test was run for this source-only investigation.

## Unknowns
- No blocking open questions remain for planning. Confirmed scope is the existing edit flow described in the request; new-point and draft flows are excluded.

## Confirmed Decisions
- After the location-unavailable prompt is dismissed, editing continues at the existing Taoyuan-area fallback and the user can manually move the map.
- During one form opening, make one additional system permission request after the first denial. If permission is denied again or Android no longer shows its permission dialog, show the app prompt without looping.
- If permission is already granted but no location fix is available, perform one bounded location reacquisition attempt; if it still fails, show the app prompt.
- If permission is permanently denied, the app prompt offers a Settings action and a dismiss/continue action. Returning from Settings with permission granted resumes location acquisition once.
- Fine or coarse foreground permission is sufficient; the map location is not displayed as a marker/dot.
- The map remains under user control: a manual pan before a location callback prevents a late recenter.
- Location is used only for the initial map viewport and is never written to node coordinates or form fields.

## Evidence Confidence
- Root-cause confidence: **95/100 for the current source revision** (`b5254c76d22b9eed77805b8d32601e2572358f9d`). The sheet-to-host handoff, main-map-only fallback, nullable session snapshot, session restoration, and form-map camera branch are all directly connected in code.
- Remaining uncertainty is limited to runtime confirmation on the user's installed build. No device reproduction or screenshot was available. If “other location” refers to a different screen state, a valid saved coordinate or a user-triggered nearby-point import can explain it without contradicting the missing-coordinate fallback.
