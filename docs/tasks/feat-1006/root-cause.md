# FEAT-1006 Root Cause

## Failure

The first focused Android 14 instrumentation run failed both offline polygon tests. The GeoPackage path raised `SQLiteException: no such module: rtree` when querying its existing `rtree_line_buffer_polygon_geom` virtual table. The tile-provider test then received `TileProvider.NO_TILE` because the source query failed.

## Root Cause

`OfflinePolygonDataStore` assumed that Android's platform SQLite build includes the optional RTree virtual-table module. The tested Android 14 image does not include it. The same assumption also exists in the GeoJSON index builder, which creates an RTree virtual table during first-use import. Therefore neither local source can be queried on this platform.

## Acceptance Criteria Affected

- AC-001: local packaged source loading fails during spatial query.
- AC-002: no polygon tiles are produced, so visual colors, holes, and alignment cannot be satisfied.
- AC-003 through AC-005: the hosts can create providers, but their overlays return no tiles; source-query failures also prevent proving interaction and lifecycle behavior.

## Evidence and Classification

- Issue: `ISS-FEAT-1006-002`
- Category: `implementation_regression`
- Priority: P1
- Evidence: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml`

## Regression Risk

Replacing RTree calls may affect index creation time, disk use, and tile-query selectivity. The cell index must preserve bounding-box filtering, deduplicate features that occupy multiple cells, and remain buildable on Android 9+ platform SQLite.

## Resolution

The implementation now stores a versioned 512-meter cell index in ordinary SQLite tables, filters candidate features by their exact bounds, and rebuilds stale indexes when the source checksum or index version changes. The focused Android 14 AVD instrumentation tests passed after the change; see `execution-report.md`. Physical-device host scenarios remain unverified.

## Follow-up Test Harness Failure — ISS-FEAT-1006-003

The new AC-006 layout assertion initially failed before reaching either label assertion. The test inflated `sheet_layers.xml` with the bare application context, whose default `Theme.DeviceDefault` does not define the app's `selectableItemBackgroundBorderless` attribute. The production layout uses `Theme.TaoYuanGutter`; the minimum test-only fix is to inflate with a `ContextThemeWrapper` for that theme. No production UI behavior is implicated.

## Pixel Test Sampling Failure — ISS-FEAT-1006-004

The new color assertion selected a tile from the feature bounding-box center, which is not guaranteed to lie inside a polygon. Host-side geometry inspection confirmed this for the sampled data: the first GeoJSON feature's outer-ring centroid is outside its shell, and the first GPKG feature intersecting the test extent also has its outer-ring centroid outside its shell. The non-`NO_TILE` result only proves a bbox candidate was returned, not that the sampled tile covers polygon fill. This is a test-sampling defect; no production change is justified without evidence from a provably interior point.

The test now finds a sample inside an exterior ring and outside polygon holes, then checks the decoded zoom-20 PNG for the exact layer color. The Android 14 AVD run passed for both datasets, so ISS-FEAT-1006-004 is closed without a production-code change.
