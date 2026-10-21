# Requirement — Version API Follows Selected Backend

## Background

The completed `feat-1013-app-version-check` pins `GET /api/appVersion` to the test BASE server. The user now wants the version check to follow the same backend selection used by ordinary API calls.

## Goal

Route app-version checks to the API base URL corresponding to the currently selected `ApiBackendTarget`, so selecting TAIPEI uses `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`.

## Functional Requirements

- Use the currently selected backend target at the start of each version check; do not cache the target across checks.
- Resolve the base URL through the existing `BackendEndpoints.apiBaseUrl(target)` mapping:
  - BASE: `http://192.168.10.84/TY_RSGDBIP/`
  - TAIPEI: `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`
  - DEMO: `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/`
- Append the existing no-parameter path `api/appVersion`.
- Preserve current response validation, version comparison, optional update UI, and fail-open behavior. A failed or unavailable selected endpoint must not silently fall back to another target or block app use.
- Do not change backend selection behavior. The selector remains available only where currently supported; when no selection has been changed, the existing TAIPEI default applies.

## Acceptance Criteria

- AC-001: A version check with BASE selected sends a parameter-free GET to `http://192.168.10.84/TY_RSGDBIP/api/appVersion`.
- AC-002: A version check with TAIPEI selected sends a parameter-free GET to `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion`.
- AC-003: A version check with DEMO selected sends a parameter-free GET to `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/api/appVersion`.
- AC-004: Changing the selected target between checks changes the next check URL; an already-started request remains on the target captured when it began.
- AC-005: Existing parsing, version comparison, prompt/cancel behavior, and fail-open handling remain unchanged; selected-target HTTP/network failures do not block use or trigger fallback.

## Constraints

- Keep the change scoped to version-check routing and focused tests.
- Reuse `BackendEndpoints.apiBaseUrl` and `GutterApiClient.selectedTarget` rather than duplicating endpoint mappings.
- The prior feat-1013 release-ready task and its verified artifacts are not modified by this follow-up.
