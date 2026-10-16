# Requirement

## Source and Scope
- User instruction: change the empty form opened from AddGutterBottomSheet so its mini-map viewport uses the user's current location; implement the bugfix with minimal changes and validate only on an emulator.
- Scope is a normal, online, writable new/add form opened from a non-restored AddGutterBottomSheet with no saved waypoint coordinates.
- The location is for initial camera positioning only. It must not populate form coordinate fields or waypoint/session coordinates.
- This request supersedes the earlier DBG-1007 exclusion for this exact new/add flow. Offline forms and restored drafts remain out of scope.

## Goal
- Center the empty new/add form's initial map viewport on the user's current location when a usable fix is available.

## Functional Requirements
- FR-001: For an eligible empty new/add form with no saved coordinates, use the existing foreground permission and fused-location acquisition flow to center the initial form-map viewport on a usable current location.
- FR-002: Use the acquired location for camera positioning only; do not set Waypoint coordinates, form coordinate fields, or persisted draft coordinates.
- FR-003: Keep saved waypoint coordinates as the initial camera target.
- FR-004: Keep offline and restored-draft map initialization unchanged.
- FR-005: If permission or a usable location is unavailable, reuse the existing location-unavailable prompt and Taoyuan fallback behavior.

## Non-functional Requirements
- No new dependency.
- Keep the change limited to the new/add form launch signal, the existing location policy, and focused regression coverage.

## Acceptance Criteria
- AC-001: A normal online new/add form with no saved coordinates and a usable emulator location centers its form-map viewport on that location.
- AC-002: Camera centering does not populate or persist Waypoint coordinates or NODE_X/NODE_Y form fields.
- AC-003: Saved-coordinate forms continue to center on the saved coordinates without starting initial location acquisition.
- AC-004: Offline and restored-draft forms do not start the new/add initial-location flow.
- AC-005: If no usable location is available, the existing unavailable prompt and Taoyuan fallback remain usable.

## Constraints
- Validation uses Android emulators only; do not run tests on connected physical devices.
- No production code changes outside the described form-map location flow.

## Open Questions
- None for this scope. Reuse the current edit form's permission, location retry, unavailable prompt, and fallback behavior.
