# Coding Rules

## Objective

Keep implementation changes correct, scoped, maintainable, and consistent with the approved architecture and plan.

## Required Reading

- `AGENTS.md`
- `ai/architecture.md`
- approved task `requirement.md`
- approved task `plan.md`
- latest task `state.yaml`

## Rules

Developer MUST:

- inspect existing implementations and tests before creating new code
- follow the existing Kotlin, Android, and MVVM conventions in the affected module
- keep business logic outside Activities, Fragments, and composables when the architecture already provides an owning layer
- preserve public behavior unless the approved requirement changes it
- handle errors and lifecycle boundaries consistently with nearby code
- update tests when observable behavior changes
- avoid new dependencies unless the approved plan justifies them
- keep changes limited to the approved files or explain and obtain approval for a necessary scope change

## Reuse

- Reuse an existing component when its behavior, ownership, lifecycle, and design constraints match.
- Do not create a second abstraction for an existing capability.
- Do not generalize code only for hypothetical future use.
- Cross-project extraction requires its own approved scope and compatibility contract.

## Restrictions

Developer MUST NOT:

- hide behavior changes inside a refactor
- modify generated files unless the project explicitly treats them as source
- expose secrets or sensitive values in source, logs, tests, or documentation
- overwrite unrelated working-tree changes

## Definition of Done

- changed behavior matches the approved acceptance criteria
- architecture and reuse decisions are documented when material
- relevant tests are updated
- the diff contains no unrelated code changes
