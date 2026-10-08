# FEAT-1006 Issue Log

## ISS-FEAT-1006-005 — AC-007 outline zoom behavior clarified

- Phase: planning
- Category: requirement_gap
- Priority: P2
- Status: closed
- Impact: The previously approved constant-width AC-007 did not match the user's clarified need for a slightly thicker line at broad scale and a restrained line at close scale.
- Evidence: User clarification on 2026-10-08; the requirement, analysis, plan, and plan-review addendum now define a continuous 2.5 px at/below zoom 16 to 1.5 px at/above zoom 20 profile, with tunable parameters.
- Resolution: Planning re-entry and plan review completed; implementation may proceed under the revised AC-007.
- Next action: implementation

## ISS-FEAT-1006-001 — Physical Android device unavailable for planned host scenarios

- Phase: implementation
- Category: environment
- Priority: P1
- Status: closed
- Impact: The previous plan requested physical-device checks for AC-001 through AC-005.
- Evidence: On 2026-10-07, targeted AC-007 instrumentation passed on `XQ-AU52` Android 12. The planned four-host network, alignment, toggle, pan/zoom, and repeated-lifecycle scenarios for AC-001 through AC-005 had not been run under the prior scope. See execution report for exact coverage.
- Next action: implementation
- Remediation: Superseded by the user's emulator-only testing direction; use `emulator-5554` and record results as emulator evidence.

### Test target resolution (2026-10-08)

- The earlier no-emulator-test statements above are historical and were superseded by the user's explicit emulator-only direction.
- `emulator-5554` (`sdk_gphone64_arm64`, Android 14) is the sole ADB target. The amended GPKG source and zoom-dependent style were tested there; no physical phone was used. See the current emulator evidence in `execution-report.md`.
- The prior Android 12 `XQ-AU52` AC-007 result remains historical and does not substitute for current-source emulator evidence.
- Next action: verification

## ISS-FEAT-1006-006 — Shared emulator is occupied by another instrumentation run

- Phase: infrastructure
- Category: environment
- Priority: P1
- Status: closed
- Impact: The remaining emulator-only map-host UI, pan/zoom, endpoint-observation, and lifecycle checks cannot be run safely while another app's instrumentation test is driving the shared emulator.
- Evidence: The other project's `AndroidJUnitRunner` had occupied `emulator-5554` during earlier manual checks. On 2026-10-08 its runner was no longer active; FEAT-1006 installed and completed `OfflinePolygonMapHostInstrumentedTest` on that emulator (`OK (3 tests)`, 36.767 seconds). A second AVD could not start because host disk space was low, but it was not required. No other project's app or test was stopped.
- Resolution: The shared emulator became available for FEAT-1006 instrumentation. Focused host tests ran successfully on `emulator-5554`; remaining unverified acceptance evidence is listed in `execution-report.md`.
- Next action: verification

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
