# Knowledge Resolution

## Sources Reviewed
- User-approved task request and decisions in this conversation (version properties, API contract, lifecycle, optional update UX, signing key, and test environment).
- Repository baseline: branch `codex/debug-1012`, commit `13865d875fad6f949695f7d01a626736c41b317d`; its `docs/tasks/debug-1012/state.yaml` records verification passed and release pending.
- `app/build.gradle.kts` and `app/version.properties`.
- `app/src/main/java/com/example/taoyuangutter/common/BackendEndpoints.kt`, `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`, and `GutterRepository.kt`.
- `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/network_security_config.xml`, and `app/src/main/res/xml/file_paths.xml`.
- `ai/architecture.md`, `ai/knowledge-resolution-rules.md`, and available files under `docs/product/` and `docs/api/`. Product/API directories contain templates only; no pre-existing version-check contract was found.

## Resolved Decisions

| Source | Decision or constraint | Confidence | Acceptance criteria |
|---|---|---|---|
| User | API is parameter-free GET `http://192.168.10.84/TY_RSGDBIP/api/appVersion`; response contains exactly one Android record with a latest downloadable three-number version and APK URL. | Approved | AC-003, AC-009, AC-010 |
| User | Compare numeric major/minor/patch components to `BuildConfig.VERSION_NAME`; prompt only if the server version is newer. | Approved | AC-002, AC-005 |
| User | Check on cold launch and normal return to foreground. API failures do not prompt or block use. | Approved | AC-004, AC-009 |
| User | Show「發現新版本 x.y.z」with「前往更新」and「繼續使用」over the current screen; continue dismisses the dialog and leaves the user on that screen. | Approved | AC-005 |
| User | Download in app with progress/cancel; failures offer retry/continue. On success open the Android installer. If install-source permission is needed, direct the user to Settings and resume installer after permission is granted. Installer cancellation is not followed by an immediate repeated prompt. | Approved | AC-006, AC-007 |
| User | Only a successful release assemble advances `P0_VERSION_CODE`; next release APK uses the new code (2 when starting at 1), then the property is 2. Failed/debug builds do not advance. Version-name parts remain manually maintained. | Approved | AC-001 |
| User | The single external JKS is used for APKs distributed as updates, including test and formal builds; production/test must retain update signature compatibility. | Approved | AC-008 |
| User | Use the test API endpoint now, independent of the debug backend selector. Replace it with the production endpoint before production release. | Approved | AC-010 |

## Repository Findings
- Existing Gradle configuration already loads integer `P0_VERSION_CODE` and joins `P1_VERSION_NAME_MAJOR`, `P2_VERSION_NAME_MINOR`, and `P3_VERSION_NAME_BUILD`; it has no release-only increment task or release signing configuration.
- `BackendEndpoints.BASE_API_URL` already equals the user-provided test API base URL. `GutterApiClient` has per-target Retrofit services and a debug selector; the version request must explicitly use BASE so it is not redirected by that selector.
- `network_security_config.xml` already permits cleartext for exactly `192.168.10.84`; the test API's HTTP scheme does not require widening cleartext access.
- `LoginActivity` is the launcher and offers authenticated login and offline entry. The app has several other Activities, an existing FileProvider, and no app-level foreground coordinator or APK installer flow.
- The existing FileProvider exposes internal files and external Pictures; the app manifest does not yet declare the APK-install permission.
- CI currently runs selected JVM tests and assembles debug only. Its local properties file contains a placeholder Maps key; release signing secrets are not configured in CI.

## Unresolved Conflicts
- None for the test implementation. The production API URL remains unavailable by explicit decision and is required before production release.

## Assumptions Safe for Planning
- Use the existing BASE Retrofit service for version checks, explicitly bypassing the debug target selector.
- Keep the current test URL in one clearly named endpoint setting; change that setting to the production URL only when the user supplies it.
- Store the JKS path, alias, and passwords in ignored local configuration or protected CI secrets; never store those values in tracked files.
- User approval of one signing key means all backend-distributed test/formal APKs are signed with the same certificate. Locally installed debug builds are not update packages unless signed with that key.
- A failed API check, empty/malformed result, or missing Android row follows the already approved quiet fail-open behavior.

## Questions Requiring Approval
- None.
