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
| Emulator runtime | PARTIAL | Configured local Maps key rendered the main/NLSC map; picker/form maps opened; debug and release TLS requests passed; layer sheet opened and the plan overlay checkbox toggled off/on. |
| Authenticated emulator runtime | BLOCKED | Main/NLSC map, point-picker, and form map screens opened, but no authorized test account/session is available for authenticated API/photo and WFS/GetFeatureInfo cases. No credentials were used. |

## Environment Classification

The certificate hostname mismatch is confirmed as a server configuration issue. The approved exact-host
workaround lets the Android client complete TLS while preserving CA-chain validation; it does not fix
normal TLS clients. Authenticated API/photo/WFS/GetFeatureInfo runtime and full overlay-regression
evidence plus CI remain unavailable. Main/NLSC map, picker, and form map screens opened with the local
key; emulator API root, WMS capabilities, all three WMTS tile requests, and one WMS toggle passed.
Local unit tests and debug/release builds also passed. Both debug and release app clients completed the same unauthenticated API/WMS/WMTS TLS smoke on emulator; release used a temporary test-only signing/variant setup, which was reverted.

## Remediation Needed

1. Provide an authorized test account/session for scoped login/API, photo, and authenticated service
   checks on the available emulator. Do not send account credentials in task artifacts or chat.
2. Provide a CI run/status URL for the fixed commit, or make the applicable CI workflow available and
   run it.
3. Fix the Taipei certificate SAN to include `taipei.srgeo.com.tw` before retiring the explicitly
   approved temporary host-only workaround.

When an authorized emulator test session and CI evidence are available, return to Verification using
the approved exact-host workaround. After the server certificate is fixed, remove the runtime
hostname workaround and verify the resulting committed revision.

## Decision

Infrastructure remains **BLOCKED** on authenticated emulator prerequisites and CI evidence. The
temporary TLS workaround is verified for API root, WMS capabilities, and WMTS tiles; server certificate
repair remains required before removing the workaround. Task remains in `phase: infrastructure`, with
`next_action: infrastructure`.
