# Implementation and Developer Validation Report

## Revision and Environment

- Task: `feat-1013-app-version-check`
- Branch: `codex/feat-1013-app-version-check-implementation`
- Implementation commit: `30ac2b7` (`feat(feat-1013-app-version-check): add optional APK updater`)
- Package: `com.example.taoyuangutter`
- Test API: fixed test environment endpoint from the approved requirement; live response was sanitized and contained one Android record at version `1.0.0` with an HTTPS APK URL.
- Device: Android emulator `emulator-5554`, Medium_Phone (AVD), Android 16 / API 36. No physical device was used.
- Signing material: external JKS and ignored local configuration were used. No signing values are recorded here or tracked.

## Acceptance Criteria Evidence

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Started from P0=1. An injected Release dependency failure and a Debug build left P0 unchanged. Successful signed Release builds produced APK version codes 2 and 3 in sequence and left P0 at 2 then 3. P1/P2/P3 stayed 1/0/0. APK manifest metadata was inspected with `aapt`. |
| AC-002 | PASS | `AppVersionModelsTest` covers strict three-component numeric versions and numeric ordering, including `1.10.0` vs `1.9.9`; focused JVM suite passed. |
| AC-003 | PASS | Repository/model JVM tests passed; live test endpoint returned the approved single Android record. The service uses a parameter-free endpoint rooted at the fixed test BASE URL, independent of debug API target selection. |
| AC-004 | PASS | Emulator instrumentation verified cold-start/foreground request cadence without duplicate calls across internal Activity transitions and verified no immediate repeat on the tested return path. |
| AC-005 | NOT VERIFIED | Instrumentation verified Continue dismisses the prompt while preserving the Login screen and entered UI state. Map/form screen preservation and equal/older/no-update presentation cases were not exercised on-device. |
| AC-006 | NOT VERIFIED | Instrumentation verified that canceling a download returns without a failure prompt. The full visible-progress and failure/retry/continue interaction was not exercised end to end. |
| AC-007 | NOT VERIFIED | Installer and install-source permission/settings resume behavior is implemented but was not exercised end to end on the emulator. |
| AC-008 | PASS | Signed Release APKs at codes 2 and 3 reported the same signer SHA-256. The emulator accepted code 2 and then `adb install -r` updated it to code 3; `dumpsys package` reported versionCode 3. The signing material stayed outside tracked files and logs. |
| AC-009 | NOT VERIFIED | Unit coverage verifies malformed response/version/URL rejection and fail-open repository behavior. Full emulator usability under network outage, HTTP redirect downgrade, and failing APK download was not exercised. |
| AC-010 | PASS | Service construction is fixed to the test BASE endpoint rather than selected login backend; live request and repository tests confirmed the version contract. Production endpoint is still a release prerequisite. |
| AC-011 | NOT VERIFIED | Sequential successful builds proved monotonic code 2 then 3, but the specifically planned rollback simulation was not run. The rollback rule is documented in the approved plan. |

## Commands and Results

- `./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.api.AppVersionModelsTest' --tests 'com.example.taoyuangutter.api.AppVersionRepositoryTest' :app:assembleDebug` — PASS.
- `./gradlew --no-daemon :app:assembleDebugAndroidTest` — PASS.
- `ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AppVersionUpdateFlowTest` — PASS; 3 tests, 0 failures.
- `./gradlew --no-daemon :app:assembleRelease` from an isolated task copy with an injected dependency failure — expected failure; P0 remained 1.
- `./gradlew --no-daemon :app:assembleDebug` from that isolated task copy — PASS; P0 remained 1.
- `./gradlew --no-daemon :app:assembleRelease` from the isolated task copy and implementation worktree — PASS; code 2/P0=2, followed by code 3/P0=3. P1/P2/P3 unchanged.
- `aapt dump badging` and `apksigner verify --print-certs` — PASS; codes 2 and 3, same signer SHA-256.
- Emulator `adb install` of code 2, followed by `adb install -r` of code 3 — PASS; installed package reports versionCode 3.
- `git diff --check` on implementation source changes — PASS. The staged approved `plan-review.md` retains its original trailing spaces; a whole-index diff check reports those pre-existing review-artifact formatting lines.

## Build Artifact

- Variant: Release
- APK: `app/build/outputs/apk/release/app-release.apk`
- Manifest: `com.example.taoyuangutter`, versionName `1.0.0`, versionCode `3`
- The APK is a local validation artifact and is not added to Git.

## CI and Release Gates

- CI: NOT VERIFIED. No CI workflow run was started from this local implementation branch.
- Production endpoint: NOT VERIFIED. The approved production URL has not been supplied; the test endpoint must be replaced and verified before production release.
- Independent verification and release decision remain pending.

## Worktree and Secret Handling

- Signing configuration is supplied via ignored `local.properties`; the JKS remains outside the repository.
- No credential values were printed or committed.
- The implementation branch was isolated from the unrelated primary checkout.

## Current HEAD Revalidation (2026-10-14)

- Revision inspected: `2b7d8ba3f27dbfebc3ee7a9004e0f752f6853fca` on `codex/feat-1013-app-version-check-implementation`.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AppVersionUpdateFlowTest` — PASS; Medium Phone AVD / Android 16, 3 tests, 0 failures, 0 errors, 0 skipped. No physical device was targeted.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug` — PASS.
- Android Studio Run configuration installed and launched the Debug app on `emulator-5554`. Cold launch displayed「發現新版本 1.0.0」over Login; tapping「繼續使用」dismissed the dialog and left the same Login screen visible.
- `git diff --check` — PASS. The Debug build and instrumentation run left `app/version.properties` unchanged.
- GitHub Actions Android CI run #12 for exact HEAD `2b7d8ba3f27dbfebc3ee7a9004e0f752f6853fca` — PASS.
- Independent verification of this HEAD found a separate release-counter regression: P0 was reset from 3 to 0 while prior task evidence records Release artifacts through versionCode 5. This fails AC-011; AC-001 and AC-008 remain NOT VERIFIED for this exact version configuration. See `root-cause.md` and `fix-plan.md`. Do not use the preceding PASS evidence on commit `02ad7e5` as verification of the current HEAD.

## AC-011 Debug Fix Validation (2026-10-14)

- Restored `P0_VERSION_CODE=5`, retaining the highest successful Release APK code recorded in the task evidence. The three manual version-name components remain `0/0/0`.
- `git diff --check` — PASS.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug` — PASS.
- `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` — package `com.example.taoyuangutter`, Debug APK `versionCode=6`, `versionName=0.0.0`.
- Re-read `app/version.properties` after Debug build — P0 remained 5 and P1/P2/P3 remained 0/0/0.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AppVersionUpdateFlowTest` — PASS; Android 16 Medium Phone AVD, 3 tests, 0 failures, 0 errors, 0 skipped. No physical device was targeted.
- No Release build was run because a successful Release assembly consumes its version code and updates P0; the next Release must be intentionally built from P0=5 and will use code 6.
- CI and independent Verification are pending for the resulting fix commit.
