# Project Summary

Android Kotlin application using MVVM.

- Primary language: Kotlin
- Target: Android 9+
- Architecture: `ai/architecture.md`

# Goal

Complete each task from requirement understanding through verified release readiness without changing approved product intent, lowering acceptance criteria, or introducing unrelated work.

# Success Criteria

A task is complete only when:

- its requirements and acceptance criteria are traceable to implementation and evidence
- all required phase artifacts are present under `docs/tasks/<task-id>/`
- relevant validation has passed, or any unavailable validation is explicitly marked `NOT VERIFIED`
- the implementation is committed on a task branch
- CI passes
- independent Verification passes
- Release records the final decision and the task state is `done`

# Instruction Precedence

For repository instructions, use this order:

1. Current user request and explicitly approved decisions
2. Approved `requirement.md` and acceptance criteria
3. This `AGENTS.md`
4. The active phase rule in `ai/`
5. Templates and examples

Do not silently resolve a conflict that would change requirements, acceptance criteria, public API behavior, data handling, or release risk. Record the conflict and request the smallest decision needed.

# Sources of Truth

- `AGENTS.md`: workflow, routing, gates, and global constraints
- `ai/*-rules.md`: phase-specific execution rules
- `ai/templates/`: canonical artifact and state formats
- `docs/product/`: product requirements and fixed product decisions, when present
- `docs/design/`: design specifications and asset guidance, when present
- `docs/api/`: API contracts, when present
- `docs/assets/`: asset inventory and usage constraints, when present
- `docs/tasks/<task-id>/`: task requirement, analysis, plan, state, execution evidence, issues, and verification

Examples never override an approved requirement or a canonical template.

# Workflow

## Intake Flow

Use Knowledge Resolution when a task depends on multiple specifications, design files, API contracts, assets, or conflicting evidence.

```text
Task Intake
    |
    +-- Specifications are clear and consistent --> Planning
    |
    +-- Multiple, missing, or conflicting sources --> Knowledge Resolution
                                                    |
                                                    +-- Resolved --> Planning
                                                    +-- Blocked --> Requirement Clarification
```

A small task with one clear requirement may proceed directly to Planning.

## Success Flow

```text
Planning
-> Plan Review
-> Implementation
-> Developer Validation
-> Git Commit
-> Verification
-> Release
-> Authorized Merge or Deployment
-> Done
```

## Failure Flow

Verification MUST classify failures before any code change:

```text
requirement    -> Knowledge Resolution or Planning
planning       -> Planning
implementation -> Debug -> Re-Implementation -> Developer Validation -> Git Commit -> Verification
environment    -> Infrastructure -> blocked phase or Verification
unknown        -> Investigation -> classified destination
```

Only an `implementation` failure enters Debug automatically.

# Task Type Routing

Planning MUST classify the task before producing a plan:

| Task type | Purpose | Review |
|---|---|---|
| `feature` | Add new product behavior | Full Plan Review |
| `bugfix` | Restore intended behavior | Plan Review; mini plan allowed |
| `debug` | Find the cause of an unknown failure | Evidence review before implementation |
| `refactor` | Improve internal structure without changing behavior | Plan Review plus behavior-preservation evidence |
| `hotfix` | Urgent production correction | Expedited review; no skipped verification |
| `docs` | Documentation-only change | Proportionate review and validation |

Detailed requirements are defined in `ai/planning-rules.md`.

# Role Routing

| Phase | Required rule | Primary output | Normal next action |
|---|---|---|---|
| Knowledge Resolution | `ai/knowledge-resolution-rules.md` | `knowledge-resolution.md` | `planning` |
| Planning | `ai/planning-rules.md` | `analysis.md`, `plan.md`, `state.yaml` | `plan_review` |
| Plan Review | `ai/plan-critic-rules.md` | `plan-review.md` | `implementation` |
| Implementation | `ai/developer-rules.md`, `ai/coding-rules.md` | code, tests, execution report | `verification` |
| Verification | `ai/verification-rules.md`, `ai/testing-rules.md` | `verification.md` | `release` or failure route |
| Debug | `ai/implementation-debug.md` | `root-cause.md`, `fix-plan.md` | `implementation_debug` |
| Infrastructure | `ai/infrastructure-rules.md` | environment evidence | blocked phase or `verification` |
| Investigation | `ai/investigation-rules.md` | classification evidence | classified destination |
| Release | `ai/release-rules.md` | release decision | `human_release` or `done` |

Every Agent MUST read the latest `state.yaml`, this file, its active phase rule, and the task artifacts required by that rule before acting.

# Gates

```text
Requirements Resolved
-> Plan Approved
-> Implementation Complete
-> Developer Validation Complete or Limitations Recorded
-> Git Commit
-> CI PASS
-> Verification PASS
-> Release Approval
-> Merge
-> Done
```

- `NOT VERIFIED` is not PASS and cannot advance to Release.
- Environment limitations may explain missing evidence but never convert it to PASS.
- Verification reviews a committed revision and does not modify production code.
- Merge is not part of Developer or Verifier responsibility and requires explicit authorization.

# Issue Management

Read `ai/issue-management.md` whenever a blocker, regression, requirement mismatch, or verification failure is found.

Task state tracks the main workflow. `issue-log.md` tracks individual problems.

Canonical issue-to-verification mapping:

| Issue category | Verification category | Route |
|---|---|---|
| `requirement_gap` | `requirement` | `knowledge_resolution` or `planning` |
| `planning_gap` | `planning` | `planning` |
| `implementation_regression` | `implementation` | `debug` |
| `verification_failure` | classification required | route by classified cause |
| `environment` | `environment` | `infrastructure` |
| `unknown` | `unknown` | `investigation` |
| `enhancement_request` | not a failure | backlog or separate task |

Priority guidance:

- `P0`: core flow broken, data/security risk, or broad regression
- `P1`: major flow blocked; workaround may exist
- `P2`: local flow, edge case, or UI degradation
- `P3`: non-blocking cleanup or improvement

# State Contract

`ai/templates/state-template.yaml` is the canonical state shape. Use a mapping for `task`, never a scalar.

```yaml
task:
  id: TYG-205
  type: feature

phase: planning
status: plan_in_progress
next_action: planning
```

Use one-line scalar values for `next_action`. Do not invent new phases, statuses, categories, or next actions without updating the canonical template and the owning phase rule.

# Autonomy and Stop Rules

Continue with reasonable, reversible assumptions when they do not change product behavior or acceptance criteria. Record material assumptions in `analysis.md`.

Stop the active phase and record a blocker when:

- required task artifacts are missing
- requirements or authoritative sources conflict materially
- an action may cause data loss, expose secrets, change a public contract, or require destructive migration
- the approved plan does not cover a necessary production-code change
- the current branch or working tree makes isolation unsafe
- required validation cannot run and no sufficient alternative evidence exists

Ask only for the smallest missing decision. Do not ask for information that can be obtained safely from the repository or available evidence.

# Validation and Evidence

After changes, run the most relevant available validation:

- targeted unit or UI tests for changed behavior
- build and static checks for affected modules
- regression checks for nearby behavior
- a minimal smoke test when full validation is impractical

Record commands, results, failures, and limitations. If a check cannot run, mark it `NOT VERIFIED`, explain why, and name the next best check.

# Allowed Side Effects

- Knowledge Resolution, Planning, Plan Review, Verification, Investigation, and Infrastructure MUST NOT modify production code.
- Implementation, including re-entry from an approved Debug fix plan, may modify only approved source, test, and task artifact scope.
- Release may update release records but MUST NOT merge, publish, deploy, or change production without explicit authorization.
- All phases MUST preserve unrelated user changes.

# Reuse Rules

Before creating a component, API abstraction, or asset wrapper, check for an existing equivalent in the repository.

- Prefer reuse when behavior, ownership, lifecycle, and design constraints match.
- Do not generalize a component solely for hypothetical future reuse.
- Cross-project extraction requires an explicit requirement or separate approved task.
- Refactoring for reuse must preserve behavior and remain separate from unrelated feature or bug-fix commits.

# Final Report

At the end of a phase, report:

- completed work and artifacts
- validation performed and results
- assumptions or unverified items
- blockers, if any
- resulting `phase`, `status`, and `next_action`

# Global Rules

- Do not modify approved requirements outside Planning or Knowledge Resolution.
- Do not lower acceptance criteria.
- Do not skip required validation or report unexecuted checks as PASS.
- Do not perform unrelated refactoring.
- Do not invent evidence, specifications, API behavior, or completion claims.
