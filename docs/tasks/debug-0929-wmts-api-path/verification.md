# Verification Report

## Verification Scope

- Fixed source revision: `c4721ee` on `codex/debug-0929-wmts-api-demo-reservation`.
- Scope: endpoint source review, focused/full JVM tests, debug/release builds, and security-boundary review.
- Physical device and live Taipei service checks: `NOT VERIFIED`.

## Acceptance Criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | `BackendEndpointsTest` and `Wmts3857TileProviderTest` confirm Taipei WMTS endpoint and existing WMTS parameters. |
| AC-002 | PASS | `BackendEndpoints` contains named legacy DEMO values; runtime source scan found no DEMO selection or fallback. |
| AC-003 | PASS | Main map, point picker and form map retain the shared `Wmts3857TileProvider`. |
| AC-004 | PASS (static) | Retrofit and hardcoded GeoServer WMS consumers resolve to Taipei; live request evidence is not available. |
| AC-005 | PASS (static) | WMS/WMTS/API endpoint constants use `taipei.srgeo.com.tw`; NLSC URLs remain unchanged. |
| AC-006 | PASS (source/build/probe) | Hostname exception is gated by an explicit temporary flag and exact Taipei host; debug/release builds passed. A no-credential Taipei probe reached the API and returned `401` when verification was bypassed. |
| AC-007 | PASS (static) | NLSC WMTS URL templates were not changed. |
| AC-008 | PASS (unit/static) | Full JVM tests and source review passed; physical overlay rendering is not verified. |

## Security Decision

- No trust-all certificate manager was added.
- The temporary bypass does not disable CA-chain validation and is limited to `taipei.srgeo.com.tw`; API, remote photos and GeoServer tiles now share that client boundary.
- The Taipei server certificate must still be corrected and the temporary release flag removed for long-term release use.

## Final Result

- Developer validation: PASS with environment limitations.
- Independent physical verification: `NOT VERIFIED`.
- CI: `NOT VERIFIED`.
- Release decision: temporary workaround is implemented, but long-term release remains blocked pending Taipei certificate repair, physical/runtime verification, and CI evidence.

## Independent Verification Recheck

- Date: `2026-09-29`
- Fixed implementation revision: `c4721ee` (`git show` resolves to the implementation commit tested).
- Source snapshot: `/private/tmp/verify-wmts-c472`, exported from that commit. Source and test files
  were not edited; the only local build input was a temporary ignored `local.properties` with
  `MAPS_API_KEY=verification-placeholder`.
- Device availability: `emulator-5554`, `sdk_gphone64_arm64`, Android 14 / API 34 was discoverable.
  The plan requires physical-device API/photo/WMS/WMTS checks with an authenticated test account;
  that account and a physical device were not available, so those cases were not run.

### Independent checks

| Check | Result | Evidence |
|---|---|---|
| Focused endpoint/WMTS/WMS/hostname tests | PASS | `:app:testDebugUnitTest` for `BackendEndpointsTest`, `Wmts3857TileProviderTest`, `Wms3857RequestBuilderTest`, and `BackendHttpClientTest`; `BUILD SUCCESSFUL`. |
| Full JVM suite | PASS | `:app:testDebugUnitTest`; 137 tests, 0 failures, 0 errors, 0 skipped (42 XML suites). |
| Debug and release builds | PASS | `:app:assembleDebug :app:assembleRelease`; `BUILD SUCCESSFUL`. |
| Endpoint and trust-boundary source review | PASS | Active endpoint constants point to Taipei; DEMO values are only inactive constants; runtime consumers use the shared endpoint/client. The hostname verifier only short-circuits the normalized exact Taipei host; other hosts use the platform verifier. OkHttp's default certificate-chain validation remains configured, with no trust-all manager. NLSC WMTS templates remain unchanged. |
| CI | NOT VERIFIED | No repository CI workflow or result is present in the fixed revision. |
| Physical/runtime cases | NOT VERIFIED | No authenticated device run for API/login/photo/WMS/WMTS, no NLSC map usability result, and no on-device debug/release hostname case. |

### Acceptance status

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Shared WMTS request builder and active path/layer parameters are covered by source review and focused tests. |
| AC-002 | PASS | Named DEMO constants remain inactive; runtime source scan found no DEMO fallback. |
| AC-003 | PASS | Main map, point picker, and form map continue to use the shared WMTS provider. |
| AC-004 | NOT VERIFIED | Static endpoint review passes, but authenticated live API/WFS/WMS/GetFeatureInfo runtime requests were not exercised. |
| AC-005 | PASS | Formal endpoint constants resolve to Taipei; DEMO is retained only as inactive constants. |
| AC-006 | NOT VERIFIED | Source, hostname gate tests, CA-chain configuration, and both builds pass; planned debug/release device TLS behavior is untested. |
| AC-007 | NOT VERIFIED | NLSC WMTS URL templates are unchanged in source, but on-device map usability was not checked. |
| AC-008 | NOT VERIFIED | Static/request-builder checks pass; physical overlay toggles, tile rendering, and z-index behavior were not exercised. |

### Final result

`NOT VERIFIED` (`environment`). Keep `next_action: infrastructure` until the scoped authenticated
device cases and CI evidence are available. The known Taipei certificate hostname mismatch and
temporary release workaround remain a release risk; remove the workaround after the server
certificate is corrected.
