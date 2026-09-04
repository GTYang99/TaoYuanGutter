# Issue Log

Task: DBG-0903
Date: 2026-09-04

## ISS-DBG-0903-005

```yaml
issue_id: ISS-DBG-0903-005
task_id: DBG-0903
phase: debug
category: implementation_regression
priority: P1
title: Samsung foldable devices may calculate an incorrect camera fit for long gutter routes
status: implemented_pending_device_verification
impact: Inspecting a long gutter route on Samsung foldable devices may not keep the whole route visible above the inspect or edit form.
repro_steps:
  - Open the app on a Samsung foldable phone.
  - Log in and open the main map.
  - Select a long gutter route for inspection.
  - Observe the automatic map fit while the inspect or edit form is visible.
expected: The full long gutter route should remain visible in the usable map viewport above the form.
actual: The automatic zoom/fit calculation may crop or hide part of the long route.
evidence:
  - Reported during Samsung foldable device operation.
  - Google Maps logo moves upward to around the top 1/3 area during zoom/fit.
suspected_area:
  - MapWorkspaceFragment inspect/edit route fit
  - MapCameraController screen-height based bottom offset calculation
  - Foldable window metrics, split/expanded display dimensions, and bottom sheet inset timing
current_assessment:
  - The logo movement strongly indicates excessive GoogleMap bottom padding.
  - AddGutterBottomSheet sets the sheet height/peekHeight to resources.displayMetrics.heightPixels / 2.
  - MapCameraController.fitCameraToWaypointsWithViewportFraction(1/3) maps to bottomOffsetRatio = 2/3 and adds screenHeight * 2/3 on top of persistentBottomInsetPx.
  - When currentSheetBottomInsetPx already represents the visible sheet height, the fit path may effectively reserve the sheet height plus another large screen-based offset.
  - Foldable devices can amplify this because displayMetrics.heightPixels may not match the actual map container height after folding, taskbar/nav bar, split layout, or bottom sheet measurement.
probable_root_cause:
  - Primary: height calculation double-counts reserved bottom space by combining persistent sheet inset with a second screen-height based bottom offset.
  - Secondary: bottom sheet expansion order can still contribute because initial fit may run before the actual sheet height is known, then inset callback triggers another fit with the same oversized padding formula.
debug_analysis:
  - AddGutterBottomSheet.setupBottomSheetBehavior() sets sheet height and peekHeight from resources.displayMetrics.heightPixels / 2.
  - AddGutterBottomSheet.notifySheetViewportInset() reports the actual visible sheet height to the host.
  - MapWorkspaceFragment.onSheetViewportInsetChanged() stores that value as currentSheetBottomInsetPx and applies it as persistent map bottom padding.
  - MapWorkspaceFragment.fitInspectRouteAboveSheet() then requests viewportHeightFraction = 1/3 with resetPaddingAfter = false.
  - MapCameraController.fitCameraToWaypointsWithViewportFraction() converts 1/3 visible viewport into bottomOffsetRatio = 2/3.
  - MapCameraController.fitCameraToWaypoints() applies persistentBottomInsetPx + screenHeight * bottomOffsetRatio as GoogleMap bottom padding.
  - Because GoogleMap UI controls, including the Google logo, obey map padding, the logo moving to around the top 1/3 is direct evidence that the calculated bottom padding is too large.
  - On Samsung foldable devices, displayMetrics.heightPixels can represent the physical/window display differently from the actual map container after fold posture, navigation/taskbar, or multi-window adjustments, so the screenHeight-based offset can be even less reliable.
error_source:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:583
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:664
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:755
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:762
  - app/src/main/java/com/example/taoyuangutter/map/MapCameraController.kt:61
  - app/src/main/java/com/example/taoyuangutter/map/MapCameraController.kt:68
  - app/src/main/java/com/example/taoyuangutter/map/MapCameraController.kt:100
fix_direction:
  - Stop mixing persistent sheet inset with an additional screenHeight-ratio offset for inspect/edit route fitting.
  - Base inspect route fitting on the actual map view height and actual sheet inset, or pass an explicit bottom padding rather than a viewport fraction converted from full displayMetrics height.
  - Refit only after the bottom sheet has reported its measured inset, and avoid repeated refits while the sheet is still animating unless the inset changed meaningfully.
implementation:
  - Added MapCameraController.fitCameraToWaypointsWithBottomPadding() so inspect/edit fit can use an explicit bottom padding instead of deriving a second offset from displayMetrics height.
  - Updated MapWorkspaceFragment and legacy MainActivity inspect route helpers to pass actual sheet inset when available.
  - Updated fallback sizing to use the current root view height instead of full displayMetrics height for foldable/windowed layouts.
validation:
  - compileDebugKotlin, testDebugUnitTest, and assembleDebug passed.
  - QV710EDR3A install and launch passed.
  - Samsung foldable visual verification is still required.
next_action: debug
owner: developer
```

## ISS-DBG-0903-006

```yaml
issue_id: ISS-DBG-0903-006
task_id: DBG-0903
phase: verification
category: unknown
priority: P2
title: Some WMS base layers fail to render at zoom 18.3
status: closed
impact: At map zoom 18.3, some WMS base map layers may disappear or fail to display.
repro_steps:
  - Open the app and switch to a WMS base layer.
  - Zoom the map to approximately 18.3.
  - Compare layer rendering across available WMS base layers.
expected: WMS base layers should continue rendering tiles at zoom 18.3 if the layer supports that scale.
actual: Some WMS layers do not display at zoom 18.3.
evidence:
  - Reported during app operation at zoom 18.3.
suspected_area:
  - WMS tile URL zoom parameter handling
  - Google Maps fractional zoom to WMS tile matrix conversion
  - Per-layer min/max zoom or tile matrix availability
  - Layer-specific server response behavior at fractional zoom
next_action: investigation
owner: developer
close_reason: Deferred because tile stitching/rendering issues also occur on other platforms and should be tracked separately from DBG-0903 Samsung foldable camera-fit investigation.
```

## ISS-DBG-0903-007

```yaml
issue_id: ISS-DBG-0903-007
task_id: DBG-0903
phase: debug
category: implementation_regression
priority: P1
title: Main map remains constrained to inspect viewport after returning from gutter inspection
status: implemented_pending_device_verification
impact: After leaving gutter inspection, the main map still behaves as if only the upper 1/3 viewport is usable, so users cannot pan and inspect gutters across the full screen.
repro_steps:
  - Open the app on a real device.
  - Inspect an existing gutter route.
  - Return from the inspect screen to the main map without staying in edit mode.
  - Try to pan the main map and observe the usable map area.
expected: Returning to the main map should restore GoogleMap padding to the normal full-screen main-map state.
actual: The map remains constrained to the previous inspect/form viewport and behaves like the visible area is still around 1/3 height.
evidence:
  - Reported across multiple real devices after the ISS-DBG-0903-005 fix.
  - The symptom matches leftover GoogleMap padding from inspect route fitting.
error_source:
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:762
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:767
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:288
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:294
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:302
  - app/src/main/java/com/example/taoyuangutter/map/MapCameraController.kt:109
  - app/src/main/java/com/example/taoyuangutter/map/MapCameraController.kt:119
root_cause:
  - The ISS-DBG-0903-005 fix intentionally changed inspect route fitting to use explicit GoogleMap bottom padding with resetPaddingAfter = false.
  - That is correct while the inspect Activity or edit bottom sheet is visible, but the non-edit inspect return path does not reset map padding before restoring the main map.
  - Because fitCameraToWaypointsWithBottomPadding() leaves the explicit padding active when resetPaddingAfter = false, the main map keeps the inspect viewport padding after the overlay is gone.
debug_analysis:
  - MapWorkspaceFragment.fitInspectRouteAboveSheet() calls fitCameraToWaypointsWithBottomPadding(..., resetPaddingAfter = false).
  - MapCameraController.fitCameraToWaypointsWithBottomPadding() applies explicit GoogleMap padding and only restores persistentBottomInsetPx when resetPaddingAfter is true.
  - In MapWorkspaceFragment.inspectLauncher, the non-edit return branch clears edit/inspect flags, reference route, preview layer, and markers, then calls loadGuttersByViewport(showFeedback = true).
  - That branch does not call setPersistentBottomInset(0), does not reset currentSheetBottomInsetPx, and does not otherwise clear GoogleMap padding.
  - Therefore the following main-map viewport query and user pan/zoom operate under stale inspect padding.
  - The same pattern should be audited in legacy MainActivity because it has a similar inspect return branch.
fix_direction:
  - Add a centralized main-map viewport reset helper that clears currentSheetBottomInsetPx, lastInspectRouteRefitInsetPx, indicator inset, and GoogleMap padding.
  - Call that helper on every terminal path that leaves inspect/edit/form overlay and returns to the normal main map.
  - Keep explicit inspect padding only while inspect Activity or edit bottom sheet is still visible.
  - Audit launch failure and inspect API error paths because they also leave inspect flow early after explicit padding may already have been applied.
implementation:
  - Added restoreMainMapViewport() to MapWorkspaceFragment to clear currentSheetBottomInsetPx, lastInspectRouteRefitInsetPx, the main-map indicator inset, and GoogleMap padding together.
  - Added restoreMainMapViewport() to legacy MainActivity for the same viewport reset contract.
  - Called the reset helper from non-edit inspect return, inspect launch failure, inspect API error, edit sheet close, update reopen failure, and update reload error paths.
validation:
  - compileDebugKotlin, testDebugUnitTest, and assembleDebug passed.
  - git diff --check passed.
  - QV710EDR3A install and launch passed.
  - Manual visual verification is still required after logging in and returning from inspection to the main map.
next_action: debug
owner: developer
```
