# Independent Verification

## Revision and Worktree

- Fixed revision under test: `f5e31eff808c9aa3d04df23927bd660978d93931` (`feat/feat-1015-restore-nearby-waypoints` at verification start).
- Verification used a clean detached worktree at that commit. No production or test source was changed during verification. The ignored `local.properties` contained only the repository CI placeholder `MAPS_API_KEY=ci-placeholder`.
- Device scope was emulator-only, as directed. No command installed or ran the app on the connected XQ-AU52 physical device.
- This revision includes the FEAT-1006 integration and the DBG-1016 nearby fresh-location fix; the prior verification at `91959ac` does not cover this revision.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Source review confirms the sheet creates `最近存取節點`, `附近點位`, `搜尋點位` in that order and selects index 0 (`ImportExistingWaypointBottomSheet.kt:315-328`). The first emulator UI attempt passed the visible-label and selected-index assertions (`ImportExistingWaypointUiTest.kt:145-153`) before failing later in the test. The existing layout and sheet styling remain in place. |
| AC-002 | PASS | `GutterRepositoryNearbyTest` passed 3/3 tests, including `recentSavedLookupKeepsNoQueryCallShape` and the nearby coordinate/Bearer request test. The sheet calls the no-query repository overload on every Recent entry (`ImportExistingWaypointBottomSheet.kt:352-359, 462-487`); the first UI attempt also passed its initial request-count assertion. |
| AC-003 | NOT VERIFIED | The first emulator attempt passed its fake-location checks for Nearby entry, location-button refresh, candidate response, and coordinates (`ImportExistingWaypointUiTest.kt:161-176`). Source review confirms the fresh-fix policy, marker/camera update, and query (`GutterFormActivity.kt:914-961`); the `NearbyLocationFixPolicy` unit tests are included in the passing JVM suite. Physical GPS acquisition, permission/error handling, the live marker/camera behavior, and authenticated backend results were not exercised. |
| AC-004 | FAIL | On the first `Medium_Phone` Android 16 attempt, after `typeText("A001")` and Search submit, the fake API recorded `A0`; the assertion expected `A001` (`ImportExistingWaypointUiTest.kt:187-188`). The one permitted retry stopped earlier: after tapping Nearby, `selectedTabPosition` remained 0 instead of 1 (`ImportExistingWaypointUiTest.kt:161-165`). The failure is reproducible only as a failed UI test, and its cause is not classified; no further retry was run. |
| AC-005 | NOT VERIFIED | `ImportWaypointMapMarkerTest` passed 1/1 on the emulator and checks candidate marker objects, selection highlight, candidate restoration, and cleanup. The import-sheet UI test did not complete its integrated search/page-switch/import path. Visual marker rendering and retaining old rows under the loading overlay were not verified. |

## Test, Build, and CI Evidence

| Check | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest` | PASS | 167 tests, 0 failures/errors. `GutterRepositoryNearbyTest`: 3/3 passed. |
| `:app:assembleDebug` | PASS | Completed on the fixed revision using Android Studio JDK and the CI placeholder. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Completed on the fixed revision. |
| `:app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554 -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportExistingWaypointUiTest` | FAIL | Attempt 1 failed at line 188 (`expected [A001], got [A0]`). One permitted retry failed at line 165 (`expected tab index 1, got 0`). Both ran only on `Medium_Phone` / `emulator-5554`, Android 16. Retry limit for this evidence cycle is exhausted. |
| `:app:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554 -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.ImportWaypointMapMarkerTest` | PASS | 1/1 on `emulator-5554`, Android 16. |
| `git diff --check f5e31eff^ f5e31eff` | PASS | No whitespace errors. |
| CI for `f5e31eff` | NOT VERIFIED | GitHub combined status returned no status checks; workflow-run lookup returned no runs for this commit. Local checks are not represented as CI. |
| Physical-device GPS/backend cases | NOT VERIFIED | Not run under the emulator-only scope. |

## Implementation and Regression Review

- Recent Saved keeps the no-query request and is invoked again on page entry. Search records only submitted text and uses it on re-entry; the submitted-text UI assertion failed as described above.
- Nearby uses the shared endpoint with `lng`/`lat` and Bearer authorization. The DBG-1016 change accepts the first fresh fix within the accuracy threshold, then updates the current-location marker/camera and queries with that fix. The location policy unit tests passed.
- Successful responses feed the common candidate callback and preserve server order. Tab changes clear selection/candidates, requests are cancelled or sequence-guarded, and loading does not replace existing rows before a response.
- No source change was made during verification. The inconsistent tab/search assertions need investigation before changing production code or test code.

## Final Result

**FAIL**

AC-004 failed its executed UI assertion. The failure category remains `unknown`: the first attempt captured a truncated submitted value, while the retry failed earlier at tab selection. Route to Investigation before any re-implementation. AC-003 and AC-005 also remain `NOT VERIFIED`, and CI evidence is unavailable for the fixed revision.
