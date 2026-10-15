# Repository Analysis

## Task Classification

- Type: `feature`; this restores the nearby import mode and adds a third tab to the existing import flow.

## Current Behavior

- `ImportExistingWaypointBottomSheet` currently has two modes: no-query recent-saved points and XY_NUM search. The header location button is hidden.
- The activity still owns the former GPS, permission, current-location marker/camera, and candidate marker lifecycle, but opening the sheet now loads recent-saved points and does not start GPS lookup.
- Results from recent and search tabs are held separately, but switching tabs only changes the displayed rows; it does not re-query. Search runs only after the user submits XY_NUM.
- Search success currently updates its list but does not call `onCandidateWaypointsChanged`; the recent-saved request does call that map callback.
- The current repository has no import-specific UI or API tests.

## Expected Behavior

- Present recent access, nearby, and search tabs in that order with recent access selected first, preserving the sheet's existing visual style and recent-saved behavior.
- Restore the legacy GPS-based nearby flow and location button without the coordinate hint. Re-query each tab on entry, with search remaining idle until a query has been submitted.
- Route all successful tab results through the shared map marker, selection-highlight, and import flow. Preserve the legacy loading overlay and server result order.

## Affected Modules

- `app/src/main/res/layout/bottom_sheet_import_existing_waypoint.xml` — expose the location button and preserve current sheet styles.
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt` — add the nearby page, tab mapping, request lifecycle, re-entry refresh, and cross-tab candidate callbacks.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — connect tab entry/location button to the existing GPS lifecycle and route nearby/search results to shared map candidates.
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` and `GutterRepository.kt` — expose both confirmed query modes on the shared route.
- `app/src/test/java/com/example/taoyuangutter/api/GutterRepositoryNearbyTest.kt` — verify nearby coordinates and unchanged no-query recent-saved call shape.
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/ImportExistingWaypointUiTest.kt` — exercise the tab and result-selection flow using the existing ActivityScenario/Espresso setup.

## Dependencies

- Existing `ImportWaypointAdapter` and `NodeDetails` / `NodeDetailsResponse` types.
- `GutterFormActivity` host location, permission, timeout, location marker/camera, candidate marker, selected-point, and import callbacks.
- Recent-saved no-query lookup and XY_NUM lookup remain existing service/repository operations.
- Existing UI tests use ActivityScenario/Espresso, and repository tests use a dynamic `GutterApiService` proxy; no additional test dependency is needed for focused coverage.
- Same-route support for no-query recent-saved lookup and `lng`/`lat` nearby lookup, confirmed by the user; use the historical query and response contract.

## Risks

- The current 0917 API contract defines no-query `closestNodeDetails`, while the historical nearby client adds `lng` and `lat`; the user confirmed that the backend supports both modes on this route.
- Re-entry refresh must cancel stale jobs and avoid responses from an inactive tab overwriting the visible list or map candidates.
- GPS callbacks and permission results can arrive after a tab switch or sheet dismissal; lifecycle cancellation must remain correct.
- Search re-entry must use only the last submitted XY_NUM, not unsubmitted text.
- The unrelated uncommitted AGP version change in `gradle/libs.versions.toml` must remain untouched and excluded from task commits.

## Unknowns

- 無。 Nearby request shape is the historical `lng`/`lat` query on `closestNodeDetails`.
