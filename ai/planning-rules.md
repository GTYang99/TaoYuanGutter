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
- Repository
- Existing Architecture
- Existing Tests
- AGENTS.md
- knowledge-resolution.md (when Knowledge Resolution was required)
- applicable files under docs/product/, docs/design/, docs/api/, and docs/assets/

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
- hotfix
- docs

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

---

### Refactor

Purpose:

Improve internal structure without changing observable behavior.

Required Outputs:

- requirement.md
- analysis.md
- plan.md
- state.yaml:

```yaml
task:
  id: REF-001
  type: refactor
```

Planning MUST identify:

- the behavior boundaries that must remain unchanged
- baseline tests or evidence
- structural objective and affected dependencies
- migration and rollback strategy when applicable
- reusable components affected by the change

Feature work and unrelated bug fixes MUST NOT be included in a refactor task. Plan Review is required.

---

### Hotfix

Purpose:

Correct an urgent production issue with the smallest safe change.

Required Outputs:

- requirement.md
- analysis.md
- plan.md (Mini Plan)
- state.yaml

Planning MAY be expedited but MUST include acceptance criteria, regression risk, validation, and rollback. Verification is never skipped.

---

### Documentation

Purpose:

Change documentation without changing production behavior.

Required Outputs:

- requirement.md
- plan.md (Mini Plan)
- state.yaml

Analysis and validation MAY be proportionate to the affected documentation, links, examples, and workflow contracts.

---

## Update state.yaml

When Planning starts:

```yaml
phase: planning
status: plan_in_progress

planning:
  status: in_progress

next_action: planning
```

When Planning is ready for review:

```yaml
phase: planning
status: plan_ready

planning:
  status: completed

next_action: plan_review
```

---

## Restrictions

Planning MUST NOT:

- modify production code
- change requirements
- lower acceptance criteria
- perform unrelated refactoring
- invent missing requirements
- bypass Knowledge Resolution when authoritative sources materially conflict

---

## Definition of Done

Planning is complete ONLY IF:

- all artifacts required by the selected task type are completed
- the Implementation Plan is actionable when a plan is required
- the root-cause and fix plan are evidence-based when the task type is debug
- state.yaml updated
- every acceptance criterion is traceable to a plan step and validation check
