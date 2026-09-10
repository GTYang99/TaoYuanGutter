# Issue Log

## ISS-0910-2-01

- Task: feat-0910-2
- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Title: Android validation environment has no Java runtime
- Impact: Gradle compilation, unit tests, Android UI tests, build, and lint cannot be executed locally.
- Evidence: `./gradlew compileDebugAndroidTestKotlin --no-daemon` stopped with `Unable to locate a Java Runtime`.
- Next action: verification

## ISS-0910-2-02

- Task: feat-0910-2
- Phase: verification
- Category: unknown
- Priority: P0
- Status: open
- Title: User reports real-device crash in the form flow
- Impact: Core form validation is blocked and release readiness cannot be established.
- Evidence: User report on 2026-09-10; captured logcat did not include the crash stack trace.
- Repro steps: Open the form on the affected real device and reproduce the crash; capture logcat immediately.
- Next action: investigation
