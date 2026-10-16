# Independent Verification

## Revision and Worktree

- Fixed revision under test for this round: `c7fa5f71873168187ce0276a28a4aab46f31db28` (`feat/feat-1015-restore-nearby-waypoints`). It is documentation-only on top of source revision `f5e31eff808c9aa3d04df23927bd660978d93931`; the app and test sources are unchanged.
- Verification used a clean task worktree at the fixed revision. No production or test source was changed during this round. The ignored `local.properties` contained only the repository CI placeholder `MAPS_API_KEY=ci-placeholder`.
- Device scope was emulator-only, as directed. Tests ran only on `emulator-5554` (Medium_Phone, Android 16). The connected XQ-AU52 physical device was not used.
- The source revision includes the FEAT-1006 integration and the DBG-1016 nearby fresh-location fix; the prior verification at `91959ac` does not cover it.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Source review confirms the sheet creates `最近存取節點`, `附近點位`, `搜尋點位` in that order and selects index 0 (`ImportExistingWaypointBottomSheet.kt:315-328`). Both the prior run and this round's UI run passed the visible-label, selected-index, and initial recent-request assertions (`ImportExistingWaypointUiTest.kt:145-154`) before failing at candidate-row click. |
| AC-002 | PASS | `GutterRepositoryNearbyTest` passed 3/3 tests, including `recentSavedLookupKeepsNoQueryCallShape` and the nearby coordinate/Bearer request test. The sheet calls the no-query repository overload on every Recent entry (`ImportExistingWaypointBottomSheet.kt:352-359, 462-487`); the UI runs reached the initial recent-request count assertion. |
| AC-003 | NOT VERIFIED | This round's UI test failed before reaching Nearby. The earlier emulator attempt passed its fake-location checks for Nearby entry, location-button refresh, candidate response, and coordinates (`ImportExistingWaypointUiTest.kt:161-176`). Source review confirms the fresh-fix policy, marker/camera update, and query (`GutterFormActivity.kt:914-961`); the `NearbyLocationFixPolicy` unit tests are included in the passing JVM suite. Physical GPS acquisition, permission/error handling, the live marker/camera behavior, and authenticated backend results were not exercised. |
| AC-004 | FAIL | On the earlier `Medium_Phone` Android 16 attempt, after `typeText("A001")` and Search submit, the fake API recorded `A0`; the assertion expected `A001` (`ImportExistingWaypointUiTest.kt:187-188`). In this round, the UI test failed earlier at line 155 when Espresso could not click the displayed candidate row. The one retry after setting emulator animation scales to zero failed at the same line. Search was not reached this round, and the cause remains unclassified. |
| AC-005 | NOT VERIFIED | `ImportWaypointMapMarkerTest` passed 1/1 on the emulator in this round and checks candidate marker objects, selection highlight, candidate restoration, and cleanup. The import-sheet UI test did not complete its integrated search/page-switch/import path. Visual marker rendering and retaining old rows under the loading overlay were not verified. |

## Test, Build, and CI Evidence

| Check | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | PASS (previous evidence) | 167 tests, 0 failures/errors on source revision `f5e31eff`; `GutterRepositoryNearbyTest`: 3/3 passed. Not rerun in this emulator-only round. |
| `:app:assembleDebug` | PASS (previous evidence) | Completed on source revision `f5e31eff` using Android Studio JDK and the CI placeholder. Not rerun in this emulator-only round. |
| `:app:compileDebugAndroidTestKotlin` | PASS (previous evidence) | Completed on source revision `f5e31eff`. Not rerun in this emulator-only round. |
| `:app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554 -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` | FAIL | On fixed revision `c7fa5f7` (source unchanged from `f5e31eff`), the first attempt failed at line 155 while Espresso clicked displayed text `UI-CANDIDATE-1`; the exception reported three failed click attempts and noted animations/transitions were enabled. After setting window, transition, and animator animation scales to zero on `emulator-5554`, the single retry failed at the same line after three click attempts. No more retries were run. |
| Earlier UI evidence on `f5e31eff` | FAIL | First attempt failed at line 188 (`expected [A001], got [A0]`); its one retry failed at line 165 (`expected tab index 1, got 0`). These earlier results concern the same app and test source as `c7fa5f7`. |
| `:app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554 -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportWaypointMapMarkerTest` | PASS | 1/1 on `emulator-5554`, Android 16, at fixed revision `c7fa5f7`. |
| Emulator animation settings after tests | PASS | Restored and confirmed window scale `1.0`, transition scale `1.0`, animator scale unset (`null`). |
| `git diff --check f5e31eff^ f5e31eff` | PASS (previous evidence) | No whitespace errors. |
| CI for `c7fa5f71873168187ce0276a28a4aab46f31db28` | NOT VERIFIED | GitHub combined status returned no status checks; workflow-run lookup returned no runs for this commit. Local checks are not represented as CI. |
| Physical-device GPS/backend cases | NOT VERIFIED | Not run under the emulator-only scope. |

## Implementation and Regression Review

- Recent Saved keeps the no-query request and is invoked again on page entry. Search records only submitted text and uses it on re-entry; the submitted-text UI assertion failed as described above.
- Nearby uses the shared endpoint with `lng`/`lat` and Bearer authorization. The DBG-1016 change accepts the first fresh fix within the accuracy threshold, then updates the current-location marker/camera and queries with that fix. The location policy unit tests passed.
- Successful responses feed the common candidate callback and preserve server order. Tab changes clear selection/candidates, requests are cancelled or sequence-guarded, and loading does not replace existing rows before a response.
- No source change was made during verification. The inconsistent tab/search assertions need investigation before changing production code or test code.

## Final Result

**FAIL**

AC-004 failed in prior execution when the submitted value was captured as `A0` instead of `A001`. In this round, the UI test could not click the candidate row even after the emulator animations were disabled; the retry limit is exhausted. The cause remains `unknown`, so route to Investigation before any re-implementation. AC-003 and AC-005 also remain `NOT VERIFIED`, and CI evidence is unavailable for the fixed revision.
