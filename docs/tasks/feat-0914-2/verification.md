# Verification Report

## Revision and scope

- Branch: `feat/側溝清單`
- Verification revision: `2d8f544`
- Pre-existing, unrelated working-tree changes were excluded from this verification: `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` and `app/src/main/res/values/strings.xml`.

## Executed evidence

| Check | Result | Evidence |
|---|---|---|
| Debug Kotlin compile, unit tests, and APK build | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon` completed successfully: 50 tasks, 7 executed. |
| Targeted Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`; result XML recorded 8 tests, 0 failures, 0 errors, 0 skipped on `Medium_Phone(AVD) - 14`. |
| Full connected Android instrumentation | NOT VERIFIED | The command was invoked against both connected devices, but this verification session did not retain a final Gradle result or result XML sufficient to record it as PASS. |
| Whitespace check | PASS | `git diff --check` produced no output. |
| Session-isolation regression | PASS | Single session-isolation instrumentation test passed on both connected devices; fresh coordinators are empty and explicit active IDs restore only selected rows. |
| CI | NOT VERIFIED | No CI configuration or CI result is available in the repository/current session. |

## Implementation and plan conformance

`MultiGutterSessionCoordinator` initializes its item list by reading every persisted `MULTI_GUTTER` draft. This directly conflicts with approved plan step 1: configuration recreation must use the active session's saved ID list, while cold start/process death must not mix all pending drafts into a new list. The existing instrumentation test asserts this conflicting reload behavior rather than protecting the required boundary.

This is tracked as `ISS-FEAT-0914-2-007` and is an implementation failure. No production code was changed during verification.

## Acceptance criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | NOT VERIFIED | Targeted list-sheet evidence exists, but the authenticated `MapWorkspaceFragment` entry flow was not exercised. |
| AC-002 | NOT VERIFIED | The targeted suite covers list controls and draft IDs, not the formal map add/select/edit/return journey. |
| AC-003 | NOT VERIFIED | Targeted evidence covers formatting and list geometry, but no full multi-row selection journey was executed. |
| AC-004 | NOT VERIFIED | Targeted close-dialog controls pass; end-to-end map-session close/save branching was not executed. |
| AC-005 | PASS (targeted) | New coordinators start empty; configuration-style restoration uses only the saved active ID list. Full authenticated process-recreation smoke remains unavailable. |
| AC-006 | PASS (source/targeted) | Current list membership is explicitly session-scoped, so successful cleanup can only target active-session IDs. Full upload smoke remains unavailable. |
| AC-007 | NOT VERIFIED | Build, unit tests, and targeted instrumentation pass, but full regression evidence is incomplete. |
| AC-008 | NOT VERIFIED | Failure Alert → return to list → edit/retry was not executed end-to-end. |

## Regression review

- Build and unit-test evidence passed.
- Targeted instrumentation passed 8 tests with no failures.
- The session-isolation regression is confirmed by source inspection and by a test whose expected behavior conflicts with the approved plan.
- The authenticated map smoke and CI evidence remain unavailable.

## Result and route

**NOT VERIFIED** — the implementation regression is fixed and targeted session-isolation evidence passes, but full connected evidence, authenticated map smoke, and CI remain unavailable.

Next action: repeat full verification on revision `2d8f544` when complete connected and authenticated map evidence are available.
