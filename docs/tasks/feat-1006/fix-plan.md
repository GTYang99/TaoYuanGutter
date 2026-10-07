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
