# Verification Report

## Verification Scope

- Fixed source revision: `950900d` on `codex/debug-0929-wmts-api-demo-reservation`.
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
