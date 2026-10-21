# Plan Review

## Decision: APPROVED

The requirement resolves the prior feat-1013 behavior change explicitly: version checks must use the selected API backend. The plan reuses `BackendEndpoints.apiBaseUrl(target)`, captures one target per check, and covers BASE, TAIPEI, DEMO, target changes, and no-fallback failure handling. Scope is limited to the updater service/repository, focused tests, and this task's artifacts. Rollback is documented.

### Suggestions

- Capture `selectedTarget` once per check and pass the same value through service selection.
- Assert exact mapped URLs, GET method, no query/body, and routing after target changes.
- Treat emulator/device evidence as NOT VERIFIED if unavailable; focused JVM tests and build remain required.

### Blocking issues

None.
