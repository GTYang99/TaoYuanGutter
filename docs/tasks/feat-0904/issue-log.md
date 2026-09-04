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
status: open
impact: Users can remain in an authenticated edit flow after token expiry during node detail preload.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt handles getNodeDetails ApiResult.Error by logging only.
failed_acceptance_criteria:
  - AC-002
  - AC-006
next_action: debug
```

## ISS-0904-002

```yaml
issue_id: ISS-0904-002
task_id: feat-0904
phase: verification
category: verification_failure
priority: P2
title: Auth expired Dialog copy does not match required phrase
status: open
impact: Required user-facing copy may fail acceptance and UI assertions.
evidence:
  - AuthExpiredHandler title is "登入狀態已失效" and message asks the user to re-login, but does not contain "登入狀態已失效，請重新登入" as one phrase.
failed_acceptance_criteria:
  - AC-004
next_action: debug
```

## ISS-0904-003

```yaml
issue_id: ISS-0904-003
task_id: feat-0904
phase: verification
category: verification_failure
priority: P1
title: Required auth-expired test coverage is incomplete
status: open
impact: Forced logout, draft save, duplicate handling, login/logout distinction, and UI flow are not sufficiently proven.
evidence:
  - testDebugUnitTest passed.
  - connectedDebugAndroidTest passed.
  - Existing instrumentation tests cover shell layout and tab switching only.
  - No UI test verifies edit-screen 401 saves draft, shows Dialog, confirms, and returns to LoginActivity.
failed_acceptance_criteria:
  - AC-007
  - AC-013
  - AC-014
next_action: debug
```

## ISS-0904-004

```yaml
issue_id: ISS-0904-004
task_id: feat-0904
phase: verification
category: implementation_regression
priority: P2
title: Debug group simulation flag enabled outside approved scope
status: open
impact: Development-only group switching can be exposed in this feature branch even though it is unrelated to auth-expired handling.
evidence:
  - app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt changes ENABLE_GROUP_SIMULATION from false to true.
failed_acceptance_criteria: []
next_action: debug
```
