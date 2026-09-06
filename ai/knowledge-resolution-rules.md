# Knowledge Resolution Rules

## Objective

Resolve the meaning, authority, and conflicts of product, design, API, asset, repository, and task sources before Planning commits to an implementation direction.

Knowledge Resolution interprets evidence. It does not invent product decisions or modify production code.

## When Required

Use this phase when:

- multiple source documents affect the task
- product, design, API, asset, or current behavior conflicts
- a fixed specification is incomplete or ambiguous
- Planning would otherwise need to invent a material requirement

A task with one clear, consistent requirement may skip this phase.

## Required Inputs

- current user request
- available files under `docs/product/`, `docs/design/`, `docs/api/`, and `docs/assets/`
- relevant repository behavior and tests
- task `requirement.md`, when present

## Source Resolution

For every material decision, record:

- source
- decision or constraint
- confidence
- affected acceptance criteria
- conflicts or missing evidence

Current user-approved decisions and approved requirements outrank descriptive examples. A design cannot silently redefine an API contract, and current implementation cannot silently redefine approved product behavior.

## Required Outputs

Create or update `docs/tasks/<task-id>/knowledge-resolution.md` with:

- sources reviewed
- resolved decisions
- unresolved conflicts
- assumptions safe for Planning
- questions requiring approval

Update `state.yaml` when resolved:

```yaml
phase: knowledge_resolution
status: knowledge_resolved

knowledge_resolution:
  status: resolved

next_action: planning
```

When blocked:

```yaml
phase: knowledge_resolution
status: blocked

knowledge_resolution:
  status: blocked

blocking:
  - Material specification conflict

next_action: requirement_clarification
```

## Definition of Done

- authoritative sources are identified
- material conflicts are resolved or explicitly blocked
- Planning can proceed without inventing product behavior
