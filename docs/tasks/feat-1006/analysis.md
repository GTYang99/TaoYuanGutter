# Repository Analysis

## Task Classification

- Type: `feature` — replace two WMTS feature sources with app-packaged offline GPKG data and support these overlays in all existing map contexts.

## Current Behavior

- The current FEAT-1006 implementation branch already renders the legacy overlay from a GPKG and routes main map, form, and point-picker contexts through offline polygon providers.
- The possible-gutter source is still the earlier GeoJSON file. `OfflinePolygonDataStore` indexes/imports that source separately, and `OfflinePolygonTileProvider` selects its GeoJSON WKB path.
- The current possible-layer option title ends in `.geojson`; the requested title must end in `.gpkg`.
- Prior implementation evidence and tests cover the earlier legacy-GPKG/possible-GeoJSON pair. They do not establish that the newly confirmed possible-layer GPKG parses, indexes, packages, or renders.
- Both newly confirmed inputs declare EPSG:3826. The legacy source is a `Polygon` table with 36,970 records; possible-gutter source is a `MultiPolygon` table with 61,435 records. Their raw sizes are 19,615,744 bytes and 60,661,760 bytes (80,277,504 bytes total, about 76.6 MiB).

## Expected Behavior

- Package and read both confirmed GPKGs locally in all four map hosts, and stop requesting their corresponding `legacyDitch` / `roadServey` feature data.
- Use the current GPKG staging and portable spatial cell-index strategy for both layers; validate each source's CRS and expected geometry metadata. The possible source requires accepting `MULTIPOLYGON` and mapping its `refactored` table.
- Render both sources through the GeoPackage geometry path, preserving polygon rings/holes and fill colors. Make the same-color outline wider at broad zoom (2.5 tile pixels at zoom 16 and below) and narrower at close zoom (1.5 tile pixels at zoom 20 and above), with continuous interpolation and tunable style parameters.
- Show exactly `水務局舊資料（.gpkg）` and `可能側溝位置（.gpkg）` for the two options.
- Remove the old GeoJSON asset and safely invalidate task-owned GeoJSON import/index artifacts so a prior installed build cannot retain the obsolete large cache.

## Affected Modules

- `app/src/main/assets/` — add `20260601_水務局未調查側溝_v5.gpkg`; remove `20261006_0601版可能側溝位置.geojson` from the app assets.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonDataStore.kt` — map the possible layer to its GPKG file/table, validate MultiPolygon metadata, use the GeoPackage path, and clean up obsolete task-owned import cache safely.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` — ensure both layers decode GeoPackage geometry through the same supported path.
- `app/src/main/java/com/example/taoyuangutter/map/OfflineGeometry.kt` and related tests — confirm GeoPackage `MultiPolygon` decoding and hole/member handling.
- `app/src/main/res/values/strings.xml` and affected UI/instrumentation tests — update the possible-layer title to `.gpkg`.
- `app/src/test/` and `app/src/androidTest/` — cover the real new source, package contents, GPKG metadata and geometry, styles, tile queries, and source-label behavior.
- `docs/tasks/feat-1006/` — update the source manifest and, during implementation, execution and validation evidence.

## Dependencies

- The two user-confirmed GPKGs and their schemas/metadata.
- Existing GeoPackage staging, SQLite-backed 512-meter cell index, geometry decoder, offline tile provider, Google Maps SDK, and layer controls.
- Existing Android SQLite ordinary-table index path; do not depend on optional SQLite RTree support.

## Risks

- The new source is a 61,435-feature MultiPolygon GeoPackage, while current input path is GeoJSON. Incorrect metadata validation or geometry dispatch could yield blank tiles or malformed fills.
- Bundled raw data totals about 76.6 MiB. Record APK/AAB size and compression; no trial size limit applies.
- Existing instrumented passes against the old pair cannot serve as proof for the new possible-layer source.
- A stale GeoJSON-generated private cache may consume storage or be incorrectly reused after upgrade unless it is invalidated safely.
- Physical-device scenarios AC-001 through AC-005 remain outstanding per `ISS-FEAT-1006-001`; the acceptance criterion remains no crash, no ANR, and no parse/render blocking of the UI, with observations recorded.

## Unknowns

- Whether the current GPKG reader accepts the possible source's `MULTIPOLYGON` metadata and table naming without code changes beyond format dispatch. Resolve by testing against the exact file.
- Release artifact size/compression and physical-device responsiveness with the larger possible-layer GPKG; record measurements during implementation/validation.
- Future authoritative data versions are explicitly outside this trial.

## Potential Issue Categories

- `planning_gap`: a required handling detail for the confirmed source is absent from the approved plan.
- `implementation_regression`: parse/index/render failures, bad geometry/fills, resource leaks, crashes, ANRs, or UI stalls.
- `environment`: inability to package the source, build, or run required device checks.
