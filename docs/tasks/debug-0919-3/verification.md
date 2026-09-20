# Verification

## Revision Under Test
- Production revision: `9ed2c7c` (`fix: prevent no-ditch panel ime over-offset`)
- Current branch: `fix/debug-0919-3-回報點位難點`
- Later commits on the branch only update task documentation; no production source changed after `9ed2c7c`.

## Implementation Review
- `NoDitchModeUiController` no longer reads `WindowInsetsCompat.Type.ime()` for the no-ditch panel bottom margin.
- The panel margin is recalculated from its saved base margin and the system bar bottom inset on every inset update.
- Existing no-ditch callbacks and state transitions are unchanged.
- The focused policy test is present and would fail if IME height were reintroduced into the margin calculation.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | NOT VERIFIED | The static fix removes the identified over-offset, but the Gradle test/build could not run and no foldable runtime configuration was available. |
| AC-002 | NOT VERIFIED | The calculation is based on the saved base margin on each inset update, but keyboard show/hide behavior was not executable without a JDK or matching runtime evidence. |
| AC-003 | PASS (static review) | The committed production diff changes only the inset calculation and adds a pure policy helper; point selection, reset, note validation, submit, and exit callbacks are unchanged. Runtime regression execution remains unavailable. |

## Executed Checks

| Check | Result | Details |
|---|---|---|
| `git diff --check` | PASS | Current branch diff is clean. |
| YAML parse for `state.yaml` | PASS | Parsed successfully with Ruby YAML parser. |
| `./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest` | NOT VERIFIED | No Java runtime was available, so Gradle could not start. |
| `./gradlew :app:assembleDebug` | NOT VERIFIED | Not executable for the same missing-Java environment limitation. |
| Foldable device test | NOT VERIFIED | No foldable device or approved equivalent window configuration was available. |

## Classification
- The static root cause and corresponding code correction are consistent with the requirement.
- Remaining incomplete evidence is environmental: missing JDK and missing foldable runtime configuration.
- Release cannot proceed until the affected runtime checks are executed on a usable environment.

## Next Action
- Infrastructure: provide a JDK/Android build environment and a foldable or approved compact-height IME test environment, then rerun the focused test and AC-001 through AC-003 on the fixed production revision.
