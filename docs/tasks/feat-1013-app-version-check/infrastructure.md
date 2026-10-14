# Infrastructure Resolution

## Scope

- Task: `feat-1013-app-version-check`
- Revision under verification: `02ad7e5370a9a103182b0cb908e1175e39803248`
- Branch: `codex/feat-1013-app-version-check-implementation`
- Device scope: only `emulator-5554` (`sdk_gphone16k_arm64`, API 36)
- No production source, committed test source, requirement, or plan was changed.

## Gradle and Java

- The shell did not have a Java runtime on `PATH`. The Android Studio JBR launched the Gradle wrapper; the project wrapper selected its configured Java 21 daemon/toolchain.
- Isolated Release assemblies completed for code 4 and code 5. The exact JVM-test selection and `:app:assembleDebug` from `.github/workflows/android-ci.yml` also passed locally on the isolated fixed revision.
- Local workflow success is not a substitute for a GitHub Actions run.

## Signing and AC-008

- The checkout's `local.properties` did not contain `UPDATE_*` signing values, but the provided JKS and its separate credential note were available outside Git.
- A mode-0600 temporary signing configuration was created only inside `/private/tmp` build copies. Credential values were kept out of terminal output and task artifacts.
- `keytool` reported certificate SHA-256 `00ffce0671b6bc9b857bdcd6f97d7884199f600ead66108ba0108e0a3598b386`; `apksigner` reported the same fingerprint for the isolated code-4 and code-5 Release APKs.
- On `emulator-5554`, a separate AVD snapshot was saved. The existing debug-signed package was uninstalled, the code-4 JKS-signed APK was installed, and `adb install -r` updated it to code 5. Package inspection confirmed 4 then 5.
- The saved snapshot was loaded afterward. Read-only inspection confirmed the original versionCode 4 / versionName 1.0.0 APK, original certificate fingerprint, and `REQUEST_INSTALL_PACKAGES=default` were restored.

## AC-004 and AC-009 Evidence

- AC-004: a temporary instrumentation case in an isolated copy used the persisted installer-return suppression flags written by `launchInstaller`. The immediate return made zero repository calls; the next ordinary foreground return made exactly one. The actual Android settings/installer return and cancellation had already been observed on the same emulator under AC-007.
- AC-009: a temporary host-side HTTPS server redirected to a local HTTP target while exercising the `AppUpdateDownloader` source from the fixed revision. The downloader raised the HTTPS-only redirect exception, the HTTP target saw zero requests, and no APK was created. The harness source and generated certificate existed only under `/private/tmp`.

## Remote CI Result

- With the user's authorization, exact commit `02ad7e5370a9a103182b0cb908e1175e39803248` was pushed to `origin` branch `codex/feat-1013-app-version-check-implementation`.
- GitHub Actions [Android CI run #11](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38355107296) completed with conclusion `success`. The `Unit tests and debug build` job passed both the task-scoped JVM test step and `:app:assembleDebug` step.
- The remote CI blocker is resolved. Merge remains outside this verification task.
