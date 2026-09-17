# Fix Plan

## Status

- Ready for implementation. Root cause and physical-device evidence are recorded in `root-cause.md`.

## Candidate Minimum Scope

- Introduce a single short-lived location policy that emits a usable cached result first, then at most one newer high-accuracy result when it is materially more accurate.
- Apply it to the main workspace map and standalone point picker; update form/import behavior to use the same freshness and improvement criteria without leaving active callbacks.
- Prevent disruptive duplicate camera moves and repeated nearby-node searches by accepting the second result only when it is meaningfully better than the first.
- Bind cancellation to the owning map/form lifecycle.
- Add tests for result ordering, improvement criteria, null results, and cancellation.

## Implementation Gate

- Implementation may begin only after the concrete freshness threshold and "materially better" accuracy rule are recorded in the implementation plan and reviewed.
