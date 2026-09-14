# Verification

## Revision

- Branch: `feat/修改儀表板`
- Implementation commit: `991aecb`
- Validation evidence commit: `a06968c`

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `DashboardViewModel` creates same-day and no-date queries; local debug build passed. Authenticated API execution is not covered locally. |
| AC-002 | PASS | `accountMileage` skips `全部` and matches saved username; `DashboardModelsTest` covers `10362` → `2.34`. |
| AC-003 | PASS | New layout exposes only start/end date fields; Fragment sends `DashboardQuery(start, end)`. |
| AC-004 | PASS | Date picker, search, clear, empty/result rendering are implemented; no device UI evidence available. |
| AC-005 | NOT VERIFIED | XML contains 20dp margins, 8dp cards, purple mileage cards, white bordered search card; Figma/device visual comparison not executed. |
| AC-006 | NOT VERIFIED | Existing 401 handler and MainShell tab structure remain; instrumentation regression test not executed. |

## Validation

- `git diff --check`: PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest assembleDebug`: PASS; BUILD SUCCESSFUL, 50 actionable tasks.
- Instrumentation/device smoke test: NOT VERIFIED.
- Authenticated API complete-time query: NOT VERIFIED.

## Result

NOT VERIFIED. AC-005 and AC-006 require device/instrumentation evidence before Release. No implementation failure was observed in local compilation or unit tests.

## Next Action

Infrastructure/device validation, then re-run Verification and update the release decision.
