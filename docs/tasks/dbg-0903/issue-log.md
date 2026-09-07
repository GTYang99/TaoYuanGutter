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

## ISS-DBG-0903-008

```yaml
issue_id: ISS-DBG-0903-008
task_id: DBG-0903
phase: debug
category: implementation_regression
priority: P1
title: Main-map gutter inspection remains clickable after the plan investigation layer is disabled
status: implemented_pending_device_verification
impact: Users can still open gutter inspection from the main map even after disabling the layer that visually represents the current plan investigation/gutter layer.
repro_steps:
  - Open the main map.
  - Open the layer sheet.
  - Disable the current plan investigation/gutter layer.
  - Tap a previously loaded gutter line location on the map.
expected: When the plan/gutter layer is disabled, its visual geometry and inspection hit target should both be disabled.
actual: The visible layer can be disabled, but tapping the map can still trigger gutter inspection.
evidence:
  - Reported after multi-device testing.
  - MapWorkspaceFragment.setOnPolylineClickListener() always calls openInspectBottomSheet(polyline) unless measure mode is active.
  - onOverlayTogglesChanged() only calls scopeGutterPolylineController.setVisible(showPlan) for scope polylines.
error_source:
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:557
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:559
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:680
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:689
  - app/src/main/java/com/example/taoyuangutter/map/ScopeGutterPolylineController.kt:93
  - app/src/main/java/com/example/taoyuangutter/map/ScopeGutterPolylineController.kt:182
root_cause:
  - The overlay toggle changes visual visibility through ScopeGutterPolylineController.setVisible(showPlan), but it does not disable the polyline click route.
  - ScopeGutterPolylineController.drawFeatures() creates inner polylines with clickable=true and setVisible(false) later only updates isVisible.
  - MapWorkspaceFragment.onPolylineClick does not check mapOverlayController.currentState().showPlan before opening inspection.
  - Therefore stale clickable polyline objects remain eligible for inspection routing even when the product state says the layer is off.
debug_analysis:
  - The code separates visibility from interaction.
  - Visual state is controlled by the layer sheet toggle.
  - Interaction state is controlled by GoogleMap's global polyline click listener and each polyline's clickable flag.
  - The two states are not synchronized when the layer is disabled.
  - The naming also increases confusion: UI label is "本次計畫調查", while code paths include both showPlan for scope polylines and showPossible for roadServey WMS. The inspectable gutter polyline path currently follows showPlan, not showPossible.
fix_direction:
  - Gate MapWorkspaceFragment.onPolylineClick with the same overlay state that controls inspectable gutter polyline visibility.
  - Prefer also updating ScopeGutterPolylineController.setVisible() to keep clickable aligned with visible if its handle abstraction is extended to support clickable.
  - Clarify whether the product meaning of "本次計畫調查圖層" maps to showPlan scope polylines or showPossible roadServey WMS before changing labels or broader behavior.
implementation:
  - Added an overlay-state gate to MapWorkspaceFragment polyline click handling so hidden plan/scope gutter layers cannot open inspection.
  - Added the same gate to legacy MainActivity for retained entry points.
  - Extended ScopeGutterPolylineController handles with clickable state and synchronized clickable=false when the layer is hidden.
  - Preserved each polyline's original interaction setting when visibility is restored.
validation:
  - git diff --check passed.
  - ScopeGutterPolylineControllerTest passed for hidden-state click disabling and restore behavior.
  - Full testDebugUnitTest passed.
  - assembleDebug passed.
  - Emulator install and launch passed.
  - Real-device manual layer-off tap verification is still required.
next_action: verification
owner: developer
```

## ISS-DBG-0903-009

```yaml
issue_id: ISS-DBG-0903-009
task_id: DBG-0903
phase: debug
category: implementation_regression
priority: P2
title: No-ditch WMS point layer has a smaller practical tap target than users expect
status: implemented_pending_device_verification
impact: Users see no-ditch points on the WMS layer but cannot reliably tap them to open the point note.
repro_steps:
  - Open the main map.
  - Enable the no-ditch point layer.
  - Tap near a visible no-ditch point on the WMS overlay.
expected: Tapping the visible no-ditch point should reliably open its note within a practical touch tolerance.
actual: The hit area is too small, so users often cannot select the point.
evidence:
  - Reported during real-device operation.
  - The app displays no-ditch points as a WMS tile overlay and separately loads WFS markers for interaction.
  - Map click fallback calls WMS GetFeatureInfo with a single exact screen pixel I/J and no click buffer.
error_source:
  - app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt:146
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:535
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:540
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1360
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1407
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1432
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:1477
root_cause:
  - The visible no-ditch layer is a WMS TileOverlay, which is not itself clickable in Google Maps.
  - Interaction is implemented separately through WFS-loaded markers and a WMS GetFeatureInfo fallback.
  - The marker icon is rendered at 20dp and uses the default marker hit behavior, while the map-click fallback queries exactly one screen coordinate without expanding the search area.
  - Because visual WMS pixels, WFS marker positions, and exact GetFeatureInfo I/J hit testing are not unified, the practical tap target is smaller than the visible point symbol users see.
debug_analysis:
  - Enabling showNoDitchPoints creates the WMS overlay and triggers loadNoDitchPointsForVisibleArea().
  - loadNoDitchPointsForVisibleArea() uses the current visible BBOX to create GoogleMap markers from WFS results, but only points returned by the visible BBOX become marker hit targets.
  - handleMainMapTap() calls fetchAndShowNoDitchPointNoteAt() when no-ditch layer is enabled and report-pick mode is not active.
  - fetchAndShowNoDitchPointNoteAt() computes I/J from projection.toScreenLocation(targetLatLng) and sends that single pixel to GetFeatureInfo.
  - No radius, nearby-marker search, expanded BBOX, or multi-pixel sampling is used.
  - On high-density screens, small WMS symbols, fractional zoom, or slight WMS/WFS coordinate differences make a normal finger tap miss the exact feature pixel.
fix_direction:
  - Add a practical hit tolerance for no-ditch taps, preferably by first searching loaded WFS markers/points near the tapped LatLng within a zoom-aware meter radius.
  - If no nearby loaded point is found, query GetFeatureInfo with a small pixel grid or server-supported buffer if available.
  - Increase the interactive marker icon/hit target independently from the WMS visual symbol if necessary.
  - Keep WMS visibility and WFS marker lifecycle synchronized when showNoDitchPoints is toggled.
implementation:
  - Added NoDitchPointHitTester to search loaded WFS points near the tapped LatLng before falling back to WMS GetFeatureInfo.
  - Added zoom-aware tap radii so high zoom remains precise and lower zoom keeps a practical finger target.
  - Kept WMS GetFeatureInfo fallback for WMS-visible points that are not present in the currently loaded WFS marker set.
  - Added no-ditch marker click guards so stale markers cannot show notes after the layer is disabled.
validation:
  - git diff --check passed.
  - NoDitchPointHitTesterTest passed for nearest-point hit and outside-radius miss behavior.
  - Full testDebugUnitTest passed.
  - assembleDebug passed.
  - Emulator install and launch passed.
  - Real-device manual WMS-nearby tap verification is still required.
next_action: verification
owner: developer
```
