# Independent Verification

## Revision and Worktree

- Fixed verification target: `8dfbd3e5aeae525f38f3d645d83ba9b8fb0f466c`
- Feature implementation commit: `231ba769a3bf5bece73422848fbf298119d0901c`
- Callback coverage commit: `75d1e3fe0d4ec0e87f96825b8619fb23b1bfb04d`
- `8dfbd3e` records the callback test evidence after the coverage commit; it changes task documentation only.
- Branch: `feat/feat-1015-restore-nearby-waypoints`
- Worktree at verification start: the pre-existing `gradle/libs.versions.toml` modification and this task's untracked verification report were present. No production or test source changes were uncommitted.
- The unrelated AGP version edit (`9.4.0` to `9.4.1`) remains untouched and excluded from task commits.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `ImportExistingWaypointBottomSheet.setupTabs` adds `最近存取節點`, `附近點位`, `搜尋點位` in order, selects Recent first, and initializes `Page.RECENT`. The recorded emulator UI test asserts all labels are displayed and selected position is 0. The existing bottom-sheet layout and tab style are retained. (`ImportExistingWaypointBottomSheet.kt`; `bottom_sheet_import_existing_waypoint.xml`; `ImportExistingWaypointUiTest.kt`; execution report.) |
| AC-002 | PASS | Recent Saved still uses the no-query repository overload and the same `GET /api/v1/node/closestNodeDetails` route. `GutterRepositoryNearbyTest.recentSavedLookupKeepsNoQueryCallShape` verifies the service call shape. `renderPage(Page.RECENT)` invokes `performRecentSavedSearch()` on every entry. (`GutterApiService.kt`; `GutterRepository.kt`; `ImportExistingWaypointBottomSheet.kt`; recorded unit suite.) |
| AC-003 | NOT VERIFIED | Source review confirms the Nearby tab and location button use the existing activity location/permission flow, query the shared endpoint with `lng`/`lat` and Bearer auth, hide the coordinate hint, and pass server-ordered results to the candidates callback. The emulator UI test uses a fake location callback to verify entry/button query triggers and coordinates. Physical GPS acquisition, permission/error behavior, current-location marker/camera behavior, and authenticated backend result handling remain unexecuted. The user explicitly declined installing or running the task APK on the connected physical device, so those physical checks were not attempted. (`GutterFormActivity.kt`; `ImportExistingWaypointBottomSheet.kt`; execution report.) |
| AC-004 | PASS | Search still submits XY_NUM through the existing request. The code records only submitted input and reruns it on return; when there is no submitted query it shows the existing prompt and makes no request. The recorded emulator test checks the blank prompt/no request and reruns `A001` after returning to Search. (`ImportExistingWaypointBottomSheet.kt`; `ImportExistingWaypointUiTest.kt`; execution report.) |
| AC-005 | NOT VERIFIED | The updated emulator test now verifies the three result modes invoke the common candidate callback, taps a result and checks the row radio selection and enabled import button, verifies tab changes clear the selected item, and checks the selected item reaches the import callback. This covers sheet-level callback and selection behavior. The test replaces the activity callbacks with recording fakes, so it does not demonstrate actual Google Map candidate-marker rendering, map selected-marker highlighting/camera behavior, or marker cleanup at runtime. It also does not assert retained rows beneath the loading overlay. Those map behaviors remain NOT VERIFIED as directed. (`ImportExistingWaypointUiTest.kt` lines 50–53, 115–127, 155–207; `GutterFormActivity.kt` callbacks and map marker functions; execution report.) |

## Test, Build, and CI Evidence

The task execution report records these validations for the implementation and callback-test commits included in the fixed target:

| Check | Recorded result | Verification assessment |
|---|---|---|
| `:app:testDebugUnitTest` | PASS; 98 tests, no failures | Includes nearby coordinates/Bearer authorization, unchanged recent no-query call shape, and coordinate log redaction. |
| `:app:assembleDebug` | PASS | Affected Android debug app build completed. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Updated instrumentation test sources compiled. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` | PASS; one test on `emulator-5554` (Android 16) | Updated test covers tab order/default, query triggers, callback data, radio selection, tab-switch selection clearing, and import callback handoff. It uses a fake location callback and recording callbacks rather than the live map/GPS flow. |
| `git diff --check` | PASS | Recorded clean in the execution report. |
| CI for fixed revision `8dfbd3e` | NOT VERIFIED | No CI result is attached. A tracked-file scan found no GitHub Actions, GitLab CI, Jenkins, Azure Pipelines, CircleCI, or Bitbucket Pipelines configuration. Local Gradle runs do not establish a CI result. |

## Implementation and Regression Review

- The latest commit changes only task documentation. The coverage commit changes the instrumentation test, execution report, and task records; no production code changed after implementation commit `231ba769`.
- Recent Saved keeps its no-query service call. Nearby uses the same route with `lng` and `lat`, and Search keeps the XY_NUM request.
- Each page entry triggers its required request behavior. Empty Search stays idle until submission; a submitted query is replayed on re-entry.
- Successful page responses route their result list through `onCandidateWaypointsChanged`; the activity callback maps that to candidate markers. Row selection routes through `onWaypointSelected`, and the bottom button forwards the selected item through `onImport`.
- Switching pages cancels the prior request, clears sheet selection, and asks the activity to clear import markers. Sequence/page guards prevent stale results from replacing the active page.
- The loading overlay is layered over the RecyclerView, and refresh code retains saved row data until a response replaces it. The updated test does not directly assert this refresh display behavior.
- Source review found no implementation contradiction. Runtime Google Map behavior, physical location behavior, and CI evidence remain gaps rather than demonstrated failures.

## Environment and Scope Limits

- The connected physical device is explicitly out of scope for this verification because the user declined installing/running the APK there. No device action was taken.
- The repository has no checked-in CI configuration and no CI result for the fixed revision. This verification did not create CI infrastructure or claim local checks as CI.
- The successful emulator test is retained as developer evidence; it does not replace the unapproved physical GPS run or prove Google Map marker rendering/highlighting.

## Final Result

**NOT VERIFIED**

AC-003 remains NOT VERIFIED for physical GPS, permission/error, and current-location marker/camera behavior. AC-005 remains NOT VERIFIED for actual map markers, map highlight/cleanup, and loading-row retention; sheet-level callbacks, radio state, and import callback are emulator-tested. CI status is also NOT VERIFIED. No unexecuted behavior is marked PASS.

Keep `next_action: verification`: the remaining work is to resume verification when acceptable device/map evidence and CI results become available; this pass did not identify a production-code failure or authorize creating CI infrastructure.

### Follow-up Evidence

- AC-003: If physical testing is later authorized, use a device with location permission controls and GPS. Open Nearby, exercise permission and current-location refresh, and record marker/camera, hidden coordinates, nearby rows, and relevant error outcomes.
- AC-005: Use a map-capable run to verify candidate markers for each tab, selected-map-marker highlighting, marker cleanup on tab switch, and row retention under the refresh overlay. Sheet-level radio/import callback behavior is already covered by the emulator run.
- CI: Record build and test results for a CI job targeting the fixed task revision if such a job becomes available.
