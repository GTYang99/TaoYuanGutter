# Implementation Plan

## Goal
- Add release version-code management and a non-blocking, signed-APK update flow driven by the test version API.

## Scope
- Increment only `P0_VERSION_CODE` for successful `:app:assembleRelease` runs, with the produced APK carrying the next code; keep the three version-name components manual. Treat version-code automation and release signing as retained release infrastructure during rollback of the optional updater.
- Add a process-foreground version check against the fixed BASE test API, numeric three-component comparison, update prompt, APK download, and Android installer handoff.
- Configure the external JKS for update-distributed Release APKs using ignored local signing values; do not commit key material.
- Do not change authentication rules, offline access, other API target selection, or unrelated release behavior. The production endpoint replacement remains a pre-production-release action.

## Affected Files
- `app/build.gradle.kts` — Release version-code behavior and external-key signing inputs.
- `app/version.properties` — preserve current manual version fields; successful Release task persists only the next P0 value.
- `app/src/main/java/com/example/taoyuangutter/common/BackendEndpoints.kt` — clearly name the fixed version API test base URL.
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`, `GutterRepository.kt`, and a focused app-version model file — no-parameter endpoint, response parsing, and BASE-target request.
- New app-level Application/lifecycle coordinator and update-flow/download classes — cold/foreground checks, active Activity prompt, download state, settings return, and installer launch.
- `app/src/main/AndroidManifest.xml` — register the coordinator Application and declare package-install permission.
- `app/src/main/res/xml/file_paths.xml` — expose only the update APK cache through FileProvider.
- `app/src/main/res/values/strings.xml` — update dialog, progress, permission, retry, and failure copy.
- `app/src/test/java/com/example/taoyuangutter/` — version comparator, response, repository, and release-version calculation tests.
- `app/src/androidTest/java/com/example/taoyuangutter/` — lifecycle, on-screen dialog/state preservation, download cancellation/retry, permission return, and installer handoff tests.
- `.github/workflows/android-ci.yml` — include the new focused JVM tests in the existing CI test step.

## Implementation Steps
1. Add a strict three-component numeric version parser/comparator and response validation that rejects overflow, malformed versions, malformed URLs, non-HTTPS URLs, and any download redirect that downgrades to HTTP before a prompt can be shown.
2. Add the no-parameter `GET api/appVersion` contract and repository handling through the BASE service regardless of the selected debug target; accept exactly one Android record and treat unsuccessful, empty, malformed, or missing Android data as a quiet no-update result.
3. Implement one app-level lifecycle coordinator that checks once at cold start and once per normal process foreground entry, tracks the currently resumed Activity, and suppresses only the immediate return event from the package installer/permission settings.
4. Display the approved dialog over the active screen; ensure「繼續使用」dismisses it without navigation or losing the current screen state.
5. Implement cancellable APK download with visible progress and retry/continue failure handling; store only in app-private cache, add the narrow FileProvider cache path, route missing permission to app-specific install settings, and resume installer handoff automatically after grant.
6. Launch Android's package installer with a content URI and user confirmation; clear the temporary APK after completion/cancellation when safe and prevent immediate repeat prompting after installer cancellation.
7. Read P0 during Gradle configuration and configure the Release variant's `versionCode` as P0 + 1. Attach a persistence action directly to the `:app:assembleRelease` lifecycle task (not a finalizer); it writes that same computed code atomically to `version.properties` only after all assembly dependencies have succeeded. Since the lifecycle task has no outputs, verify its action runs for each invocation; if a dependency fails, the lifecycle task action must not run. Prove successful, injected-failure, and debug builds behave as required and P1/P2/P3 remain unchanged.
8. Configure Release APK signing from the user-provided external JKS through ignored local values; never add keystore/password material to Git or logs. Require the same signer for APKs that update one another.
9. Add focused JVM/instrumentation coverage, add JVM test class filters to CI, then run test-environment and signed emulator checks and record their evidence.

## Test Plan
- JVM unit tests: numeric version ordering; exactly-three-component syntax and integer overflow rejection; HTTPS URL parsing and rejection of malformed/non-HTTPS URLs; app-version success/error/empty/type parsing; API target pinning; P0-only version calculation.
- Release build checks in one isolated task copy, in this exact order and without resetting `version.properties`: start at P0=1; inject a failing dependency into `:app:assembleRelease` using a temporary Gradle init script and verify P0 remains 1; run a debug build and verify P0 remains 1; run a successful Release and inspect the signed APK at versionCode 2 and P0=2; run the next successful Release and inspect versionCode 3 and P0=3. Confirm P1/P2/P3 are unchanged throughout. No scenario restores P0 after a successful APK consumes it.
- Android emulator checks with test API network access: cold-start and background/foreground request cadence; no duplicate checks during internal Activity navigation; newer/equal/older API response outcomes; API outage fail-open; dialog actions preserve login, map, and in-progress form screens.
- Android emulator update-flow checks: controlled newer-version response and JKS-signed APK; progress, user cancel, failed download retry/continue, unknown-source Settings return with automatic installer continuation, system install confirmation, successful replacement, and installer cancellation without immediate prompt repetition.
- API integration check: call the live test endpoint without parameters and record the returned single Android record, three-number version, and HTTPS APK URL. Do not alter backend state or download/install an uncontrolled production APK. If the live contract violates validation rules or the endpoint is unavailable, record the evidence as failed/NOT VERIFIED; controlled mock responses still verify rejection cases.
- CI: run the updated task-scoped unit tests and existing debug build. Release signing/artifact check is local because CI currently has no signing secrets.

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android emulator running Android 9+ (prefer the existing Android 14 emulator), test API reachable on the configured LAN, and a current APK signed with the provided JKS.
- In-scope Acceptance Criteria: AC-004 through AC-010.
- Regression risk: app startup/resume, active-screen state, existing login/offline entry, debug API selection, APK signing, and Android install permission/confirmation.
- Full regression required: No.
- Full regression trigger: login/offline entry failure, unintended API target switching, lost in-progress screen state, install/signature mismatch, or Release version property corruption.
- Stop condition: all specified cases pass, or a reproducible failure is captured and classified.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Gradle Release/debug builds and APK manifest inspection | In one isolated copy at P0=1, run injected Release failure, debug build, successful Release, then the next successful Release, without resetting the property | Failure/debug leave P0=1; first successful APK is code 2 and leaves P0=2; next APK is code 3 and leaves P0=3; P1/P2/P3 remain unchanged | Ordered Gradle task output, before/after properties after each step, APK manifest metadata |
| AC-002 | JVM unit tests | Compare equal, lower, higher, 1.9.9/1.10.0; parse malformed, non-three-part, and overflow versions | Valid numeric tuple ordering is correct; invalid versions are rejected; prompt decision is true only for newer server version | Test results |
| AC-003 | JVM/API contract test plus live test endpoint | Invoke version repository for each debug target; assert no query/body; feed malformed data and non-HTTPS URLs | Request targets fixed BASE endpoint; exactly one Android row with strict version and HTTPS URL is accepted; invalid row yields no update | Mock-server request record, rejection assertions, sanitized live response |
| AC-004 | Android emulator | Cold launch; move app to background and return; navigate among internal Activities; return from installer/settings | One check per cold/normal foreground entry, none on internal Activity switches or immediate installer/settings return | Instrumented lifecycle event/request log |
| AC-005 | Android emulator with controlled version responses | Show update prompt on login, map, and active form; choose continue | Exact version text/actions; prompt dismisses and the same screen/state remains | Instrumented assertions and scenario result |
| AC-006 | Android emulator with controlled APK URL | Start download, observe progress, cancel; simulate failure and retry/continue | Progress is visible; cancel preserves the screen; failure offers retry/continue | Instrumented result and downloader state |
| AC-007 | Android emulator Android 8+ install-source permission flow | Download same-signer APK with permission off/on; grant in Settings; confirm or cancel installer | Correct Settings screen and auto-resume; Android installer asks for confirmation; cancellation does not immediately re-prompt | Instrumented intents and manual/emulator observations |
| AC-008 | Signed emulator package update | Install code-1 APK signed by JKS; update with code-2 APK signed by same JKS | Existing installation updates in place; certificate matches; no signing material appears in tracked files/logs | APK signer reports, package version before/after, repository scan |
| AC-009 | Unit and Android emulator tests | Simulate API/network/parse failures, invalid/non-HTTPS URL, HTTP redirect, and download failure | No version dialog for invalid check data or downgrade redirect; app remains usable; download failure offers retry/continue | Test results and mock redirect chain |
| AC-010 | Android emulator on test network | Select BASE, TAIPEI, and DEMO in the debug control and run version check | Version check always calls the test endpoint, independent of the selected backend | Request host/path evidence |
| AC-011 | Isolated Git/task copy | Start at P0=1; successfully assemble code-2 APK; simulate rollback by reverting only optional updater source/tests/manifest/provider/strings/CI changes while explicitly retaining `app/build.gradle.kts` version-code and signing changes plus P0=2; assemble Release again | Rollback diff retains the version-code and signing mechanisms; next signed APK uses code 3 and P0 becomes 3 | Before/after properties, APK version/signature metadata, exact rollback diff and retained Gradle configuration |

## Regression Plan
- Verify login success/failure behavior and saved credentials are unchanged.
- Verify offline entry remains available when API checking fails or the user chooses continue.
- Verify update checks never redirect ordinary repository calls away from the selected API target.
- Verify switching among existing Activities does not duplicate checks, dismiss or recreate forms, or lose draft/photo state.
- Verify debug builds do not mutate release version properties and no build type silently changes manually managed version-name components.
- Verify FileProvider still serves existing photo flows and exposes no broader new path than required.

## Risks
- The lifecycle task must execute its persistence action only after all assembly dependencies succeed. The implementation must prove it has no up-to-date shortcut that skips persistence and must not use a finalizer.
- URL validation must happen before showing the prompt, and the download client must reject redirect chains that switch from HTTPS to HTTP.
- Sideload installation depends on same application ID, increasing version code, same signing certificate, user confirmation, and per-source permission.
- A test API response of 1.0.0 matching the current app will not exercise the newer-version path; use controlled test responses and a same-key candidate APK without modifying server data.
- LAN-only test API availability may limit live integration evidence; do not convert an unavailable endpoint into a pass.
- Production API URL is not known yet; the test URL must not be shipped in a production APK. Production release checklist must record replacement of the test URL and a successful endpoint/response verification.

## Rollback Plan
- Rollback boundary: revert the optional version-check/download/install flow, its tests, manifest registration/permission, FileProvider path, strings, and CI additions as needed, but retain the Release version-code calculation and success-gated persistence in `app/build.gradle.kts`, the Release signing configuration, and the highest P0 consumed by any successful Release APK in `app/version.properties`. Never restore Gradle version logic from the pre-task baseline while keeping only the P0 property: that baseline assigns P0 directly and would reuse the consumed code. Verify the retained release controls before building; the next Release must use a strictly higher code.

## Current Behavior
- The current app version is 1.0.0 with integer code 1 from `app/version.properties`; Gradle does not increment either value.
- There is no startup/foreground version check, update dialog, APK download, install-permission handling, or release signing configuration.

## Expected Behavior
- Successful Release APKs use monotonically advancing version codes while the user manually controls the three-part version name.
- A newer server version triggers the approved optional dialog and signed APK install flow; failures and user cancellation preserve app use and current screen state.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 7 | Successful, injected-failure, and debug build checks in isolated copy; APK manifest inspection |
| AC-002 | 1 | Strict parser/comparator JVM tests |
| AC-003 | 1–2 | Mock-server contract assertions, bad URL/version rejection, and live test API check |
| AC-004 | 3 | Lifecycle instrumentation and request-count evidence |
| AC-005 | 4 | Dialog text/action/screen-state instrumentation |
| AC-006 | 5 | Downloader progress/cancel/failure instrumentation |
| AC-007 | 5–6 | Android Settings/package-installer emulator flow |
| AC-008 | 8 | Same-key signed APK upgrade and repository/log scan |
| AC-009 | 1–2, 5 | Invalid version/URL/redirect rejection, failure-mode tests, and app-use assertions |
| AC-010 | 2 | Selected-target matrix with captured version-check host/path |
| AC-011 | 7 | Rollback simulation retains Gradle version/signing configuration and consumed P0; verify the following APK is code 3 after code 2 was consumed |

## Failure Behavior
- Version API transport, HTTP, JSON, response-success, type, strict three-number version, URL parsing, or HTTPS validation failures resolve to no update and do not block or alter normal app flows. APK download redirects must stay HTTPS.
- Download failure shows an error with retry/continue; explicit download cancel and installer cancel return to the current app screen, preserving its state.
- Missing unknown-source permission opens app-specific settings and resumes installation if granted. An invalid APK or signature/version/package mismatch leaves Android's installer error visible and the app usable; it never bypasses system confirmation.

## Security and Privacy
- The version API has no parameters and sends no account or device identifier.
- Permit cleartext only for the already configured test IP; keep the production endpoint HTTPS when supplied.
- Save the downloaded APK in app-private cache and share it only through the existing FileProvider with a narrow cache path.
- Keep the JKS outside the repository and load signing credentials from ignored local configuration or protected CI secrets; redact them from logs.

## Open Questions
- None for test-environment implementation. Before production release, replace the test endpoint and verify the production response/HTTPS APK URL; capture this as a release gate.
