# Repository Analysis

## Current Behavior
- `ScopeViewportLoader` reads the current Google Maps visible bounds and calls `GutterRepository.getGuttersByScope()`.
- `ScopeMapCoordinator` controls debounced and forced scope loads, guards stale responses with `latestExecutionId`, and calls `drawFeatures()` only for the latest successful response.
- `ScopeGutterPolylineController` stores rendered scope polylines in `scopePolylines`, keyed by `spiNum`.
- `ScopeGutterPolylineController.drawFeatures()` currently removes and replaces only matching `spiNum` values from the incoming response.
- `ScopeGutterPolylineController.drawFeatures()` applies `isGlobalVisible` to the inner polyline, but pending-deploy outline polylines are created without explicitly applying the stored visibility state.
- Polylines from older map ranges remain in `scopePolylines` when they are not present in the latest response.
- `MainActivity.buildScopeLoadHooks()` and `MapWorkspaceFragment.buildScopeLoadHooks()` currently clear `submittedPolylines` before drawing, but do not clear `scopeGutterPolylineController`.
- Several inspect/edit flows already call `scopeGutterPolylineController.clear()` explicitly when switching context.

## Expected Behavior
- A successful latest `scopeSearch` response should atomically replace the scope gutter layer.
- The previous scope polyline set should be removed only after the new request succeeds and passes the stale-response guard.
- Failed scope searches should keep the previous visible scope layer, preserving current map context.
- Both `MainActivity` and `MapWorkspaceFragment` should call the same controller-level replacement behavior.

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/map/ScopeGutterPolylineController.kt`
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/test/java/com/example/taoyuangutter/map/ScopeMapCoordinatorTest.kt`
- `app/src/test/java/com/example/taoyuangutter/map/ScopeGutterPolylineControllerTest.kt`

## Dependencies
- `ScopeMapCoordinator` already determines whether a response is stale before invoking drawing hooks.
- `ScopeGutterPolylineController.clear()` already removes every tracked scope polyline from the map and clears memory references.
- `ScopeGutterPolylineController.setVisible()` stores global visibility state and applies it to existing scope polylines.
- Google Maps `Polyline.remove()` is the existing cleanup mechanism used throughout the project.
- A narrow controller rendering seam can isolate Google Maps `Polyline` creation/removal for JVM unit tests while keeping the public map behavior unchanged.

## Risks
- Clearing too early would leave the map empty if the API request fails.
- Clearing from a stale response would incorrectly remove the newer visible range.
- Clearing in only one host would leave behavior inconsistent between the legacy `MainActivity` and fragment-based `MapWorkspaceFragment`.
- If replacement is implemented by host hooks only, future scope draw call sites could forget to clear first.
- Unit-testing `ScopeGutterPolylineController` directly requires a narrow renderer/handle seam so AC-002 can be verified without relying only on coordinator callback tests.
- Pending-deploy outline polylines must apply `isGlobalVisible`; otherwise hidden scope layers could reappear after replacement.

## Unknown Assumptions
- `MainActivity` is still relevant and should remain behaviorally aligned with `MapWorkspaceFragment`.
- The intended product behavior is to show only the latest successful API response, even if some older polylines are still geographically visible.
- API failure should preserve the previous rendered scope data to avoid an empty map caused by temporary network/server failure.

## Potential Issue Categories
- `implementation_regression`: Scope layer disappears on failed requests.
- `implementation_regression`: Stale response clears newer scope layer.
- `verification_failure`: The controller test seam does not faithfully represent Google Maps polyline cleanup.
- `environment`: Local Android/Gradle tooling cannot run required test tasks.
