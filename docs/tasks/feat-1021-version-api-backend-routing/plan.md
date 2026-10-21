# Implementation Plan

## Goal

Route each version check through the currently selected backend while preserving existing update behavior.

## Scope

- Replace the fixed version-check service route with the existing selected-target endpoint mapping.
- Add focused tests for BASE, TAIPEI, DEMO, and target changes between checks.
- Preserve all other update flow behavior.

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` — target-aware Retrofit service factory.
- `app/src/main/java/com/example/taoyuangutter/api/AppVersionRepository.kt` — snapshot and pass selected target per check.
- `app/src/test/java/com/example/taoyuangutter/api/AppVersionRepositoryTest.kt` and API-client tests — request URL and routing coverage.
- `docs/tasks/feat-1021-version-api-backend-routing/` — task phase artifacts.

## Implementation Steps

1. Reuse `BackendEndpoints.apiBaseUrl(target)` to build or retrieve a version-check service for the passed target.
2. Capture `GutterApiClient.selectedTarget` once at the start of each repository check and use that target for the entire request.
3. Extend focused tests to assert exact scheme/host/path, no parameters, and updated routing after selection changes.
4. Run focused JVM tests, debug build, and the relevant emulator lifecycle regression check; record exact results.

## Test Plan

- Run `AppVersionRepositoryTest` with a request-recording test server for all three targets.
- Verify changing target between calls changes the next request URL and does not mutate an in-flight request.
- Run focused app-version JVM tests and `:app:assembleDebug`.
- Run existing app-version instrumentation tests to confirm fail-open/prompt behavior is unchanged.

### Physical Device Test Scope

- Requires physical device: No
- Device/environment: JVM MockWebServer tests and Android emulator, Android 9+.
- In-scope Acceptance Criteria: AC-001 through AC-005.
- Regression risk: fixed-route regressions, selected-target staleness, and failure behavior.
- Full regression required: No.
- Full regression trigger: update prompt behavior changes, ordinary API routing changes, or state-loss evidence.
- Stop condition: all targeted routing checks pass, or sufficient reproducible failure evidence is recorded.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | JVM MockWebServer | Select BASE and invoke version check | GET reaches BASE `/api/appVersion` with no query/body | Recorded request URL and method |
| AC-002 | JVM MockWebServer | Select TAIPEI and invoke version check | GET reaches Taipei HTTPS `/api/appVersion` with no query/body | Recorded request URL and method |
| AC-003 | JVM MockWebServer | Select DEMO and invoke version check | GET reaches DEMO HTTPS `/api/appVersion` with no query/body | Recorded request URL and method |
| AC-004 | JVM MockWebServer | Change target between successive checks | Each new call uses its captured target; no stale URL | Request sequence assertion |
| AC-005 | JVM and emulator | Return HTTP/network failure from selected target | No fallback request, no prompt, app remains usable | Repository and focused instrumentation results |

## Regression Plan

- Confirm ordinary Retrofit services continue to route according to the selected target.
- Confirm invalid API data and network failures remain fail-open.
- Confirm version comparison and prompt cancellation behavior remain unchanged.

## Risks

- Existing Retrofit caching may accidentally bind a service to the initial target. Keep cache keys target-specific or pass a fresh target-specific base URL.
- The DEMO and BASE servers may not currently expose compatible version data; failure must remain non-blocking and must not redirect to Taipei.

## Rollback Plan

Revert only the follow-up target-aware service and tests, restoring the previously committed updater behavior without altering the release-ready feat-1013 task.

## Current Behavior

Ordinary API calls use `GutterApiClient.selectedTarget`; the version repository always uses the fixed test BASE service. AC-010 in the released feat-1013 task captured that behavior, which the user has now changed for this follow-up.

## Expected Behavior

Version checks use the selected target's existing URL mapping for each request. Selecting TAIPEI uses `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion`.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–2 | BASE request URL/method assertion |
| AC-002 | 1–2 | TAIPEI request URL/method assertion |
| AC-003 | 1–2 | DEMO request URL/method assertion |
| AC-004 | 2–3 | Successive target-change request sequence |
| AC-005 | 2–4 | No-fallback failure assertions and instrumentation regression |

## Failure Behavior

A selected endpoint failure returns no update; it does not fall back to another endpoint or block app use.

## Security and Privacy

No credentials or user data are added to version-check requests. Preserve the existing HTTP/HTTPS endpoint scheme for each selected target.

## Open Questions

None. The user directed that version checking follow backend selection, and the repository already defines the target-to-base-URL mapping.
