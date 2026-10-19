# Issue Log — debug-1019-map-user-location

## ISS-DBG-1019-001 — Blank restored/offline form maps do not attempt current location

- Task: `debug-1019-map-user-location`
- Phase found: investigation
- Category: `requirement_gap` (scope expansion)
- Priority: P2
- Status: resolved_pending_validation
- Impact: A form map with no saved waypoint coordinate can remain at the Taoyuan fallback in offline/restored-draft flows.
- Expected: Keep saved waypoint coordinates centered; when the map has no point coordinate, attempt to center on the user's current location.
- Actual: `EditMapLocationPolicy` excludes offline forms; restored drafts do not receive the fresh-add location flag.
- Evidence:
  - `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt:1672-1696`
  - `app/src/main/java/com/example/taoyuangutter/gutter/EditMapLocationPolicy.kt:5-12`
  - `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1218-1224`
- Resolution: editable blank forms now qualify for a camera-only location attempt when no usable waypoint coordinate exists; valid saved coordinates keep precedence.
- Next action: verification of AC-002 on a functioning location provider

## ISS-DBG-1019-002 — Shared locate action may retain a cached map center

- Task: `debug-1019-map-user-location`
- Phase found: investigation
- Category: `unknown`
- Priority: P2
- Status: resolved_pending_validation
- Impact: The host map or point-picker locate button may appear not to move to a newer device position when the accepted cache is already accurate.
- Expected: An explicit locate action moves the viewport to the current usable device location without changing a saved waypoint coordinate.
- Actual: `MyLocationController` can keep an accepted cached fix when a newer result improves horizontal accuracy by less than 10 m, regardless of geographic displacement.
- Evidence:
  - `app/src/main/java/com/example/taoyuangutter/map/MyLocationController.kt:37-54`
  - `app/src/main/java/com/example/taoyuangutter/map/LocationFixQualityPolicy.kt:23-35`
  - `app/src/test/java/com/example/taoyuangutter/map/LocationFixQualityPolicyTest.kt`
- Limitation: No current-task runtime capture shows this branch on the host map or point-picker FAB.
- Resolution: explicit host and picker location actions use fresh-preferred refinement and retain a usable cache as fallback; policy tests pass.
- Limitation: no successful runtime capture from either explicit locate action is available.
- Next action: verification of AC-003 on a functioning location provider

## ISS-DBG-1019-003 — Host form handoff substitutes `(0,0)` when camera target is unavailable

- Task: `debug-1019-map-user-location`
- Phase found: investigation
- Category: `requirement_gap`
- Priority: P2
- Status: resolved_pending_validation
- Impact: The host camera helper receives an invalid geographic target when the waypoint and host map target are both absent.
- Expected: A missing map target remains absent; the form's blank-map location flow or existing fallback handles viewport initialization.
- Actual: Both host callbacks use `LatLng(0,0)` as the final fallback. The controller currently no-ops when the map provider is null, so a visible jump is not established.
- Evidence:
  - `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:768`
  - `app/src/main/java/com/example/taoyuangutter/MainActivity.kt:1605`
- Resolution: host form callbacks pass only a valid waypoint or camera target and do not synthesize `(0,0)`.
- Next action: verification of AC-005

## ISS-DBG-1019-004 — Emulator did not deliver the injected fused location

- Task: `debug-1019-map-user-location`
- Phase found: implementation validation
- Category: `environment`
- Priority: P2
- Status: open
- Impact: The emulator cannot provide end-to-end evidence for blank restored/offline form centering.
- Expected: The mock fused fix centers the blank form map while leaving form and waypoint coordinates unchanged.
- Actual: `restoredBlankFormWithoutFreshAddFlagUsesLocationCameraOnly` fails at the camera-center assertion in `EditMapInitialLocationInstrumentedTest.kt:336` after the high-accuracy request change.
- Evidence: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 16.xml`; prior device diagnostics recorded the mock fix in the Android location service, but GMS location calls did not produce a callback. Emulator Google Maps/GMS setup is not valid for this integration check.
- Next action: verification on an emulator with working Google Play Services location or a physical Android device
