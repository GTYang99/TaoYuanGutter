# Fix Plan

## Scope

No production fix is authorized or needed to complete this root-cause analysis. Confirmed intent for the edit flow: request location permission when needed, inform the user if location cannot be obtained, and use the location only to move the form map's initial viewport. Do not write the location to waypoint coordinates or coordinate fields.

## Feasibility

Yes. The app already passes the host's last known location to the form for nearby-point lookup, and `GutterFormActivity` already has a fused-location permission and acquisition flow for that nearby-point feature. That existing flow uses a retry dialog for a recoverable permission denial, sends a non-retryable denial to Settings, and caps location acquisition at 25 seconds (`GutterFormActivity.kt:587-610, 822-839, 970-1004`). It is not invoked during initial form-map setup today.

A future change can use a valid device location as the missing-coordinate form map's provisional initial target. It should use an explicit valid fix or await a location result rather than relying on the main map camera, because location loading is asynchronous and the form map is independent. Reusing the existing 25-second acquisition timeout would keep the new flow consistent with nearby-point lookup.

## Confirmed Decisions

- After the unavailable-location prompt is dismissed, editing continues at the fixed Taoyuan-area map center and allows manual map movement.
- Make one additional permission request after the first denial per form opening. After a second denial or when Android no longer shows its permission dialog, show the app prompt without looping.
- If permission is granted but no location fix is available, perform one bounded location reacquisition attempt, using the existing 25-second timeout, before showing the prompt.
- If permission is permanently denied, include a Settings action in the app prompt, while allowing the user to dismiss/continue.
- Scope is the edit flow that prompted this analysis; new-point and draft flows are excluded unless separately requested.
- Location is used only for the form map's initial viewport. It does not populate or persist waypoint coordinates.
- If permission is denied or a location cannot be obtained, retry the permission flow; if permission is still not granted or location remains unavailable, inform the user that the app cannot obtain their location.

No blocking open questions remain for planning.

## Candidate Validation After Requirements Are Approved

- Verify an available device fix becomes the initial center for a point with no saved coordinates.
- Verify a point with saved coordinates still opens at its saved coordinate.
- Verify permission denial and unavailable-location behavior match the approved fallback.
- Verify the location is used only to center the initial map viewport and does not change saved waypoint coordinates or form coordinate fields.
