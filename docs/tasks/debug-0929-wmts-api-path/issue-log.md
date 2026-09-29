# Issue Log

## ISS-DBG-0929-001

- task_id: debug-0929-wmts-api-path
- phase: planning
- category: requirement_gap
- priority: P1
- title: Taipei API and GeoServer host/scope are not uniquely specified
- status: resolved
- impact: The active runtime endpoint and the set of consumers to migrate cannot be safely selected; an incorrect choice can split reads/writes or mix DEMO and Taipei map data.
- evidence:
  - User message names Taipei API as `taipei.srgeo.com.tw`.
  - User message labels TaipeiGeoserver but supplies the DEMO host `demo.srgeo.com.tw`.
  - `GutterApiClient` actually uses internal `BASE_URL`; `DEMO_URL` is unused.
  - WMTS is centralized, while other GeoServer WMS endpoints are independent.
- expected: One confirmed active host/path per approved endpoint scope, with DEMO retained only as an inactive reserve.
- next_action: implementation
- owner: developer

## ISS-DBG-0929-002

- task_id: debug-0929-wmts-api-path
- phase: planning
- category: environment
- priority: P2
- title: Taipei TLS trust and service reachability are unverified
- status: open
- impact: The app may fail before WMTS request behavior can be evaluated if Taipei DNS, certificate chain, or GeoServer capabilities differ.
- evidence:
  - `network_security_config.xml` explicitly names `demo.srgeo.com.tw` for additional CA trust.
  - No Taipei runtime request or device evidence was supplied.
- expected: Taipei endpoint responds with the approved service and its certificate is accepted by the intended build.
- next_action: infrastructure
- owner: environment

## Resolution

- User confirmed `taipei.srgeo.com.tw` as the active GeoServer host, all GeoServer WMS/WFS migration, inactive-only DEMO constants, and debug-only hostname workaround.
- The endpoint decisions are resolved, but the certificate itself remains an external infrastructure issue; release verification stays blocked until the server certificate includes `taipei.srgeo.com.tw`.
