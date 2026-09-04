# Root Cause

Task: feat-0904
Date: 2026-09-04
Phase: debug

## Summary

Verification failed because the first implementation did not fully close every authenticated 401 path, did not exactly satisfy the required Dialog copy, and did not provide enough test evidence for draft preservation and edit-flow logout behavior. One unrelated debug flag change also entered the feature diff.

## Failed Acceptance Criteria Mapping

| AC | Issue | Root Cause |
|---|---|---|
| AC-002 | ISS-0904-001 | `AddGutterBottomSheet.preloadEditWaypointDetails()` still treats `getNodeDetails(...)` 401 as a normal partial preload failure by logging and setting `hasError = true`. |
| AC-004 | ISS-0904-002 | `AuthExpiredHandler` splits the required text between title/message and does not contain the exact phrase `登入狀態已失效，請重新登入` as one contiguous Dialog copy. |
| AC-006 | ISS-0904-001 | Edit preload 401 has no forced-draft callback before Dialog/logout. Import bottom sheet 401 uses its own handler and does not ask the parent editing sheet/host to save active waypoints first. |
| AC-007 | ISS-0904-003 | Draft preservation is plausible by source review, but no test proves the forced-saved draft remains visible with local photo paths and upload state preserved. |
| AC-013 | ISS-0904-003 | Unit coverage only verifies `isAuthExpired()` and `ScopeMapCoordinator` routing. Required coverage for once-only handler behavior, logout/login distinction, draft-save trigger, and upload 401 distinction is missing. |
| AC-014 | ISS-0904-003 | Instrumentation coverage only exercises shell layout/tab switching, not the required authenticated edit-screen 401 flow. |

## Issue Details

### ISS-0904-001: Edit preload 401 does not trigger forced logout

`AddGutterBottomSheet.preloadEditWaypointDetails()` remained on the older behavior: `ApiResult.Error` from node detail preload is treated as partial loading failure. Because this branch never calls `AuthExpiredHandler`, a token-expired user can stay in the edit sheet instead of being forced back to login.

This also affects draft preservation because the handler's optional save callback is never reached.

### ISS-0904-002: Dialog copy mismatch

The Dialog currently has title `登入狀態已失效` and message `登入狀態已失效，請重新登入。系統即將登出。`. The requirement expects copy containing `登入狀態已失效，請重新登入`. To avoid fragile UI acceptance failures, the phrase should be present exactly in one Dialog-visible string.

### ISS-0904-003: Test evidence incomplete

The implementation added useful first tests, but the high-risk behavior is user-flow oriented: save draft before logout, suppress duplicates, distinguish login/logout 401, and preserve photo state. Those are not yet proven.

### ISS-0904-004: Debug group simulation flag enabled

`GutterApiClient.ENABLE_GROUP_SIMULATION` was changed from `false` to `true`. This is unrelated to auth-expired handling and creates a regression risk by exposing a development-only group switch path.

## Regression Risk

- Auth-expired routing touches many UI owners, so a broad fix could accidentally change non-401 error behavior.
- Draft-save callbacks must run before navigation but should not block logout forever if saving fails.
- UI tests may need a small test-only injection point; that point must not change release behavior.
- Reverting the debug flag should preserve the user's unrelated API URL change if it is intentional.

## Minimum Fix Direction

Use the existing `AuthExpiredHandler`, `ApiResult.Error.isAuthExpired()`, `onWaypointsChanged`, and draft coordinators. Avoid changing repository contracts, API DTOs, or non-401 behavior.
