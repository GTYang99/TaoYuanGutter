# Execution Report

## Implementation

- Added process-local API targets for base, Taipei, and DEMO; default remains Taipei.
- `GutterApiClient` now returns a Retrofit service for the selected target and rejects target changes when the debug gate is false.
- `GutterRepository` now resolves its service per API call so existing repositories use subsequent target changes.
- Added a login-page environment control in `activity_login.xml`, visible only when `ENABLE_GROUP_SIMULATION` is true. The control is absent from the authenticated map UI.
- WMS/WMTS endpoints remain unchanged. No automatic target fallback was added.

## Login-Page Correction Revision

- Branch: `codex/debug-0930-1`
- Commit: `947c8208477130f32c5623c97d59ea593d72ef6d`
- The previous verification record is scoped to commit `397ebd7d5cd76f3ce1286aba279a58cafd4528f9`; it does not verify the login-page correction. The current revision remains pending a fresh verification.

## Developer Validation

| Check | Result | Evidence |
|---|---|---|
| `:app:testDebugUnitTest --tests BackendEndpointsTest --tests GutterApiClientEnvironmentTest` | PASS | Gradle `BUILD SUCCESSFUL`; base/Taipei/DEMO mapping and debug client selection tests passed. |
| `:app:assembleDebug` | PASS | Debug APK assembled successfully. |
| `:app:compileDebugAndroidTestKotlin` | PASS | Instrumentation sources compile, including gate/target selection assertion. |
| `:app:compileReleaseKotlin` | PASS | Release source compiles with the gate disabled by `BuildConfig.DEBUG`. |
| `git diff --check` | PASS | No whitespace errors in tracked source diff. |
| `:app:assembleDebug` after moving the selector to LoginActivity | PASS | Updated login layout binding and Kotlin compile; `BUILD SUCCESSFUL` on 2026-09-30. |
| `:app:assembleDebug :app:assembleRelease :app:testDebugUnitTest --tests BackendEndpointsTest --tests GutterApiClientEnvironmentTest :app:compileDebugAndroidTestKotlin` after the Taipei label correction | PASS | Debug/release APKs assembled, targeted debug tests and instrumentation source compilation completed; `BUILD SUCCESSFUL`. |
| Emulator login selector check | PASS | Android 14 emulator (`emulator-5554`): selector visible as `API：台北`; dialog offered base/Taipei/DEMO; selecting base updated it to `API：內網 BASE`; force-stop/relaunch reset it to Taipei. No backend request was made. |
| `:app:testReleaseUnitTest --tests GutterApiClientEnvironmentTest` | NOT AVAILABLE | The project has no `testReleaseUnitTest` Gradle task. Release gate evidence is `BuildConfig.DEBUG=false`, the source guard, and successful `:app:assembleRelease`. |
| CI workflow local parity | PASS | The exact task-scoped JVM test classes and `:app:assembleDebug` from `.github/workflows/android-ci.yml` passed locally. |
| Remote CI result | NOT VERIFIED | CI has not run on the remote branch. |
| Runtime UI verification | PASS | Scoped login-screen selector and process-reset behavior verified on Android 14 emulator. |
| Live API write | NOT RUN | No backend data was written. |

## Environment Note

The first Gradle invocation could not process the manifest because the worktree had no Maps API placeholder. A gitignored `local.properties` with `MAPS_API_KEY=debug-placeholder-not-a-real-key` was created in this worktree only. The subsequent validation passed; no user key was read or copied.

## Remaining Release Gates

- Commit the login-page selector correction on `codex/debug-0930-1`.
- Independent Verification and CI remain pending.
