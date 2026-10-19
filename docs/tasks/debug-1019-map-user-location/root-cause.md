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

- `ISS-DBG-1019-001`: `requirement_gap` / scope expansion for blank offline/restored form maps. The prior exclusion belonged to the earlier fresh-add scope. The user clarified that blank editable forms should use current location while valid saved points remain centered; this is captured in the approved requirement and plan. Eligibility and coordinate checks are implemented; end-to-end location centering remains unverified in the current emulator.
- `ISS-DBG-1019-002`: `unknown` for the shared locate-button refinement path. The former accuracy gate is removed for fresh-preferred host and picker actions, with a usable cached fix retained as fallback. Unit coverage passes, but this task has no successful runtime capture from those buttons.
- `ISS-DBG-1019-003`: source-confirmed invalid host-camera fallback. A `(0,0)` target was possible only when both the waypoint and map target were missing. Both callbacks now keep the target nullable and skip camera movement when no usable point exists.
- `ISS-DBG-1019-004`: `environment` for emulator location integration. The restored blank-form test still does not receive or apply the injected fused fix on Android 16 emulator `emulator-5554`, despite enabling its mock provider and switching the form request to high accuracy. The test fails at its expected camera-center assertion; do not treat it as evidence that a real-device path passes.

## Confidence and Limits

- High confidence in the source-level causes: blank offline/restored forms were excluded, `(0,0)` could be treated as a saved camera target or synthesized host target, and the shared controller applied a fixed 10 m accuracy improvement threshold.
- Unit tests and the affected debug APKs pass after the changes. The emulator did not return a fused location to the restored blank-form test, so runtime confidence for AC-002 is low and that criterion remains `NOT VERIFIED`.
- The static source path does not prove that the current user's device actually hit any of the former branches. No successful physical-device or valid-GMS end-to-end run is available in this task.
