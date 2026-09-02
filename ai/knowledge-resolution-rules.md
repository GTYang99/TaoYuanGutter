# Knowledge Resolution Rules

## Objective

Knowledge Resolution Agent MUST convert task intent and project specifications into a consistent, traceable set of constraints before Planning begins.

Knowledge Resolution validates and connects existing knowledge. It MUST NOT invent, approve, or implement product behavior.

## File Path

`docs/tasks/[task-id]/`

## Required Inputs

Knowledge Resolution Agent MUST read:

- `AGENTS.md`
- Task requirement or intake
- `docs/index.md`
- `docs/product-spec.md`
- `docs/design-spec.md`
- `docs/api-contract.md`
- `docs/reusable-assets.md`
- `ai/architecture.md`
- `ai/knowledge-resolution-rules.md`

## Resolution Process

Knowledge Resolution Agent MUST:

- Resolve every product, design, API, asset, and component ID referenced by the task.
- Confirm each referenced item exists and record its version and status.
- Confirm standard implementation tasks reference only `fixed` specification items.
- Trace design, API, asset, and component references back to applicable product rules.
- Extract implementation constraints without creating an implementation plan.
- Identify missing, `draft`, `TBD`, deprecated, version-mismatched, or conflicting knowledge.
- Identify whether a requested change requires a separate `spec_change` task.
- Create or update a `requirement_gap` issue when unresolved knowledge blocks the task.

For a task whose type is already `spec_change`, Knowledge Resolution validates the current fixed source, requested change, affected IDs, and impact boundary. It MUST NOT route that same task back to `SPEC_CHANGE_REQUIRED` when those inputs are complete.

Knowledge Resolution MUST NOT infer unresolved specification content.

## Required Outputs

Knowledge Resolution Agent MUST create or update:

- `knowledge-resolution.md`
- `state.yaml`
- `issue-log.md` when resolution is blocked

`knowledge-resolution.md` MUST use `ai/templates/knowledge-resolution-template.md` and include:

- Task and source documents
- Resolved references with versions and statuses
- Derived constraints
- Conflicts
- Missing information
- Specification change impact
- Decision and next action

## Decisions

The decision MUST be one of:

- `READY_FOR_PLANNING`
- `BLOCKED`
- `SPEC_CHANGE_REQUIRED`

`READY_FOR_PLANNING` means every implementation-relevant reference is resolved and no blocking conflict remains.

`BLOCKED` means clarification or source correction is required before Planning.

`SPEC_CHANGE_REQUIRED` means the task would override a fixed specification and a separate `spec_change` task is required.

## Update state.yaml

When resolution starts:

```yaml
phase: knowledge_resolution
status: resolution_in_progress

knowledge_resolution:
  result: pending

next_action: knowledge_resolution
```

When ready:

```yaml
phase: knowledge_resolution
status: resolution_ready

knowledge_resolution:
  result: ready
  document: knowledge-resolution.md

next_action: planning
```

When clarification is required:

```yaml
phase: knowledge_resolution
status: resolution_blocked

knowledge_resolution:
  result: blocked
  issues:
    - ISS-001

blocking:
  - Missing or conflicting specification

next_action: requirement_clarification
```

When a fixed specification must change:

```yaml
phase: knowledge_resolution
status: spec_change_required

knowledge_resolution:
  result: spec_change_required
  affected_specifications:
    - PRD-001

next_action: spec_change
```

## Restrictions

Knowledge Resolution Agent MUST NOT:

- modify production code
- modify or approve fixed specifications
- modify task requirements to hide a conflict
- choose between conflicting sources without an explicit authority rule
- convert `draft` or `TBD` content into fixed behavior
- create an implementation plan
- lower acceptance criteria

## Definition of Done

Knowledge Resolution completes ONLY IF:

- every task reference has been checked
- versions and statuses are recorded
- derived constraints are traceable to specification IDs
- conflicts and missing information are documented
- a decision is recorded
- `state.yaml` points to the correct next action
