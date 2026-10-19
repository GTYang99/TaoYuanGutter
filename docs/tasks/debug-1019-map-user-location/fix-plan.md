# Fix Plan

## Root Cause
- `EditMapLocationPolicy` excludes offline forms and requires edit mode or a fresh-add-only flag, so an editable blank restored/offline form can remain at the Taoyuan fallback.
- `GutterFormActivity` treats a `(0,0)` waypoint snapshot as a valid target because it checks only for non-null latitude/longitude.
- `MyLocationController` applies a cached fix first and rejects a newer fix unless its accuracy improves by at least 10 m. Explicit locate actions can therefore finish at the cached coordinate.
- Both host `openWaypointForEdit` callbacks substitute `(0,0)` when the map camera target is unavailable. The camera controller currently no-ops without a map, but the sentinel should not be passed as a target.

## Minimum Fix Boundary
- Make initial location eligibility depend on an editable form, absence of a usable waypoint coordinate, and non-read-only state; do not exclude offline/restored editable forms.
- Treat non-finite, out-of-range, and `(0,0)` coordinates as unusable for the form map viewport while leaving the underlying draft data untouched.
- Add a fresh-preference option to the existing shared location controller for explicit locate actions. Prefer a newer usable fix, then fall back to the cached fix if the fresh request fails or is unusable.
- Make the host form handoff camera target nullable and skip the host camera move when neither the waypoint nor host map supplies a target.
- Add focused unit and emulator regression coverage for policy, camera-only behavior, saved-point precedence, latest-fix recentering, and no-map-target handling.

## Risks
- Editable offline/restored blank forms may request foreground location permission on open; this follows the clarified blank-map behavior and reuses the existing flow.
- Explicit locate may wait for a fresh result before moving instead of immediately jumping to a stale cached result.
- `(0,0)` remains in any pre-existing draft fields; this change treats it only as unusable for camera centering and must not silently rewrite stored data.

## Validation
- Run the focused location-policy unit tests.
- Build the debug app and androidTest APK.
- Run focused gutter-form instrumentation cases on the configured emulator only, explicitly selecting its serial.
- Record any unavailable runtime or CI evidence as `NOT VERIFIED`.
