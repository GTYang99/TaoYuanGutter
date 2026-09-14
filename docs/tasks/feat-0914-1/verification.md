# Verification

## Revision

- Branch: `feat/修改儀表板`
- Implementation commit: `c1d5281`
- Validation evidence commit: `a06968c`

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `DashboardViewModel` creates same-day and no-date queries; local debug build passed. Authenticated API execution is not covered locally. |
| AC-002 | PASS | `accountMileage` skips `全部` and matches saved username; `DashboardModelsTest` covers `10362` → `2.34`. |
| AC-003 | PASS | New layout exposes only start/end date fields; Fragment sends `DashboardQuery(start, end)`. |
| AC-004 | PASS | Date picker, search, clear, empty/result rendering are implemented; no device UI evidence available. |
| AC-005 | NOT VERIFIED | Figma MCP specifications were read for nodes `2374:25616`, `2374:25617`, and `2374:25667`, and the XML was aligned to those specifications. A device screenshot comparison is still unavailable. |
| AC-006 | PASS | Direct instrumentation: `MainShellActivityTest` 2 tests passed and `AuthExpiredUiFlowTest` 1 test passed on `Medium_Phone(AVD) - 14`. |

## Validation

- `git diff --check`: PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebug`: PASS; BUILD SUCCESSFUL, 50 actionable tasks.
- Instrumentation/device smoke test: NOT VERIFIED.
- `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.MainShellActivityTest ...`: PASS, 2 tests.
- `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.AuthExpiredUiFlowTest ...`: PASS, 1 test.
- Full `connectedDebugAndroidTest`: NOT VERIFIED; instrumentation worker remained running without output and was terminated after process inspection.
- Authenticated API complete-time query: NOT VERIFIED.
- Figma MCP specification readback: PASS; runtime screenshot comparison: NOT VERIFIED.

## Result

NOT VERIFIED. AC-005 and authenticated complete-time API behavior still require evidence before Release. No implementation failure was observed in local compilation, unit tests, or the targeted auth/tab tests.

## Next Action

Infrastructure/device validation, then re-run Verification and update the release decision.
