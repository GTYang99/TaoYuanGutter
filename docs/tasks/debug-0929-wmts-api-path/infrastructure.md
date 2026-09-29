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
| Emulator runtime | PASS (scoped) | User signed in and confirmed map overlays can be switched on/off and basemaps switched normally. Sanitized Taipei logs show authenticated API, WFS GetFeature, WMS GetFeatureInfo, and WMS GetMap success; all three WMTS layers returned successful tiles, with some other tiles returning HTTP 400 and omitted individually. |
| Authenticated emulator runtime | PASS for AC-004 scope | User signed in directly on the emulator. Login and scopeSearch, WFS GetFeature, and WMS GetFeatureInfo all returned HTTP 200. No credentials or response bodies were captured. |

## Environment Classification

The certificate hostname mismatch is confirmed as a server configuration issue. The approved exact-host
workaround lets the Android client complete TLS while preserving CA-chain validation; it does not fix
normal TLS clients. User-assisted authenticated API/WFS/GetFeatureInfo runtime and scoped manual overlay
and basemap checks now pass on emulator. Some individual WMTS tiles return HTTP 400 and are omitted by
the provider while successful tiles continue to render. CI evidence remains unavailable.
Local unit tests and debug/release builds also passed. Both debug and release app clients completed the same unauthenticated API/WMS/WMTS TLS smoke on emulator; release used a temporary test-only signing/variant setup, which was reverted.

## Remediation Needed

1. Provide a CI run/status URL for the fixed commit, or make the applicable CI workflow available and
   run it.
2. Fix the Taipei certificate SAN to include `taipei.srgeo.com.tw` before retiring the explicitly
   approved temporary host-only workaround.

The authenticated emulator prerequisite is now satisfied for the scoped acceptance checks. Resume
the remaining Infrastructure gate when CI evidence is available. After the server certificate is
fixed, remove the runtime hostname workaround and verify the resulting committed revision.

## Decision

Infrastructure remains **BLOCKED** on CI evidence. The user-assisted emulator evidence closes the
authenticated AC-004 and scoped AC-008 checks. The temporary TLS workaround is verified for the tested
Taipei routes; server certificate repair remains required before removing the workaround. Task remains
in `phase: infrastructure`, with `next_action: infrastructure`.
