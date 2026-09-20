# Verification

## Revision Under Test
- Production revision: `9ed2c7c` (`fix: prevent no-ditch panel ime over-offset`)
- Current branch: `fix/debug-0919-3-回報點位難點`
- Later commits on the branch only update task documentation; no production source changed after `9ed2c7c`.

## Physical Device Evidence
- Device details and exact commands are recorded in [connected-test-evidence.md](connected-test-evidence.md).
- The device was a Sony XQ-AU52 / Android 12, not a foldable; it supports the connected regression evidence for AC-003 only.

## Implementation Review
- `NoDitchModeUiController` no longer reads `WindowInsetsCompat.Type.ime()` for the no-ditch panel bottom margin.
- The panel margin is recalculated from its saved base margin and the system bar bottom inset on every inset update.
- Existing no-ditch callbacks and state transitions are unchanged.
- The focused policy test is present and would fail if IME height were reintroduced into the margin calculation.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | NOT VERIFIED | The code and build are verified, but no foldable runtime configuration was available to observe the panel while the IME is shown. |
| AC-002 | NOT VERIFIED | The calculation is based on the saved base margin on each inset update, but keyboard show/hide position evidence is unavailable without a foldable or approved compact-height runtime. |
| AC-003 | PASS | The committed production diff changes only the inset calculation and adds a pure policy helper; focused unit tests and `MainShellActivityTest` (12/12) passed on Sony XQ-AU52 / Android 12. |

## Executed Checks

| Check | Result | Details |
|---|---|---|
| `git diff --check` | PASS | Current branch diff is clean. |
| YAML parse for `state.yaml` | PASS | Parsed successfully with Ruby YAML parser. |
| `./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest` | PASS | Focused policy tests passed on JDK 21. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled successfully. |
| `./gradlew :app:connectedDebugAndroidTest` | FAIL, classified | 37/38 passed; one unrelated imported-waypoint test failed once with `NoActivityResumedException`. |
| Focused `MainShellActivityTest` | PASS | 12/12 passed on Sony XQ-AU52 / Android 12. |
| Retry `Debug0919ImportedWaypointUiTest` | PASS | 1/1 passed; no recurrence after one permitted retry. |
| Foldable device test | NOT VERIFIED | No foldable device or approved equivalent window configuration was available. |

## Classification
- The static root cause and corresponding code correction are consistent with the requirement.
- Remaining incomplete evidence is environmental: missing foldable runtime configuration.
- The one full-suite failure was not reproduced on the permitted retry and is unrelated to the changed files.
- Release cannot proceed until the affected runtime checks are executed on a usable environment.

## Next Action
- Infrastructure: provide a foldable or approved compact-height IME test environment, then execute AC-001 and AC-002 on the fixed production revision.
