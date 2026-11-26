# Debug Fix Plan

## Objective

Repair ISS-004 through ISS-007 with the smallest changes that restore the approved acceptance criteria without changing the accepted district-source authority decision or unrelated map behavior.

## Fixes

1. `LayersBottomSheet.kt`
   - Derive all-selection state from selected count.
   - Set `isChecked` only for all selected and `isIndeterminate` only for a partial selection; clear indeterminate for zero/all.
   - Add/extend a focused state test for zero, partial, and all.

2. `OfflinePolygonOverlayManager` / `OfflinePolygonTileProvider`
   - Introduce a candidate-generation path that prepares source providers without removing the currently active overlays.
   - Commit only the latest generation after all required sources are ready; discard stale candidates.
   - Keep at most one committed overlay per enabled layer and release the replaced generation after the swap.
   - Add a deterministic deferred-loader test for generation 1/2 completion order.

3. Source query scoping
   - Pass each `OfflinePolygonSource`'s `queryDistricts` into its own data-source query.
   - Ensure a failed district falls back with only that district; successful district sources continue independently.
   - Add a focused fallback test that asserts no rows from another selected district are returned.

4. Data-source lifecycle
   - Add close/release semantics to `OfflinePolygonDataSource` and `GeoPackageDataSource`.
   - Make the store cache evict or close entries when no active provider owns them; do not close a source still shared by another provider.
   - Add a bounded repeated-switch test asserting released source/index counts reach zero and tile cache remains bounded.

## Validation Order

1. Run the new focused JVM tests for tri-state, atomic generation, fallback scope, and release.
2. Run the existing map JVM suite and `assembleDebug`.
3. Re-run the host validator and APK asset readback.
4. Re-attempt emulator instrumentation; if ADB remains unavailable, record the affected ACs as `NOT VERIFIED`.

## Implementation Result

The four fixes were implemented in the current debug re-entry. The next gate is independent verification of the committed revision; ISS-004 through ISS-007 remain open until that verification passes.
