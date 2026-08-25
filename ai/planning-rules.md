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

---

## Repository Analysis

Planning MUST identify:

- Current Behavior
- Expected Behavior
- Affected Modules
- Dependencies
- Risks
- Unknown Assumptions

---

## Required Outputs

Planning MUST create or update:

- requirement.md
- analysis.md
- plan.md
- state.yaml

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