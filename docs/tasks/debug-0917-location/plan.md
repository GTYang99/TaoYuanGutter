# Implementation Plan

## Goal

- All supported map location flows use a recent cached location immediately, then accept one materially better high-accuracy refinement.

## Scope

- Main-map controller (including its legacy Activity consumer), standalone point picker, and form/import nearby-node lookup.
- No background tracking, WMS change, or unrelated map behavior change.

## Quality Rules

- A cached system location is usable when it is no older than 5 minutes.
- A high-accuracy result replaces an accepted location only when it is newer and improves horizontal accuracy by at least 10 m, or when no usable location was accepted.
- Import keeps its current ≤30 m requirement for a high-accuracy-only fallback; a usable cached/host result may still start the first lookup immediately.

## Affected Files

- `map/LocationFixQualityPolicy.kt` — shared, testable freshness and refinement rule.
- `map/MyLocationController.kt` — two-stage main-map location and cancellation.
- `map/MapWorkspaceFragment.kt`, `MainActivity.kt` — lifecycle cancellation for the shared controller.
- `gutter/MapPointPickerActivity.kt` — reuse shared two-stage map behavior and cancellation.
- `gutter/GutterFormActivity.kt` — immediate cache/host lookup plus one qualified refinement.
- Unit tests for the quality policy.

## Implementation Steps

1. Add the pure quality policy and unit tests for freshness, null, and 10 m refinement cases.
2. Update `MyLocationController` to move to a usable cached position, request one high-accuracy result, and animate only if the result passes the policy; expose cancellation.
3. Route point-picker current-location handling through the shared controller and cancel it on destruction.
4. Update form/import lookup to search using an available host or recent system location first, then re-search only for a qualified high-accuracy result; cancel callbacks on sheet/activity teardown.
5. Add lifecycle cancellation to both main-map consumers and run focused tests, build, and Sony physical checks.

## Test Plan

- JVM: quality-policy acceptance/rejection cases.
- Build: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Sony XQ-AU52: verify immediate first center/search from a current cache, then one correction only when accuracy improves by ≥10 m; verify no callback after closing the screen.

## Regression Plan

- Permission denial, unavailable location, import timeout, map recenter reload, and point-picker confirmation remain functional.

## Risks

- A second accepted fix can reload nearby nodes; the 10 m gate limits this to meaningful corrections.
- Device-specific cached-location ages differ; stale cached results remain excluded after five minutes.

## Rollback Plan

- Revert the single fix commit.

## Acceptance Criteria Traceability

| AC | Steps | Validation |
|---|---|---|
| AC-001 | 1–4 | policy tests and Sony device first-center/search check |
| AC-002 | 1–4 | policy tests and Sony refinement check |
| AC-003 | 2–5 | lifecycle, permission, and timeout regression checks |

## Open Questions

- None.
