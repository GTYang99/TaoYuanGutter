# Plan Review

## Result

- PASS with security constraint: implement the approved Taipei endpoint migration and debug-only exact-host hostname workaround.

## Review Findings

- Scope is limited to formal API/GeoServer consumers; NLSC WMTS basemap is explicitly excluded.
- Legacy DEMO values remain inactive and no automatic fallback is planned.
- The workaround does not trust all certificates; it only bypasses hostname matching for the exact Taipei host in debug builds and leaves CA-chain validation active.
- Because Google Maps `UrlTileProvider` does not accept the app's OkHttp client, GeoServer tile providers must use the shared HTTP tile provider for the workaround to cover WMTS/WMS tiles.
- Release build validation is required to ensure `BuildConfig.DEBUG` cannot enable the workaround.

## Decision

Approved for implementation in the dedicated worktree. Any server-side certificate repair remains outside this code change and must be verified separately.
