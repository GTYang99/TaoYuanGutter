# Knowledge Resolution

## Sources Reviewed

| Source | Finding | Confidence | Effect |
|---|---|---:|---|
| Current user request | Taipei API is named; original DEMO should remain reserved; Taipei GeoServer line contains a host conflict | High | Requires OQ-001～OQ-004 before implementation |
| `GutterApiService.kt` | Retrofit uses internal `BASE_URL`; `DEMO_URL` is unused | High | A `DEMO_URL` edit alone cannot migrate API runtime |
| `Wmts3857TileProvider.kt` | One fixed DEMO WMTS endpoint feeds all three approved WMTS layers | High | WMTS endpoint can be changed centrally |
| Three map entry points | Main map, point picker and form map instantiate the shared provider | High | No three-way endpoint duplication for WMTS |
| `MapOverlayController.kt`, `Wms3826RequestBuilder.kt` | Other WMS endpoints are independently fixed to DEMO | High | WMTS-only and all-GeoServer migration are different scopes |
| `network_security_config.xml` | Additional CA trust is scoped to DEMO host | High | Taipei TLS behavior requires separate validation/config decision |
| `docs/tasks/refactor-0918/*` | Existing approved WMTS behavior uses DEMO endpoint and specifies three layers | High | Historical source conflicts with current migration request; current user request has precedence |
| Original worktree uncommitted diff | User manually changed some Taipei URLs but did not alter Retrofit active base URL or add legacy reserve | High | Treat as user input evidence; preserve original worktree |

## Resolved Decisions

- The work is isolated in `/Users/a10362/.codex/worktrees/wmts-api-demo-reservation/TaoYuanGutter` on branch `codex/debug-0929-wmts-api-demo-reservation`.
- Investigation and artifacts do not modify production code.
- The existing WMTS provider should remain shared; there is no evidence requiring per-screen endpoint duplication.
- DEMO must not be used as an automatic runtime fallback because API writes and map reads could cross environments.

## Unresolved Conflicts

- The exact Taipei GeoServer host is contradictory in the user-provided URLs.
- The desired API runtime is unclear because the current active URL is an internal HTTP server, not the existing DEMO constant.
- The migration boundary is unclear: only the three WMTS layers versus all GeoServer WMS/WFS/GetFeatureInfo consumers.
- The desired DEMO reserve mechanism is unclear: private constants only, build variant, or explicit test injection.

## Safe Assumptions for Planning

- Keep existing WMTS request parameters, layer names, formats, and tile index mapping unchanged.
- Add no automatic fallback, no data migration, and no new credentials.
- Treat Taipei network/TLS reachability as `NOT VERIFIED` until tested against the resolved host.

## Questions Requiring Approval

- OQ-001: Should API runtime use Taipei or remain on the internal `BASE_URL`?
- OQ-002: What is the exact Taipei GeoServer host/path?
- OQ-003: Is the migration WMTS-only, or does it include all GeoServer consumers?
- OQ-004: How should the inactive DEMO reserve be exposed, if at all?

## Resolution Status

`blocked`: Planning cannot safely choose the active endpoint or migration scope until the OQs are answered.
