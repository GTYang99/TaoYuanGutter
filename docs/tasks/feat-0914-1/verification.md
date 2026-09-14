# Verification

## Revision

- Branch: `feat/修改儀表板`
- Current HEAD: `fa7bfdf` (`feat/修改儀表板`)
- Previously recorded validation evidence: `66e9815`
- Scope note: the recorded evidence predates later dashboard layout commits; current worktree also contains an uncommitted `GutterApiService.kt` change and is excluded from this verification revision.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Authenticated XQ-AU52 verification: dashboard entry sent same-day and no-date queries; both returned HTTP 200. The account-specific values rendered as today `0.18` km and cumulative `4.05` km. Screenshot: `evidence/dashboard-initial-device.png`. |
| AC-002 | PASS | Fresh `testDebugUnitTest --rerun-tasks` passed; `DashboardModelsTest` verifies `accountMileage` skips `全部`, maps `10362` to `2.34`, and returns null for an unmatched username. |
| AC-003 | PASS | Source review confirms the dashboard layout exposes start/end date fields and Fragment sends `DashboardQuery(start, end)`. |
| AC-004 | PASS | Authenticated XQ-AU52 verification: selected same-day start/end dates, received HTTP 200 search result `0.18` km, then cleared fields and returned to `暫無資料`. Screenshots: `evidence/dashboard-search-result-device.png`, `evidence/dashboard-search-cleared-device.png`. |
| AC-005 | PASS | XQ-AU52 initial/result screenshots confirm the Figma-required mobile structure: two pale-purple 8dp mileage cards, 20dp outer padding, bordered white search card, and bottom two-tab navigation. Screenshots: `evidence/dashboard-initial-device.png`, `evidence/dashboard-search-result-device.png`. |
| AC-006 | PASS | Current emulator instrumentation passed: `MainShellActivityTest` (2) verifies map/dashboard tab switching; `AuthExpiredUiFlowTest` (1) verifies 401 return-to-login behavior. |

## Validation

- `git diff --check`: PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebug`: PASS; BUILD SUCCESSFUL, 50 actionable tasks.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest --rerun-tasks`: PASS; BUILD SUCCESSFUL, 32 tasks executed.
- `./gradlew installDebug installDebugAndroidTest`: PASS. Debug and test APKs installed; no manual test was performed on the connected physical device.
- `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.MainShellActivityTest ...`: PASS, 2 tests.
- `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.AuthExpiredUiFlowTest ...`: PASS, 1 test.
- XQ-AU52 authenticated dashboard smoke test: PASS. Initial dashboard sent same-day and no-date requests, both HTTP 200; the displayed values matched the current account response.
- XQ-AU52 date-search smoke test: PASS. Same-day date range returned HTTP 200 and a `0.18` km result; clear restored both placeholders and `暫無資料`.
- XQ-AU52 visual comparison: PASS. Captured initial and result states correspond to the Figma structure and principal visual tokens documented for the referenced nodes.
- CI: NOT VERIFIED; repository contains no CI configuration or recorded CI result for `fa7bfdf`.

## Result

NOT VERIFIED. All ACs pass on the verified revision, but Release remains blocked because CI evidence is unavailable. No implementation failure was observed in the fresh unit test, debug build, emulator regression tests, or authenticated XQ-AU52 smoke test.

## Next Action

Provide CI configuration or a recorded passing CI result for `fa7bfdf`, then re-run Verification and update the release decision.
