# Root Cause Analysis

## Scope

Verification of commit `0f9125cd2714b6121480437b70b0d0fef44c3f82` failed on AC-003, AC-006, AC-007, and AC-008. The failures are implementation regressions, not source-data or build failures.

## Findings

### ISS-004 / AC-003 — missing tri-state presentation

- Direct cause: `LayersBottomSheet.bindDistrictChecks()` only assigns `cbDistrictAll.isChecked = selectedDistrictsState.size == DISTRICTS.size`.
- Root cause: the UI state model treats the all-selection control as boolean instead of deriving three states from the selected count: zero, partial, and all.
- Effect: partial selection is functionally dispatched, but the user cannot see the required indeterminate state.
- Scope: `LayersBottomSheet.kt`; add focused UI/state coverage. No map data or persistence change is required.

### ISS-005 / AC-006 — overlay is cleared before replacement is ready

- Direct cause: `OfflinePolygonOverlayManager.setVisibility()` calls `clearOverlays()` immediately when districts or generation changes.
- Root cause: source loading and overlay ownership are coupled to the active state; there is no pending generation or candidate overlay set that can be prepared independently.
- Effect: the previous visible result disappears during staging/indexing, and out-of-order completion cannot perform the planned atomic latest-generation swap.
- Scope: `OfflinePolygonOverlayManager` / tile-provider lifecycle. Preserve the current active overlays until a candidate generation is ready, then replace once and discard stale candidates.

### ISS-006 / AC-007 — fallback loses district scope

- Direct cause: a failed district source falls back to the full source, but `getTile()` calls `query(bounds, selectedDistrictsProvider())` with the complete selected set.
- Root cause: `OfflinePolygonSource.queryDistricts` is stored in the source definition but is not propagated into the query operation.
- Effect: one failed district can read rows belonging to other selected districts, duplicating successful district sources and violating per-district isolation.
- Scope: `OfflinePolygonTileProvider` and `OfflinePolygonDataSource` query contract. Each source query must use its own effective district set; fallback uses exactly the failed district.

### ISS-007 / AC-008 — source/index handles have no release path

- Direct cause: `OfflinePolygonDataStore.sources` is a static cache of completed futures, while `GeoPackageDataSource` retains open source and index `SQLiteDatabase` instances; `OfflinePolygonTileProvider.release()` only evicts tile bitmap cache.
- Root cause: data-source ownership is not tied to provider lifetime, and the source cache has no reference counting or eviction API.
- Effect: repeated district changes retain database handles and indexes, contrary to the resource lifecycle bound, even though overlays and tile caches are removed.
- Scope: `OfflinePolygonDataStore`, `GeoPackageDataSource`, and provider release. Add explicit close semantics and release unused source entries; keep only sources still owned by active providers.

## Why the emulator-like smoke path looked healthy

The build, asset packaging, source mapping, and basic tile rendering all pass. The missing behaviors require partial checkbox rendering, delayed/out-of-order source completion, an injected single-source failure, and repeated lifecycle observation. Those scenarios were not covered by the focused JVM tests, and emulator execution was unavailable because ADB could not start. Therefore visual smoke success does not contradict the verification findings.

## Root Cause Classification

All four findings are `implementation_regression` against the approved plan. No requirement clarification is needed. The minimum repair should be implemented in one debug re-entry, followed by deterministic JVM tests before any further emulator or release decision.

## Verification Round 2 Findings

### ISS-005 / AC-006 — candidate overlay attaches too early

- Direct cause: `stageVisibility()` calls `addOverlay()` while constructing `candidateWithOverlays`, before `whenSourcesReady()` completes.
- Root cause: candidate provider preparation and map attachment are coupled in the candidate construction step. The old overlay is retained, but the new overlay is already visible, so the implementation cannot guarantee non-overlap.
- Correct boundary: providers may stage and index in the background; only the main-thread commit may attach the new overlays and remove the old overlays.

### ISS-008 / AC-007 — fallback readiness deadlocks the source executor

- Direct cause: `whenSourcesReady()` runs `fallbackFuture(...).get()` inside `future.handle`.
- Root cause: the primary source completion callback can execute on the single-thread `OfflinePolygonDataStore` executor, while the fallback load is queued to that same executor. The callback blocks the only worker waiting for work that it prevents from starting.
- Correct boundary: compose the primary and fallback futures asynchronously with `thenCompose`/`handle`; never block the store executor with `get()`.

The round-2 verifier also confirms ISS-004, ISS-006, and ISS-007 are fixed. AC-005 and AC-008–AC-011 remain unverified because the required coverage was not completed; they are not additional root causes.

## Verification Round 4 — ISS-011

### AC-011 — intermittent map-host snapshot misses the offline fill

- Direct cause: `OfflinePolygonMapHostInstrumentedTest.assertMapShowsLayerInteriorColor()` captures exactly one `GoogleMap.snapshot()` after waiting for `OnCameraIdle` and `OnMapLoadedCallback`. Those callbacks establish camera/base-map readiness, but do not establish that a custom `TileOverlay` has fetched, rasterized, and been composited into the snapshot.
- Root cause: the test has no explicit offline-overlay render-readiness boundary. Provider readiness and map readiness are treated as equivalent, although the offline provider is attached asynchronously and Google Maps renders tile overlays on a separate pipeline.
- Evidence: the valid-key focused case passed once; the four-case run failed only at the LEGACY interior pixel (`expected #ff156d1d`, `observed #ffa3c2a1`); the permitted retry passed; direct provider tile checks and the other three host cases passed. This pattern is timing-sensitive and does not indicate a stable source/query failure.
- The observed color is therefore evidence that the snapshot was taken before the expected overlay tile was visible at the sample point, not evidence that the provider returned the wrong tile. The current test also uses a single exact pixel, which makes the race observable rather than classifying it as a readiness timeout.

### Classification

`ISS-011` remains an `implementation_regression` for the verification gate because the implementation/test contract does not provide a deterministic render-readiness signal. The minimum repair is test synchronization around the actual rendered result; no Maps key, source data, query scope, or overlay-selection behavior change is justified by this evidence.

### Affected files and regression risk

- `app/src/androidTest/java/com/example/taoyuangutter/map/OfflinePolygonMapHostInstrumentedTest.kt`: snapshot readiness and bounded retry logic.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt`: inspected as the provider/overlay source; no production defect is established by this failure.
- Regression risk is limited to the instrumentation assertion timing. Changing production overlay lifecycle without a reproducible provider/data failure would expand scope and could reintroduce the already-fixed generation and resource-lifecycle behavior.
