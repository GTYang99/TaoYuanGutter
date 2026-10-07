# FEAT-1006 Issue Log

## ISS-FEAT-1006-001 — Physical Android device unavailable for planned host scenarios

- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Impact: Planned physical-device checks for AC-001 through AC-005 remain outstanding. A local Android 14 AVD provides partial runtime coverage.
- Evidence: `Medium_Phone(AVD) - 14` ran the focused data-store/provider instrumentation tests successfully; no physical device is attached. See execution report for exact coverage.
- Next action: infrastructure
- Remediation: connect an Android 9+ physical device and run the plan's focused scenarios in all four map hosts.

## ISS-FEAT-1006-002 — Android SQLite lacks the RTree module

- Phase: implementation
- Category: implementation_regression
- Priority: P1
- Status: closed
- Impact: Before the fix, both local polygon data sources failed spatial queries on Android; tiles returned `NO_TILE`.
- Evidence: After the portable cell-index fix, `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonDataStoreInstrumentedTest` passed on Android 14 (`2 tests, 0 failures`), including local polygon queries for both sources and a populated legacy tile request. The provider test also confirms `release()` returns `NO_TILE` afterward.
- Root cause: The implementation depended on the optional SQLite RTree virtual-table module, which is not present in the platform SQLite build used by Android 14. The dependency affects the existing GPKG RTree and the GeoJSON index created at runtime.
- Remediation: Replaced RTree with a versioned, persistent 512-meter cell index made from ordinary SQLite tables for both data sources. Index queries also filter by exact feature bounds.
- Resolution: Re-verified on Android 14 AVD; closed.
