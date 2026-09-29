# Infrastructure Findings

## Scope

- Task: `debug-0929-wmts-api-path`
- Verification revision: `c4721ee3d785a2c6d6e1e928796912a10b062971`
- Originating blocker: `ISS-DBG-0929-002` (Taipei TLS trust and runtime reachability)
- Checked: `2026-09-29`

## Findings

| Required evidence | Result | Evidence |
|---|---|---|
| Taipei certificate | KNOWN MISMATCH; APP WORKAROUND VERIFIED | Normal curl still fails with error 60 because the SAN omits `taipei.srgeo.com.tw`. Per user direction, the exact-host client workaround was exercised on Android 14: API root 200, WMS capabilities 200, and all three WMTS z0 tiles 200 `image/png`; no credentials were sent. |
| GitHub CI status for the fixed commit | BLOCKED | GitHub combined status returned an empty `statuses` list; associated workflow runs returned an empty `workflow_runs` list. |
| Repository CI definition | BLOCKED | No workflow or CI configuration file is present in the checked repository revision. |
| Emulator availability | AVAILABLE | ADB connected to `emulator-5554` (`sdk_gphone64_arm64`, Android 14/API 34), which the user approved as the runtime basis. |
| Emulator install/start | PASS | Built and installed the task debug APK using a verification placeholder Maps key; package launched without a crash and resumed at `LoginActivity`. |
| Authenticated emulator runtime | BLOCKED | No authorized test account/session or valid Maps API key is available. The app remained at login; authenticated API/photo and in-app map UI checks were not run. Unauthenticated API root, WMS capabilities, and WMTS tile requests did pass. No credentials were used. |

## Environment Classification

The certificate hostname mismatch is confirmed as a server configuration issue. The approved exact-host
workaround lets the Android client complete TLS while preserving CA-chain validation; it does not fix
normal TLS clients. Authenticated API/photo/map UI runtime and CI evidence remain unavailable. Emulator
API root, WMS capabilities, and all three WMTS tile requests passed; local unit tests and debug/release
builds also passed in Verification.

## Remediation Needed

1. Provide a valid Maps API key and an authorized test account/session for the scoped login/API,
   photo, WMS/WMTS, NLSC, and debug/release TLS checks on the available emulator. Do not send account
   credentials in task artifacts or chat.
2. Provide a CI run/status URL for the fixed commit, or make the applicable CI workflow available and
   run it.
3. Fix the Taipei certificate SAN to include `taipei.srgeo.com.tw` before retiring the explicitly
   approved temporary host-only workaround.

When emulator credentials/key and CI evidence are available, return to Verification using the
approved exact-host workaround. After the server certificate is fixed, remove the temporary release
hostname workaround and verify the resulting committed revision.

## Decision

Infrastructure remains **BLOCKED** on authenticated emulator prerequisites and CI evidence. The
temporary TLS workaround is verified for API root, WMS capabilities, and WMTS tiles; server certificate
repair remains required before removing the workaround. Task remains in `phase: infrastructure`, with
`next_action: infrastructure`.
