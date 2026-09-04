# Requirement

## Background
- Main map calls `/api/v1/map/scopeSearch` to load gutter line segments for the current visible map bounds.
- Loaded scope gutter polylines are stored in memory and rendered on Google Maps.
- Current behavior keeps previously loaded scope gutter polylines unless the same `spiNum` is redrawn or another flow explicitly clears them.
- When users pan or reload many map ranges, old range polylines can accumulate and increase memory and rendering pressure on mobile devices.

## Goal
- Reduce main-map memory and rendering pressure by replacing old scope gutter polylines with the latest successful `scopeSearch` result.

## Functional Requirements
- When a new `scopeSearch` request succeeds, the app must clear previously rendered scope gutter polylines before drawing the new response.
- Replacement must be based entirely on the latest successful `scopeSearch` response.
- Gutter line segments from older scope ranges must not remain in the scope polyline memory map after a newer successful response is drawn.
- If a `scopeSearch` request fails, the app must keep the currently rendered scope gutter polylines instead of clearing the map.
- Both main-map implementations must follow the same replacement behavior:
  - `MainActivity`
  - `MapWorkspaceFragment`
- Stale or out-of-order `scopeSearch` responses must not clear or redraw the latest visible range.
- Existing editing, inspecting, measuring, no-ditch reporting, layer visibility, and auth-expired behavior must remain unchanged.

## Non-functional Requirements
- Do not change the backend API contract.
- Do not lower existing scope search debounce, zoom threshold, loading indicator, or auth-expired acceptance criteria.
- Keep the implementation scoped to scope gutter polyline lifecycle management.
- Avoid unrelated refactoring.

## Acceptance Criteria
- AC-001: After a successful main-map `scopeSearch`, previously rendered scope gutter polylines are removed from Google Maps before the new response is drawn.
- AC-002: After a successful main-map `scopeSearch`, `ScopeGutterPolylineController` retains only polylines represented by the latest response.
- AC-003: If the latest `scopeSearch` fails with a non-auth error, existing scope gutter polylines remain visible and are not cleared.
- AC-004: If a stale older `scopeSearch` response finishes after a newer request, it does not clear or redraw the current scope layer.
- AC-005: `MainActivity` and `MapWorkspaceFragment` use the same replacement behavior.
- AC-006: Layer visibility state remains respected after replacement; if the scope layer is hidden, newly drawn scope polylines stay hidden.
- AC-007: Inspect/edit workflows that explicitly clear scope polylines continue to work as before.
- AC-008: Existing unit tests pass, and focused tests are added or updated for latest-response replacement behavior.

## Constraints
- Planning phase must not modify production code.
- Existing completed `docs/tasks/feat-0904` is reserved for a separate auth-expired task, so this planning package uses `docs/tasks/feat-0904-scope-refresh`.
- Do not modify unrelated task files or existing requirements.

## Open Questions
- 無
