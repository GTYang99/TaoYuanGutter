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
