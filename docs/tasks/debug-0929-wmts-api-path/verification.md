# Verification Report

## Verification Scope

- Fixed source revision: `c4721ee` on `codex/debug-0929-wmts-api-demo-reservation`.
- Scope: endpoint/source review, focused/full JVM tests, debug/release builds, and Android 14 emulator runtime checks on the fixed source revision.
- Runtime test basis: user-approved Android 14 emulator; no authorized test account was available. A configured local Maps key was used for the map UI smoke test and was not printed or committed.

## Acceptance Criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Request-builder tests and emulator requests confirm the active Taipei host and all three WMTS layers; each z0 tile returned HTTP 200 `image/png`. |
| AC-002 | PASS | `BackendEndpoints` contains named legacy DEMO values; runtime source scan found no DEMO selection or fallback. |
| AC-003 | PASS | Source review confirms shared `Wmts3857TileProvider`; emulator launched main map, point picker, and form map surfaces. |
| AC-004 | NOT VERIFIED | API root and WMS capabilities returned 200, but authenticated API, WFS, and GetFeatureInfo flows were not exercised. |
| AC-005 | PASS (static) | WMS/WMTS/API endpoint constants use `taipei.srgeo.com.tw`; NLSC URLs remain unchanged. |
| AC-006 | PASS | Exact-host gate, platform validation for other hosts, and CA-chain behavior pass source/unit review. Debug and release clients both completed emulator TLS requests to Taipei. |
| AC-007 | PASS | NLSC URL template is unchanged; emulator selected NLSC `EMAP01` on the main map and rendered it. Point-picker/form map surfaces also opened. |
| AC-008 | NOT VERIFIED | All three WMTS tiles returned images; main map rendered and the plan overlay checkbox toggled off/on. Other WMS/WFS cases and complete overlay/z-index regression coverage remain open. |

## Security Decision

- No trust-all certificate manager was added.
- The temporary bypass does not disable CA-chain validation and is limited to `taipei.srgeo.com.tw`; API, remote photos and GeoServer tiles now share that client boundary.
- The Taipei server certificate must still be corrected before removing the approved exact-host workaround. The temporary release-only instrumentation/signing setup was removed after testing.

## Final Result

- Developer validation: PASS with environment limitations.
- Independent emulator verification: `PARTIAL`; AC-001, AC-003, AC-005, AC-006, and AC-007 pass, while AC-004 and AC-008 remain `NOT VERIFIED`.
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
| Emulator runtime | PARTIAL | Debug/release TLS requests passed; main/NLSC map, picker/form maps, three WMTS tile requests, and one overlay toggle passed. Authenticated API/photo and remaining overlay regressions remain unverified. |

### Acceptance status

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Shared builder tests pass; emulator fetched z0 tiles for all three active WMTS layers from Taipei, each HTTP 200 `image/png`. |
| AC-002 | PASS | Named DEMO constants remain inactive; runtime source scan found no DEMO fallback. |
| AC-003 | PASS | Source review confirms the shared provider; all three map surfaces opened on emulator. |
| AC-004 | NOT VERIFIED | Static endpoint review passes, but authenticated live API/WFS/WMS/GetFeatureInfo runtime requests were not exercised. |
| AC-005 | PASS | Formal endpoint constants resolve to Taipei; DEMO is retained only as inactive constants. |
| AC-006 | PASS | Exact-host-only verifier and normal verification for other hosts pass source/unit review. Debug and release emulator clients received HTTP 200 from Taipei using the mismatched certificate while retaining platform CA-chain validation. |
| AC-007 | PASS | NLSC URL template is unchanged; the main map rendered NLSC EMAP01 on emulator. |
| AC-008 | NOT VERIFIED | Three WMTS tiles and one plan-overlay off/on toggle passed; remaining WMS/WFS overlays and z-index/regression scope remains open. |

### Final result

`NOT VERIFIED` (`environment`). AC-004 and AC-008 remain open; continue Infrastructure until
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
- No credentials were entered. This initial launch used a placeholder Maps key and stayed at login; a later instrumentation launch used the configured local key to reach the map workspace without a logged-in session.
- This is a PASS for debug APK install/start smoke only; it did not by itself establish API or map behavior.

#### Taipei TLS workaround and map-service smoke (2026-09-29)

- User directed continuing with the temporary Taipei hostname workaround because the app must run.
- Confirmed the production client allows hostname mismatch only for normalized `taipei.srgeo.com.tw`; it still uses OkHttp's normal trust manager. Other hosts go through the platform hostname verifier.
- Temporary instrumentation harness (test-only, removed after the run) used the production `BackendHttpClient` on `emulator-5554` (`sdk_gphone64_arm64`, Android 14/API 34). Command: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.common.TaipeiTlsSmokeTest`.
- Without credentials, Taipei API root returned HTTP 200; WMS GetCapabilities returned HTTP 200 (`text/xml`); z0 GetTile for `roadServey`, `legacyDitch`, and `regions` each returned HTTP 200 (`image/png`). The debug app completed TLS and CA-chain validation despite the SAN mismatch.
- The same API root, WMS capabilities, and three WMTS tile requests returned HTTP 200 in the release variant. The project's default test variant did not generate `connectedReleaseAndroidTest`; for this isolated run, `testBuildType=release` and debug signing were enabled temporarily, then restored. Release `BuildConfig.DEBUG` remained false.
- The API root and capabilities checks do not replace authenticated API/WFS/GetFeatureInfo checks. The NLSC main map was later exercised; remaining overlay/z-index behavior remains open.

#### Main map UI emulator smoke (2026-09-29)

- Used the configured local Maps key without exposing it in output or artifacts.
- Ran a temporary instrumentation smoke with `ActivityScenario.launch(MainShellActivity::class.java)`. `MapWorkspaceFragment` and its map view were present after launch; the Google map rendered. A second run dismissed the location prompt without granting permission, opened the layer sheet, and toggled `cbPlan` off/on; both checks passed.
- Emulator screenshot showed Google map tiles and workspace controls underneath the Android location permission dialog. No location permission was granted and no login data was entered.
- This confirms the main map, NLSC EMAP01 basemap, and one overlay control can run without login. The picker and form map activities also opened. Other overlay toggles/z-index and authenticated overlays remain unverified under AC-008.
- Visual evidence: `evidence/emulator-main-map-emap01.png`.

#### Point-picker and form map emulator smoke (2026-09-29)

- The same instrumentation suite launched `MapPointPickerActivity` with a fixed test coordinate and `GutterFormActivity` with one synthetic in-memory waypoint; both map containers were present after launch and remained active for five seconds. The test passed without login or submitting/saving data.
