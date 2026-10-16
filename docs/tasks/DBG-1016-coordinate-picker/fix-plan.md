# Fix Plan — DBG-1016 Nearby Location and Results

## Status

Root cause is identified and the user authorized a bug fix and validation on the dedicated test emulator only.

## Minimum Fix

1. In `GutterFormActivity.requestHighAccuracyImportLocation()`, preserve the legacy Nearby acceptance rule: accept the first fresh fused fix whose accuracy is within `importLocationAccuracyM`, even when its accuracy is less than 10 m better than the host/cache fix. For that fix, update the current-location marker and camera, then query Nearby with the accepted fix's own latitude/longitude. Keep this change local to Nearby; do not alter `LocationFixQualityPolicy` users elsewhere.
2. Add focused policy coverage for accepting an eligible fresh Nearby fix when its accuracy improvement is under 10 m, while rejecting stale/over-threshold fixes.
3. Validate request-to-list behavior on `Medium_Phone` only using the user's known-good coordinate (`25.065636, 121.5259048`). Confirm that the request uses that location, returns the server result, and displays candidates. A `success=true, data=[]` response at another coordinate is a valid empty result and is not itself an app defect.

## Traceability

- Issue: `ISS-DBG-1016-NEARBY-003`
- Acceptance criterion: `feat-1015` AC-003 (fresh location drives Nearby marker/camera and query); AC-005 (candidate rows use the shared map/list flow).
- Evidence: debugger captured a fresh 4.417 m fix against a 5.0 m host fix; the ≥10 m refinement check returned false and skipped both marker/camera update and follow-up query. The pre-0917 implementation accepted the first fresh fix ≤30 m without this comparative gate.

## Validation Scope

- Unit test the Nearby fix eligibility and refinement behavior.
- Run affected unit tests and debug build.
- Run targeted app/API check on dedicated `Medium_Phone` AVD only; do not use a physical device. Do not import, confirm, or save a point.
- Record any backend/emulator limitation as `NOT VERIFIED` rather than inferring a pass.

## Regression Risk

The code change is confined to the Nearby location callback in `GutterFormActivity`; shared cached/refinement behavior in MainActivity, MapWorkspaceFragment, and MapPointPickerActivity remains unchanged. Nearby will query once from the first fresh acceptable fix, matching the approved legacy behavior.
