# Fix Plan

## Status

- Pending device reproduction and root-cause evidence recorded in `root-cause.md`.

## Candidate Minimum Scope

- If layout obstruction is confirmed, adjust only the report-panel/map interaction boundary for the affected window configuration.
- If state handling is confirmed, change only the selection-state transition and add a regression test for reset and exit.

## Implementation Gate

- Do not modify production code until a reproduced failure maps to one cause and the affected acceptance criterion.
