# Root Cause Analysis

## Finding

Issue: ISS-DBG-1016-1-001.

For the reported empty new/add form, the Taoyuan camera target is produced by GutterFormActivity's explicit missing-coordinate fallback. The location lookup was restricted to normal edit mode, while an empty new/add sheet reports isEditMode=false. The host location passed with the form intent is held for nearby-point import and is not used to initialize the form map.

Emulator validation exposed a second part of the runtime path: the fused callback returned a usable fix, the manual-pan guard was false, and the policy allowed applying it, but `animateCamera()` left the form map at the fallback target. Using `moveCamera()` for the explicitly eligible new/add path made the expected camera target observable; normal edit-mode animation remains unchanged.

## Evidence Chain

1. AddGutterBottomSheet.openWaypointAt() forwards a tapped row to LocationPickerHost.openWaypointForEdit() (AddGutterBottomSheet.kt:953-960).
2. MainActivity.openWaypointForEdit() passes sheet.isEditMode() to openAddForm(). AddGutterBottomSheet.isEditMode() returns true only when editSpiNum is non-empty; a new/add sheet therefore passes false (MainActivity.kt:1595-1617; AddGutterBottomSheet.kt:1212-1219).
3. MainActivity's missing-coordinate fallback is used to position the shared host map. The form intent is built separately from currentWaypoints. GutterFormNavigator.buildAddIntent() places absent lat/lng values into the legacy arrays as 0.0, preserves nullable coordinates in the session JSON, and attaches hostLastLocation as separate extras (GutterFormNavigator.kt:65-104, 107-116).
4. GutterFormActivity.onCreate() stores the host location in hostLastLatLng, while currentLat/currentLng are initialized from the intent coordinate arrays (GutterFormActivity.kt:1255-1262, 1295-1305). The session snapshot keeps the current waypoint coordinates absent.
5. GutterFormActivity.onMapReady() uses saved waypoint coordinates when present. Otherwise, zero currentLat/currentLng select LatLng(24.9929, 121.3011), the fixed Taoyuan-area target. The call to beginEditMapLocationPermissionFlow() is conditional on EditMapLocationPolicy.shouldLocateForInitialCamera() (GutterFormActivity.kt:1645-1687).
6. EditMapLocationPolicy.shouldLocateForInitialCamera() requires isEditMode=true, as well as an online, writable, non-view form with no saved coordinates (EditMapLocationPolicy.kt:5-12). Since the new/add flow supplies false, no initial current-location request runs.
7. The separately passed hostLastLatLng is consumed by the nearby-point import path, not onMapReady(). The unit test also asserts that the initial-camera policy returns false when isEditMode=false (GutterFormActivity.kt:1255-1262, 878-889; EditMapLocationPolicyTest.kt:7-15).
8. After the explicit new/add eligibility flag was added, emulator instrumentation observed one location attempt with `userMoved=false` and the apply policy true while the camera remained at the fallback. With the same emulator GPS fix, switching the update to `moveCamera()` made the new/add camera and camera-only data assertions pass (execution-report.md).

## Conclusion

The symptom follows from two gaps in the initial-camera path: the policy did not include a fresh new/add form, and the existing animated camera update did not reliably apply the acquired fix to the form map. The host's last-known location does not bridge the first gap because it is not consulted by initial map setup.

The earlier DBG-1007 requirement explicitly excluded new-point and draft flows (FR-006 / AC-008). The current request expands the expected behavior for an empty new/add form. The source therefore explains the behavior under the earlier scope; the new behavior belongs to the current task's planning and acceptance criteria.

## Affected Code

- AddGutterBottomSheet.kt:953-960, 1212-1219
- MainActivity.kt:1595-1617, 1651-1683
- GutterFormNavigator.kt:65-116
- GutterFormActivity.kt:1255-1262, 1295-1305, 1645-1687
- EditMapLocationPolicy.kt:5-12
- EditMapLocationPolicyTest.kt:7-15

## Classification and Confidence

- Issue category: enhancement_request / scope change; the new/add exclusion was explicit in the earlier DBG-1007 criteria.
- Root-cause confidence: high. Static source tracing and the focused emulator result demonstrate both the mode gate and the camera-update behavior.
- Runtime validation: the new/add centering, camera-only coordinate handling, unmarked fallback, and saved-coordinate precedence passed on emulator-5554. The reported physical device was not tested, per the user's emulator-only constraint.
- The implementation adds an explicit fresh-add flag and uses a direct camera move for that path after a valid location callback; edit-mode animation and the existing permission, retry, manual-pan, unavailable-prompt, and fallback logic remain in place.
