# Issue Log

Task: feat-0904
Date: 2026-09-04

## ISS-0904-001

```yaml
issue_id: ISS-0904-001
task_id: feat-0904
phase: verification
category: implementation_regression
priority: P1
title: Edit preload 401 does not trigger forced logout
status: resolved
impact: Users can remain in an authenticated edit flow after token expiry during node detail preload.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt handles getNodeDetails ApiResult.Error by logging only.
  - Verification pass 2 confirmed edit preload 401 now calls onWaypointsChanged and AuthExpiredHandler.
failed_acceptance_criteria:
  - AC-002
  - AC-006
next_action: verified
```

## ISS-0904-002

```yaml
issue_id: ISS-0904-002
task_id: feat-0904
phase: verification
category: verification_failure
priority: P2
title: Auth expired Dialog copy does not match required phrase
status: resolved
impact: Required user-facing copy may fail acceptance and UI assertions.
evidence:
  - AuthExpiredHandler title is "登入狀態已失效" and message asks the user to re-login, but does not contain "登入狀態已失效，請重新登入" as one phrase.
  - Verification pass 2 confirmed Dialog message now contains "登入狀態已失效，請重新登入".
failed_acceptance_criteria:
  - AC-004
next_action: verified
```

## ISS-0904-003

```yaml
issue_id: ISS-0904-003
task_id: feat-0904
phase: verification
category: verification_failure
priority: P1
title: Required auth-expired test coverage is incomplete
status: resolved
impact: Forced logout, draft save, duplicate handling, login/logout distinction, and UI flow are not sufficiently proven.
evidence:
  - testDebugUnitTest passed.
  - connectedDebugAndroidTest passed.
  - Existing instrumentation tests cover shell layout and tab switching only.
  - No UI test verifies edit-screen 401 saves draft, shows Dialog, confirms, and returns to LoginActivity.
  - Verification pass 2 confirmed unit coverage improved for once-only handling and upload 401 distinction.
  - Verification pass 2 confirmed coverage is still missing for logout 401, login 401, pending draft/photo preservation, and the required edit-screen 401 instrumentation flow.
  - Re-implementation added GutterRepositoryAuthTest for logout 401 success and login 401 error distinction.
  - Re-implementation added GutterSessionDraftTest for pending draft photo path and upload-state preservation.
  - Re-implementation added AuthExpiredUiFlowTest for save callback, auth-expired Dialog display, confirm action, and LoginActivity navigation.
  - Focused connected AuthExpiredUiFlowTest passed on Medium_Phone(AVD) - 14 and XQ-AU52 - 12.
failed_acceptance_criteria:
  - AC-007
  - AC-013
  - AC-014
next_action: verified
```

## ISS-0904-004

```yaml
issue_id: ISS-0904-004
task_id: feat-0904
phase: verification
category: implementation_regression
priority: P2
title: Debug group simulation flag enabled outside approved scope
status: resolved
impact: Development-only group switching can be exposed in this feature branch even though it is unrelated to auth-expired handling.
evidence:
  - app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt changes ENABLE_GROUP_SIMULATION from false to true.
  - Verification pass 2 confirmed ENABLE_GROUP_SIMULATION is false.
failed_acceptance_criteria: []
next_action: verified
```
