# Implementation Plan

## Goal

- Load all three confirmed GPKGs offline, including the Taoyuan administrative-area layer that currently uses WMTS, while preserving existing layer behavior and the approved tunable outline profile for the two thematic overlays.

## Scope

- Keep the existing offline architecture; use the confirmed GPKGs for the legacy, possible-gutter, and Taoyuan administrative-area layers. Replace the `regions` WMTS in all four map hosts while preserving layer controls, z-order, thematic fills/holes, and unrelated layers.
- Add the default outline profile of 2.5 tile pixels at zoom 16 and below, linear interpolation to 1.5 at zoom 20 and above, with 2.0 at zoom 18; expose endpoints/reference zooms as tunable parameters.
- Preserve the existing visible style of the Taoyuan administrative-area layer; the solid green/red fill requirement applies to the two thematic layers. Run App/runtime checks only on Android emulator. Physical-phone testing is not in scope.

## Affected Files

- `app/src/main/assets/20260601_水務局未調查側溝_v5.gpkg` — bundle the confirmed possible-gutter GPKG.
- `app/src/main/assets/桃園市行政區.gpkg` — bundle the Taoyuan administrative-area source; table `桃園市行政區`, SRS id 100000, MultiPolygon, 13 features.
- `app/src/main/assets/20261006_0601版可能側溝位置.geojson` — remove obsolete possible-layer GeoJSON.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt` — use table `refactored`; normalize the SRS 100000 region geometry to EPSG:3826 before spatial indexing; safely invalidate obsolete task-owned GeoJSON cache.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` — decode both sources as GeoPackage geometry and calculate outline width per tile zoom.
- `app/src/main/java/com/example/taoyuangutter/map/OfflineGeometry.kt` — validate GPKG MultiPolygon decoding and geographic-to-EPSG:3826 coordinate conversion.
- `app/src/main/res/values/strings.xml` — remove the file-extension suffixes from both thematic-layer titles; preserve the existing administrative-area label.
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`, `GutterFormActivity.kt`, and `MapPointPickerActivity.kt` — replace region WMTS providers in the shared and standalone hosts.
- `docs/tasks/feat-1006/evidence/regions-wmts-z13-x6856-y3508.png` and `regions-wmts-baseline.md` — retain the HTTP 200 current-WMTS style reference for parity checks.
- Related unit/instrumentation tests under `app/src/test/` and `app/src/androidTest/` — cover region GPKG metadata, geographic CRS-to-tile coordinates, multipolygon rendering, toggles, request absence, and lifecycle.
- `docs/tasks/feat-1006/asset-manifest.md`, `execution-report.md`, and `verification.md` as implementation and validation proceed.

## Implementation Steps

1. Add the confirmed possible-gutter and Taoyuan administrative-area GPKGs byte-for-byte, remove the prior GeoJSON asset, and record all three source hashes, metadata, feature counts, raw sizes, and resulting package size. Keep derived normalized data in app-private storage only.
2. Configure legacy to read `line_buffer_polygon` and possible-gutter to read `refactored`; add the region table `桃園市行政區` with `MULTIPOLYGON` and SRS id 100000. Validate each source’s CRS and geometry metadata.
3. On the background staging executor, parse region GPKG coordinates in declared axis order (x=longitude, y=latitude) and decode each feature and write a normalized geometry/WKB plus projected bounds into a versioned app-private derived SQLite table, transforming every coordinate into EPSG:3826 using the existing GRS80 Transverse Mercator zone 121 operation: semi-major axis 6,378,137 m; inverse flattening 298.257222101; central meridian 121°; scale 0.9999; false easting 250,000 m; false northing 0 m. The source WKT identifies GCS_TWD97[2020], Taiwan_Datum_1997 (EPSG datum authority 1026), and GRS80; use that declared datum/ellipsoid and the app’s existing projection code, with no ad hoc axis swap, grid shift, or degree-as-meter interpretation.
4. Build the region source’s portable ordinary-table cell index in the normalized EPSG:3826 coordinate space using the existing 512 m grid. Query it using `WebMercatorTileMath.bounds3826`; exact-filter candidate feature bounds, then rasterize through `WebMercatorTileMath.tilePixel`. Keep the original packaged GPKG unchanged; discard/rebuild only the task-owned derived cache when its source hash or transform version changes.
5. Route all three layers through the GeoPackage geometry decoder and preserve polygon members, rings/holes, thematic fills, bounded cache behavior, and provider lifecycle. Render region polygons as the existing WMTS does: transparent interior/background, `#762E77` boundary stroke with antialiased edges, no labels, z-index `-0.5f`, zero transparency.
6. Remove the file-extension suffixes from the two thematic layer titles per the 1014 decision; preserve the existing administrative-area title and all other UI labels.
7. Add a tunable style profile function with broad/close reference zooms and widths, defaulting to (zoom 16, 2.5 px) and (zoom 20, 1.5 px); linearly interpolate between them and pass each tile request's zoom into outline rendering.
8. Extend tests to verify all real GPKGs, metadata, feature queries, MultiPolygon members/holes, exact thematic fill colors, source labels, no obsolete asset, and no feature-data requests; add profile tests for clamps, zoom-18 midpoint, custom parameters, and edge/interior pixels.
9. Add projection control-point tests. For source coordinate (121.000000°, 25.000000°), expect EPSG:3826 (250000.000 m, 2765777.564 m), then z16 tile (54795, 28065), pixel (96.711111, 51.128269). For (121.301100°, 24.992900°), expect (280394.869 m, 2765024.899 m), then z16 tile (54850, 28066), pixel (48.988160, 160.207802). Assert projected coordinates within 0.01 m and tile pixels within 0.01 px. Also query/render a populated source tile and verify indexed candidates against exact feature bounds.
10. Compare the local region tile to the recorded z13/x6856/y3508 WMTS baseline in `evidence/regions-wmts-z13-x6856-y3508.png`: transparent interior/background, `#762E77` opaque stroke core (2 px median) and antialiased footprint (4 px median at alpha ≥64 in the reference transect), and no labels in the sampled tile. Compare style metrics along local boundary transects; use the GPKG as geometry truth rather than requiring WMTS-generalized boundary locations to match pixel-for-pixel. Verify z-index `-0.5f` and zero transparency in all four hosts.
11. Add four-host instrumentation for region visibility state, z-order/transparency, teardown/re-enable deduplication, and absence of `regions` WMTS requests; retain unrelated base-map requests.
12. Run scoped JVM tests, instrumentation, debug/release packaging and all host/network/lifecycle/responsiveness scenarios on Android emulator only; record commands, emulator details, observations, and each unavailable result as `NOT VERIFIED`.

## Test Plan

- Source/package: verify all three GPKG metadata, CRS, feature counts, source hashes and APK entry hashes; verify the obsolete GeoJSON asset is absent.
- Geometry/render: query actual features and verify Polygon/MultiPolygon members, rings/holes, exact opaque thematic fill colors, and local tile coverage.
- Region coordinate path: assert source axis order and GRS80/TWD97 TM2 zone 121 normalization with the two recorded control points; query the z13 baseline tile after conversion, compare indexed candidates to exact feature bounds, and verify a populated output tile.
- Outline profile: assert 2.5 px at zoom 16 and below, 2.0 px at zoom 18, 1.5 px at zoom 20 and above, continuous linear interpolation, and alternate endpoint/reference values. Render both real GPKGs and compare edge, interior, and hole pixels.
- Region style/hosts: compare local z13/x6856/y3508 output to the saved WMTS baseline for transparent fill, `#762E77` stroke, antialiasing, and no labels; verify z-index and transparency in all hosts. Exercise all layer toggles and capture requests to ensure `regions` is not requested.
- UI/hosts/network: assert extension-free titles per the 1014 decision; exercise all three layer toggles in all four map hosts; capture app feature requests to ensure `legacyDitch` and `roadServey` are not requested while unrelated map traffic remains.
- Lifecycle/performance: repeat enable/disable and host teardown/reopen; pan/zoom populated and sparse extents; record load time, responsiveness, and available memory. Acceptance remains no crash, no ANR, and no UI-blocking parse/render.
- Run all App/runtime checks on Android emulator only. No physical phone testing. Mark anything unavailable `NOT VERIFIED` with the reason and next best evidence.

### Physical Device Test Scope

- Requires physical device: No
- Device/environment: Android emulator `emulator-5554` only; record emulator model, Android version, and available-memory observation. Do not use a physical phone.
- In-scope Acceptance Criteria: AC-001 through AC-008
- Regression risk: three-source GPKG dispatch, geographic CRS and MultiPolygon rendering, outline changes by zoom, host toggles/lifecycle, and UI responsiveness.
- Full regression required: No
- Full regression trigger: A failure affecting general map navigation, multiple unrelated layers, or broader application startup.
- Stop condition: Complete all listed emulator scenarios or collect sufficient evidence for a classified blocker; do not substitute physical-device testing.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | APK inspection and Android emulator | Verify packaged hashes/assets; activate all three layers in all hosts and capture app feature requests | All three exact GPKGs are packaged/loaded locally; no obsolete GeoJSON and no `legacyDitch`/`roadServey`/`regions` feature request | APK listing/hash output, emulator request capture, runtime results |
| AC-002 | Android emulator | Inspect populated extents in both layers against the base map | Correct colors, alignment, holes, and MultiPolygon members | Instrumentation and emulator screenshots/notes |
| AC-003 | Android emulator | Toggle all three layers in MainActivity, MapWorkspaceFragment, gutter form, and point picker | Existing controls/state show and hide the correct local layer | Host-by-host instrumentation results |
| AC-004 | Android emulator | Pan/zoom populated and sparse extents in all four hosts with any combination of the three layers enabled | No crash, ANR, or UI-blocking parse/render; responsiveness/load observations recorded | Emulator logs and timing observations |
| AC-005 | Android emulator | Repeat hide/show and teardown/reopen for each host | Provider resources are released/bounded and re-enable shows one correct overlay | Lifecycle assertions and emulator results |
| AC-006 | Android UI instrumentation | Open the layer sheet | The two thematic titles have no file-extension suffix; the administrative-area title and all other UI remain unchanged | UI assertions and diff review |
| AC-007 | JVM unit tests plus Android emulator instrumentation | Evaluate/render at zooms 15, 16, 18, 20, and 21, with default and alternate profiles | Width clamps/interpolates to 2.5/2.0/1.5 px as specified; edge pixels reflect outline, interior fill and holes remain unchanged | Style function assertions and decoded tile pixel comparisons |
| AC-008 | Android emulator, all four map hosts | Enable the Taoyuan administrative-area layer, inspect populated boundaries, toggle off/on, and capture network requests | Packaged `桃園市行政區.gpkg` renders aligned MultiPolygons using the baseline transparent fill and `#762E77` outline with matching 2 px opaque core / 4 px antialiased footprint (no labels in the representative tile), and keeps existing controls, z-index, and transparency; no `regions` request occurs | APK/hash/metadata checks, host screenshots, request capture, and lifecycle observations |

## Regression Plan

- Verify unrelated base-map/overlay requests, projection behavior, layer title layout, colors, holes, region style, and source extents remain unchanged.
- Verify all four hosts keep their existing visibility state, pan/zoom, teardown, and re-enable deduplication on emulator.
- Confirm `legacyDitch`, `roadServey`, and `regions` feature-data requests disappear; unrelated map requests remain.

## Risks

- The possible-layer MultiPolygon metadata may not pass the current Polygon-only GPKG validation path.
- The GPKG-declared geographic CRS must be transformed before using the EPSG:3826 cell index; the plan now fixes axis order, operation parameters, index units, query bounds, and known-point outputs.
- Stale GeoJSON cache invalidation must not remove unrelated app data.
- Incorrect zoom interpolation could invert or jump at zoom boundaries; verify clamps and midpoint.
- Outline rasterization could affect fill/hole pixels; assert independent pixel samples.
- Emulator-only evidence cannot be generalized to physical-phone performance; report its scope honestly.

## Rollback Plan

- Revert the source and outline feature changes to restore the previous WMTS providers, fixed-width behavior, and remove the bundled offline assets.

## Current Behavior

- At the checked-out baseline, the legacy overlay reads GPKG, the possible-gutter overlay still imports GeoJSON, and the administrative-area overlay still uses the `regions` WMTS.
- The tile renderer uses a fixed 2.0 px outline irrespective of zoom.

## Expected Behavior

- All three layers read their packaged GPKGs across all four hosts; the region WMTS is removed, and thematic titles have no extension suffix.
- Outline width varies continuously from 2.5 px at zoom 16 to 1.5 px at zoom 20, with a 2.0 px midpoint at zoom 18 and tunable parameters.
- App/runtime validation is performed only on Android emulator.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–4, 7–9 | APK source/hash inspection and emulator feature-request checks |
| AC-002 | 2–4, 7–8 | Real-file geometry/fill/hole tests and emulator alignment evidence |
| AC-003 | 4, 7–8 | Four-host emulator toggle checks |
| AC-004 | 3, 7–8 | Emulator pan/zoom/no-crash/no-ANR and responsiveness observations |
| AC-005 | 3–4, 7–8 | Cache/provider lifecycle assertions and host reopen checks |
| AC-006 | 5, 7 | Extension-free thematic title assertions and unchanged UI review |
| AC-007 | 6–8 | Profile function and real-tile pixel assertions at endpoint/midpoint zooms |
| AC-008 | 1–5, 9–11 | Packaged source/hash; CRS and control points; populated-tile query; WMTS baseline style parity; all-host toggle and no-WMTS-request evidence |

## Failure Behavior

- Missing/unreadable assets, unsupported CRS/geometry, or parse failures leave the affected layer unavailable, release partial resources, and emit actionable diagnostics without crashing or silently falling back to WMTS. Invalid style parameters must be bounded and logged; they must not corrupt tile rendering.

## Security and Privacy

- The GPKGs contain static map geometry. This work adds no user data or permissions; preserve existing network/data handling for unrelated services.

## Open Questions

- 無。Sources, layer roles, title behavior, visual-preservation requirement, outline profile, and emulator-only test scope are specified.
