# Issue Log

## ISS-feat-0923-001

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P1
- Title: Android runtime verification unavailable
- Status: resolved
- Impact: Initial device-dependent verification was blocked; the blocked check has now been rerun.
- Evidence: Restricted execution reproduced `could not install *smartsocket* listener: Operation not permitted`; host-permitted execution discovered `emulator-5554` (`Medium_Phone(AVD)`, Android 14), and `:app:connectedDebugAndroidTest` passed 51 tests with 0 failures, 0 errors, and 0 skipped.
- Root cause: restricted host socket permission, not an application failure.
- Next action: verification — assess the connected-test evidence and remaining acceptance-criteria coverage.

## ISS-feat-0923-002

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P2
- Title: Verification worktree initially lacked Maps build setting
- Status: resolved for local build checks; keep as environment prerequisite
- Impact: exact-revision build initially stopped before compilation at manifest merge.
- Evidence: missing `<MAPS_API_KEY>` substitution; a verification-only placeholder allowed the same revision to pass build, JVM tests, and Android test compilation.
- Next action: ensure verification environments provide a non-production placeholder or approved local Maps setting without committing credentials.

## ISS-feat-0923-003

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P2
- Title: CI result unavailable
- Status: open
- Impact: CI gate cannot be marked PASS and Release cannot advance.
- Evidence: no CI workflow result or CI artifact is present for `692991ca361b7fec07e117fde01a3659e337a4b5`.
- Next action: infrastructure — provide or run CI for the fixed revision.

## ISS-feat-0923-004

- Task: `feat-0923`
- Phase: debug
- Category: `verification_failure`
- Priority: P1
- Title: New submitted-draft flow has no verification evidence on its implementation revision
- Status: open
- Impact: AC-006 and AC-007 cannot be accepted or rejected; Release remains blocked.
- Evidence: `verification.md` tests `692991c`, while the submitted read-only/re-upload implementation is in `9524e03`; the recorded 51 connected tests do not cover the new flow.
- Root cause: verification was not rerun against the new implementation revision, so the current feedback is an evidence gap rather than a reproduced product failure.
- Next action: infrastructure/verification — run targeted checks against fixed commit `9524e03`; only route back to implementation debug if a concrete behavior failure is reproduced.
