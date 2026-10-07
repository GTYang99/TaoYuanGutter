# Implementation Plan

## Goal
- Center the form map on the user's current location when editing an existing point with no saved coordinates, while keeping location out of node data.

## Scope
- Add edit-only location acquisition and permission handling to the form map initialization.
- Reuse existing location policy/controller and nearby-import permission patterns where lifecycle and behavior match.
- Keep saved-coordinate, new-point, and draft flows unchanged.

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — gate initial location lookup to eligible edit forms; handle retries, timeout, prompts, manual camera interaction, and lifecycle cancellation.
- `app/src/main/java/com/example/taoyuangutter/gutter/EditMapLocationPolicy.kt` — pure policy for edit eligibility, fine/coarse grants, and bounded permission/location retries.
- `app/src/main/res/values/strings.xml` — permission retry and location-unavailable dialog copy.
- `app/src/test/java/com/example/taoyuangutter/gutter/EditMapLocationPolicyTest.kt` — cover edit-only eligibility and retry bounds.
- `app/src/main/java/com/example/taoyuangutter/map/MyLocationController.kt` — not changed; it enables the Google Map My Location layer and lacks the form's required bounded failure flow, so it does not match the viewport-only requirement.
- `docs/tasks/DBG-1007/*` — requirement, plan/review, implementation evidence, and state.

## Implementation Steps
- Use the form's existing `FusedLocationProviderClient` for a bounded current-location request. Do not use `MyLocationController`, because it enables a visible My Location layer and does not provide this form's 25-second timeout and retry behavior.
- In `GutterFormActivity.onMapReady()`, preserve saved-coordinate behavior and the Taoyuan fallback; start location acquisition only for normal edit mode with missing coordinates.
- Request fine and coarse foreground permissions together and accept either grant. Add a bounded permission flow: first request, one user-confirmed retry after recoverable denial, then the unavailable prompt; route permanent denial to app Settings.
- On granted permission with no fix, allow one additional acquisition attempt capped at 25 seconds, then show the unavailable prompt.
- When the permanent-denial Settings action returns to the Activity, recheck permission; if granted, resume one location acquisition, and if not, retain the fallback.
- Keep the fallback map interactive after the prompt; if the user moves it while a location request is pending, ignore the late camera update. Cancel pending requests/timeouts when the Activity is destroyed.
- Add targeted unit coverage for the pure flow policy and run affected unit tests plus the app build.

## Test Plan
- Unit tests for edit eligibility, fine/coarse grant handling, one permission retry limit, one location reacquisition limit, and permanent-denial Settings routing.
- Build the Android app and run the targeted location-policy unit tests.
- Physical device checks for AC-001 through AC-007 and AC-009 through AC-011 if a device with granted/denied permission states is available; otherwise record these as `NOT VERIFIED`.

### Physical Device Test Scope
- Requires physical device: Yes
- Device/environment: Android device with Google Play Services; exercise granted permission, recoverable denial, and permanent denial states.
- In-scope Acceptance Criteria: AC-001 through AC-007, AC-009 through AC-011
- Regression risk: Location permission result and form-map camera behavior can affect map initialization and existing edit flow.
- Full regression required: No
- Full regression trigger: 無
- Stop condition: All listed cases have a result, or the required device/permission state is unavailable and the affected case is recorded `NOT VERIFIED`.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Android device | Open an existing point with no coordinates while location permission is granted | Form map centers on a usable device fix; coordinate fields stay unchanged | Screen recording or camera target observation; coordinate state before/after |
| AC-002 | Android device | Open an existing point with saved coordinates and permission denied | Form map centers on the saved coordinate without requesting location | Camera target and permission-dialog observation |
| AC-003 | Android device | Open a missing-coordinate edit and grant location | Only the map viewport moves; waypoint and coordinate fields remain unchanged | Before/after form-data inspection |
| AC-004 | Android device | Deny the first permission request, then choose retry and deny again | One retry is offered; unavailable prompt appears after second denial; no loop | Dialog sequence and result |
| AC-005 | Android device | Revoke permission and enter a permanently denied state; open edit form | Unavailable prompt offers Settings and continue/dismiss | Dialog actions and result |
| AC-006 | Android device | Grant permission with device location unavailable | One bounded reacquisition runs, then unavailable prompt appears | Timing and prompt result |
| AC-007 | Android device | Dismiss unavailable prompt | Form remains usable at Taoyuan fallback and map remains manually movable | Map interaction observation |
| AC-009 | Android device | Start location acquisition, pan the map before the callback | The later callback does not move the map away from the user's manual position | Camera target before/after callback |
| AC-010 | Android device | Grant approximate/coarse location only, then open a missing-coordinate edit | The app attempts location acquisition without showing a permission-denied prompt | Permission result and map target observation |
| AC-011 | Android device | Open Settings from permanent denial, grant permission, and return | Form retries location once and centers on a fix; if permission remains denied, fallback remains usable | Permission state and resulting map target |

## Regression Plan
- Confirm existing-coordinate edit still opens at the saved point without a permission prompt.
- Confirm new-point and draft entry do not invoke the new edit-only location request.
- Confirm coordinate fields and waypoint snapshot remain unchanged after successful and failed location lookup.
- Confirm finishing/destroying the form cancels timeout and location callbacks.
- Confirm an in-flight location callback does not override a manual map movement.

## Risks
- A location result can arrive after a user manually moves the map; callbacks must be canceled or ignored once the user takes control or the Activity leaves the edit flow.
- Requesting permission can delay the map's final initial center; keep the Taoyuan fallback interactive and provide a clear continue path.
- A current-location callback must not expose a My Location dot/marker or persist coordinates.

## Rollback Plan
- Revert the edit-only permission/camera changes and retain the existing saved-coordinate and Taoyuan fallback behavior.

## Current Behavior
- Normal edit with missing waypoint coordinates passes nulls in session JSON; `GutterFormActivity.onMapReady()` centers its independent form map at `24.9929, 121.3011` and does not request location.

## Expected Behavior
- Normal edit with missing coordinates requests permission/location and centers only the form map viewport on a usable device fix; failure keeps the existing Taoyuan fallback and informs the user.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Edit-only map lookup and camera move | Targeted unit coverage plus physical device case |
| AC-002 | Preserve saved-coordinate branch | Existing-coordinate regression case |
| AC-003 | Keep location callback camera-only | Unit/source assertion and before/after form data check |
| AC-004 | Permission retry policy and dialog | Policy unit test and device denial case |
| AC-005 | Permanent-denial Settings dialog | Policy unit test and device permission state case |
| AC-006 | One bounded reacquisition attempt | Policy unit test and device location-unavailable case |
| AC-007 | Continue on fallback map | Device prompt-dismiss/manual-pan case |
| AC-008 | Gate by edit mode | Unit test plus new/draft regression case |
| AC-009 | Ignore late callbacks after manual pan | Targeted unit test for movement guard plus device interaction case |
| AC-010 | Accept either declared foreground location permission | Policy unit test plus device approximate-location case |
| AC-011 | Recheck permission after Settings return | Policy unit test and device Settings round-trip case |

## Failure Behavior
- Permission denial or location failure leaves the map at the Taoyuan fallback, shows the unavailable-location prompt, and allows editing to continue. Permanent denial includes a Settings action. A late callback after cancellation is ignored.

## Security and Privacy
- Request foreground location only. Use coordinates in memory to move the map; do not log or persist them through this feature.

## Open Questions
- 無
