# Repository Analysis

## Task Classification

- Type: `feature` — add offline data loading and rendering behavior for two existing map overlays. This is an exploratory implementation with the user-supplied data and does not establish the future authoritative dataset mapping.

## Current Behavior

- `MapOverlayController` creates the legacy layer from WMTS `legacyDitch`; its `showPossible` toggle creates WMTS `roadServey`. `MainActivity` and `MapWorkspaceFragment` each use this controller.
- `GutterFormActivity` and `MapPointPickerActivity` each create their own WMTS overlays, so replacing only the shared main-map controller would leave two contexts online.
- The app uses Google Maps `TileOverlay` and `UrlTileProvider`; Gson is already available, but no offline GeoPackage/GeoJSON map-rendering implementation or spatial index is present in the inspected map package.
- The two specified files contain 36,970 Polygon features in the GPKG and 61,435 MultiPolygon features in the GeoJSON. Both declare EPSG:3826. Their raw sizes are approximately 19 MiB and 93 MiB (about 112 MiB total).
- Existing `Wms3826RequestBuilder` contains a forward TWD97 TM2 zone 121 projection calculation; it does not expose the inverse transform needed to convert these local geometries for Google Maps coordinates.

## Expected Behavior

- Package the specifically linked files with the app, decode each according to its actual format, and render their thematic roles from local data in the `MainActivity`/`MapWorkspaceFragment`, gutter-form, and point-picker map hosts.
- Render filled polygons in the existing legend colors (`#156D1D` legacy and `#FA0000` possible), retaining polygon holes, existing controls, and removing the corresponding feature-data network requests.
- Add a same-color polygon boundary outline for broad-extent visibility, defaulting to an adjustable 2.0 tile-pixel width that remains visually stable across zoom levels.
- Append `.gpkg` to the legacy layer option title and `.geojson` to the possible-gutter option title; leave all other UI unchanged.
- Parse/index off the UI thread, render only the needed map tiles/features, and release tile/overlay resources when hidden or when the map is destroyed.

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt` — replace the two main-map online data sources and coordinate shared provider lifecycle.
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` and `MainActivity.kt` — verify both users of the shared overlay controller and state restoration.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — route its legacy and `roadServey` overlays through the local renderer.
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt` — route its legacy and `roadServey` overlays through the local renderer.
- New map data/parser/index/tile-rendering classes and unit tests under `app/src/main/java/.../map` and `app/src/test/.../map`.
- `app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt` — draw the configurable boundary outline without changing layer colors, fill behavior, or hole semantics.
- `app/src/main/assets/` — package the two supplied data files, subject to APK packaging/asset-compression confirmation during implementation.
- `app/src/main/res/values/strings.xml` — update only the two map layer-option titles with the current source suffixes.
- `app/build.gradle.kts` and version catalog only if an additional compatible GeoPackage reader is required.
- `app/src/androidTest/` — focused map-context and interaction validation if existing instrumentation setup supports it.

## Dependencies

- The two exact files linked by the source requirement; no substitute dataset is in scope.
- Google Maps SDK, existing `TileOverlay` interfaces, Gson, and existing map layer state/toggles.
- A way to decode GeoPackage polygon geometry and GeoJSON MultiPolygon geometry, spatially query features for a requested map tile, and convert EPSG:3826 coordinates to Google Maps coordinates or tile coordinates.
- Android asset packaging and installed APK sizing; the raw source files total about 112 MiB.

## Risks

- App download/install size may grow substantially when bundling about 112 MiB of raw geospatial data; compression and delivered APK/AAB size need evidence.
- Loading all ~98k polygon features and their vertices as Google Maps objects could exhaust memory or block the map; rendering needs background parsing, spatial filtering/indexing, and bounded tile caching.
- Incorrect EPSG:3826 conversion, coordinate ordering, multipolygon rings/holes, or fill rules could place or paint polygons incorrectly.
- The layer association is intentionally provisional for this trial; do not treat it as the final dataset mapping.
- Duplicated map paths can diverge in style, toggle behavior, and lifecycle if not routed through a shared renderer/provider.

## Unknowns

- Candidate GeoPackage reader/rendering strategy and whether it adds a runtime dependency. Resolve by a constrained implementation spike using the actual GPKG before selecting a dependency.
- Peak memory and pan/zoom observations on the supported physical test device; record APK/AAB size without enforcing a limit for this trial.
- Definitive dataset-to-layer association for later releases; explicitly deferred by the user and non-blocking for this exploratory task.

## Potential Issue Categories

- `planning_gap`: missing performance/resource targets discovered during plan review.
- `implementation_regression`: any map crashes, freezes, bad coordinates, incorrect geometry fills, or overlay-state regressions.
- `environment`: inability to package the supplied data, build the app, or run required physical-device validation.
