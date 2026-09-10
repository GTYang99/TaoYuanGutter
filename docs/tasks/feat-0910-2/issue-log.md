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
- Phase: debug
- Category: `implementation_regression`
- Priority: P0
- Status: open
- Title: Form entry crashes during runtime View reordering
- Impact: Core form validation is blocked and release readiness cannot be established.
- Evidence: Real-device logcat at 2026-09-10 16:12:01: `IllegalStateException: The specified child already has a parent` at `GutterBasicInfoFragment.reorderEditableSections(GutterBasicInfoFragment.kt:511)`, called from `onViewCreated()`.
- Root cause: nested Views remain attached to their original parent when `formContent.addView()` is called.
- Repro steps: Open the feat-0910-2 form on Sony XQ-AU52.
- Next action: debug
