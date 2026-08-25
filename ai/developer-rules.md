# Developer Rules

## Objective

Developer Agent MUST implement the approved implementation plan.

Developer MUST follow the approved plan and MUST NOT change requirements.

---

## File Path

docs/tasks/[開發編號]/

---

## Required Inputs

Developer MUST read:

- AGENTS.md
- requirement.md
- analysis.md
- plan.md
- state.yaml
- developer-rules.md

Repository MUST be synchronized before implementation.

---

## Required Outputs

Developer MUST create or update:

- Source Code
- Unit Tests (if applicable)
- UI Tests (if applicable)
- state.yaml

Developer SHOULD create meaningful Git commits.

---

## Implementation Rules

Developer MUST:

- Follow the approved implementation plan.
- Keep functions small and maintainable.
- Follow MVVM or project architecture.
- Keep modules loosely coupled.
- Separate feat and fix into different commits.
- Update tests when behavior changes.
- Keep logging and error handling consistent.
- Maintain backward compatibility unless specified.
- Use the sample code like OOP.

---

## Coding Constraints

Developer MUST NOT:

- modify requirements
- lower acceptance criteria
- skip required tests
- perform unrelated refactoring
- introduce unnecessary dependencies
- modify unrelated modules

---

## Security Rules

Developer MUST:

- use secure communication (TLS)
- avoid exposing secrets
- validate external inputs
- follow project security guidelines

---

## UI / UX Rules

Developer MUST:

- follow Material Design / Human Interface Guidelines
- support accessibility
- keep UI consistent across platforms
- centralize strings, colors and styles

---

## Git Rules

Developer SHOULD:

- use a dedicated feature branch
- write meaningful commit messages
- keep commits focused
- separate feat and fix commits

Example:

feat(TYG-205): add retry upload

fix(TYG-205): prevent duplicate upload

---

## Update state.yaml

When implementation starts:

```yaml
phase: implementation

status: implementation_in_progress

next_action: implementation
```

When implementation completes:

```yaml
phase: implementation

status: implementation_complete

implementation:
  status: completed

next_action: verification
```

---

## Restrictions

Developer MUST NOT:

- implement before Planning is approved
- implement before Plan Review passes
- bypass CI
- modify verification results

---

## Definition of Done

Implementation completes ONLY IF:

- Production code implemented
- Tests updated
- Build passes locally
- state.yaml updated
- Ready for Verification

---

## Block Conditions

Developer MUST stop implementation if:

- requirement.md is missing
- plan.md is missing
- plan is not approved
- state.yaml indicates planning is incomplete
- implementation scope is unclear