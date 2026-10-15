# Repository Analysis

## Task Classification

- Type: `feature` — replace the legacy and possible-gutter feature sources and the Taoyuan administrative-area WMTS with app-packaged GPKGs; retain the approved zoom-dependent outline profile.

## Current Behavior

- The checked-out baseline at `b5254c7` already has offline polygon rendering for the legacy GPKG and a GeoJSON import/index path for the possible-gutter source.
- `OfflinePolygonDataStore.openGeoPackage` is currently used only by the legacy layer and validates `POLYGON`; the possible layer reads the earlier GeoJSON.
- The currently planned thematic-layer titles include file extensions; the 1014 decision requires removing those suffixes. The administrative-area overlay still uses the `regions` WMTS provider.
- `OfflinePolygonTileProvider` draws a fixed `2.0f` tile-pixel outline independent of the requested zoom. Existing tests compare the fixed default and an injected alternate width, but do not assert the new profile at zooms 16/18/20.
- Existing execution and APK evidence refers to earlier source pairings and does not establish the final three-GPKG package or administrative-area rendering.

## Expected Behavior

- Bundle and read all three confirmed GPKGs locally: legacy table `line_buffer_polygon` (`POLYGON`) and possible-gutter table `refactored` (`MULTIPOLYGON`), both EPSG:3826; administrative area table `桃園市行政區` (`MULTIPOLYGON`, SRS id 100000).
- Use the existing background staging and portable SQLite cell index for both sources, validate source metadata, remove the old GeoJSON asset, and safely invalidate only stale app-private cache artifacts owned by the prior importer.
- Render both sources through the GeoPackage geometry path while preserving rings, holes, MultiPolygon members, colors, four-host wiring, labels, and lifecycle behavior.
- Compute outline width from the tile zoom: 2.5 px at zoom <=16, linearly interpolate from zoom 16 to 20 (2.0 px at zoom 18), and use 1.5 px at zoom >=20. Keep endpoint values and reference zooms tunable.
- `桃園市行政區.gpkg` has table `桃園市行政區`, geometry column `geom`, `MULTIPOLYGON`, SRS id 100000 (`GCS_TWD97[2020]`), 13 features, and geographic bounds 120.982033–121.479996 longitude / 24.586456–25.123613 latitude. Geometry coordinates are longitude (x), latitude (y). Normalize them on the background staging path into EPSG:3826 using the existing `Twd97Projection.longitudeLatitudeToTwd97` implementation (GRS80; TM2 zone 121; central meridian 121°; k0=0.9999; false easting 250000 m; false northing 0 m), then reuse the established 512 m cell index, `bounds3826` tile queries, and inverse projection for tile pixels. This avoids treating degrees as meters and applies the GPKG-declared Taiwan_Datum_1997/GRS80 operation with the app’s existing EPSG:3826 math.
- A baseline `regions` WMTS tile was fetched successfully at z13/x6856/y3508 and recorded in `evidence/regions-wmts-baseline.md` plus its PNG. Its fill/background is transparent, opaque stroke core is `#762E77` with antialiasing, and there are no labels in that tile. Existing overlay z-index is -0.5 and transparency is 0.0.
- Replace the `regions` WMTS in the shared controller and the form and point-picker hosts; preserve its visibility toggle and z-index.
- Run App/runtime tests only on Android emulator; physical-phone testing is out of scope.

## Affected Modules

- `app/src/main/assets/` — add the confirmed possible-gutter GPKG and `桃園市行政區.gpkg`, and remove the obsolete possible-layer GeoJSON.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt` — route all three layers through GeoPackage staging/indexing, map the possible layer to `refactored`, normalize the region layer from SRS 100000 to EPSG:3826 before building the 512 m index, validate geometry metadata, and clean up only obsolete task-owned GeoJSON cache.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` — use GPKG geometry decoding for both sources and calculate the outline width from the requested tile zoom.
- `app/src/main/java/com/example/taoyuangutter/map/OfflineGeometry.kt` — verify GeoPackage MultiPolygon members and holes decode correctly; reuse the existing decoder where supported.
- `app/src/main/res/values/strings.xml` — remove file-extension suffixes from the two thematic-layer titles; preserve other labels.
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`, `GutterFormActivity.kt`, and `MapPointPickerActivity.kt` — dispatch the region toggle to the local provider instead of `regions` WMTS, preserving z-index `-0.5f` and zero transparency.
- `docs/tasks/feat-1006/evidence/regions-wmts-z13-x6856-y3508.png` and `regions-wmts-baseline.md` — retain the current server-rendered style baseline for local parity checks.
- Related unit and instrumentation tests under `app/src/test/` and `app/src/androidTest/` — real GPKG metadata/query/render coverage, CRS conversion, region style/control checks, and zoom profile assertions.
- `docs/tasks/feat-1006/` — update manifest, execution evidence, and verification as their phases proceed.

## Dependencies

- Confirmed GPKG files and their schemas/metadata.
- Existing GPKG staging, ordinary-table 512-meter cell index, geometry codec, offline tile renderer, Google Maps SDK, and map-layer controls.
- `OfflinePolygonTileProvider.getTile(x, y, zoom)` already receives zoom; no new map-host API or external rendering dependency is needed.
- Android emulator `emulator-5554` for App/runtime evidence; no physical phone may be used.

## Risks

- The 61,435-feature possible-layer source is a MultiPolygon GPKG; strict Polygon-only metadata checks or wrong table mapping could produce blank tiles.
- The administrative-area GPKG uses geographic SRS id 100000 (`GCS_TWD97[2020]`), unlike the existing EPSG:3826 path; incorrect axis interpretation or datum/projection handling could misalign the boundary overlay.
- The three GPKGs total 81,358,848 bytes (about 77.7 MiB) raw; package all and record actual artifact size/compression. No APK/AAB size cap applies to this trial.
- Old GeoJSON index/cache files may consume storage or be reused unless invalidated safely.
- The outline profile can be inverted or discontinuous at its endpoints; tests must cover both clamps and midpoint.
- Outline painting must not alter fill colors or holes; use edge, interior, and hole pixel assertions.
- Emulator-only runtime results do not establish physical-phone performance; report the environment accurately.

## Unknowns

- The source file and schema are known. Verify the declared GCS_TWD97[2020] longitude/latitude coordinates through the specified TM2 Zone 121 transform, control-point results, and the representative WMTS visual reference.

## Potential Issue Categories

- `planning_gap`: any new source, region styling, CRS behavior, or zoom-style behavior lacks AC traceability or a validation step.
- `implementation_regression`: parse/index/render failure, incorrect colors/holes/outline profile, duplicate/leaked overlays, crash, ANR, or UI stall.
- `environment`: missing source/package/build capability or unavailable Android emulator.
