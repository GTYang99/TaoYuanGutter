# FEAT-1006 Fix Plan

## Scope

Resolve `ISS-FEAT-1006-002` by removing dependence on the optional SQLite RTree module while preserving the approved offline data behavior and all existing acceptance criteria.

## Minimum Fix

1. Use ordinary SQLite tables for a fixed 512-meter EPSG:3826 cell index; store cell coordinates and feature IDs with a composite primary key, plus each feature's exact projected bounds.
2. Build the GeoPackage cell index once on the existing background executor by scanning its feature rows and extracting GeoPackage envelope bounds; persist a source-checksum marker so later opens reuse the index.
3. Build the GeoJSON cell index in the existing streaming import transaction, recording each feature's WKB, exact bounds, and touched cells.
4. Query intersecting cells and exact feature bounds, deduplicate feature rows, then decode and render as before. Do not query or create `rtree_*` virtual tables.
5. Rerun `:app:testDebugUnitTest`, focused `:app:connectedDebugAndroidTest`, and release APK packaging/asset verification.

## Files

- `OfflinePolygonDataStore.kt`
- `OfflineGeometry.kt` only if a GeoPackage envelope-bounds reader is needed
- `OfflinePolygonDataStoreInstrumentedTest.kt`
- `execution-report.md`, `issue-log.md`, and `state.yaml`

## Validation

- Confirm both packaged datasets return polygon geometry for the Taoyuan extent on the Android 14 AVD.
- Confirm an offline provider returns a real tile for a populated tile request, and after `release()` rejects requests.
- Keep physical-device scenarios and all four host interaction checks marked `NOT VERIFIED` until an Android 9+ physical device is available.

## Completion Evidence

- Android 14 `Medium_Phone(AVD) - 14`: focused instrumentation passed, 2 tests / 0 failures.
- `:app:testDebugUnitTest`: PASS.
- `:app:assembleRelease`: PASS; both source asset byte lengths and SHA-256 values match the originals.
- Code fix committed as `dddd391` (`fix(FEAT-1006): replace SQLite RTree dependency`).

## ISS-FEAT-1006-003 Test-Only Fix

Apply `Theme.TaoYuanGutter` to the instrumentation test's layout-inflation context with `ContextThemeWrapper`, then rerun `layerSheetInflatesExactOfflineSourceLabels` and the focused test class. Do not change production resources or layout for this harness failure.

Completion: exact layer labels are verified after inflating `sheet_layers.xml` with the app theme; focused AVD suite passed.

## ISS-FEAT-1006-004 Test Sampling Fix

Choose a sample point that is verified by point-in-ring checks to lie within an exterior ring and outside its holes. Request a high-zoom tile containing that point and assert its PNG contains the exact opaque color for the layer. This keeps the pixel assertion tied to actual polygon coverage rather than a feature bounding box.

Completion: `findInteriorPoint` selects a polygon interior sample and the focused AVD test decodes both real-data tiles and finds the expected opaque color; 3 tests passed with 0 failures.
