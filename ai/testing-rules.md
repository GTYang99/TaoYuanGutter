# Testing Rules

## Objective

Produce proportionate, reproducible evidence that changed behavior works and existing behavior has not regressed.

## Test Selection

Use the smallest sufficient validation set without sacrificing required coverage:

1. targeted unit tests for changed logic
2. affected module build and static checks
3. UI or integration tests for changed user or API flows
4. regression checks for adjacent behavior
5. a smoke test when the complete suite is impractical

Every acceptance criterion MUST map to at least one test or another concrete form of evidence.

## Result Vocabulary

- `PASS`: executed evidence demonstrates the expected result
- `FAIL`: executed evidence contradicts the expected result
- `NOT VERIFIED`: evidence is unavailable, incomplete, or could not be executed

An unexecuted check is never PASS. `NOT VERIFIED` blocks Release until sufficient evidence is produced.

## Evidence

Record:

- command or manual procedure
- environment or device when relevant
- expected result
- actual result
- related acceptance criteria
- failure output or limitation

## Regression Rules

For bug fixes and refactors, include a test that would fail before the change when practical. Refactors MUST demonstrate behavior preservation at the affected public boundaries.

## Definition of Done

- all required checks have a result
- failures and unverified checks are visible
- results are reproducible from the recorded evidence
