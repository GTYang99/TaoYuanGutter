# Knowledge Resolution

## Evidence Reviewed

| Source | Finding | Effect |
|---|---|---|
| Current user request | Requires an endpoint selector gated by `ENABLE_GROUP_SIMULATION` | Gate and capability confirmed |
| User resolution | `base` is `http://192.168.10.84/TY_RSGDBIP/` | Third API target confirmed |
| Current `BackendEndpoints.kt` | Taipei and DEMO API URLs exist; base is absent | Plan must restore base as a named API target |
| Current API client | Retrofit singleton uses Taipei and consumers retain the service | Runtime selection must account for service lifecycle |
| Current map clients | WMS/WMTS have independent fixed URLs | Keep outside scope per API-only request |

## Resolved Decisions

- `base`: `http://192.168.10.84/TY_RSGDBIP/`.
- `taipei`: `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`.
- `demo`: `https://demo.srgeo.com.tw/TY_RSGDBIP_BK/`.
- The selector is gated by the existing `GutterApiClient.ENABLE_GROUP_SIMULATION`.
- Switching applies to API requests only; WMS/WMTS remain unchanged.
- Selection is process-local and resets to Taipei after App restart.
- No automatic fallback between environments.

## Status

`resolved`: endpoint identity and behavior scope are sufficiently clear for Planning.
