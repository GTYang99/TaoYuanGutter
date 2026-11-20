# Repository Analysis

## Current Behavior

- `LayersBottomSheet` already exposes the 13 district checkboxes and all-select behavior.
- `MapOverlayController` and `OfflinePolygonTileProvider` now carry district selection state and a generation value so stale tile results can be discarded.
- `MainViewModel` persists the overlay state through shared preferences and restores it on app start.

## Expected Behavior

- The overlay sheet persists district and layer state across app restarts.
- Offline polygon tiles respect the latest district selection and do not render stale results after rapid changes.
- Existing overlay behaviors outside the three targeted GPKG layers remain unchanged.

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/main/MainViewModel.kt`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt`
- `app/src/test/java/com/example/taoyuangutter/main/MainViewModelTest.kt`
- `app/src/test/java/com/example/taoyuangutter/map/MapOverlayControllerStateTest.kt`

## Risks

- SharedPreferences restore depends on app-local state and should remain aligned with future overlay state changes.
- Versioned tile rendering must continue to ignore stale results when the selection changes quickly.

## Unknowns

- None.
