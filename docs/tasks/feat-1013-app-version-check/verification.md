# Historical Independent Verification — `02ad7e5370a9a103182b0cb908e1175e39803248`

## Revision and Environment

- Task: `feat-1013-app-version-check`
- Revision under test: `02ad7e5370a9a103182b0cb908e1175e39803248`
- Source: clean archive of the committed revision; release/build scenarios used separate isolated copies.
- Original checkout: `codex/feat-1013-app-version-check-implementation`; the pre-existing uncommitted `app/version.properties` edit was preserved and excluded from all test copies.
- App runtime and live API checks ran only on test emulator `emulator-5554` (`sdk_gphone16k_arm64`, API 36), package `com.example.taoyuangutter`. The connected physical device was not used.
- Gradle and JVM tasks ran on the host against isolated copies of the fixed revision; no other Android device was targeted.
- Final read-only state check on `emulator-5554`: model `sdk_gphone16k_arm64`, API 36; package `com.example.taoyuangutter` installed at versionCode 4 / versionName 1.0.0; Wi-Fi and mobile data enabled, airplane mode off, and fine-location permission denied. No other device serial was used.
- Signing setup: the checkout's `local.properties` has no `UPDATE_*` properties. The user-provided JKS was found outside Git and used only in a mode-0600 temporary `local.properties` inside isolated build copies. `keytool` and `apksigner` reported the same SHA-256 certificate fingerprint (`00ffce0671b6bc9b857bdcd6f97d7884199f600ead66108ba0108e0a3598b386`) for the JKS and both update APKs. No credential values were recorded.
- The user unlocked the Mac for the follow-up UI checks. Installer and Settings interactions were performed only in the Android Studio emulator `emulator-5554`.
- For AC-007 and AC-008, installer/settings/update interactions were performed only on `emulator-5554`. An AVD snapshot was saved before changing the APK signer and restored afterward. Final read-only package inspection confirmed the original versionCode 4 / versionName 1.0.0 APK and certificate were restored; `REQUEST_INSTALL_PACKAGES` is `default`.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | In an isolated copy initialized at P0=1, an injected `:app:assembleRelease` dependency failure returned nonzero and left P0=1. `:app:assembleDebug` passed and left P0=1. A successful signed Release produced package `com.example.taoyuangutter`, versionCode 2, versionName 1.0.0, and P0=2; the next successful Release produced versionCode 3 and P0=3. `aapt dump badging` and `apksigner verify` passed. P1/P2/P3 remained 1/0/0. A disposable test-only signer was used because the required external JKS was unavailable; real-key compatibility is covered separately by AC-008. |
| AC-002 | PASS | `AppVersionModelsTest` passed, including numeric `1.10.0 > 1.9.9`, equality, malformed components, and integer overflow rejection. |
| AC-003 | PASS | `GutterApiService.getAppVersion()` is `@GET("api/appVersion")` with no query/body parameters; `versionCheckService()` uses the fixed version-check BASE URL. Repository/model JVM tests passed. A parameter-free HTTP GET from the emulator returned HTTP 200, `success=true`, one `type=android` record, version `1.0.0`, and an HTTPS URL; the full URL was suppressed. |
| AC-004 | PASS | `AppVersionUpdateFlowTest.foregroundChecksOnceAcrossActivityRecreationAndAgainAfterBackgroundReturn` passed on `emulator-5554`: one cold launch check, no extra call on Activity recreation, and one call after an ordinary background/foreground return. A temporary instrumentation case in the isolated fixed-revision copy set the same persisted flags as `launchInstaller`: the immediate return made 0 repository calls; the next normal foreground return made exactly 1. The actual Settings grant, automatic installer resume, and installer-cancel return were observed under AC-007. |
| AC-005 | PASS | The Login-screen prompt/Continue instrumentation test passed on its single-test retry and confirmed both actions and preserved entered fields. The first class run failed at the request-start latch in this case; the one allowed isolated retry passed. On the emulator, the live equal-version (`1.0.0`) response left Login visible with no prompt. A temporary versionName `2.0.0` build with the live `1.0.0` API response also left Login visible. A temporary versionName `0.0.0` build displayed「發現新版本 1.0.0」and both expected buttons on Login; after Continue, Login remained visible. The same newer-version prompt was then shown over the existing offline `MainActivity` map and open「離線草稿」form sheet after an actual task background/Recents return; Continue dismissed it while the same MainActivity and form sheet remained visible with its fields and controls. The fixed committed `1.0.0` Debug build was restored afterward. The unavailable-response no-dialog case is covered by the offline smoke evidence under AC-009. |
| AC-006 | PASS | The emulator instrumentation test passed for canceling a download and returning to Login without a failure prompt. A real download-flow UI check on the same emulator showed the download dialog, visible `ProgressBar` node, and「取消下載」action. Wi-Fi/mobile data were then disabled before tapping Go to Update; the attempt failed without downloading an APK and displayed「重試」and「繼續使用」. Continue returned to Login. |
| AC-007 | PASS | On `emulator-5554`, a completed download opened the app-specific “Install unknown apps” settings page while the source permission was off. With the user's approval, the permission was temporarily enabled. Returning to the app automatically resumed the package installer, which displayed Android's “Do you want to update this app?” confirmation with Update and Cancel actions. Cancel returned to Login without an immediate update prompt. The downloaded APK was not installed; the original installed APK and source permission were restored after the check. The subsequent normal foreground request cadence is tracked separately under AC-004. |
| AC-008 | PASS | A code-4 Release APK and code-5 Release APK built from separate isolated copies with the provided JKS had the same signer fingerprint. On `emulator-5554`, after saving a full AVD snapshot, the existing debug-signed package was removed, the JKS-signed code-4 APK was installed, and `adb install -r` updated it to code 5 successfully. Package inspection showed versionCode 4 then 5. Restoring the snapshot returned the emulator to the original code-4 APK, original signer, and `REQUEST_INSTALL_PACKAGES=default`. No key or password values appeared in tracked files or recorded command output. |
| AC-009 | PASS | Existing JVM tests cover malformed/empty/type-invalid records, invalid versions/URLs, and unsuccessful HTTP responses. With Wi-Fi and mobile data disabled before cold start, Login and offline entry remained usable without an update prompt; download failure displayed Retry/Continue and Continue returned to Login. A temporary host-side test against the fixed-revision `AppUpdateDownloader` used a controlled local HTTPS server redirecting to HTTP: the downloader raised `IOException("APK redirect must remain HTTPS")`, the HTTP target received 0 requests, and no destination APK was created. |
| AC-010 | PASS | The repository test selected the DEMO target and verified the version repository still requests BASE; source review confirms the version service uses the fixed test URL. The emulator's parameter-free request reached the test endpoint and returned the expected Android record. |
| AC-011 | PASS | Rollback simulation started from baseline source `13865d875fad6f949695f7d01a626736c41b317d`, retained the task's version-code/signing Gradle configuration and P0=2, then assembled a signed APK at code 3 and persisted P0=3. P1/P2/P3 remained 1/0/0. The artifact used the disposable verification-only signer. |

## Commands and Results

- `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AppVersionUpdateFlowTest` — initial run: 2 passed, 1 failed at the Continue test's request-start latch. The single permitted retry, after force-stopping the app on the same emulator, passed.
- `JAVA_HOME=... ./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.api.AppVersionModelsTest' --tests 'com.example.taoyuangutter.api.AppVersionRepositoryTest'` — PASS.
- `:app:assembleDebug` in isolated P0=1 copy — PASS; P0 remained 1.
- `:app:assembleRelease` with an injected failing dependency — expected failure; P0 remained 1.
- Two sequential signed Release builds in the isolated P0=1 copy — PASS; APK version codes 2 then 3, P0 values 2 then 3, and manual version-name fields unchanged.
- Rollback simulation Release build — PASS; APK versionCode 3 and P0=3 after restoring baseline app sources while retaining the approved Gradle version/signing changes.
- `aapt dump badging` and `apksigner verify` — PASS for inspected isolated Release artifacts.
- JKS-backed isolated Release builds from the committed revision — PASS; code-4 and code-5 APK signer SHA-256 fingerprints matched the provided JKS.
- AC-008 on `emulator-5554`: snapshot current AVD; install JKS-signed code 4; `adb install -r` JKS-signed code 5 — both installs PASS and package metadata advanced 4→5; load saved snapshot — PASS, original code 4/signature and install-source app-op restored.
- AC-004 temporary instrumentation case in an isolated copy — PASS; after setting the persisted installer-return suppression flags, immediate foreground return produced 0 repository calls; the next normal foreground return produced exactly 1. Combined with the actual settings/installer/cancel path recorded under AC-007.
- AC-009 temporary host-side redirect harness — PASS; the exact downloader source from the fixed revision rejected a local HTTPS→HTTP redirect, made no request to the HTTP destination, and created no APK file.
- GitHub Actions [Android CI run #11](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38355107296) for branch `codex/feat-1013-app-version-check-implementation`, commit `02ad7e5370a9a103182b0cb908e1175e39803248` — PASS (`Unit tests and debug build`; task-scoped JVM tests and `:app:assembleDebug` both succeeded). The exact workflow commands also passed locally on the isolated fixed revision.
- Parameter-free GET to `http://192.168.10.84/TY_RSGDBIP/api/appVersion` from `emulator-5554` — HTTP 200; one Android record at `1.0.0`; APK URL scheme HTTPS. The full URL was not recorded.
- Fixed-commit Debug APK installed only on `emulator-5554`; cold launch against the live equal-version response — Login visible, update prompt absent, app versionName 1.0.0.
- Controlled version comparisons on `emulator-5554` — test-only versions 0.0.0 and 2.0.0 against the live 1.0.0 endpoint respectively showed the prompt and no prompt. On 0.0.0, Continue dismissed the prompt on Login, on the offline map, and on the open offline form sheet; UI hierarchy and resumed-Activity checks confirmed screen/state preservation.
- Download failure recovery on `emulator-5554` — with a newer-version prompt already open, Wi-Fi/mobile data were disabled, then Go to Update was tapped. The download dialog exposed a `ProgressBar` and Cancel action; the request failed and exposed Retry/Continue. Continue returned to Login. No APK download or install occurred.
- The initial temporary version fixtures were removed and the fixed committed 1.0.0 Debug APK was reinstalled. Wi-Fi/mobile-data toggles were restored to enabled, airplane mode remained off, and the temporary while-in-use location permission was revoked to restore its original denied state.
- AC-007 follow-up on `emulator-5554`: installed the disposable `0.0.0` verification APK; confirmed the app opened “Install unknown apps” with the toggle initially off; enabled it with user approval; returned to the app and observed automatic handoff to the Android package installer and its Update/Cancel confirmation; selected Cancel; Login returned with no immediate update prompt. A later Home-to-app return showed Login without an update prompt, but the request count was not observable in the UI. Reinstalled `/private/tmp/feat-1013-installer-fixture/original-installed.apk`; package inspection confirmed versionCode 4 / versionName 1.0.0. Restored the install-source app-op to `default` and confirmed it with `appops get`. No downloaded update APK was installed.
- Emulator offline smoke check — Wi-Fi/data values were recorded as enabled, both were disabled, then the app was cold-started. Login and offline-entry UI remained visible with no update prompt. Wi-Fi/data were restored to enabled and airplane mode remained off.

## Implementation and Regression Review

- The committed implementation compiles in the connected instrumentation build and the focused API/model JVM tests pass.
- The version request is separate from the selected login backend and has no request parameters.
- The lifecycle tests cover Activity recreation, ordinary background/foreground cadence, and one-shot suppression after the installer-return flags are set. The actual Settings return automatically resumed the installer and installer cancellation returned to Login without an immediate prompt. The later ordinary return was separately counted by the test hook. The Login Continue path preserved entered UI state on retry.
- A real process background/foreground return through Android Recents brought the same offline MainActivity back and displayed the prompt over both the map and its open draft form sheet; Continue left that Activity and sheet in place.
- The release counter changes only in the `assembleRelease` task's success action; injected failure and Debug build preserved P0. Rollback simulation retained the versioning mechanism and consumed code.
- Offline login entry, map/form state preservation, download failure recovery, permission/settings return, installer cancellation, and same-key in-place update are covered on `emulator-5554`. The CI workflow's exact test/build commands also passed locally on the host.
- The initial instrumentation class run was flaky: the Continue test missed its request-start latch, then passed on the single allowed isolated retry. No implementation change was made during Verification.

## CI, Limitations, and Final Result

- CI build and test: PASS remotely. GitHub Actions run #11 completed successfully for the exact fixed revision; the task-scoped JVM tests and debug APK assembly both succeeded.
- All acceptance criteria have PASS evidence, and the required CI gate passed for the fixed revision.
- Verification issues and their resolution status are recorded in [issue-log.md](issue-log.md).
- No production code, approved requirements, implementation plan, or committed test source was modified during Verification. Temporary test harnesses were used only in isolated copies under `/private/tmp`; task evidence and state were updated in the checkout.

**Historical result for `02ad7e5370a9a103182b0cb908e1175e39803248`: PASS.** The current branch tip is assessed in the re-verification section below.

## Historical Next Action for `02ad7e5370a9a103182b0cb908e1175e39803248`

- Proceed to Release for commit `02ad7e5370a9a103182b0cb908e1175e39803248`. GitHub Actions run #11 passed. Merge remains outside this verification phase.

## Current HEAD Re-verification (2026-10-14)

This section supersedes the historical final result above for the current branch tip. The earlier full flow verification was performed on `02ad7e5370a9a103182b0cb908e1175e39803248`; the application and Gradle implementation are unchanged since that verified source revision. The current verification target is:

- Commit: `04500ab743be646951edd4dba7d083198b98dd96`
- Branch: `codex/feat-1013-app-version-check-implementation`
- Worktree: clean at verification start; branch is ahead of origin by two commits.
- Diff from the AC-011 failed revision `2b7d8ba3f27dbfebc3ee7a9004e0f752f6853fca`: `app/version.properties` restores `P0_VERSION_CODE` from 0 to 5. No production source, Gradle logic, or test source changed. `P1/P2/P3` remain manually configured as `0/0/0`.
- `git diff --check 2b7d8ba..HEAD` — PASS.
- Current Debug validation recorded in the committed execution report: `:app:testDebugUnitTest` and `:app:assembleDebug` passed; APK metadata is `com.example.taoyuangutter`, versionCode 6, versionName 0.0.0. `app/version.properties` remained P0=5 and P1/P2/P3=0/0/0.
- Current emulator instrumentation recorded in the committed execution report: `ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.AppVersionUpdateFlowTest` — PASS; Android 16 Medium Phone AVD, 3 tests, 0 failures, 0 errors, 0 skipped. No physical device was used.
- The app's cold-start update dialog and Continue behavior were manually observed on `emulator-5554` immediately before the P0-only fix. The fix changed no application source. Earlier full emulator scenarios for the unchanged updater implementation are documented above.
- AC-011 fix: prior successful Release evidence recorded consumed codes through 5. Current P0 is restored to 5, and the unchanged Gradle configuration computes the next Release code as P0+1 and persists that value only after successful `assembleRelease`. The fix plan deliberately avoided a Release build in the primary checkout so it would not consume code 6. This restores the monotonic counter invariant; next Release is expected to use code 6.
- CI for this exact revision: **PASS**. GitHub Actions [Android CI run #13](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38504607896) completed successfully for commit `04500ab743be646951edd4dba7d083198b98dd96`.

### Current-HEAD Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | The isolated P0=1 failure/debug/successful sequential Release checks and APK metadata were verified on unchanged Gradle implementation in the earlier fixed-source verification. Current-head Debug build independently confirms P0=5 and P1/P2/P3=0/0/0 are unchanged by a non-Release build. No Release build was run from the current primary checkout because it would consume code 6. |
| AC-002 | PASS | Strict numeric comparator and invalid-input cases passed in the focused JVM suite; implementation/test source unchanged. |
| AC-003 | PASS | Parameter-free fixed endpoint and BASE routing were verified by repository tests and sanitized live endpoint evidence; implementation/test source unchanged. |
| AC-004 | PASS | Emulator lifecycle instrumentation and installer-return checks passed on the unchanged implementation; current-head connected instrumentation also passed 3/3. |
| AC-005 | PASS | Prior emulator checks covered newer/equal/older responses, dialog actions, and Login/map/form state preservation. Current pre-fix launch on `emulator-5554` showed the 1.0.0 prompt and Continue preserved Login; the P0-only fix changes no app source. |
| AC-006 | PASS | Emulator cancellation, visible progress, failure, retry, and continue scenarios passed on unchanged updater source; current-head cancellation instrumentation passed. |
| AC-007 | PASS | Same-emulator Settings permission return, installer handoff, confirmation, and cancellation scenarios passed on unchanged updater source. |
| AC-008 | PASS | Same-JKS code-4 to code-5 in-place update and signer fingerprint checks passed in the previous full verification. Signing configuration is unchanged; current P0=5 makes the next Release code 6. No new Release artifact was generated. |
| AC-009 | PASS | JVM invalid-response/URL tests, offline emulator usability, failing-download recovery, and HTTPS-to-HTTP redirect rejection passed on unchanged implementation. |
| AC-010 | PASS | Fixed test endpoint independence from selected debug target was covered by repository and live endpoint evidence; implementation source unchanged. |
| AC-011 | PASS | The failed revision reset P0 below consumed code 5. Current HEAD restores P0=5; Gradle's unchanged `P0+1` calculation yields next code 6. Current-head Debug validation confirms no decrement/mutation of P0. |

### Current Result

**PASS** — all acceptance criteria have PASS evidence, including CI run #13 for this exact revision. Release may proceed under the existing release rules; no merge or deployment is authorized by this verification.

CI build and test: PASS (GitHub Actions run #13). The historical CI limitation above has been resolved for this exact HEAD.
