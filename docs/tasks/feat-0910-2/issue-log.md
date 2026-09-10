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
