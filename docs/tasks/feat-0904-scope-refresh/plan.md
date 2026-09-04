# Implementation Plan

## Goal
- Replace old main-map scope gutter polylines with the latest successful `scopeSearch` result to reduce memory and rendering pressure.

## Scope
- Update scope gutter polyline lifecycle management for main-map `scopeSearch` results.
- Keep failed requests preserving the previous scope layer.
- Keep both `MainActivity` and `MapWorkspaceFragment` aligned.

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/map/ScopeGutterPolylineController.kt` - Add or expose a replacement entry point that clears existing scope polylines and draws the latest response as one operation.
- `app/src/main/java/com/example/taoyuangutter/map/ScopeGutterPolylineController.kt` - Add a narrow internal renderer/handle seam if needed so controller replacement and removal can be unit-tested without real Google Maps objects.
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt` - Ensure replacement is called only after success and stale-response validation.
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` - Use replacement behavior for legacy main-map scope drawing.
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` - Use replacement behavior for fragment main-map scope drawing.
- `app/src/test/java/com/example/taoyuangutter/map/ScopeMapCoordinatorTest.kt` - Add or update tests for success-only replacement callback behavior, failed-request preservation, and stale-response protection.
- `app/src/test/java/com/example/taoyuangutter/map/ScopeGutterPolylineControllerTest.kt` - Add controller tests for latest-response retention, old polyline removal, and hidden scope-layer replacement including pending-deploy outlines.

## Implementation Steps
- Add a controller method such as `replaceFeatures(features, savedGroupId)` to `ScopeGutterPolylineController`.
- Implement `replaceFeatures()` by calling `clear()` and then `drawFeatures(features, savedGroupId)` after the latest API response succeeds.
- Ensure every polyline created by `drawFeatures()` applies the stored scope visibility state, including pending-deploy outline polylines.
- Keep `drawFeatures()` available for any existing call site that intentionally wants incremental drawing.
- Update `MainActivity` scope coordinator wiring to call `scopeGutterPolylineController.replaceFeatures(features, savedGroupId)` instead of `drawFeatures()`.
- Update `MapWorkspaceFragment` scope coordinator wiring to call the same replacement method.
- Keep `buildScopeLoadHooks().onBeforeDraw` limited to existing `submittedPolylines` cleanup unless implementation evidence shows it should also move into the controller method.
- Introduce a narrow internal renderer/handle seam for `ScopeGutterPolylineController` if direct Google Maps `Polyline` assertions are not practical in JVM tests.
- Add `ScopeGutterPolylineControllerTest` with a fake renderer: draw response A, then replace with response B using different `spiNum` values, assert response A handles were removed and `entries()` contains only response B.
- Add `ScopeGutterPolylineControllerTest` coverage where `setVisible(false)` is called before replacement and both inner and pending-deploy outline handles are created hidden.
- Add `ScopeMapCoordinatorTest` coverage proving an API error does not invoke replacement and therefore keeps the previous scope layer.
- Add `ScopeMapCoordinatorTest` coverage proving a stale response does not invoke replacement after a newer request has completed.

## Test Plan
- Run focused map unit tests, including `ScopeMapCoordinatorTest`.
- Run new `ScopeGutterPolylineControllerTest` using the controller renderer seam to verify real controller retention/removal behavior for AC-002.
- Add coordinator test coverage using fake replacement callbacks to assert the replacement callback is invoked only for the latest successful response.
- Verify hidden-before-replacement behavior by asserting fake inner and outline polyline handles are both hidden when `setVisible(false)` was applied before `replaceFeatures()`.
- Verify existing tests covering loading state, auth-expired routing, and stale response behavior still pass.
- Run the project unit test task used by the repo for debug unit tests.

## Regression Plan
- Confirm scope layer visibility toggle still affects newly drawn polylines after replacement.
- Confirm pending-deploy outline polylines also respect the scope layer visibility toggle after replacement.
- Confirm failed `scopeSearch` does not clear currently displayed scope lines.
- Confirm stale/out-of-order responses do not remove the latest scope layer.
- Confirm inspect/edit flows that explicitly clear scope polylines still clear them.
- Confirm no-ditch, measurement, working markers, submitted polylines, and auth-expired map behavior are unchanged.

## Risks
- The internal renderer/handle seam must stay narrow and must not change the map rendering contract.
- Clearing in the wrong lifecycle hook could remove data on failures or stale responses.
- Keeping both `drawFeatures()` and `replaceFeatures()` may require clear naming to avoid future accidental incremental use.

## Rollback Plan
- Revert the scope controller replacement method and restore both hosts to incremental `drawFeatures()` behavior.

## Current Behavior 
- Each successful scope response draws incoming features and replaces only duplicate `spiNum` polylines.
- Scope polylines from older map ranges remain in memory when absent from the latest response.
- Failed scope requests already do not draw new features, but there is no explicit latest-response replacement contract.

## Expected Behavior
- Each latest successful scope response removes the previous scope polyline set and draws only the latest response.
- Failed scope responses keep the previously visible scope layer.
- Stale responses remain ignored and cannot clear or redraw the scope layer.

## Open Questions
- 無
