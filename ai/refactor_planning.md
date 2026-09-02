# Refactor Planning Rules

## Objective

Define a safe, reviewable path for improving internal structure without changing externally observable behavior.

## Refactor Boundary

Refactor MAY change:

- Internal class, function, or package structure
- Responsibility boundaries between existing modules
- Duplication, naming, dependency direction, or testability

Refactor MUST NOT change without a separate approved task:

- Product requirements or acceptance criteria
- User-visible behavior
- Public API request/response contracts
- Persisted data meaning or migration behavior
- Authentication, permissions, or security behavior

## Required Planning Content

The refactor task MUST use the normal task files:

- `requirement.md`
- `analysis.md`
- `plan.md`
- `state.yaml`

The plan MUST explicitly record:

- Current behavior and the evidence that describes it
- Refactor scope and non-goals
- Affected modules and dependency boundaries
- Public interfaces, API, persistence, and navigation risks
- Behavior-preservation and regression tests
- Commit split and rollback strategy

## Planning Gate

Refactor implementation MUST NOT begin until Plan Review confirms:

- The change is structural rather than a hidden feature or bug fix
- Existing behavior has been identified sufficiently to test
- The scope excludes unrelated cleanup
- Regression coverage is actionable
- Any required API or data contract change has its own task

## Verification Handoff

Verification MUST compare the refactor result with the documented current behavior and run the listed regression tests.

If behavior differs, classify the issue as `implementation_regression` and route it to `Debug`.

If the desired behavior was never defined, classify it as `requirement_gap` and route it to `Planning`.

## Example Scope

```yaml
task:
  id: TYG-301
  type: refactor
scope:
  - Extract upload retry policy from repository
  - Preserve upload, retry, offline, and cancel behavior
non_goals:
  - Add new retry modes
  - Change API payloads
  - Change persisted draft data
```
