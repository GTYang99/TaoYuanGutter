# Infrastructure Findings

## Scope

- Task: `debug-0929-wmts-api-path`
- Verification revision: `c4721ee3d785a2c6d6e1e928796912a10b062971`
- Originating blocker: `ISS-DBG-0929-002` (Taipei TLS trust and runtime reachability)
- Checked: `2026-09-29`

## Findings

| Required evidence | Result | Evidence |
|---|---|---|
| Taipei DNS / HTTPS endpoint | BLOCKED | A read-only `curl -I` to `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/` resolved and reached TLS negotiation, then failed verification with curl error 60: no certificate subject alternative name matches `taipei.srgeo.com.tw`. No credentials were sent. |
| GitHub CI status for the fixed commit | BLOCKED | GitHub combined status returned an empty `statuses` list; associated workflow runs returned an empty `workflow_runs` list. |
| Repository CI definition | BLOCKED | No workflow or CI configuration file is present in the checked repository revision. |
| Emulator availability | AVAILABLE | ADB connected to `emulator-5554` (`sdk_gphone64_arm64`, Android 14/API 34), which the user approved as the runtime basis. |
| Emulator runtime test readiness | BLOCKED | The emulator has no installed app package; there is no map/API-focused instrumentation test. A valid Maps API key and authorized test account/session were unavailable, so login/photo/map-service flows were not run. No credentials were requested or recorded. |

## Environment Classification

The certificate hostname mismatch is confirmed as a current server configuration issue, matching the
known limitation. The source's temporary exact-host workaround does not repair the server certificate
and must not be treated as evidence that normal TLS verification succeeds. Emulator runtime and CI
results are also unavailable; local unit tests and debug/release builds passed in Verification.

## Remediation Needed

1. Update the Taipei server certificate so its SAN includes `taipei.srgeo.com.tw`; then repeat a
   normal certificate-validating HTTPS probe.
2. Install the task build on the available emulator and provide a valid Maps API key plus an
   authorized test account/session for the scoped login/API, photo, WMS/WMTS, NLSC, and debug/release
   TLS checks. Do not send account credentials in task artifacts or chat.
3. Provide a CI run/status URL for the fixed commit, or make the applicable CI workflow available and
   run it.

Once these conditions are available, return to Verification. After the server certificate is fixed,
remove the temporary release hostname workaround as required by the task and verify the resulting
committed revision.

## Decision

Infrastructure work is **BLOCKED** pending server certificate repair, emulator runtime prerequisites,
and CI evidence. Task remains in `phase: infrastructure`, with `next_action: infrastructure`.
