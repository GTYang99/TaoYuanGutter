# Requirement

## Source and Scope
- User request: prevent the gutter form map from unexpectedly centering on Taoyuan or `(0,0)`; use the user's location for blank maps and explicit locate actions.
- Clarified behavior: preserve a selected/saved waypoint as the map center; locate the user only when the map has no usable point coordinate or when the user presses a locate button.
- Scope includes the editable gutter form map, the normal and offline host-map locate actions, and the point-picker locate action.
- Camera centering is viewport-only. Selecting a point in the point picker remains the only action in this scope that writes a newly selected coordinate.

## Goal
- Ensure blank editable gutter maps and explicit locate actions center on the latest usable device location, while saved waypoint centers and coordinate data remain unchanged.

## Functional Requirements
- FR-001: A valid saved waypoint coordinate remains the initial form-map center and does not start automatic location acquisition.
- FR-002: An editable form map with no usable saved coordinate, including a missing coordinate or the `(0,0)` sentinel, attempts to center on the current device location. This applies to online, offline, and restored-draft forms.
- FR-003: Camera-only location acquisition must not populate or persist `NODE_X`, `NODE_Y`, or waypoint/session coordinates.
- FR-004: An explicit locate action on the host map or point picker must use a newer usable location result even when its accuracy improves on the cached fix by less than 10 m. If no usable fresh result arrives, an existing usable cached fix may be used as fallback.
- FR-005: If location permission or a usable fix is unavailable, preserve the existing fallback and location-unavailable behavior.
- FR-006: When the host map has no camera target and the waypoint has no coordinate, opening the gutter form must not move the host camera to `(0,0)`.
- FR-007: The point-picker Confirm action continues to return its camera target as the selected coordinate; pressing Locate alone does not write it to the form.
- FR-008: A blank form must not visibly show the Taoyuan fallback while a usable host/current location is still being resolved. Use a fresh usable host fix as the synchronous initial center; if none exists, reveal the blank map only after a usable current fix is applied or the existing unavailable/fallback path is selected.

## Acceptance Criteria
- AC-001: A valid saved-coordinate form opens centered on that coordinate and does not start initial location acquisition.
- AC-002: An editable blank form, including offline and restored-draft flows, centers its map on a usable current fix without changing `NODE_X`, `NODE_Y`, or waypoint/session coordinates.
- AC-003: The host-map and point-picker locate actions recenter to a newer usable fix even when the accuracy improvement is less than 10 m; a stale/invalid result does not replace a usable cached fallback.
- AC-004: A blank form or point picker with no usable fix retains its existing fallback and location-unavailable behavior.
- AC-005: A missing waypoint coordinate plus an unavailable host map target does not issue a camera move to `(0,0)`.
- AC-006: The point-picker Locate action changes only the viewport; Confirm continues to return the chosen camera coordinate.
- AC-007: When a blank form opens without a usable host handoff location, the form map stays hidden before location resolution; it becomes visible centered on a usable current fix, or reveals the existing Taoyuan fallback only after permission/location failure. A valid saved point remains immediately visible and centered.

## Non-functional Requirements
- No new dependency.
- Reuse the existing fused-location client, permission flow, map controller, and form camera flow.
- Keep saved waypoint data authoritative and avoid unrelated form or draft changes.

## Constraints
- Validate with targeted unit tests, the affected Android build, and emulator instrumentation for form camera behavior.
- Do not run the connected-device task without selecting the emulator serial; physical-device validation is not in scope.

## Open Questions
- None. The user clarified that saved point centers take precedence and current location applies only to blank maps or explicit locate actions.
