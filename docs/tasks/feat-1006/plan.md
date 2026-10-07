# Implementation Plan

## Goal

- Replace the legacy `legacyDitch` and possible-gutter `roadServey` feature overlays with locally rendered, app-packaged polygon data in every existing map context, for this exploratory dataset pairing.

## Scope

- Include only the two explicitly linked files, their offline parse/index/tile-render path, wiring in `MainActivity`, `MapWorkspaceFragment`, form, and point-picker map hosts, tests, and task evidence.
- Change only the two layer-option titles to append `.gpkg` and `.geojson`; keep layout, all other labels, and other online base/region/no-ditch/deleted-area layers unchanged.
- Add a configurable same-color boundary outline to both offline polygon layers, default 2.0 tile pixels; preserve existing fill colors and holes, with screen-space thickness stable across zoom levels.
- Treat current file-to-layer pairing as trial-only; definitive data mapping remains a follow-up before later releases.

## Affected Files

- `app/src/main/assets/20260824_水務局既有側溝更新v3_線轉面.gpkg` and `app/src/main/assets/20261006_0601版可能側溝位置.geojson` — proposed bundled source data.
- `app/src/main/res/values/strings.xml` — append the corresponding file suffixes to the two layer-option titles only.
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt` — local providers for main map layer toggles.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — shared local overlay wiring.
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt` — shared local overlay wiring.
- New map data/parser/index and offline tile provider classes — isolate IO, projection, geometry conversion, filled raster-tile generation, cache bounds and lifecycle.
- New `app/src/test/java/com/example/taoyuangutter/map/` tests for parsing, projection, geometry conversion, tile bounds, fill colors, and provider/toggle behavior.
- Focused `app/src/androidTest/` coverage for main/form/point-picker overlays and toggle/lifecycle behavior, subject to existing test harness support.
- `app/build.gradle.kts` and `gradle/libs.versions.toml` only if the implementation spike establishes a necessary compatible GeoPackage reader dependency.
- `docs/tasks/feat-1006/` execution and validation evidence as work advances.

## Implementation Steps

1. Add the two specified source files as app assets and record their checksums, source names, true formats, declared CRS, feature counts, and size in an asset manifest/task evidence; do not substitute the extra GeoJSON file.
2. Spike GPKG feature iteration and GeoJSON streaming decode with the existing stack first; select a compatible GPKG reader only if the actual file cannot be decoded safely with available APIs, and record APK-size implications.
3. Define a shared local data source that parses on a background dispatcher, stages the GPKG to app-private storage if required by the reader, validates EPSG:3826, preserves polygon rings/holes and GeoJSON MultiPolygon members, and spatially indexes features without retaining every geometry as a Google Maps object.
4. Implement EPSG:3826 tile/query bounds and coordinate conversion using a tested projection path; expose or reuse projection math without changing existing WMTS request behavior.
5. Implement an offline `TileProvider` (or equivalent bounded viewport renderer) that generates 256px tiles with opaque `#156D1D` / `#FA0000` polygon fills and preserved holes, uses bounded/releasable cache state, and never performs feature-data network requests.
6. Update the two layer-option strings to `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`; preserve the rest of the UI unchanged.
7. Route `MapOverlayController`, `GutterFormActivity`, and `MapPointPickerActivity` through the shared offline renderer while preserving existing toggles, z-order, state restore, and unrelated WMTS layers.
8. Add unit tests for both real file formats, CRS/projection known points, Polygon/MultiPolygon rings and holes, tile intersection/fill output, empty/out-of-range requests, and visibility/resource lifecycle; add a UI assertion for the two option titles.
9. Run targeted unit tests, assemble the configured APK/AAB, record packaged size and asset compression, then perform focused physical-device checks for AC-001–AC-006 across all four map hosts; record any unavailable check as `NOT VERIFIED`.
10. Draw each queried polygon path using the existing fill, then its same-color outline using `OfflinePolygonStyle.OFFLINE_POLYGON_STROKE_WIDTH_PX` (default `2.0f`). Allow the provider to receive an alternate width for tests, and verify at broad and close zooms that width variation changes only the outline.

## Test Plan

- Unit: actual GPKG metadata and feature parsing, GeoJSON FeatureCollection/MultiPolygon streaming parse, EPSG:3826 conversion, ring/hole preservation, spatial filtering, and tile rendering colors (`#156D1D` / `#FA0000`) and coordinates.
- Unit/instrumentation: existing overlay state/toggle restoration and provider lifecycle (disable/remove/re-enable without duplicated overlays or retained cache).
- Build/package: assemble release artifact and inspect that both assets are packaged; record APK/AAB size and whether packaging compresses either file.
- UI: verify the two option titles show the correct current source suffixes and that no other labels/layout changed.
- Rendering: verify the default and alternate outline widths for both real data layers at broad and close zooms, while checking fill colors and holes remain correct.
- Network: verify no feature-data requests go to WMTS `legacyDitch` or `roadServey`; verify unrelated base map and other overlays retain their current requests.
- Physical device: Android 9+ device, feature-source network requests blocked; use `MainActivity`, `MapWorkspaceFragment`, gutter form, and point-picker map hosts to inspect alignment, opaque fill, toggles, pan/zoom, and return/reopen lifecycle.

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android 9+ physical device, preferring the lowest-memory available supported device; record model, OS build, and available memory in the execution report. If no suitable device is available, mark device-only checks `NOT VERIFIED`.
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003, AC-004, AC-005
- Regression risk: map rendering responsiveness, app memory use, overlay state persistence, and unaffected neighboring WMTS overlays.
- Full regression required: No
- Full regression trigger: A failure affecting general map navigation, multiple unrelated layers, or broader application startup.
- Stop condition: All listed scenarios complete in the four map hosts, or sufficient failure evidence identifies a blocker.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | APK inspection and Android device with feature-data network unavailable | Inspect package contents; launch each map context and enable both layers | Both bundled files load locally, with no `legacyDitch`/`roadServey` feature-data request | Package listing/size, HTTP request log, result; screenshot on failure |
| AC-002 | Android device, representative dataset extents | Enable each layer and compare geometry against basemap | Legacy polygons use opaque `#156D1D`; possible polygons use opaque `#FA0000`; holes and alignment are correct | Device run notes and screenshot on failure |
| AC-003 | Android device | Repeat layer on/off in MainActivity, MapWorkspaceFragment, gutter form, and point-picker maps | Existing controls reveal/hide the correct local overlays in all four hosts | Instrumentation result or device run record |
| AC-004 | Android device | With each/both layers enabled, pan and zoom through populated and sparse coverage in all four hosts | No crash, ANR, or blocking parse/render operation; map remains interactive; record load/memory observations | Instrumentation/device result; record observed loading behavior |
| AC-005 | Android device, memory/resource inspection where available | Toggle both layers off/on repeatedly and leave/reopen all four map hosts | Removed layer is absent; re-enable restores one copy with provider-owned cache released/bounded and no duplicate overlay | Instrumentation result or device run record |
| AC-006 | Android UI test | Open the layer sheet and inspect the two overlay option titles | Titles exactly identify `.gpkg` and `.geojson`; other labels/layout are unchanged | UI assertion/result; screenshot on failure |
| AC-007 | Unit/instrumented tile-render test | Render both real polygon layers at broad and close zoom levels using default and alternate widths | Same-color outline is visible at 2.0 tile pixels by default, remains stable in screen space across zoom levels, alternate widths are honored, and fills/holes remain correct | Pixel assertions for fill, outline, and hole regions |

## Regression Plan

- Confirm base map tile layer, regions, no-ditch points, deleted-area overlay, measurement labels, user gutter lines, and layer state restoration remain unchanged.
- Confirm form and point-picker gestures/camera navigation still work with offline overlays enabled.
- Confirm app startup and map teardown release offline provider/cache resources.

## Risks

- The assets total about 112 MiB uncompressed; the release artifact may be substantially larger.
- The GPKG contains 36,970 Polygon records and the GeoJSON contains 61,435 MultiPolygon features; eagerly materializing all geometries in map objects is out of scope for the renderer design.
- Both inputs use EPSG:3826; conversion and polygon fill/hole semantics need explicit tests.
- Android 9 support and available memory constrain GeoPackage and tile-cache choices.
- The selected data-to-layer pairing is trial-only and may change after the test. No APK/AAB size cap applies to this trial; record artifact size. Use qualitative performance criteria and record device observations.

## Rollback Plan

- Revert the feature commit to restore the previous WMTS providers and remove the packaged data assets.

## Current Behavior

- The main maps load legacy data from `legacyDitch` WMTS and the possible-gutter toggle from `roadServey`; the form and point-picker maps duplicate these online loads.

## Expected Behavior

- The existing map hosts load two app-packaged polygon sources and render opaque `#156D1D` / `#FA0000` local tiles while preserving polygon holes, existing controls, and unrelated online layers.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1, 2, 5, 6 | APK content/size check; network request log with feature endpoints unavailable |
| AC-002 | 3, 4, 5 | Geometry/projection/tile-render unit tests and device alignment/color check |
| AC-003 | 7 | Controller/provider tests and device toggle check in all four map hosts |
| AC-004 | 2, 3, 5, 9 | Background parsing tests and physical pan/zoom/no-ANR scenarios in all four map hosts |
| AC-005 | 5, 7, 8 | Cache/provider lifecycle tests and repeated device toggling/teardown |
| AC-006 | 6, 8 | UI assertion for exact layer option labels; review other labels/layout for no changes |
| AC-007 | 10 | Unit/instrumented tile-pixel assertions for outline configuration, zoom stability, fill colors, and holes |

## Failure Behavior

- If an asset is missing, unreadable, has an unsupported CRS/geometry, or fails parsing, do not crash the map; leave the affected overlay unavailable, release partial resources, and emit actionable diagnostic evidence. Do not silently fall back to the online feature source.

## Security and Privacy

- The bundled data is static map geometry; it adds no user information, credentials, or new permissions. Preserve existing data handling and network behavior for unrelated services.

## Open Questions

- OQ-001: After this test, confirm the definitive production source files and permanent file-to-layer mapping. The trial uses the two linked files and labels the options by their current file suffixes.
