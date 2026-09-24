# Implementation Execution Report

## Task

- Task: `feat-0923`
- Branch: `feat/草稿tag`
- Baseline commit: `9524e03`
- New implementation commit: `452915a`
- Package: `com.example.taoyuangutter`

## Implementation Completed

- Added `hasSubmittedStoreDitch` to the Gson draft model and Room entity.
- Added Room `MIGRATION_3_4`; legacy rows default to `false`.
- Preserved `true` through later auto-save/upsert operations.
- Added a shared `GutterRepository.storeDitch` method-entry boundary. The local marker completes before logging or Retrofit invocation; marker failure returns a local error and prevents the request.
- Connected both add and SPI_NUM edit flows to the boundary. Direct inspect-edit without a pending draft id remains a no-op and does not create a tagged draft.
- Added submitted/unsubmitted pending-list tags and existing-`SPI_NUM` hiding policy.
- Added JVM boundary/state/serialization tests and Android migration/UI tests.

## New Work Item: Submitted Draft Read-Only Resume and Re-upload

- Submitted ordinary drafts now resume from the Room snapshot by draft ID and enter a read-only form flow.
- The editable map form and waypoint editor are covered by a read-only overlay; back navigation remains available.
- Re-upload rereads the complete snapshot from Room, including all waypoints, coordinates, fields, photos, attachments, type, virtual/import state, and metadata.
- Successful re-upload reuses the existing success cleanup path; failed, timed-out, or interrupted uploads leave the submitted draft retryable.
- Submitted form launches no longer carry the full waypoint snapshot or full basic-data payload through Intent or saved instance state.
- Added `SubmittedDraftResumePolicyTest` for ordinary, editable, and existing-`SPI_NUM` routing.

## Validation

| Check | Result | Evidence |
|---|---|---|
| Kotlin compile | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin` |
| JVM tests | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest`; 124 tests, 0 failures |
| Debug APK | PASS | `./gradlew :app:assembleDebug`; [app-debug.apk](/Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/build/outputs/apk/debug/app-debug.apk) |
| Android test compilation | PASS | `./gradlew :app:compileDebugAndroidTestKotlin` |
| Source diff whitespace check | PASS | `git diff --check -- app/src/main app/src/test` |
| Full diff whitespace check | NOT VERIFIED | Existing task-document trailing whitespace remains in `docs/tasks/feat-0923/plan-review.md`; no source/test whitespace errors |
| Targeted submitted read-only instrumentation | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.SubmittedDraftReadOnlyUiTest`; 1 test, 0 failures on `emulator-5554` / Android 14 |
| Full connected/device tests | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:connectedDebugAndroidTest`; 52 tests, 0 failures, 0 errors, 0 skipped on `Medium_Phone(AVD)` / Android 14 |

## Worktree Safety

- Preserved the pre-existing `AddGutterBottomSheet.kt` message change.
- Preserved the pre-existing `gradle/libs.versions.toml` AGP change.
- Preserved untracked `.worktrees/` content.
- No API payload or third-party dependency was changed.

## Handoff

- APK variant: `debug`
- APK path: `/Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/build/outputs/apk/debug/app-debug.apk`
- Application package: `com.example.taoyuangutter`
- Independent Verification remains required for AC-007 end-to-end retry behavior and CI evidence; developer validation does not substitute for that gate.

## Follow-up Debug Re-Implementation: Submitted Retry Isolation

- Fix commit: `ee55f08`.
- Added an immutable Room-sourced submitted retry snapshot and separate transport projections.
- Submitted retry photo upload no longer propagates mutable waypoint updates into the form auto-save callback; successful upload metadata remains in the request projection.
- Added focused unit coverage for snapshot deep-copy behavior and submitted callback suppression.

### Minimum Validation

| Check | Result | Evidence |
|---|---|---|
| JVM unit tests | PASS | `:app:testDebugUnitTest`; 126 tests, 0 failures |
| Submitted read-only instrumentation | PASS | `SubmittedDraftReadOnlyUiTest` targeted run |
| Existing shell regression retry | PASS | `MainShellActivityTest`; 12/12 after one first-run focus timing failure |
| Full connected suite | LIMITED | First run had 1 unrelated `RootViewWithoutFocusException`; no further broad rerun under the 10-minute minimum-test scope |
| CI | NOT VERIFIED | No CI result available |

Independent Verification remains required for AC-007 end-to-end retry/no-write behavior and all uncovered acceptance criteria.

## Follow-up UI Bug Fix: Pending Draft Subtitle Layout

- Fix commit: `e595311`.
- Changed the pending-row subtitle anchor from nested `tvPendingDraftTitle` to the direct-child `layoutPendingDraftTitle` container.
- Added a measured-layout regression test covering title row → time subtitle → node-count subtitle ordering.
- Targeted Android test: `PendingDraftAdapterUiTest`, 3/3 PASS on Android 14 `Medium_Phone(AVD)`.
- Independent Verification should target `e595311`, since it is now the latest implementation revision.

## New Feature: Existing-Gutter Draft Tag Policy

- Implementation commit: `f8f40fe`.
- Added `EXISTING_GUTTER` as the unique higher-precedence tag for drafts whose START waypoint has `SPI_NUM.trim().isNotEmpty()`.
- Reused `PendingDraftTagPolicy.hasValidSpiNum` for submitted read-only routing, list title, and delete confirmation identity.
- Existing-gutter drafts remain editable and use the existing resubmit flow; general submitted drafts retain read-only behavior.
- Added JVM boundary and cross-policy coverage for valid, empty, whitespace-only, missing-START, and non-START-only `SPI_NUM` cases.
- Added Android assertions for the new tag text/style and single-tag rendering.

### Developer Validation

| Check | Result | Evidence |
|---|---|---|
| JVM unit tests | PASS | `:app:testDebugUnitTest`; 130 tests, 0 failures, 0 errors |
| Pending draft Android UI tests | PASS | `PendingDraftAdapterUiTest`; 3/3 on Android 14 `Medium_Phone(AVD)` |
| Android build/test compilation | PASS | Targeted `:app:connectedDebugAndroidTest` completed successfully and packaged the debug app/tests |
| Source diff whitespace check | PASS | `git diff --check -- app/src/main app/src/test app/src/androidTest` |
| CI | NOT VERIFIED | No CI result available |

Independent Verification remains required for AC-003, AC-004, AC-008 runtime flows and the existing task acceptance criteria.
