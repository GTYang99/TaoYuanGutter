# Implementation Plan

## Goal

- Restore the GPS-based nearby import mode and add it between the existing recent-saved and XY_NUM search tabs while preserving their behavior.

## Scope

- Update the existing import bottom sheet and its activity/API integration only as needed for the three-tab behavior.
- Preserve the recent-saved feature, search contract, visual style, GPS permission flow, map integration, and import action.
- Use the confirmed shared `closestNodeDetails` route: no query for recent-saved points and historical `lng`/`lat` query for nearby points.

## Affected Files

- `app/src/main/res/layout/bottom_sheet_import_existing_waypoint.xml` — reveal current-location button; keep existing style.
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt` — three tab state, refresh on entry, search prompt/last-query handling, loading/results routing.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — wire nearby tab entry and location button into the existing GPS lifecycle; feed result lists into shared map callbacks.
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` and `GutterRepository.kt` — restore the legacy `lng`/`lat` request while preserving the existing no-query method.
- `app/src/test/java/com/example/taoyuangutter/api/GutterRepositoryNearbyTest.kt` — verify nearby coordinates and unchanged no-query recent-saved lookup.
- `app/src/androidTest/java/com/example/taoyuangutter/gutter/ImportExistingWaypointUiTest.kt` — verify tab, search, refresh, and import interactions using existing ActivityScenario/Espresso dependencies.

## Implementation Steps

1. Restore the historical nearby request (`GET /api/v1/node/closestNodeDetails?lng=...&lat=...`, Bearer authorization, `NodeDetailsResponse`) alongside the existing no-query recent-saved request.
2. Expand the tab model and tab callbacks to recent saved, nearby, and search in the approved order, defaulting to recent saved.
3. Re-query on tab entry: recent saved via its unchanged no-query operation, nearby via the legacy GPS/location-button lifecycle, and search via the last submitted XY_NUM only.
4. Route successful results from all three modes through the common candidate-map callback; preserve selection clearing, map highlighting, import action, and legacy loading overlay behavior.
5. Add focused repository and instrumentation tests for tab order/default, request-on-entry, blank and submitted search behavior, loading/result replacement, map candidate callbacks, and import selection.
6. Run the planned unit/UI tests and affected Android build checks; record any unavailable device/API checks as `NOT VERIFIED`.

## Test Plan

- Add focused tests for three-tab order/default and per-tab request behavior.
- Verify a blank search shows the existing prompt with no request, while a submitted XY_NUM request re-runs when returning to Search.
- Verify Nearby uses a GPS-derived `lng`/`lat` request and Recent Saved remains a no-query request on the shared route.
- Verify loading retains rows beneath the overlay, success replaces rows in server order, empty/error states are preserved, and all modes share map candidate and import callbacks.
- Planned commands after implementation: `./gradlew :app:testDebugUnitTest`, `./gradlew :app:assembleDebug`, and `./gradlew :app:compileDebugAndroidTestKotlin`; run focused connected tests if a suitable device is available.

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android device with location permission controls, GPS, and authenticated API access; exact device to be recorded at verification.
- In-scope Acceptance Criteria: AC-001, AC-003, AC-005
- Regression risk: GPS permission/result lifecycle, map candidate markers, selected marker, and sheet import handoff.
- Full regression required: No
- Full regression trigger: An approved plan change, release risk, or failure evidence; otherwise `無`.
- Stop condition: Complete the listed cases or obtain sufficient failure evidence.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Physical device | Open import sheet and inspect tabs | Existing visual style; three labels in order; Recent Access selected | Device observation; screenshot if visual mismatch |
| AC-003 | Physical device | Open Nearby, allow location, then tap location button | Legacy GPS behavior and marker/camera restored; no coordinate text; nearby rows update | API request evidence and observed map/list behavior |
| AC-005 | Physical device | Select a result in each tab and import it | Candidate markers appear, selected point highlights, and import uses the selected `NodeDetails` | Observed map and form handoff |

## Regression Plan

- Confirm recent-saved no-query results still load, select, and import unchanged.
- Confirm XY_NUM search, blank prompt, loading, empty/error states, and import selection remain functional.
- Confirm switching tabs cancels old requests, clears prior map selection/candidates, and does not allow stale responses to overwrite the active tab.
- Confirm sheet dismissal stops GPS callbacks and clears temporary markers.
- Confirm existing offline-mode guard and authentication-expiry draft handling remain unchanged.

## Risks

- Nearby and recent-saved modes share the endpoint with distinct query modes as confirmed by the user; verify each request remains correctly parameterized.
- GPS permission, stale cached location, high-accuracy refinement, and timeout callbacks can race tab changes or sheet dismissal.
- Search text may be present without a submitted query; only submitted text may trigger re-query.
- Existing unrelated `gradle/libs.versions.toml` change must be preserved and excluded from commits.

## Rollback Plan

- Revert only the task commit(s) on the feature branch, preserving the pre-existing `gradle/libs.versions.toml` modification.

## Current Behavior

- The sheet exposes only recent-saved points and XY_NUM search; it hides the location button and does not enter the GPS query flow.
- Returning to a tab re-displays its stored rows without re-querying.

## Expected Behavior

- The sheet exposes three tabs in the approved order, re-queries on return, and restores nearby GPS and common map/import behavior without changing recent-saved or search semantics.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Step 2 | Tab/UI test; physical sheet inspection |
| AC-002 | Step 3 | Repository request test and recent-saved regression test |
| AC-003 | Steps 1, 3, 4 | Request test after contract resolution; GPS/location flow test; device check |
| AC-004 | Step 3 | Search state/request test for empty and submitted XY_NUM |
| AC-005 | Steps 4, 5 | Callback/instrumentation coverage and physical selection/import flow |

## Failure Behavior

- Preserve existing loading, empty, error-row, toast, auth-expiry, GPS permission retry/settings, and location timeout behavior. Nearby uses the confirmed historical request/response contract; do not add a new API error contract.

## Security and Privacy

- Use the existing location permission and authenticated repository paths; do not restore historical request-URL/error logging that exposes GPS coordinates, and do not log bearer tokens.

## Open Questions

- 無。
