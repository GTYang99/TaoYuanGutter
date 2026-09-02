# Project Summary
Android Kotlin application.

Architecture:
MVVM

Primary language:
Kotlin

Target:
Android 9+

See:
ai/architecture.md

# Specification Sources

Project specifications are stored in:

- `docs/index.md`
- `docs/product-spec.md`
- `docs/design-spec.md`
- `docs/api-contract.md`
- `docs/reusable-assets.md`

`docs/index.md` defines specification status and change rules.

Specification status:

- `fixed`: approved source of truth
- `draft`: not ready for implementation
- `deprecated`: must not be used by new tasks
- `TBD`: unresolved and MUST NOT be inferred

Specification precedence:

1. Fixed product specification
2. Fixed design, API, and reusable asset contracts
3. Approved task requirement
4. Approved implementation plan
5. Implementation

A task MUST NOT override a `fixed` specification. Changing a fixed specification requires a separate `spec_change` task, impact analysis, version update, and re-verification.

Every new or revised task MUST declare applicable specification IDs and versions in `requirement.md`. Empty layers use an empty list.

Before Planning is complete, Planning MUST resolve every referenced ID, confirm it is `fixed`, and stop if a conflict, `draft`, or `TBD` affects implementation.

# Workflow
## Success Flow
```
Specification Validation
↓
Planning
↓
Plan Review
↓
Implementation
↓
Developer Validation
(Build / Local Test)
↓
Git Commit
↓
Verification
↓
Release
```
## Fail Flow
```
Verification FAIL
↓
Failure Classification
↓
Requirement -> Planning
Planning -> Planning
Implementation -> Debug
Environment -> Infrastructure
Unknown -> Investigation
↓
Debug
↓
Re-Implementation
↓
Developer Validation
↓
Git Commit
↓
Verification

```

## Refactor Flow
```
Refactor Request
↓
Refactor Planning
↓
Plan Review
↓
Implementation
↓
Behavior Preservation Validation
↓
Developer Validation
↓
Git Commit
↓
Regression Verification
↓
Release
```

Refactor rules:

- Refactor MUST preserve existing externally observable behavior.
- Refactor MUST NOT introduce product features or change requirements.
- Refactor planning MUST define current behavior, affected boundaries, and regression tests before implementation.
- Refactor MUST NOT begin until the plan is approved.
- If behavior changes are discovered, stop the refactor and route the work to `feature`, `bugfix`, or `planning`.
- If a regression is found, create or update an issue with category `implementation_regression` and route it to `Debug`.

Read:

- ai/refactor_planning.md

## Issue Management

Issue Management is the shared way to track blockers, regressions, and requirement mismatches.

The task lifecycle remains the main flow. Issue records explain what went wrong and which phase should handle it next.

Read:

- ai/issue-management.md

Rules:

- `requirement_gap` -> `Planning`
- `implementation_regression` -> `Debug`
- `verification_failure` -> `Debug`
- `environment` -> `Infrastructure`
- `unknown` -> `Investigation`

Priority guidance:

- `P0` breaks a core flow or risks data
- `P1` blocks a major flow
- `P2` affects a local flow or edge case
- `P3` is a non-blocking improvement

You are an AI engineer.
When doing Planning:

    read ai/planning-rules.md
    Planning MUST NOT modify production code.
    
    input:
    Task Requirement
    Specification References
    Repository
    Architecture
    Existing Tests

    Planning Agent MUST create these files.
    at: 
    docs/tasks/
    fix-0831/ 
    + 資料夾名[修正狀態-四碼月日]
        + 檔案名[四碼月日-開發狀態]
        + [0831]requirement.md
        + [0831]analysis.md
        + [0831]plan.md
        + [0831]state.yaml
        Planning is complete only when plan.md contains an actionable Implementation Plan.
        state.yaml needs write these:
        ```yaml
        phase: planning
        status: plan_in_progress
        ```
        Planning 完：
        ```yaml
        phase: planning
        status: plan_ready
        next_action:
        plan_review
        ```

When doing develope:

    read ai/developer-rules.md
    input:
    plan.md
    state.yaml
    approved plan
    state.yaml needs write these:
    ```yaml
    phase: implementation

    status: implementation_complete

    implementation:
        status: completed

    next_action: verification
    ```

When doing Verification:

    read ai/verification-rules.md
    Verification Agent MUST create verification file.
    at: 
    docs/tasks/
    fix-0831/ 
    + 資料夾名[修正狀態-四碼月日]
        + 檔案名[四碼月日-verification]
        + [0831]verification.md
    input:
        requirement
        plan
        CI
        Git Diff
        Tests
    state.yaml needs write these:
    + PASS
    ```yaml
    phase: verification

    status: verification_passed

    next_action: release
    ```

    + FAIL：
    ```yaml
    phase: verification

    status: verification_failed

    verification:
      result: fail
      category: implementation
      failed_acceptance_criteria:
        - AC-003

    next_action: debug
    ```
    + BLOCK:
    ```
    blocking:

        - AC-003

        - TEST-014

        - Missing Retry Test
    ```
Sample state.yaml:
    ```yaml
    task: TYG-205

    phase: verification

    status: verification_failed

    planning:
      status: approved

    implementation:
      status: completed
      commit: abc1234

    verification:
      result: fail
      category: implementation
      failed_acceptance_criteria:
        - AC-003

    blocking:
      - TEST-014
      - Missing Retry Test

    next_action: debug
    ```

## Failure Classification

Verification MUST classify every failure.

Supported categories:
- requirement
- planning
- implementation
- environment
- unknown

State transition rules:
- requirement -> next_action: planning
- planning -> next_action: planning
- implementation -> next_action: debug
- environment -> next_action: infrastructure
- unknown -> next_action: investigation

```yaml
phase: verification
status: verification_failed

verification:
  result: fail
  category: implementation
  failed_acceptance_criteria:
    - AC-003

blocking:
  - TEST-014
  - Missing retry test

next_action: debug
```

# Task Lifecycle
```
Task
↓
Planning
↓
Plan Review
↓
Implementation
↓
Developer Validation
↓
Git Commit
↓
Verification
↓
Release
↓
Done
```
# Role Routing
```
Planning
↓
ai/planning-rules.md
Plan Review
↓
ai/plan-critic-rules.md
Developer
↓
ai/developer-rules.md
Verifier
↓
ai/verification-rules.md
Debug
↓
ai/implementation-debug.md
Refactor
↓
ai/refactor_planning.md
```
# Required Reading
```
Planning
↓
planning-rules
↓
architecture
Plan Review
↓
plan-critic-rules
Developer
↓
developer-rules
↓
coding-rules
Verifier
↓
verification-rules
↓
testing-rules
Debug
↓
implementation-debug
Refactor
↓
refactor_planning
```

# Gates
```
Planning
↓
Plan Approved
↓
Implementation Complete
↓
Git Commit
↓
CI PASS
↓
Verification PASS
↓
Merge
```

# Global Rules:
+ Do not modify requirement
+ Do not lower acceptance criteria
+ Do not skip tests
+ Do not perform unrelated refactoring
