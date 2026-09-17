# Fix Plan

## Status

- Pending runtime evidence recorded in `root-cause.md`.

## Candidate Minimum Scope

- Reuse a single short-lived location request policy that emits a recent location first and a better high-accuracy result second.
- Bind cancellation to the owning map/form lifecycle.
- Add tests for result ordering, improvement criteria, null results, and cancellation.

## Implementation Gate

- Do not modify production code until runtime evidence confirms the root cause and the camera-correction criteria are documented.
