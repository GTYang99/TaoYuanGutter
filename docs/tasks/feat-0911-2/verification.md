# Verification

## Verification Target

- Committed revision: `cc86ed605b342b45502f685222b159c39a3aa3d0` (`feat(feat-0911-2): add deleted-area WMS overlay`)
- Verification scope: approved requirements, implementation diff, test source, public GeoServer capabilities, local validation evidence, CI availability, and regression risk.
- Verification ran on physical device `XQ-AU52` over ADB. No production source or test evidence was modified.

## Evidence Collected

| Evidence | Result | Details |
|---|---|---|
| Committed diff review | PASS | Only the approved main-map provider, controller/state, layer sheet, two map hosts, resource, and targeted tests changed. Point picker and form maps are not in the diff. |
| GeoServer GetCapabilities (2026-09-11) | PASS | `deleted_area` declares `SRS=EPSG:3826`; its style is `TY_RSGDBIP_0910刪除資料`. The implementation builds WMS 1.1.0 requests with that layer, style, SRS, PNG8, 256px dimensions, transparent background, and minX,minY,maxX,maxY BBOX ordering. |
| Targeted test source review | PASS | `Wms3826RequestBuilderTest` independently asserts the fixed four-corner EPSG:3826 vector and encoded WMS contract. `MapOverlayControllerStateTest` covers the default-on state and an off toggle without changing plan state. |
| Clean-revision Gradle check | PASS | A `git archive` copy of the target commit was used to avoid the unrelated dirty `GutterApiService.kt` change. After supplying the local map configuration only to that temporary copy, `:app:testDebugUnitTest` produced 18 passing XML suites (including both task-specific suites) and `:app:assembleDebug` produced `app-debug.apk`. |
| CI | NOT VERIFIED | No CI workflow/configuration was found in the repository and no CI build/test result is attached to this revision. |
| Main-map physical-device smoke test | PASS | Installed the clean-revision APK on `XQ-AU52`, logged in, and exercised the main map. The new checkbox was initially checked; off/on changes and sheet reopening matched; a portrait→landscape→portrait Activity recreation retained the off state; the layer remained on after switching to PHOTO2. No app fatal exception was found in the captured device log. |

## Acceptance Criteria

| AC | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | On the physical device's first main-map load, `cbDeletedArea` displayed `0910刪除資料` and exposed `checked=true`. |
| AC-002 | PASS | The installed clean-revision APK loaded the main map without a crash while its default-on deleted-area state was active. Local tests passed for the fixed EPSG:3826 BBOX and encoded request contract; the live GetCapabilities response confirmed the layer, style, and CRS. |
| AC-003 | PASS | Toggling `cbDeletedArea` to false immediately updated the control; closing and reopening the sheet retained `checked=false`. Toggling it back to true and reopening the sheet retained `checked=true`. |
| AC-004 | PASS | With the layer off, portrait→landscape→portrait Activity recreation retained `checked=false`. With it on, switching to PHOTO2 and reopening the sheet retained `checked=true`. |
| AC-005 | PASS | The physical run retained the existing plan, water-old, possible, and region selections, retained no-ditch as off, showed existing map controls (layers, no-ditch report, measure, and location), and completed the base-map switch without a crash. Diff review confirms picker/form maps and existing WMS providers were unchanged. |

## Implementation and Regression Review

- The implementation follows the approved plan: it introduces a dedicated EPSG:3826 request builder/provider and does not alter `Wms3857TileProvider`.
- The BBOX builder converts all four Google tile corners and takes global min/max values before formatting `minX,minY,maxX,maxY` for WMS 1.1.0.
- The new overlay is isolated as `deletedAreaWmsOverlay`; its state default is true and removal clears the overlay reference. Existing overlays and non-main-map screens remain outside the change scope.
- The physical checks exercised Google Maps `TileOverlay` state lifecycle, sheet listener dispatch, Activity recreation, and a base-map switch. The selected map view did not contain a visually distinguishable deleted-area polygon, so precise visual alignment of a feature edge is supported by request/BBOX evidence rather than a screenshot comparison.

## Issues and Routing

- `ISS-FEAT-0911-2-002` now records the unavailable CI evidence as the only remaining environment blocker.
- No implementation failure was observed in this review; Debug is not indicated.

## Final Result

**NOT VERIFIED.** All acceptance criteria pass with local, source, live-capabilities, and physical-device evidence. Release remains blocked because no CI workflow/result exists for this committed revision; this task must not advance to Release until that gate is resolved or release authority records an exception.
