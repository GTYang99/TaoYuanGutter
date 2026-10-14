# FEAT-1006 Issue Log

## ISS-FEAT-1006-001 — Physical Android device unavailable for planned host scenarios (superseded)

- Phase: implementation
- Category: environment
- Priority: P1
- Status: closed
- Impact: The earlier plan required physical-device checks for AC-001 through AC-005. The updated user constraint now limits App/runtime tests to Android emulator and excludes physical-phone testing.
- Evidence: On 2026-10-07, targeted AC-007 instrumentation passed on `XQ-AU52` Android 12. The planned four-host network, alignment, toggle, pan/zoom, and repeated-lifecycle scenarios for AC-001 through AC-005 have not been run. See execution report for exact coverage.
- Next action: none
- Resolution: Superseded by the user-approved emulator-only test scope. This closure does not claim that any emulator test passed; emulator results must be recorded separately in execution/verification evidence.

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

## ISS-FEAT-1006-005 — Region geographic coordinate path is underspecified

- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: closed
- Impact: AC-008 could render blank or misaligned if geographic degree coordinates were treated as EPSG:3826 meters.
- Evidence: `analysis.md` and Plan Steps 3, 4, and 9 now define GPKG axis order, the GPKG-declared GCS_TWD97[2020]/Taiwan_Datum_1997/GRS80 source, TM2 zone 121 operation parameters, normalized EPSG:3826 coordinate/index units, tile-bound query path, and two expected source-to-map control-point results. The tests also query a populated region tile and exact-filter cell-index candidates.
- Resolution: Planning gap resolved and re-reviewed. No production code was changed in Planning.
- Next action: none
## ISS-FEAT-1006-006 — Region style parity has no verifiable baseline

- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: closed
- Impact: AC-008's approved visual-preservation requirement was not objectively testable without a WMTS reference.
- Evidence: The user confirmed “沿用現有 WMTS 外觀”. The 2026-10-14 WMTS request returned HTTP 200 for z13/x6856/y3508; its image, request metadata, response properties, and SHA-256 are recorded in `evidence/regions-wmts-baseline.md` and `evidence/regions-wmts-z13-x6856-y3508.png`. The reference shows transparent fill/background, an opaque `#762E77` stroke core (2 px median) with antialiased footprint (4 px median at alpha ≥64 in the recorded transect), and no labels in that tile. Plan Steps 5 and 10 and AC-008 now require matching those style metrics while using the GPKG as geometry truth, and preserving z-index/transparency in all hosts.
- Resolution: Planning gap resolved and re-reviewed. No production code was changed in Planning.
- Next action: none
