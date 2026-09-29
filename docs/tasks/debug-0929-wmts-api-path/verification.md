# Verification Report

## Verification Scope

- Fixed source revision: `c4721ee` on `codex/debug-0929-wmts-api-demo-reservation`.
- Scope: endpoint/source review, focused/full JVM tests, debug/release builds, and Android 14 emulator runtime checks on the fixed source revision.
- Runtime test basis: user-approved Android emulator; no authorized account or valid Maps key was supplied.

## Acceptance Criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Request-builder tests and emulator requests confirm the active Taipei host and all three WMTS layers; each z0 tile returned HTTP 200 `image/png`. |
| AC-002 | PASS | `BackendEndpoints` contains named legacy DEMO values; runtime source scan found no DEMO selection or fallback. |
| AC-003 | PASS | Main map, point picker and form map retain the shared `Wmts3857TileProvider`. |
| AC-004 | NOT VERIFIED | API root and WMS capabilities returned 200, but authenticated API, WFS, and GetFeatureInfo flows were not exercised. |
| AC-005 | PASS (static) | WMS/WMTS/API endpoint constants use `taipei.srgeo.com.tw`; NLSC URLs remain unchanged. |
| AC-006 | PASS | Exact-host gate, platform validation for other hosts, and CA-chain behavior pass source/unit review; both builds pass, and the emulator debug client completed TLS to Taipei. |
| AC-007 | NOT VERIFIED | NLSC URLs are unchanged in source; emulator map usability was not tested. |
| AC-008 | NOT VERIFIED | Three WMTS tile requests returned images; overlay controls, visual rendering, and z-index behavior were not exercised. |

## Security Decision

- No trust-all certificate manager was added.
- The temporary bypass does not disable CA-chain validation and is limited to `taipei.srgeo.com.tw`; API, remote photos and GeoServer tiles now share that client boundary.
- The Taipei server certificate must still be corrected and the temporary release flag removed for long-term release use.

## Final Result

- Developer validation: PASS with environment limitations.
- Independent emulator verification: `PARTIAL`; AC-001 and AC-006 pass, while AC-004/007/008 remain `NOT VERIFIED`.
- CI: `NOT VERIFIED`.
- Release decision: temporary exact-host workaround was exercised successfully on emulator; release remains blocked pending remaining acceptance evidence and CI. Repair the server certificate before removing the workaround.

## Independent Verification Recheck

- Date: `2026-09-29`
- Fixed implementation revision: `c4721ee` (`git show` resolves to the implementation commit tested).
- Source snapshot: `/private/tmp/verify-wmts-c472`, exported from that commit. Source and test files
  were not edited; the only local build input was a temporary ignored `local.properties` with
  `MAPS_API_KEY=verification-placeholder`.
- Device availability: `emulator-5554`, `sdk_gphone64_arm64`, Android 14 / API 34. User approved emulator as the runtime basis; authenticated scenarios remain unavailable without a test session.

### Independent checks

| Check | Result | Evidence |
|---|---|---|
| Focused endpoint/WMTS/WMS/hostname tests | PASS | `:app:testDebugUnitTest` for `BackendEndpointsTest`, `Wmts3857TileProviderTest`, `Wms3857RequestBuilderTest`, and `BackendHttpClientTest`; `BUILD SUCCESSFUL`. |
| Full JVM suite | PASS | `:app:testDebugUnitTest`; 137 tests, 0 failures, 0 errors, 0 skipped (42 XML suites). |
| Debug and release builds | PASS | `:app:assembleDebug :app:assembleRelease`; `BUILD SUCCESSFUL`. |
| Endpoint and trust-boundary source review | PASS | Active endpoint constants point to Taipei; DEMO values are only inactive constants; runtime consumers use the shared endpoint/client. The hostname verifier only short-circuits the normalized exact Taipei host; other hosts use the platform verifier. OkHttp's default certificate-chain validation remains configured, with no trust-all manager. NLSC WMTS templates remain unchanged. |
| CI | NOT VERIFIED | No repository CI workflow or result is present in the fixed revision. |
| Emulator runtime | PARTIAL | Debug app TLS, API root, WMS capabilities and three WMTS tile requests passed; authenticated API/photo, NLSC map usability and map overlay UI remain unverified. |

### Acceptance status

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Shared builder tests pass; emulator fetched z0 tiles for all three active WMTS layers from Taipei, each HTTP 200 `image/png`. |
| AC-002 | PASS | Named DEMO constants remain inactive; runtime source scan found no DEMO fallback. |
| AC-003 | PASS | Main map, point picker, and form map continue to use the shared WMTS provider. |
| AC-004 | NOT VERIFIED | Static endpoint review passes, but authenticated live API/WFS/WMS/GetFeatureInfo runtime requests were not exercised. |
| AC-005 | PASS | Formal endpoint constants resolve to Taipei; DEMO is retained only as inactive constants. |
| AC-006 | PASS | Exact-host-only verifier and normal verification for other hosts are covered by source/unit review; debug and release builds pass. On Android 14, the debug app's shared client received HTTP 200 from the mismatched Taipei certificate while retaining platform CA-chain validation. |
| AC-007 | NOT VERIFIED | NLSC WMTS URL templates are unchanged in source, but on-device map usability was not checked. |
| AC-008 | NOT VERIFIED | Static/request-builder checks pass; physical overlay toggles, tile rendering, and z-index behavior were not exercised. |

### Final result

`NOT VERIFIED` (`environment`). AC-004, AC-007 and AC-008 remain open; continue Infrastructure until
authorized runtime prerequisites and CI evidence are available. The approved host-only workaround
currently enables the tested Taipei endpoints; fix the server certificate before removing it.

### Emulator-basis recheck (2026-09-29)

- User approved emulator testing as the runtime basis for this verification.
- Fixed implementation revision: `c4721ee3d785a2c6d6e1e928796912a10b062971`; task branch head contains documentation only above it.
- ADB connected to `emulator-5554` (`sdk_gphone64_arm64`, Android 14 / API 34).
- No app package was installed on the emulator, and the repository has no instrumentation test for WMTS/WMS tile rendering or authenticated API requests. The test plan's runtime cases therefore could not be executed. No test account/session or valid Maps API key was available, so no credentials or live data were used.
- Repeated normal HTTPS probe to `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`; TLS failed with curl error 60 because the certificate SAN does not match `taipei.srgeo.com.tw`.
- At this initial check, the emulator was connected but the task APK had not yet been installed; a later launch smoke and TLS/map-service instrumentation run follows below.

The task stayed in Infrastructure while the approved TLS workaround and remaining runtime prerequisites were evaluated.

#### Emulator launch smoke check

- Built and installed the fixed-source debug variant with `MAPS_API_KEY=verification-placeholder`: `:app:installDebug --console=plain` completed `BUILD SUCCESSFUL` and installed to `emulator-5554`.
- Launched package `com.example.taoyuangutter`; Android reported `.login.LoginActivity` as resumed, and the process remained alive. The UI hierarchy identified the app package on screen. No crash was observed.
- No credentials were entered. The app stayed at login, so authenticated API, photo, GeoServer WMS/WMTS, NLSC map, and on-device TLS cases were not reached. The placeholder Maps key also cannot provide valid Google Maps tiles.
- This is a PASS for debug APK install/start smoke only; it did not by itself establish API or map behavior.

#### Taipei TLS workaround and map-service smoke (2026-09-29)

- User directed continuing with the temporary Taipei hostname workaround because the app must run.
- Confirmed the production client allows hostname mismatch only for normalized `taipei.srgeo.com.tw`; it still uses OkHttp's normal trust manager. Other hosts go through the platform hostname verifier.
- Temporary instrumentation harness (test-only, removed after the run) used the production `BackendHttpClient` on `emulator-5554` (`sdk_gphone64_arm64`, Android 14/API 34). Command: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.common.TaipeiTlsSmokeTest`.
- Without credentials, Taipei API root returned HTTP 200; WMS GetCapabilities returned HTTP 200 (`text/xml`); z0 GetTile for `roadServey`, `legacyDitch`, and `regions` each returned HTTP 200 (`image/png`). The debug app completed TLS and CA-chain validation despite the SAN mismatch.
- The API root and capabilities checks do not replace authenticated API/WFS/GetFeatureInfo checks. Map controls, NLSC runtime, and visual overlay/z-index behavior were not exercised; AC-004, AC-007, and AC-008 remain `NOT VERIFIED`.
