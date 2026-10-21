# Knowledge Resolution — Version API Backend Routing

## Conflict

The approved `feat-1013-app-version-check` requirement and AC-010 pin version checks to the test BASE URL, independent of the selectable backend. The user has now directed that version checks follow the user's selected backend and specifically use the Taipei endpoint when TAIPEI is selected.

## Resolution

The current user request supersedes the old fixed-endpoint behavior for this follow-up task. The completed and independently verified `feat-1013-app-version-check` artifacts remain unchanged. This task will route version checks through the existing `ApiBackendTarget` mapping:

- BASE → `http://192.168.10.84/TY_RSGDBIP/`
- TAIPEI → `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`
- DEMO → `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/`

The endpoint path remains `/api/appVersion`, with no query or body parameters. Existing target selection remains owned by `GutterApiClient`; Release defaults to TAIPEI because the selector is debug-only.

## Status

Resolved by the user's explicit direction on 2026-10-21 (Asia/Taipei). No API contract or endpoint availability beyond the existing endpoint mapping is inferred; unavailable selected endpoints continue to fail open with no update prompt.
