# Implementation Execution Report

## Task

- Task: `feat-0923`
- Branch: `feat/草稿tag`
- Baseline commit: `692991c`
- New implementation commit: pending
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
| JVM tests | PASS | `./gradlew :app:testDebugUnitTest`; 124 tests completed successfully |
| Debug APK | PASS | `./gradlew :app:assembleDebug`; [app-debug.apk](/Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/build/outputs/apk/debug/app-debug.apk) |
| Android test compilation | PASS | `./gradlew :app:compileDebugAndroidTestKotlin` |
| Source diff whitespace check | PASS | `git diff --check -- app/src/main app/src/test` |
| Full diff whitespace check | NOT VERIFIED | Existing task-document trailing whitespace remains in `docs/tasks/feat-0923/plan-review.md`; no source/test whitespace errors |
| Connected/device tests | NOT VERIFIED | `adb devices` could not start the ADB daemon (`Operation not permitted`); no device serial/model/Android version was available |

## Worktree Safety

- Preserved the pre-existing `AddGutterBottomSheet.kt` message change.
- Preserved the pre-existing `gradle/libs.versions.toml` AGP change.
- Preserved untracked `.worktrees/` content.
- No API payload or third-party dependency was changed.

## Handoff

- APK variant: `debug`
- APK path: `/Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/build/outputs/apk/debug/app-debug.apk`
- Application package: `com.example.taoyuangutter`
- Physical-device verification remains required for AC-001 through AC-004.
