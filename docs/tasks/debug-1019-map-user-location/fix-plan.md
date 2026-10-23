# Fix Plan

## Root Cause
- `EditMapLocationPolicy` excludes offline forms and requires edit mode or a fresh-add-only flag, so an editable blank restored/offline form can remain at the Taoyuan fallback.
- `GutterFormActivity` treats a `(0,0)` waypoint snapshot as a valid target because it checks only for non-null latitude/longitude.
- `MyLocationController` applies a cached fix first and rejects a newer fix unless its accuracy improves by at least 10 m. Explicit locate actions can therefore finish at the cached coordinate.
- Both host `openWaypointForEdit` callbacks substitute `(0,0)` when the map camera target is unavailable. The camera controller currently no-ops without a map, but the sentinel should not be passed as a target.
- `GutterFormActivity` validates a fused location result with finite/range checks but omits `MapCoordinatePolicy`, so a provider result of `(0,0)` can still move a blank form map to `(0,0)`.

## Minimum Fix Boundary
- Make initial location eligibility depend on an editable form, absence of a usable waypoint coordinate, and non-read-only state; do not exclude offline/restored editable forms.
- Treat non-finite, out-of-range, and `(0,0)` coordinates as unusable for the form map viewport while leaving the underlying draft data untouched.
- Add a fresh-preference option to the existing shared location controller for explicit locate actions. Prefer a newer usable fix, then fall back to the cached fix if the fresh request fails or is unusable.
- Make the host form handoff camera target nullable and skip the host camera move when neither the waypoint nor host map supplies a target.
- Add focused unit and emulator regression coverage for policy, camera-only behavior, saved-point precedence, latest-fix recentering, and no-map-target handling.
- Apply the shared coordinate policy to fused form-map results so `(0,0)` follows the existing retry/fallback path; cover this with provider-result instrumentation and coordinate-preservation assertions.

## Risks
- Editable offline/restored blank forms may request foreground location permission on open; this follows the clarified blank-map behavior and reuses the existing flow.
- Explicit locate may wait for a fresh result before moving instead of immediately jumping to a stale cached result.
- `(0,0)` remains in any pre-existing draft fields; this change treats it only as unusable for camera centering and must not silently rewrite stored data.

## Validation
- Run the focused location-policy unit tests.
- Build the debug app and androidTest APK.
- Run focused gutter-form instrumentation cases on the configured emulator only, explicitly selecting its serial.
- Record any unavailable runtime or CI evidence as `NOT VERIFIED`.

## Debug Follow-up: Fresh-Add Instrumentation Failure
- Failed case: `freshNewPointWithEmulatorGpsFixCentersCameraOnly()` expected the injected Taipei coordinate, but its setup omitted `setMockLocation(testPoint)`. The emulator supplied its default Mountain View coordinate instead.
- Classification: test-fixture defect (ISS-DBG-1019-005); this result does not establish a production location regression.
- Minimum fix: set the test provider to `testPoint` before launching the form and clear it in `finally`, matching the other location instrumentation cases.
- No production code change is included in this follow-up. Rerun this targeted case once after the fixture correction.

## Debug Follow-up: `(0,0)` Location Provider Result
- Failed criteria: AC-002 and AC-003; the form accepts `(0,0)` from the location provider and can move the camera there.
- Classification: implementation regression (ISS-DBG-1019-006), priority P2.
- Minimum fix: replace the form callback's inline coordinate checks with `MapCoordinatePolicy.isUsableCoordinate()` and preserve its existing retry/fallback behavior.
- Add an instrumentation case that injects a `(0,0)` fused mock fix, verifies the camera does not target `(0,0)`, and confirms form/session coordinates remain unchanged.
- Result: implemented in `GutterFormActivity.kt`; `zeroZeroFusedLocationDoesNotMoveBlankFormMap()` passed on Android 14 API 34 with fallback camera, unavailable prompt, and coordinate-preservation assertions.

## Debug Follow-up: Nearby Import `(0,0)` Result

- Finding: `NearbyLocationFixPolicy` is a separate gate for the form's Nearby import flow. It accepts fresh accurate fixes without checking coordinates, so `(0,0)` can reach the map camera.
- Minimum fix: reuse `MapCoordinatePolicy.isUsableCoordinate()` inside the Nearby fix policy, wire latitude/longitude from the provider callback, and add focused unit cases proving `(0,0)` is rejected while valid fixes retain the existing age and accuracy rules.
- Validation: run the focused Nearby policy unit test, the existing location-policy unit tests, debug and androidTest builds, and form-map instrumentation on the configured emulator. Record CI separately.
- Result: implemented and validated with `NearbyLocationFixPolicyTest` (3/3 passed), the focused location-policy test set, `assembleDebug`, and `assembleDebugAndroidTest`. Existing Android 14 form instrumentation covers invalid and valid initial form fixes; this Nearby callback was validated at its policy boundary and by source wiring review.

## Debug Follow-up: Blank Form Briefly Shows Taoyuan Before GPS

- Failed behavior: opening a blank point from `AddGutterBottomSheet` can visibly show the fixed Taoyuan fallback before a usable asynchronous GPS result arrives.
- Classification: implementation regression (`ISS-DBG-1019-010`), priority P2; source sequence confirms fallback camera assignment precedes location acquisition, and current tests only check the eventual camera target.
- Minimum fix: synchronously seed from a fresh usable host fix attached to the intent. If none exists, keep the blank map surface hidden until a usable current fix has been applied or the existing permission/location failure path has selected and exposed the Taoyuan fallback. Keep saved-coordinate precedence and camera-only data behavior unchanged.
- Result: `GutterFormActivity` now hides an eligible blank map during location resolution, uses a recent usable host fix before first display, and reveals the map at a provider fix or at the existing fallback after failure. Saved coordinates remain visible and take precedence.
- Validation: on `emulator-5554` (Android 16/API 36), first-visible-frame host-fix, permission-denial/fallback, fresh-add GPS, and saved-coordinate cases passed. `manualPanBeforeFusedMockCallbackKeepsManualCameraTarget` failed twice because the swipe did not move the camera before the callback; cause is unknown and tracked separately as `ISS-DBG-1019-011`. Do not claim the delayed-callback pan case passed.

## Debug Follow-up: Blank Point Picker Shows Taoyuan Before GPS

- Finding: invalid initial coordinates enter the point picker's Taoyuan fallback branch before its asynchronous location request resolves.
- Minimum fix: hide `mapPicker` and disable Confirm for an unusable initial coordinate; reveal at a usable location, or reveal the existing fallback after permission/location failure. Preserve immediate display of valid initial coordinates.
- Tests: add a focused unit test for blank versus valid initial camera decisions and emulator coverage asserting the first visible blank-picker frame is not the Taoyuan fallback.
