# Implementation Plan

## Goal

- Update FEAT-1006 so both offline map overlays use the two user-confirmed GPKG sources in all existing map hosts.

## Scope

- Replace the possible-gutter GeoJSON input with the confirmed GPKG; retain the existing legacy GPKG, colors, offline tile architecture, controls, and host coverage. Update the outline behavior to vary smoothly with zoom so it is clearer at broad scale and restrained at close scale.
- Bundle only the two confirmed GPKGs for these overlays and update the two option titles to `.gpkg`.
- Do not change unrelated map layers, UI, or permanent data-source policy.

## Affected Files

- `app/src/main/assets/20260601_水務局未調查側溝_v5.gpkg` — new bundled possible-gutter source.
- `app/src/main/assets/20261006_0601版可能側溝位置.geojson` — remove obsolete source asset.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt` — possible-layer GPKG table selection, metadata checks, indexing, and obsolete cache invalidation.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` and `OfflineGeometry.kt` — GPKG geometry dispatch, MultiPolygon coverage, and zoom-aware outline styling.
- `app/src/main/res/values/strings.xml` and relevant tests — update/assert both `.gpkg` titles.
- Related map parser/index/provider unit and instrumentation tests under `app/src/test/` and `app/src/androidTest/`.
- `docs/tasks/feat-1006/asset-manifest.md`, `execution-report.md`, and `verification.md` as their corresponding phases proceed.

## Implementation Steps

1. Add the confirmed possible-gutter GPKG unchanged and remove the earlier GeoJSON asset; record both actual input hashes, schema metadata, feature counts, raw sizes, and release packaging size.
2. Update offline source configuration so legacy reads `line_buffer_polygon` and possible-gutter reads `refactored`; validate EPSG:3826 and the expected Polygon/MultiPolygon table geometry types.
3. Use the existing background GPKG staging and portable SQLite cell index for both sources; do not depend on optional RTree support. Safely invalidate only obsolete app-private artifacts created by the previous GeoJSON importer.
4. Route both layers through the GeoPackage geometry decode path; preserve rings, holes, MultiPolygon members, and existing fill colors. Calculate the same-color outline width continuously from zoom using tunable endpoint values: 2.5 tile pixels at zoom 16 and below, 1.5 at zoom 20 and above.
5. Change only the possible-layer option suffix from `.geojson` to `.gpkg`; assert exact titles for both options.
6. Extend focused tests to parse/query/render the exact two GPKGs, validate the possible source MultiPolygon path, and check layer lifecycle, package contents, style, and absence of the old GeoJSON asset/source.
7. Run relevant unit and instrumentation checks, debug/release packaging checks, and the approved Android-emulator scenarios; record observations and any unavailable checks without treating them as PASS.

## Test Plan

- Data: verify both packaged source hashes, GPKG metadata, CRS, feature counts, geometry types, and successful spatial queries for real features.
- Geometry/render: verify Polygon and MultiPolygon members, holes, exact opaque fill colors, and broad/close outline widths at zooms 16 and 20, including a midpoint interpolation check and an alternate injected profile.
- Package/UI: inspect APK for both GPKGs and no obsolete GeoJSON source; verify both exact `.gpkg` layer titles and no unrelated UI changes.
- Network/hosts: check that enabling either offline layer does not fetch `legacyDitch`/`roadServey` feature data and verify existing toggles in all four map hosts.
- Lifecycle/performance: exercise hide/show, map teardown/reopen, and pan/zoom. Record observed load time/responsiveness and available memory; acceptance is no crash, no ANR, and no UI-blocking parse/render.
- Run relevant project unit-test source checks, Android instrumentation, and debug/release builds. Run app/runtime tests only on the Android emulator; do not test on a physical phone. Checks not run or unavailable must be marked `NOT VERIFIED` with a reason.

### Emulator Test Scope

- Requires physical device: No
- Device/environment: Android emulator `emulator-5554`; record emulator model, Android version, and available-memory observation.
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007
- Regression risk: local GPKG loading, coordinate/geometry rendering, layer controls, UI responsiveness, and provider/cache lifecycle across map hosts.
- Full regression required: No
- Full regression trigger: A failure affecting general map navigation, multiple unrelated layers, or broader application startup.
- Stop condition: Complete the listed scenarios in all four map hosts, or record sufficient evidence for a classified blocker.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | APK inspection and Android emulator with feature-data network unavailable | Inspect packaged assets; launch each map host and enable both layers | Both exact GPKGs load locally; no `legacyDitch`/`roadServey` feature-data request | APK listing/hashes, request capture, emulator runtime result |
| AC-002 | Android emulator, populated extents in both data sets | Enable each overlay and compare representative features with basemap | Correct opaque colors, geographic alignment, holes, and MultiPolygon rendering | Emulator notes and screenshots on failure |
| AC-003 | Android emulator | Toggle each layer in `MainActivity`, `MapWorkspaceFragment`, gutter form, and point picker | Existing controls reveal/hide the correct offline overlays | Host-by-host results |
| AC-004 | Android emulator | Pan and zoom across populated and sparse areas with each/both layers enabled | No crash, ANR, or parse/render UI blocking; record observed responsiveness/load behavior | Emulator notes, logs, and observations |
| AC-005 | Android emulator | Repeatedly disable/enable layers and leave/reopen each host | Removed overlay releases/bounds its provider resources; re-enable shows one correct overlay | Lifecycle results and available resource observations |
| AC-006 | Android UI test on emulator | Open layer sheet and inspect titles | Exact titles are `水務局舊資料（.gpkg）` and `可能側溝位置（.gpkg）`; other UI unchanged | UI assertion/result |
| AC-007 | Instrumented tile-render test on emulator | Render both real GPKGs at zooms 16, 18, and 20 using default and alternate profiles | Width is 2.5 px at zoom 16 and below, 2.0 px at zoom 18, and 1.5 px at zoom 20 and above; fills and holes stay intact | Style-function assertions and rendered tile pixel comparisons |

## Regression Plan

- Verify base-map requests, regions, no-ditch points, deleted-area overlay, measurement labels, user gutter lines, and layer-state restoration are unchanged.
- Verify form and point-picker gestures/camera navigation remain usable with both local overlays enabled.
- Verify map teardown releases provider/cache resources and no obsolete GeoJSON cache is reused.

## Risks

- The new possible-layer source uses MultiPolygon GeoPackage data; metadata or decoder assumptions may reject it or produce empty/incorrect tiles.
- The source bundle totals about 76.6 MiB raw; record packaged size and compression, with no trial APK/AAB cap.
- Prior runtime evidence covers the previous GeoJSON source and must be repeated for this GPKG.
- Request-capture and basemap-alignment checks remain outstanding until their specific emulator evidence is recorded. Physical phones are out of scope by user direction.

## Rollback Plan

- Revert the feature change to restore the prior online providers and remove the bundled offline assets.

## Current Behavior

- The existing FEAT-1006 branch reads the legacy overlay from GPKG but still reads the possible-gutter overlay from a GeoJSON file and labels it `.geojson`.
- Prior tests and APK evidence are for that earlier source pair.

## Expected Behavior

- Both overlays use the two confirmed packaged GPKGs through the local GPKG reader and tile-render path, with existing colors, holes, controls, and host coverage preserved. Their outline is slightly thicker at broad scale and thinner at close scale through a continuous, tunable zoom profile.
- The layer options identify both files as `.gpkg`, and the old GeoJSON source is not packaged or loaded.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–3, 6 | APK asset/hash inspection and feature-network request check |
| AC-002 | 2, 4, 6 | Real-file geometry/projection/tile assertions and emulator alignment/color check |
| AC-003 | 4, 6 | Existing controller tests and host-by-host toggle checks |
| AC-004 | 3, 6, 7 | Background parsing checks and four-host emulator pan/zoom observations |
| AC-005 | 3, 6, 7 | Cache/provider lifecycle tests and repeated host toggling/teardown |
| AC-006 | 5, 6 | Exact option-title UI assertions; verify other labels/layout unchanged |
| AC-007 | 4, 6 | Style and rendered tile assertions for zoom interpolation, endpoint widths, fills, and holes |

## Failure Behavior

- If an asset is missing, unreadable, has an unsupported CRS or geometry type, or fails parsing, do not crash the map; release partial resources, leave the affected overlay unavailable, and emit actionable diagnostics. Do not silently fall back to the online feature source.

## Security and Privacy

- The bundled files contain static map geometry and add no user data or permissions. Preserve existing data handling and network behavior for unrelated services.

## Open Questions

- 無。The input files and trial layer roles are confirmed; future source refresh decisions are outside this task.
