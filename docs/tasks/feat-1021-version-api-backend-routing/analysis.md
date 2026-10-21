# Repository Analysis

## Task Classification

- Type: `feature` — change the version-check route to follow the selected backend, reversing the fixed-endpoint behavior approved in feat-1013.

## Current Behavior

`BackendEndpoints.apiBaseUrl(ApiBackendTarget)` already maps BASE, TAIPEI, and DEMO. Ordinary `GutterApiClient.instance` requests use the currently selected target. `AppVersionRepository` instead requests `GutterApiClient.versionCheckService()`, which is pinned to `VERSION_CHECK_TEST_BASE_URL`.

## Expected Behavior

Each version check snapshots the current selected target and uses the corresponding existing base URL plus `api/appVersion`. The selected service is not silently substituted if the request fails.

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/api/AppVersionRepository.kt` — select the target for each check.
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` — create/reuse a version-check Retrofit service for a specified target using the existing endpoint mapping.
- `app/src/test/java/com/example/taoyuangutter/api/AppVersionRepositoryTest.kt` and related API client tests — assert routing for each target and target changes between checks.
- `docs/tasks/feat-1021-version-api-backend-routing/` — task requirement, analysis, plan, review, and state evidence.

## Dependencies

- Existing `ApiBackendTarget` enum and `BackendEndpoints.apiBaseUrl` mapping.
- Existing Retrofit/Gson client and `GET api/appVersion` contract.

## Risks

- Cached Retrofit service could retain a prior target if target selection is not captured at each request.
- The BASE endpoint is cleartext while the other target URLs use HTTPS; preserve existing network-security behavior.
- Availability or response compatibility of each selected server is not established by endpoint constants alone. A server failure must retain existing fail-open behavior.

## Assumptions

- The user's direction means all selectable targets (BASE, TAIPEI, DEMO) should map through the existing selector; TAIPEI must use the provided URL.
- In Release, target selection remains at the existing TAIPEI default because the selector is debug-only.

## Potential Issue Categories

- A wrong or stale target is an `implementation_regression`.
- An unavailable selected endpoint is an `environment` result, not permission to fall back.
