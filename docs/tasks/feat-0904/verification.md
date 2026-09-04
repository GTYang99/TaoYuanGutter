# Verification

Task: feat-0904
Date: 2026-09-04
Verifier: Codex
Pass: 3

## Result

PASS

## Summary

The latest implementation satisfies the approved requirements and closes the previous verification gaps.

Fresh validation is green:

- `testDebugUnitTest`: PASS
- `assembleDebugAndroidTest`: PASS
- focused `AuthExpiredUiFlowTest`: PASS on both connected devices
- `assembleDebug`: PASS

The previously open coverage issue `ISS-0904-003` is resolved by new repository auth tests, pending draft serialization tests, and focused connected UI coverage for the auth-expired flow.

## Inputs Reviewed

- `AGENTS.md`
- `ai/verification-rules.md`
- `ai/issue-management.md`
- `ai/architecture.md`
- `docs/tasks/feat-0904/requirement.md`
- `docs/tasks/feat-0904/analysis.md`
- `docs/tasks/feat-0904/plan.md`
- `docs/tasks/feat-0904/plan-review.md`
- `docs/tasks/feat-0904/root-cause.md`
- `docs/tasks/feat-0904/fix-plan.md`
- `docs/tasks/feat-0904/execution-report.md`
- `docs/tasks/feat-0904/issue-log.md`
- `docs/tasks/feat-0904/state.yaml`
- Current source files
- Current test files
- Git status and diff
- Fresh local test/build results

Note: `ai/verification-rules.md` lists `ai/testing-rules.md` as a required input, but that file does not exist in this repository. Verification continued with the available project rules and test files.

## Git Evidence

- Implementation working tree status before this verification pass: clean except the existing verification document update.
- No current implementation diff was present for this pass.
- Verification is based on current repository content and fresh validation results.

## Test Evidence

### Unit And Android Test Build

Command:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDebugUnitTest assembleDebugAndroidTest
```

Result:

PASS

Evidence:

- `BUILD SUCCESSFUL in 1s`
- `68 actionable tasks: 68 up-to-date`

Coverage verified:

- `ApiResultAuthTest` covers 401-only auth-expired classification and non-401 exclusion.
- `AuthExpiredHandlerTest` covers once-only handling, non-401 exclusion, draft-save failure continuation, and reset behavior.
- `GutterRepositoryAuthTest` covers logout HTTP 401 as successful local logout and login HTTP 401 as a login error.
- `GutterSessionDraftTest` covers pending draft serialization preserving local photo paths and upload state.
- `ScopeMapCoordinatorTest` covers dedicated auth-expired routing for map scope search.
- `UploadFailureClassifierTest` covers store/photo/batch 401 reference-code distinction.

### Focused Connected Auth UI Test

Command:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AuthExpiredUiFlowTest
```

Result:

PASS

Evidence:

- Devices: `XQ-AU52 - 12`, `Medium_Phone(AVD) - 14`
- `BUILD SUCCESSFUL in 1m 55s`
- `78 actionable tasks: 1 executed, 77 up-to-date`

Coverage verified:

- `AuthExpiredUiFlowTest.editScreen401SavesDraftShowsDialogAndReturnsToLogin()` launches `MainShellActivity`, triggers an auth-expired 401 through `AuthExpiredHandler`, verifies draft-save callback invocation, verifies the Dialog is displayed, confirms the Dialog, and observes `LoginActivity`.

### Debug Build

Command:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew assembleDebug
```

Result:

PASS

Evidence:

- `BUILD SUCCESSFUL in 1s`
- `42 actionable tasks: 42 up-to-date`

## Acceptance Criteria Review

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `DashboardFragment` routes `errorCode == 401` through `AuthExpiredHandler`; `AuthNavigator.clearAuthAndGoLogin()` clears auth values and opens `LoginActivity` with clear-task flags. |
| AC-002 | PASS | `ScopeMapCoordinator` exposes `onAuthExpired`; map/delete/no-ditch/update/import paths route 401 through `AuthExpiredHandler`; `AddGutterBottomSheet` edit preload routes `getNodeDetails(...)` 401 through the handler. |
| AC-003 | PASS | Photo batch failures in `MainActivity` and `MapWorkspaceFragment` detect 401 and invoke `AuthExpiredHandler`; `GutterFormActivity` direct photo upload handles 401 with `syncSessionDraftNowBlocking()`. |
| AC-004 | PASS | `AuthExpiredHandler` Dialog message contains `登入狀態已失效，請重新登入` and informs the user that the system will log out. |
| AC-005 | PASS | Dialog positive button calls `AuthNavigator.clearAuthAndGoLogin()`, which removes `auth_token`, `user_name`, `user_company`, `group_id`, then opens `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TASK`. |
| AC-006 | PASS | Editable 401 paths push current waypoints or call draft sync before handling logout: add/edit store and edit preload call `onWaypointsChanged`, form upload/import uses draft sync callbacks, and upload paths save current waypoints before logout. |
| AC-007 | PASS | `GutterSessionDraftTest.serializationPreservesPendingDraftPhotoPathsAndUploadState()` verifies draft serialization preserves `SPI_NUM`, local `photo1`, pending photo path, failed upload state, and error message. |
| AC-008 | PASS | `AuthExpiredHandler` logs draft-save failures and continues handling; `AuthExpiredHandlerTest.draftSaveFailureStillContinuesHandling()` verifies save failure does not block forced logout handling. |
| AC-009 | PASS | `GutterRepository.logout()` maps HTTP 401 to `ApiResult.Success`; `GutterRepositoryAuthTest.logout401IsTreatedAsSuccessfulLocalLogout()` verifies this behavior. |
| AC-010 | PASS | Login 401 stays in login error handling; `GutterRepositoryAuthTest.login401RemainsLoginError()` verifies HTTP 401 from login returns `ApiResult.Error` with code 401 and the login error message. |
| AC-011 | PASS | `AuthExpiredOnceGuard` suppresses duplicate handling; `AuthExpiredHandlerTest.duplicate401OnlySavesDraftAndHandlesOnce()` verifies one save and one handle for duplicate 401 calls. |
| AC-012 | PASS | `isAuthExpired()` returns true only for `code == 401`; unit tests cover non-401 exclusion and existing source review shows non-401 branches preserve normal error handling. |
| AC-013 | PASS | Unit coverage now verifies auth-expired classification, non-401 exclusion, logout 401 success, login 401 distinction, draft-save failure/duplicate handling, upload 401 distinction, and draft photo path/upload-state preservation. |
| AC-014 | PASS | `AuthExpiredUiFlowTest` provides focused connected UI coverage for save callback, Dialog display, confirm action, and navigation to `LoginActivity` after a simulated authenticated edit-flow 401. |

## Plan Conformance Review

PASS

- Shared `ApiResult.Error.isAuthExpired()` helper exists.
- Shared `AuthExpiredHandler` and `AuthExpiredOnceGuard` exist.
- Dashboard uses shared auth-expired handling.
- `ScopeMapCoordinator` exposes code-aware auth-expired routing.
- Map, import, delete, no-ditch, update-state, edit preload, add/edit store, and upload paths route 401 to forced logout handling.
- Dialog copy includes the required phrase.
- Draft-save callbacks run before Dialog/logout on editable paths.
- `ENABLE_GROUP_SIMULATION` is `false`.
- Login 401 remains outside forced logout handling.
- Logout 401 remains local logout success.
- Required unit and focused instrumentation coverage is present and passing.

## Regression Review

- Login 401 regression: PASS by source review and `GutterRepositoryAuthTest`.
- Logout 401 regression: PASS by source review and `GutterRepositoryAuthTest`.
- Non-401 regression: PASS by helper/source review and unit tests.
- Duplicate navigation regression: PASS by `AuthExpiredHandlerTest`.
- Debug behavior regression: PASS; `ENABLE_GROUP_SIMULATION` remains `false`.
- Draft preservation regression: PASS by `GutterSessionDraftTest`.
- Auth-expired UI regression: PASS by focused connected `AuthExpiredUiFlowTest`.

## Issues

Resolved:

- ISS-0904-001: edit preload 401 now triggers forced logout handling.
- ISS-0904-002: Dialog copy now includes the required phrase.
- ISS-0904-003: required auth-expired unit/UI coverage is now present.
- ISS-0904-004: debug group simulation flag is restored to `false`.

Open:

- None.

## Final Result

PASS

## Next Action

Release.
