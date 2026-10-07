# Requirement

## Background
- When an existing gutter point has no saved coordinates, the editing form map currently opens at the fixed Taoyuan-area fallback instead of the user's location.
- The form already has location permission and acquisition behavior for nearby-point lookup, but that flow does not initialize the form map.

## Goal
- For an existing point with missing coordinates, center the form map's initial viewport on the user's current location when permission and a usable fix are available.

## Functional Requirements
- FR-001: Apply the location-centered initial viewport only when editing an existing point whose waypoint has no saved `latLng`. Points with saved coordinates keep opening at those coordinates.
- FR-002: Request foreground location permission when required. After a recoverable denial, offer one additional system permission request. If denied again, or Android no longer presents a permission dialog, show an in-app message that the app cannot obtain the user's location. For a permanent denial, offer a Settings action and a continue/dismiss action.
- FR-008: Request the app's declared fine and coarse location permissions together; either granted permission is sufficient to attempt initial map centering.
- FR-009: If the user opens system Settings from the permanent-denial prompt and returns with either foreground permission granted, retry location centering once. If permission remains denied, keep the fallback and continue editing.
- FR-003: If permission is granted but a usable location is not obtained, perform one bounded location reacquisition attempt using the existing 25-second acquisition timeout. If it still fails, show the location-unavailable message.
- FR-004: Location is used only to center the form map's initial viewport. Do not write it into waypoint coordinates, form coordinate fields, draft data, or a persistent My Location dot/marker.
- FR-005: After the location-unavailable message is dismissed, continue editing at the existing Taoyuan-area fallback and allow manual map movement.
- FR-006: Do not change new-point or draft flows as part of this task.
- FR-007: If the user manually moves the form map while location is being acquired, a later location callback must not override that manual map position.

## Non-functional Requirements
- No new dependency.
- Cancel outstanding location work when the Activity is destroyed; callbacks from canceled attempts must not show stale dialogs or move the map after the flow ends.

## Acceptance Criteria
- AC-001: Opening the normal edit form for a point without `latLng`, with location permission and a usable fix, moves the form map's initial viewport to the device location.
- AC-002: Opening an edit form for a point with saved coordinates continues to center on that saved point and does not request device location for initial centering.
- AC-003: Location used for map centering does not alter `Waypoint.latLng`, `currentLat`/`currentLng`, node coordinate fields, or persisted draft data.
- AC-004: A first recoverable permission denial offers one additional system permission request; a second denial shows the location-unavailable message without looping.
- AC-005: A permanent permission denial shows the location-unavailable message with a Settings action and a continue/dismiss action.
- AC-006: With permission granted but no usable fix, the flow performs one reacquisition attempt capped at 25 seconds; if unavailable, it shows the location-unavailable message.
- AC-007: Dismissing the location-unavailable message leaves the form usable at the Taoyuan-area fallback and allows manual map movement.
- AC-008: New-point and draft map initialization behavior remains unchanged.
- AC-009: If the user manually moves the form map before a location callback arrives, the later callback does not recenter the map.
- AC-010: Fine or coarse foreground location permission is accepted; a coarse-only grant does not trigger the denied-permission prompt.
- AC-011: Returning from Settings with foreground location permission granted triggers one location acquisition attempt; returning without permission leaves the fallback usable.

## Constraints
- Limit the behavior change to normal existing-point edit mode.
- Preserve existing coordinate display and submission semantics.
- Keep the existing Taoyuan fallback as the failure fallback.
- Do not add a My Location indicator or marker to the form map.

## Open Questions
- None. Permission retry, timeout, failure fallback, Settings action, and viewport-only use are confirmed.
