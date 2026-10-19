# Root Cause Analysis

## Finding

Issue: ISS-DBG-1019-001.

There is no single current-location initialization path shared by every map surface. The online host map, offline host map, form background map, point picker, and Nearby import sheet each apply location at different lifecycle stages and under different eligibility rules.

The code-level gap matching the clarified expectation is the empty form map in offline/restored-draft flows: the form uses the fixed Taoyuan fallback when its waypoint snapshot has no coordinates, but the initial location policy excludes offline forms, and restored drafts do not receive the fresh-add flag. These maps therefore do not attempt the user's location. The form also treats a `(0,0)` waypoint pair as saved because it checks only for non-null values, so it can skip location acquisition for a sentinel coordinate.

A second confirmed code path can make an explicit locate action retain an older center. The host map and point-picker FAB use `MyLocationController`, which accepts a cached fix up to five minutes old and only applies a newer fix if its accuracy is at least 10 m better. Geographic displacement is not considered. A newer fix at a different position with similar accuracy is rejected, so the camera can remain at the accepted cached position. The policy and its unit test prove the gate exists; this task has no incident-time callback/camera capture proving that it occurred on the user's screen.

The host form-opening callbacks synthesize `LatLng(0,0)` if both the selected waypoint and host camera target are missing. The camera controller currently no-ops when its map provider is absent, so that expression alone does not prove a visible jump; nullable handling removes the invalid target request.

## Evidence

1. The online launcher is `LoginActivity -> MainShellActivity -> MapWorkspaceFragment` (`LoginActivity.kt:165`, `MainShellActivity.kt:38-40, 63-72`). `MapWorkspaceFragment.onMapReady()` moves to `(24.9929, 121.3011)` and then calls `MyLocationController.requestLocationAndMove()` (`MapWorkspaceFragment.kt:554-574`).
2. The offline login action starts `MainActivity` (`LoginActivity.kt:181-188`). Its map similarly starts at the Taoyuan fallback and requests current location (`MainActivity.kt:861-872`).
3. `AddGutterBottomSheet` delegates map actions to its host. The normal host is `MapWorkspaceFragment`; the legacy offline host is `MainActivity`. When a waypoint has coordinates, opening its form passes that point as the map center. When absent, the host falls back to its current camera target; the literal `LatLng(0,0)` is reached only if the host map has no target (`MapWorkspaceFragment.kt:761-781`, `MainActivity.kt:1595-1618`). The camera controller is a no-op when the map provider is null, so this expression alone does not establish an actual jump to `(0,0)`.
4. The form map uses the session waypoint's latitude/longitude when present. Otherwise, it moves to the Taoyuan fallback and calls the location flow only if `EditMapLocationPolicy.shouldLocateForInitialCamera()` returns true (`GutterFormActivity.kt:1672-1696`).
5. That policy requires an edit or explicit fresh-add flag and excludes view mode, offline mode, and submitted read-only mode (`EditMapLocationPolicy.kt:5-12`). The fresh-add flag is true only for a fresh online add sheet with no restored draft (`AddGutterBottomSheet.kt:1218-1224`). This explains why the earlier `debug1016-1` fix covers fresh online additions but not blank offline/restored maps.
6. The point picker preserves a supplied non-zero initial coordinate; only an empty `(0,0)` input triggers the Taoyuan fallback and an automatic locate request (`MapPointPickerActivity.kt:216-239`). Its FAB calls the shared `MyLocationController` (`MapPointPickerActivity.kt:188-207`), so the cache-refinement gate also applies to explicit recentering.
7. `MyLocationController` immediately applies any cached fix within five minutes, then requests a high-accuracy refinement (`MyLocationController.kt:37-54`). `LocationFixQualityPolicy.shouldUseRefinement()` rejects candidates that are not newer or do not improve accuracy by at least 10 m; it does not compare latitude/longitude displacement (`LocationFixQualityPolicy.kt:7-10, 23-35`). `LocationFixQualityPolicyTest.refinementRequiresNewerTenMeterAccuracyImprovement()` codifies that threshold.
8. Nearby import uses a separate rule: a fresh fix within the existing 30 m threshold can move the map and trigger a new query without the 10 m improvement requirement (`GutterFormActivity.kt:943-959`, `NearbyLocationFixPolicy.kt`). Do not attribute the shared-controller gate to the current Nearby flow.
9. The earlier `debug1016-1` implementation passed its fresh online add-map centering case on an emulator, but its state still records the unavailable-location interaction and CI as not verified (`docs/tasks/debug1016-1/execution-report.md:33-37`, `docs/tasks/debug1016-1/state.yaml`). Those results do not verify offline/restored empty maps or the point-picker FAB.

## Classification

- `ISS-DBG-1019-001`: `requirement_gap` / scope expansion for blank offline/restored form maps. The prior exclusion belonged to the earlier fresh-add scope. The user clarified that blank editable forms should use current location while valid saved points remain centered; this is captured in the approved requirement and plan. Eligibility and coordinate checks are implemented; Android 14 end-to-end cases now pass.
- `ISS-DBG-1019-002`: `unknown` for the shared locate-button refinement path. The former accuracy gate is removed for fresh-preferred host and picker actions, with a usable cached fix retained as fallback. Unit coverage passes, but this task has no successful runtime capture from those buttons.
- `ISS-DBG-1019-003`: source-confirmed invalid host-camera fallback. A `(0,0)` target was possible only when both the waypoint and map target were missing. Both callbacks now keep the target nullable and skip camera movement when no usable point exists.
- `ISS-DBG-1019-004`: `environment` for the earlier Android 16 emulator run, where mock location did not reach GMS under an invalid Maps key. The same restored blank-form case passes on Android 14 API 34 with the configured key.
- `ISS-DBG-1019-005`: `environment` (test fixture) for the fresh-add instrumentation failure. The test omitted mock-location setup, so the emulator's default Mountain View location was a valid result; add the expected test fix before judging that case.
- `ISS-DBG-1019-006`: `implementation_regression` for the form's live location callback. It checks finite/ranged latitude and longitude inline, so `(0,0)` passes and is sent to `CameraUpdateFactory`; the shared `MapCoordinatePolicy` that rejects `(0,0)` is used for saved form targets but not this callback.

## Confidence and Limits

- High confidence in the source-level causes: blank offline/restored forms were excluded, `(0,0)` could be treated as a saved camera target or synthesized host target, and the shared controller applied a fixed 10 m accuracy improvement threshold.
- Focused unit tests and affected debug APKs pass. Saved-coordinate, ordinary blank-edit, restored blank, offline blank, fresh-add, no-fix fallback, saved `(0,0)`, and fused-provider `(0,0)` cases pass on Android 14 API 34.
- There is no physical-device evidence, and host-map/point-picker Locate actions lack end-to-end runtime coverage. Confidence is good for the exercised form-map cases and limited for those remaining flows.

## Debug Follow-up: Fresh-Add Instrumentation Failure

- Failed case: `freshNewPointWithEmulatorGpsFixCentersCameraOnly()` initially expected Taipei but did not call `setMockLocation(testPoint)`. The emulator returned its default Mountain View fix, matching the observed camera target.
- This is a test-fixture defect, not evidence of a production camera/location regression. The minimum correction is to inject the expected fix and clear the provider in `finally`.
- No production location-request change was justified by this failure. After correcting the fixture, the targeted case passed on Android 14 API 34.

## Debug Follow-up: `(0,0)` Location Provider Result

- Failed requirement: AC-002 requires a usable current fix for a blank form; AC-003 requires invalid fixes not to replace a usable fallback. `(0,0)` is explicitly unusable under the approved coordinate policy.
- Root cause: `GutterFormActivity.startEditMapLocationAttempt()` accepts any finite in-range provider result. Unlike `MyLocationController`, it does not call `MapCoordinatePolicy.isUsableCoordinate()`, so a `(0,0)` provider fix is treated as a valid camera target.
- Evidence: `GutterFormActivity.kt` validates only finiteness/ranges before moving the camera; `MapCoordinatePolicy.kt` rejects `(0,0)`; the existing zero-zero instrumentation covers a saved waypoint sentinel, not a provider result.
- Affected files: `GutterFormActivity.kt` and `EditMapInitialLocationInstrumentedTest.kt`.
- Regression risk: low and localized; the invalid fix should follow the existing retry/fallback/unavailable path and must not mutate form/session coordinates.
- Minimum fix: use the shared coordinate policy when filtering the provider result and add a mock-provider `(0,0)` regression case asserting the camera remains at fallback and coordinate data remains untouched.
- Resolution: `GutterFormActivity.startEditMapLocationAttempt()` now calls `MapCoordinatePolicy.isUsableCoordinate()` for fused results. The new `zeroZeroFusedLocationDoesNotMoveBlankFormMap()` instrumentation test passed on Android 14 API 34 and verified fallback camera, unavailable prompt, and unchanged form/session coordinates.

## Developer Audit Follow-up: Nearby Import `(0,0)` Result

- Issue: `ISS-DBG-1019-007`, category `implementation_regression`, priority P2.
- Observation: A final audit of every form-map camera update found that the Nearby import flow has a separate fused-location callback from the initial form-map flow. It uses `NearbyLocationFixPolicy.shouldAccept()` before calling `showImportMyLocationMarker()`.
- Root cause: The Nearby policy only checks timestamp and accuracy; it does not validate latitude/longitude with `MapCoordinatePolicy`. Thus an accurate fresh `(0,0)` fix passes and the form map camera moves to the invalid coordinate.
- Affected files: `GutterFormActivity.kt`, `NearbyLocationFixPolicy.kt`, and `NearbyLocationFixPolicyTest.kt`.
- Acceptance mapping: AC-004 / FR-005 — an unusable fix must not replace the existing fallback and must follow the unavailable/error path.
- Minimum fix: pass the candidate coordinate into the Nearby policy and reject any coordinate that `MapCoordinatePolicy` rejects. Add unit coverage for `(0,0)` and preserve the existing freshness/accuracy behavior for valid points.
- Resolution: `NearbyLocationFixPolicy` now delegates coordinate checks to `MapCoordinatePolicy`; `GutterFormActivity` passes the provider latitude/longitude into the policy before its camera update. The focused policy tests pass, including the new `(0,0)` case.
