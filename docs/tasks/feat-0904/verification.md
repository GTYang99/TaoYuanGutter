# Verification

Task: feat-0904
Date: 2026-09-04
Verifier: Codex

## Result

FAIL

## Summary

Implementation adds a shared `AuthExpiredHandler`, a 401 helper, and routes several authenticated API error paths through forced logout. Local unit tests and connected instrumentation tests both pass.

Verification still fails because several acceptance criteria are not fully satisfied by evidence or source review:

- The required Dialog copy does not contain the exact phrase `登入狀態已失效，請重新登入`.
- `AddGutterBottomSheet` edit preload 401 is logged as a normal preload failure and does not trigger forced logout.
- UI/instrumentation coverage does not verify the required 401 edit-flow sequence.
- `ENABLE_GROUP_SIMULATION` was changed to `true`, which is outside the approved auth-expired scope and may expose a debug-only behavior.

## Inputs Reviewed

- `AGENTS.md`
- `ai/verification-rules.md`
- `ai/issue-management.md`
- `ai/architecture.md`
- `docs/tasks/feat-0904/requirement.md`
- `docs/tasks/feat-0904/analysis.md`
- `docs/tasks/feat-0904/plan.md`
- `docs/tasks/feat-0904/plan-review.md`
- `docs/tasks/feat-0904/state.yaml`
- Git diff
- Unit test results
- Connected instrumentation test results

Note: `ai/verification-rules.md` lists `ai/testing-rules.md` as a required input, but that file does not exist in this repository.

## Test Evidence

### Unit Tests

Command:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDebugUnitTest
```

Result:

PASS

Evidence:

- `BUILD SUCCESSFUL in 4s`
- `32 actionable tasks: 10 executed, 22 up-to-date`

### Connected Instrumentation Tests

Command:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest
```

Result:

PASS

Evidence:

- Devices: `Medium_Phone(AVD) - 14`, `XQ-AU52 - 12`
- `BUILD SUCCESSFUL in 2m 43s`
- `78 actionable tasks: 8 executed, 70 up-to-date`

Coverage limitation:

- Existing instrumentation tests cover shell layout and tab switching only.
- No instrumentation test verifies auth-expired 401 from an authenticated edit screen, draft persistence before Dialog, Dialog text, confirm action, or navigation to `LoginActivity`.

## Acceptance Criteria Review

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `DashboardFragment` routes `errorCode == 401` through `AuthExpiredHandler.handleIfAuthExpired(...)`, and `AuthNavigator.clearAuthAndGoLogin()` removes auth values and starts `LoginActivity` with clear-task flags. |
| AC-002 | FAIL | Many map/import/delete/no-ditch/update paths now route 401 through the handler, but `AddGutterBottomSheet.preloadEditWaypointDetails()` handles `repository.getNodeDetails(...)` 401 by setting `hasError = true` and logging only. It does not call `AuthExpiredHandler`. |
| AC-003 | PASS | Photo batch failures in `MainActivity` and `MapWorkspaceFragment` detect failure code 401 and invoke `AuthExpiredHandler`; `GutterFormActivity` direct photo upload also handles 401 with draft sync. |
| AC-004 | FAIL | `AuthExpiredHandler` title is `登入狀態已失效` and message asks the user to re-login, but the Dialog does not contain the exact required copy `登入狀態已失效，請重新登入`. |
| AC-005 | PASS | Positive button calls `AuthNavigator.clearAuthAndGoLogin()`, which removes `auth_token`, `user_name`, `user_company`, `group_id`, then opens `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TASK`. |
| AC-006 | FAIL | Some edit/upload paths pass draft-save callbacks before logout, but `AddGutterBottomSheet` edit preload 401 does not trigger forced logout or draft save. Import bottom sheet 401 also does not explicitly invoke a parent draft-save callback before Dialog. |
| AC-007 | NOT VERIFIED | Source review shows `MainActivity`/`MapWorkspaceFragment` use `autoSaveSessionDraft(...)` and photo upload paths preserve caller waypoints, but there is no test or UI evidence proving the forced-saved draft appears in the pending list with local photo paths/status preserved. |
| AC-008 | PASS | `AuthExpiredHandler` catches draft-save exceptions, logs `save draft before logout failed`, and continues to show Dialog/logout. |
| AC-009 | PASS | `GutterRepository.logout()` maps HTTP 401 to `ApiResult.Success`; logout callers also clear auth and navigate on success. |
| AC-010 | PASS | `LoginActivity` handles `repository.login(...)` errors by showing a login failure Toast and does not use `AuthExpiredHandler`. |
| AC-011 | PASS | `AuthExpiredHandler` suppresses duplicate handling while `dialogShown` is true. |
| AC-012 | PASS | Shared helper returns true only for `code == 401`; other error branches continue existing flow-specific UI. |
| AC-013 | FAIL | Unit tests cover `ApiResult.Error.isAuthExpired()` and `ScopeMapCoordinator` 401 hook. Missing required coverage remains for handler once-only behavior, logout 401 success behavior, login 401 exclusion, draft-save trigger, and 401 upload classifier distinction. |
| AC-014 | FAIL | Connected instrumentation tests pass, but no UI/instrumentation test verifies the required edit-screen 401 flow: save draft first, show Dialog, confirm, return to `LoginActivity`. |

## Plan Conformance Review

PASS where implemented:

- Shared `ApiResult.Error.isAuthExpired()` helper exists.
- Shared `AuthExpiredHandler` exists.
- Dashboard uses the shared handler.
- `ScopeMapCoordinator` exposes `onAuthExpired`.
- Multiple map, upload, delete, no-ditch, update-state, import activity, and form upload paths route 401 through forced logout handling.
- Login 401 remains local to login error handling.
- Logout 401 is treated as local logout success.

FAIL / deviation:

- `AddGutterBottomSheet` edit preload `getNodeDetails()` 401 does not route to forced logout.
- Dialog copy does not match the required phrase.
- Required test strategy is only partially implemented.
- `GutterApiClient.ENABLE_GROUP_SIMULATION` was changed from `false` to `true`; this is outside the approved implementation plan.

## Regression Review

- Login 401 regression: PASS by source review; login errors remain Toast-only.
- Logout 401 regression: PASS by source review; repository maps 401 to success.
- Non-401 regression: PASS by helper/source review; non-401 errors do not satisfy `isAuthExpired()`.
- Duplicate navigation regression: PASS by source review; handler guards with `dialogShown`.
- Debug behavior regression: FAIL risk; enabling group simulation is unrelated to auth-expired handling and can expose development-only long-press group switching.
- Draft preservation regression: NOT VERIFIED; current tests do not prove forced-logout draft list/photo-path preservation.

## Failure Classification

Category: implementation

Failed acceptance criteria:

- AC-002
- AC-004
- AC-006
- AC-007
- AC-013
- AC-014

Blocking issues:

- ISS-0904-001: Missing forced-logout handling for edit preload 401.
- ISS-0904-002: Dialog copy does not include required phrase.
- ISS-0904-003: Missing required auth-expired and forced-draft test coverage.
- ISS-0904-004: Unrelated debug group simulation flag enabled.

## Required Next Action

Debug before re-implementation.
