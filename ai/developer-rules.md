# Developer Rules

## Objective

Developer Agent MUST implement the approved implementation plan.

Developer MUST follow the approved plan and MUST NOT change requirements.

---

## File Path

docs/tasks/[開發編號]/

---

## Required Inputs

Developer MUST read:

- AGENTS.md
- requirement.md
- analysis.md
- plan.md
- state.yaml
- developer-rules.md

Repository MUST be synchronized before implementation.

---

## Required Outputs

Developer MUST create or update:

- Source Code
- Unit Tests (if applicable)
- UI Tests (if applicable)
- state.yaml
- Implementation Report (optional) or Execution Report

Developer SHOULD create meaningful Git commits.

---

## Implementation Rules

Developer MUST:

- Follow the approved implementation plan.
- Keep functions small and maintainable.
- Follow MVVM or project architecture.
- Keep modules loosely coupled.
- Separate feat and fix into different commits.
- Update tests when behavior changes.
- Keep logging and error handling consistent.
- Maintain backward compatibility unless specified.
- Use the sample code like OOP.

---

## Coding Constraints

Developer MUST NOT:

- modify requirements
- lower acceptance criteria
- skip required tests
- perform unrelated refactoring
- introduce unnecessary dependencies
- modify unrelated modules

---

## Security Rules

Developer MUST:

- use secure communication (TLS)
- avoid exposing secrets
- validate external inputs
- follow project security guidelines

---

## UI / UX Rules

Developer MUST:

- follow Material Design / Human Interface Guidelines
- support accessibility
- keep UI consistent across platforms
- centralize strings, colors and styles

---

## Git Workflow

Developer MUST:

- use a dedicated feature branch
- create a Git commit after implementation completes
- complete local validation before commit OR document execution limitations
- keep one logical change per commit
- avoid mixing unrelated changes
- separate feat, fix and refactor commits

Verification MUST verify a committed revision.

---

## Commit Workflow

Developer MUST create a Git commit after implementation and local validation are complete.

Developer MUST ensure:

- Source code changes are complete.
- Required tests have been updated.
- Local validation has been executed OR execution limitations documented.
- state.yaml has been updated.

Developer MUST create a Git commit before Verification begins.

Verification MUST review a committed revision instead of uncommitted local changes.

Example:

feat(TYG-205): add retry upload

fix(TYG-205): prevent duplicate upload

---

## Commit Rules

Developer MUST:

- keep one logical change per commit
- separate feat, fix and refactor into different commits
- avoid mixing unrelated changes
- keep commits reviewable
- ensure every commit builds or documents execution limitations

---

## Commit Message Convention

Commit message format:

<type>(<task-id>): <summary>

Types:

- feat
- fix
- refactor
- test
- docs
- chore

Examples:

feat(TYG-205): add retry upload

fix(TYG-205): prevent duplicate upload

refactor(TYG-205): simplify upload repository

test(TYG-205): add retry unit tests

docs(TYG-205): update implementation report

---

## Commit Evidence

Developer SHOULD document:

- Commit ID
- Branch Name
- Files Changed
- Build Result
- Unit Test Result
- Git Diff

---

## Update state.yaml

When implementation starts:

```yaml
phase: implementation

status: implementation_in_progress

next_action: implementation
```

When implementation completes:

```yaml
phase: implementation

status: implementation_complete

implementation:
  status: completed

next_action: verification
```

```yaml
phase: implementation

status: implementation_complete

implementation:

  build:
    status: not_executed
    reason: sandbox_limitation

  unit_test:
    status: not_executed
    reason: sandbox_limitation

next_action: verification
```

```yaml
phase: implementation

status: implementation_complete

implementation:

  status: completed

validation:
  status: completed

git:
  branch: feature/TYG-205
  commit: abc1234
  status: committed

next_action: verification
```

```yaml
phase: implementation
status: implementation_complete

implementation:
  status: completed
  mode: debug_fix

git:
  commit: abc1234
  status: committed

next_action: verification
```

---

## Restrictions

Developer MUST NOT:

- implement before Planning is approved
- implement before Plan Review passes
- bypass CI
- modify verification results
- changes have not been committed

---

## Definition of Done

Implementation completes ONLY IF:

- Production code implemented
- Tests updated
- Local validation completed OR execution limitation documented
- Git commit created
- state.yaml updated
- Ready for Verification

---

## Block Conditions

Developer MUST stop implementation if:

- requirement.md is missing
- plan.md is missing
- plan is not approved
- state.yaml indicates planning is incomplete
- implementation scope is unclear
- current branch is incorrect

---

## Execution Report Requirements

Developer MUST document:

- Executed command
- Result
- Error message
- Impact
- Recommendation

## Environment Constraints

Developer MUST distinguish between:

- Build Passed
- Build Failed
- Build Not Executed

Developer MUST NOT report PASS if execution was impossible.

If execution is blocked by environment limitations, the limitation MUST be documented.

Developer MUST include:

- Executed command
- Result
- Error message
- Impact
- Recommended next action

---

## Execution Evidence

Developer SHOULD provide:

- Build Result
- Unit Test Result
- UI Test Result
- Git Commit
- Git Diff

---

## Re-Implementation After Verification Failure

If state.yaml indicates:

next_action: implementation_debug

Developer MUST read:
- verification.md
- root-cause.md
- fix-plan.md
- latest committed diff

Developer MUST:
- implement only the approved debug fix scope
- avoid unrelated code changes
- update or add tests for the failed acceptance criteria
- create a new commit for the debug fix
- return the task to verification