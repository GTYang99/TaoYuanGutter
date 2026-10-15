# Independent Verification

## Revision and Worktree

- Fixed verification target: `91959ac4561b0b759797177d9908542b7e96bb73`
- Branch: `feat/feat-1015-restore-nearby-waypoints`
- At verification start, `HEAD` matched the fixed target. The only pre-existing working-tree change was `gradle/libs.versions.toml`; it is unrelated and was left untouched. No other tracked or untracked task source changes were present.
- The target commit updates task state only. Implementation and tests under review are committed through `399b37d`.
- Verification scope did not include the physical XQ-AU52 because the user explicitly declined installation and testing there.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | The sheet creates tabs `最近存取節點`, `附近點位`, `搜尋點位` in that order and initializes the recent page. The emulator UI test checks the labels, selected index 0, and the page-specific location-button visibility. Existing layout styling is retained. (`ImportExistingWaypointBottomSheet.kt`; layout XML; `ImportExistingWaypointUiTest.kt`.) |
| AC-002 | PASS | Recent Saved calls the existing no-query `getClosestNodeDetails(token)` overload on every entry. The repository test verifies the no-query Retrofit call shape; the UI test verifies the initial recent request. (`GutterRepository.kt`; `GutterApiService.kt`; `GutterRepositoryNearbyTest.kt`; `ImportExistingWaypointBottomSheet.kt`.) |
| AC-003 | NOT VERIFIED | Source review confirms Nearby requests location on entry and button tap, uses the shared endpoint with `lng`/`lat` plus Bearer authorization, hides the coordinate hint, and forwards the response list without client-side filtering or count limits. Emulator UI evidence uses a fake location callback. Physical GPS acquisition, permission/error handling, current-location marker/camera behavior, and authenticated backend results were not exercised because the user declined the physical-device run. (`GutterFormActivity.kt`; `ImportExistingWaypointBottomSheet.kt`; repository tests and emulator UI test.) |
| AC-004 | PASS | The sheet records only submitted XY_NUM input and re-runs it when Search is re-entered. Without a submitted query it retains the prompt and makes no request. The emulator UI test verifies the blank prompt/no request and that `A001` is sent again after returning to Search. (`ImportExistingWaypointBottomSheet.kt`; `ImportExistingWaypointUiTest.kt`.) |
| AC-005 | NOT VERIFIED | Emulator UI coverage verifies candidate callbacks from each mode, row radio selection, import enablement and callback handoff, and selection clearing on tab changes. A separate emulator test exercises actual `GoogleMap` marker objects: candidate coordinates/title/tag/z-index, selected marker visibility/highlight, candidate restoration after deselection, and cleanup. However, it does not visually inspect rendered markers or run the full sheet page-switch callbacks against the live map; the refresh test also does not assert that old rows remain visible below the loading overlay. Those portions of AC-005 remain unverified. (`ImportExistingWaypointUiTest.kt`; `ImportWaypointMapMarkerTest.kt`; `GutterFormActivity.kt`.) |

## Test, Build, and CI Evidence

These focused emulator commands were run against the fixed committed revision with Android Studio's bundled JDK and `emulator-5554` (Android 16):

| Check | Result | Evidence |
|---|---|---|
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportWaypointMapMarkerTest` | PASS | Gradle completed successfully. The test inspects actual GoogleMap marker state, selection highlight/visibility, deselection restoration, and marker cleanup. |
| `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` | PASS after one permitted retry | First run failed before showing the sheet at the test fixture's `ActivityScenario` RESUMED assertion (`ImportExistingWaypointUiTest.kt:140`); it did not reach feature assertions. The one retry passed. This is recorded as test initialization intermittency, not as evidence of a product behavior failure. |
| Existing developer validation: `:app:testDebugUnitTest` | PASS (recorded) | Execution report records 98 passing unit tests, including shared-route request arguments, Bearer auth, no-query recent request, and location-coordinate log redaction. |
| Existing developer validation: `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `git diff --check` | PASS (recorded) | Results are recorded in `execution-report.md`; no CI result is inferred from local runs. |
| CI for fixed revision | NOT VERIFIED | No checked-in CI configuration or CI result is available in this repository. |

## Implementation and Regression Review

- Recent Saved remains on the no-query request and Search retains the existing XY_NUM request and prompt behavior.
- Nearby uses the historical GPS-based route and response contract confirmed by the user. The activity's existing location lifecycle is connected to tab entry and the header button. The coordinate hint remains hidden.
- Successful results from the three pages route through the candidate callback. Selection routes through the selected-waypoint callback and bottom import action. Tab changes cancel the prior request, clear selection and map candidates, and stale responses are guarded by request sequence and active page.
- Refresh begins with the existing rows still bound to the adapter and uses the loading overlay. The source supports the required retention, but no test asserts the visible list during refresh, so that part of AC-005 is not treated as passed.
- The map-marker emulator test adds direct evidence about GoogleMap marker objects and marker state. It does not prove visual rendering or the integrated page-switch callback path.
- No implementation contradiction was found in the reviewed diff. The first UI-test invocation's fixture assertion failed once and passed on its single retry; retain this as a reproducibility caveat.

## Final Result

**NOT VERIFIED**

AC-003 remains unverified for physical location and backend behavior per the user's instruction. AC-005 remains unverified for integrated page-switch/map rendering and loading-row retention. CI evidence is unavailable. No unexecuted behavior is represented as PASS.

Keep `next_action: verification`: the remaining evidence can be gathered in a future verification pass when map/refresh coverage and acceptable GPS evidence are available. This pass found no implementation failure requiring Debug.
