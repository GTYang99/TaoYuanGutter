# FEAT-1006 Issue Log

## ISS-FEAT-1006-001 — Physical Android device unavailable for planned host scenarios

- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Impact: Planned physical-device checks for AC-001 through AC-005 remain outstanding. A local Android 14 AVD and a connected Android 12 device provide partial runtime coverage.
- Evidence: On 2026-10-07, targeted AC-007 instrumentation passed on `XQ-AU52` Android 12. The planned four-host network, alignment, toggle, pan/zoom, and repeated-lifecycle scenarios for AC-001 through AC-005 have not been run. See execution report for exact coverage.
- Next action: verification
- Remediation: run the remaining plan-scoped scenarios on the available Android 9+ device; record any unavailable case as `NOT VERIFIED`.

## ISS-FEAT-1006-002 — Android SQLite lacks the RTree module

- Phase: implementation
- Category: implementation_regression
- Priority: P1
- Status: closed
- Impact: Before the fix, both local polygon data sources failed spatial queries on Android; tiles returned `NO_TILE`.
- Evidence: After the portable cell-index fix, the focused instrumentation class passed on Android 14 (`3 tests, 0 failures`), including local polygon queries, exact-color tiles from both sources, the label assertions, and legacy provider release.
- Root cause: The implementation depended on the optional SQLite RTree virtual-table module, which is not present in the platform SQLite build used by Android 14. The dependency affects the existing GPKG RTree and the GeoJSON index created at runtime.
- Remediation: Replaced RTree with a versioned, persistent 512-meter cell index made from ordinary SQLite tables for both data sources. Index queries also filter by exact feature bounds.
- Resolution: Re-verified on Android 14 AVD; closed.

## ISS-FEAT-1006-003 — Layer-label test initially inflated app layout without the app theme

- Phase: debug
- Category: implementation_regression
- Priority: P2
- Status: closed
- Impact: The initial test harness could not inflate the app Material layout, so AC-006 runtime evidence was delayed.
- Evidence: Applying `Theme.TaoYuanGutter` with `ContextThemeWrapper` allowed `layerSheetInflatesExactOfflineSourceLabels` to pass on Android 14 AVD.
- Root cause: The instrumentation test inflated the app's Material layout using the application context without applying `Theme.TaoYuanGutter`.
- Remediation: Wrapped the layout-inflation context in `ContextThemeWrapper` using `Theme.TaoYuanGutter` and used the layout's actual `MaterialCheckBox` type.
- Resolution: Focused AVD run passed the exact two label assertions; closed.

## ISS-FEAT-1006-004 — Tile output lacked expected opaque color in sampled feature tile

- Phase: debug
- Category: implementation_regression
- Priority: P2
- Status: closed
- Impact: The initial pixel-level check did not establish that either overlay drew its required color because the sample point was not guaranteed to be in the polygon.
- Initial observation: `packagedSourcesReturnLocalPolygonGeometryForTaoyuanExtent` obtained a non-`NO_TILE` legacy tile at zoom 14 based on the first candidate feature's bounding-box center, but no pixel exactly matched the legacy fill color. Geometry inspection later confirmed this sample was outside the polygon.
- Root cause: The test used a feature bounding-box center as if it were guaranteed to lie inside the polygon. Geometry inspection disproved that assumption: the first GeoJSON feature's outer-ring centroid is outside its shell, and the first GPKG feature intersecting the test extent also has an outer-ring centroid outside its shell. A bounding box can overlap a tile while the polygon itself does not.
- Resolution evidence: The test now finds a point inside an exterior ring and outside all holes, requests a zoom-20 tile containing that point, decodes the returned PNG, and verifies the exact opaque layer color. The complete focused Android 14 AVD class passed (3 tests, 0 failures) for both layers.
- Remediation: Replaced bounding-box-center sampling with verified polygon interior sampling. No production rendering change was needed.
