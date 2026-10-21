# Plan Review

## Inputs Reviewed
- `AGENTS.md`
- `requirement.md`
- `analysis.md`
- `plan.md`
- `state.yaml`
- `ai/plan-critic-rules.md`

## Review
- Requirement and acceptance criteria are clear: preserve the same map fragment/viewport while switching to Dashboard and back, with only the selected tab visible and active.
- Repository evidence identifies `replace()` plus unconditional map initialization as the cause. The change is scoped to the shell and its existing instrumentation test.
- FragmentManager show/hide with stable tags reuses the map view; lifecycle caps keep the inactive tab at `STARTED`.
- Test coverage will assert stable fragment identity, mutually exclusive hidden state, and selected/inactive lifecycle states on the approved Android test emulator.
- Initial map setup and explicit locate behavior stay unchanged. Rollback is the task commit.
- Emulator-only instrumentation scope is appropriate; no physical device or full regression is required for this isolated navigation fix.

## Findings

### Blocking Issues
- None.

### Improvement Suggestions
- Add explicit lifecycle assertions to the focused navigation test, because lifecycle capping is part of the plan's regression risk.

## Decision
APPROVED, with the lifecycle assertions added to the implementation test.
