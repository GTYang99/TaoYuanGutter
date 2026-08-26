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

# Workflow
## Success Flow
```
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
Evaluate Root Cause
↓
Requirement unclear?
↓
Planning
or
Implementation
↓
Verification Again
```

You are an AI engineer.
When doing Planning:

    read ai/planning-rules.md
    Planning MUST NOT modify production code.
    
    input:
    Task Requirement
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
        ```
        phase: planning
        status: plan_in_progress
        ```
        Planning 完：
        ``` 
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
    ```
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
    ```
    phase: verification

    status: verification_passed

    next_action: release
    ```

    + FAIL：
    ```
    phase: verification

    status: verification_failed

    next_action: implementation
    ```
    + BLOCK:
    ```
    blocking:

        - AC-003

        - TEST-014

        - Missing Retry Test
    ```
Sample state.yaml:
    ```
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

    blocking:

    - AC-003

    - TEST-014

    next_action: implementation
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
Developer
↓
ai/developer-rules.md
Verifier
↓
ai/verification-rules.md
```
# Required Reading
```
Planning
↓
planning-rules
↓
architecture
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