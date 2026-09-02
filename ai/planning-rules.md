# Planning Rules

## Objective

Planning Agent MUST transform a requirement into an executable implementation plan.

Planning does NOT modify production code.

Every plan.md MUST be generated from templates/plan-template.md.

The template structure MUST match the required sections exactly.

---


##  File Path: docs/tasks/[開發編號]/

---

## Required Inputs

- requirement.md
- docs/index.md
- Referenced product, design, API, and reusable asset specifications
- Repository
- Existing Architecture
- Existing Tests
- AGENTS.md

---

## Repository Analysis

Planning MUST identify:

- Current Behavior
- Expected Behavior
- Affected Modules
- Dependencies
- Risks
- Unknown Assumptions
- Potential issue categories that should be tracked separately

## Specification Validation

Planning MUST:

- Read `docs/index.md`.
- Resolve every specification ID declared in `requirement.md`.
- Confirm each referenced item exists and has status `fixed`.
- Record the referenced document version.
- Check for conflicts across product, design, API, and reusable asset specifications.
- Record a blocking `requirement_gap` issue when `draft`, `TBD`, missing, or conflicting specifications affect implementation.

Planning MUST NOT infer unresolved specification content.

---

## Required Outputs

Planning MUST create or update:

- requirement.md
- analysis.md
- plan.md
- state.yaml

Planning SHOULD log requirement gaps or early regressions as issues instead of folding them into the task state.

---

## plan.md MUST include

- Specification References
- Goal
- Current Behavior
- Expected Behavior
- Affected Files
- Implementation Steps
- Test Plan
- Regression Plan
- Risks
- Open Questions

---

## Task Types

Planning Agent MUST determine the task type before analysis.

Supported task types:

- feature
- bugfix
- debug
- refactor

For debug tasks:

Required outputs:

- root-cause.md
- fix-plan.md

A full implementation plan is not required unless the fix significantly changes the architecture.

---

### Feature

Purpose:

Implement new functionality.

Required Outputs:

- requirement.md
- analysis.md
- plan.md
- state.yaml :

```yaml
task:
  id: TYG-205
  type: feature
```


Plan Review:

Required

---

### Bug Fix

Purpose:

Correct existing functionality without changing product behavior.

Required Outputs:

- requirement.md
- analysis.md
- plan.md (Mini Plan)
- state.yaml :

```yaml
task:
  id: FIX-0831
  type: bugfix
```

Repository analysis MAY be simplified.

Plan SHOULD focus on:

- Root Cause
- Affected Modules
- Fix Strategy
- Regression Risk

Plan Review:

Required

---

### Debug

Purpose:

Identify the root cause of an unknown issue.

Required Outputs:

- root-cause.md
- fix-plan.md
- state.yaml :

```yaml
task:
  id: DBG-010
  type: debug
```

Planning focuses on investigation instead of implementation.

Implementation MUST NOT begin until the Root Cause has sufficient evidence.

### Refactor

Purpose:

Improve internal structure while preserving externally observable behavior.

Required outputs:

- requirement.md
- analysis.md
- plan.md
- state.yaml

Refactor planning MUST also read `ai/refactor_planning.md` and document:

- Current behavior that MUST remain unchanged
- Refactor-only scope and explicit non-goals
- Affected modules, public interfaces, persistence, and API boundaries
- Existing tests and additional behavior-preservation tests
- Rollback boundaries and commit split strategy

Refactor planning MUST NOT approve a scope that introduces a new feature, changes an acceptance criterion, or changes an API/data contract without a separate task.

Plan Review:

Required

---

## Restrictions

Planning MUST NOT:

- modify production code
- change requirements
- lower acceptance criteria
- perform unrelated refactoring
- invent missing requirements

---

## Definition of Done

Planning is complete ONLY IF:

- analysis.md completed
- plan.md completed
- Implementation Plan is actionable
- state.yaml updated
