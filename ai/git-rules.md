# Git Rules

## Objective

All AI Agents MUST follow a consistent Git workflow.

Git history MUST be traceable, reviewable and easy to rollback.

Git operations MUST support the project workflow defined in AGENTS.md.

---

## Branch Strategy

Developer MUST work on a dedicated feature branch.

Branch naming format:

<type>/<task-id>-<short-description>

Examples:

feature/TYG-205-photo-retry

fix/TYG-0831-login-crash

refactor/TYG-301-upload-repository

hotfix/TYG-999-production-crash

---

## Branch Types

Supported branch types:

- feature
- fix
- refactor
- hotfix
- release
- chore
- docs
- test

---

## Commit Workflow

Developer MUST:

- complete implementation before committing
- complete local validation OR document execution limitations
- update state.yaml before committing
- commit only one logical change at a time

Verification MUST verify committed revisions only.

---

## Commit Message Convention

Format:

<type>(<task-id>): <summary>

Examples:

feat(TYG-205): add retry upload

fix(TYG-0831): prevent login crash

refactor(TYG-301): simplify upload repository

test(TYG-205): add retry unit tests

docs(TYG-205): update implementation report

---

## Commit Types

Supported commit types:

- feat
- fix
- refactor
- test
- docs
- chore

---

## Commit Rules

Each commit MUST:

- represent one logical change
- be independently reviewable
- avoid unrelated modifications
- keep history clean

Developer MUST NOT:

- mix feature and refactor changes
- mix unrelated bug fixes
- commit generated temporary files

---

## Git Evidence

Developer SHOULD document:

- Branch Name
- Commit ID
- Files Changed
- Git Diff
- Build Result
- Unit Test Result

Verification MAY use this information as supporting evidence.

---

## Merge Rules

Merge is NOT part of Developer responsibilities.

Developer MUST stop after:

- implementation completed
- commit completed
- state.yaml updated

Merge MUST occur only after:

- Verification PASS
- CI PASS
- Human approval (if required)

---

## Rollback

Every commit SHOULD be independently reversible.

Large implementations SHOULD be split into multiple commits.

Avoid large commits that cannot be safely reverted.

---

## Restrictions

AI Agents MUST NOT:

- commit directly to main
- commit directly to release branches
- rewrite Git history without explicit instruction
- force push unless explicitly requested

---

## Definition of Done

Git workflow completes ONLY IF:

- Branch created
- Commit created
- Commit message follows convention
- state.yaml updated
- Ready for Verification