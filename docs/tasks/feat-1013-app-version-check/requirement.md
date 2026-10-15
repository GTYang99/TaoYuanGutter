# Requirement

## Background
- The app already reads its Android version code and three-part version name from `app/version.properties`, but it does not automatically advance the release version code or check the server's latest APK version.
- The backend provides a no-parameter version endpoint and an APK download URL. The current test environment is used for validation; a production URL will be supplied before production release.

## Goal
- Manage release version codes without changing the manually maintained version name, and offer an optional in-app update flow when a newer Android APK is available.
- Preserve access to the current app when the user continues, the API is unavailable, or the download/install flow is canceled.

## Functional Requirements
- Only a successful `:app:assembleRelease` advances `P0_VERSION_CODE`; the APK produced by the next release build uses the advanced value. Starting at 1, the next signed release APK uses code 2 and the properties file is left at 2. Failed builds and non-release builds do not advance it. A code consumed by a successfully assembled APK remains consumed during rollback; P0 must never be restored below a successful release artifact's code.
- `P1_VERSION_NAME_MAJOR`, `P2_VERSION_NAME_MINOR`, and `P3_VERSION_NAME_BUILD` remain manually managed. The installed app exposes their joined three-number version as `BuildConfig.VERSION_NAME`.
- Check the fixed test endpoint `GET http://192.168.10.84/TY_RSGDBIP/api/appVersion` without parameters, independently of the debug API target selector. The response has a single Android record with a version matching exactly three numeric components and a valid HTTPS URL that serves an APK. Reject malformed versions, malformed URLs, and non-HTTPS URLs before showing any update prompt. Download redirects must remain HTTPS.
- Check once on cold app launch and on each normal app return from background to foreground. Do not prompt again immediately when returning from the Android installer or the install-permission settings flow; check again on the next normal foreground return.
- Compare three numeric version components in order. Show an update prompt only when the API version is newer than the installed `BuildConfig.VERSION_NAME`; equal or older API versions do not prompt.
- Show the prompt over the currently active app screen with text such as「發現新版本 2.0.1」and buttons「前往更新」and「繼續使用」. Continuing closes the prompt and preserves the same screen and in-progress UI state.
- On「前往更新」, download the APK in the app with visible progress and a cancel action. A user-canceled download returns to the same screen without an error prompt. A failed download offers「重試」and「繼續使用」.
- After a successful download, open Android's package installer. If the app is not allowed to install APKs from this source, explain and open the relevant system settings; when the user returns with permission granted, continue to the installer automatically. Installation always requires Android's user confirmation. If the user cancels installation, do not immediately repeat the update prompt; check again on the next normal foreground return.
- APKs distributed for in-place updates use the same signing key. The provided JKS stays outside Git; signing values are supplied only through ignored local configuration or protected CI secrets.
- API/network/HTTP/JSON/type/URL failures do not show an update prompt and do not block login, offline entry, or current app use.

## Non-functional Requirements
- Version checking and APK download must run off the main thread and must not interrupt form data or photo state.
- Do not log or commit signing passwords, private key material, or other signing credentials.

## Acceptance Criteria
- AC-001: With `P0_VERSION_CODE=1`, a successful next `:app:assembleRelease` packages a signed APK with version code 2 and leaves `P0_VERSION_CODE=2`; a failed release build and debug build leave the property unchanged. The three manually managed version-name fields are unchanged by the increment task.
- AC-002: Version-name comparison handles numeric components correctly (for example, 1.10.0 is newer than 1.9.9) and only reports an update when the API version is newer.
- AC-003: The app makes a parameter-free GET to the fixed test version endpoint and parses the single `type=android` record, its version, and APK URL regardless of the selected debug API target.
- AC-004: The version check runs at cold start and normal process foreground entry without duplicate calls during internal Activity transitions. Returning from the installer/settings flow does not immediately repeat the prompt; a later normal foreground return checks again.
- AC-005: A newer version displays the specified dialog over the current screen.「繼續使用」dismisses it while preserving that screen and its current UI state; equal, older, or unavailable server versions show no dialog.
- AC-006:「前往更新」downloads the APK with visible progress and cancellation. Download cancellation returns to the same screen; download failure offers retry or continue.
- AC-007: A completed download opens the system installer. Missing install-source permission leads to the correct settings page and automatically resumes installation after permission is granted. The user can confirm or cancel installation, and cancellation does not immediately reopen the update prompt.
- AC-008: A test APK signed with the provided single JKS can update an already installed APK signed with that key; signing material is not present in tracked files or logs.
- AC-009: API errors, malformed or empty responses, missing Android records, unreachable test network, and invalid/download-failing URLs leave the app usable and do not require an update.
- AC-010: The version API endpoint is fixed to the provided test environment for this task and is independent of the login-page backend selector. Production release cannot use the test URL; the production endpoint must be set and verified before that release.
- AC-011: Rollback after any successful Release assembly retains the highest `P0_VERSION_CODE` consumed by an assembled APK; subsequent Release builds use a strictly higher code, even when feature code is reverted or the APK was not distributed.

## Constraints
- The backend contract is `GET /TY_RSGDBIP/api/appVersion`, no request parameters, one `android` record, `version` formatted as three numeric components, and `url` pointing to an APK.
- Updates are optional; the app must not force-update or block login/offline use.
- Use the existing out-of-repository JKS for all APKs distributed as updates; do not copy it into the repository.
- Production API URL has not yet been provided. Current implementation and validation use the specified test endpoint only.

## Open Questions
- None. The production URL is a known prerequisite for production release, not an unresolved decision for test-environment implementation.
