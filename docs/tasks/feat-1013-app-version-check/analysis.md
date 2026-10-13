# Repository Analysis

## Task Classification
- Type: `feature` — add optional app-version checking and an APK update/install flow, plus release version-code management and signing.

## Current Behavior
- `app/build.gradle.kts` reads `versionCode` and `versionName` from `app/version.properties`. Current values are code 1 and name 1.0.0; no task changes either value during Release builds.
- `GutterApiService` and `GutterRepository` provide the existing Retrofit/network pattern. `GutterApiClient` selects BASE/TAIPEI/DEMO in debug, and `BASE_API_URL` is already the requested test server.
- `LoginActivity` is the launcher. The app has multiple Activities but no central process-foreground update check, APK downloader, install permission, or release signing configuration.
- Android network security already permits HTTP only to the specified test IP. The existing FileProvider can share internal files but has no dedicated update-cache path.
- CI runs a scoped JVM test selection and a debug build only. No app-version endpoint or update tests exist.

## Expected Behavior
- A successful release assembly emits the next monotonic version code and persists only `P0_VERSION_CODE`; manually maintained P1/P2/P3 values continue to drive the three-part name. Rollback of the optional updater must retain the Gradle version-code calculation/persistence and signing configuration as release infrastructure, plus the consumed P0 value; reverting only to the repository baseline would remove the increment mechanism.
- The app checks the fixed BASE test endpoint on cold start and each normal process foreground entry. Before prompting, it accepts only a version with exactly three numeric components and a valid HTTPS APK URL, and offers an update only when that numeric version is newer than the installed version name. APK download redirects must remain HTTPS.
- The optional update path downloads an APK, reports progress, permits cancellation/retry, and hands installation to Android with the same signing key; failures preserve the current screen and app use.

## Affected Modules
- `app/build.gradle.kts`, `app/version.properties`, and local signing configuration — Release-only next-code generation/persistence and JKS-backed release signing.
- `app/src/main/java/com/example/taoyuangutter/api/` — app-version response model, no-parameter endpoint, and BASE-target repository access.
- New app-level application/lifecycle coordinator and update-flow classes — foreground checks, active-Activity dialog, download, permission return, and installer handoff.
- `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml`, and `app/src/main/res/values/strings.xml` — lifecycle registration, install permission, update-cache FileProvider access, and user-visible copy.
- `app/src/test/` and `app/src/androidTest/` — comparator, repository, lifecycle, dialog, download, install handoff, and signing/update coverage.
- `.github/workflows/android-ci.yml` — ensure new focused JVM tests are included in task-scoped CI.

## Dependencies
- Test backend endpoint and response contract supplied by the user.
- Android platform package-installer flow and per-source unknown-app permission.
- The user-provided JKS and local alias/password configuration for signed update artifacts.
- Existing Retrofit/OkHttp/Gson, coroutine support, FileProvider, and debug target routing.
- Android emulator with access to the test backend for runtime checks.

## Risks
- Release version values are read during Gradle configuration. Set the Release variant's code to current P0 + 1 and persist that exact value only in an action on the `:app:assembleRelease` lifecycle task, which runs after dependencies succeed. Do not use a finalizer because it may run after failure.
- Keep version-code management and release signing as a distinct rollback boundary from the optional app updater. A rollback may remove the updater flow but must retain those release controls and the consumed P0 value so the next release remains monotonic.
- Lifecycle detection must avoid duplicate requests across internal Activities and avoid an immediate re-prompt on return from installer/settings.
- APK update installation fails if the package name, version code, or signer does not match the installed package; signing values must remain outside tracked files.
- Android can require an explicit settings grant and user confirmation for sideloaded APKs; silent install is not part of the approved behavior.
- The test API is HTTP and LAN-specific. Runtime update checks are not verified if the emulator/device cannot reach the test network; failures must remain quiet.
- The production endpoint is not yet provided and is a gate before production release.

## Unknowns
- The exact local signing property names/credentials are not in tracked files. Implementation should reuse existing local configuration if present and ask only if required values are absent; never print or commit them.
- Validate the `:app:assembleRelease` lifecycle-task action against this project's AGP 9.4.1 task graph: successful assembly persists the value embedded in the APK; a failed dependency prevents the persistence action from running.
- Android installer/permission behavior must be verified on an Android 9+ emulator using APKs signed by the provided JKS.
