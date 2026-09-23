# Issue Log

## ISS-feat-0923-001

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P1
- Title: Android runtime verification unavailable
- Status: open
- Impact: AC-001 through AC-005 cannot be promoted to PASS because instrumentation and physical-device evidence are unavailable.
- Evidence: `adb devices` failed to start the daemon with `could not install *smartsocket* listener: Operation not permitted`.
- Root cause: host-level ADB permission/socket failure; no device serial, model, or Android version is available.
- Next action: infrastructure — restore ADB/device access and rerun the committed revision.

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
