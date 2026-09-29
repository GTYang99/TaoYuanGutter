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

- User confirmed `taipei.srgeo.com.tw` as the active GeoServer host, all GeoServer WMS/WFS migration, inactive-only DEMO constants, and temporary exact-host hostname workaround in release as well as debug.
- The endpoint decisions are resolved, but the certificate itself remains an external infrastructure issue; release verification stays blocked until the server certificate includes `taipei.srgeo.com.tw`.

## ISS-DBG-0929-003

- task_id: debug-0929-wmts-api-path
- phase: implementation
- category: implementation_regression
- priority: P1
- title: Remote photo loaders did not use the Taipei certificate workaround
- status: resolved
- impact: Login/API and GeoServer requests used the shared OkHttp client, but Glide remote-photo requests used its default loader and could still fail on the `taipei.srgeo.com.tw` hostname mismatch.
- evidence:
  - `GutterPhotosFragment`, `GutterBasicInfoFragment`, `GutterInspectPhotosFragment` and `ImageDetailDialogFragment` call `Glide.load` with remote URLs.
  - `BackendHttpClient` was only wired to Retrofit, raw image downloads and custom GeoServer tile providers.
- resolution: Registered `BackendGlideModule` in the manifest; its remote URI loader fetches through `BackendHttpClient`, while local URI schemes retain Glide defaults.
- validation: Full JVM tests, debug build and release build passed; physical-device photo runtime is `NOT VERIFIED`.
