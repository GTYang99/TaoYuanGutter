# Requirement

## Source

- Task ID: `FEAT-1006`.
- User-provided requirements and clarifications recorded in the FEAT-1006 task history.
- `/Users/a10362/Desktop/markdown file/TY/ty_feat_1007.md`, including its 1014 decision to replace the Taoyuan administrative-area WMTS with `桃園市行政區.gpkg`.
- User confirmed that the administrative-area layer must retain its existing WMTS appearance when sourced from GPKG.
- The 1014 amendment adds the Taoyuan administrative-area layer to this exploratory offline-data task.

## Background

- Replace WMTS/API feature data for the Water Department legacy and possible-gutter overlays with bundled offline GPKGs, and replace the Taoyuan administrative-area WMTS with its bundled GPKG so all three layers can be displayed without their corresponding feature-data requests.
- The three source-to-layer pairings in this trial are exploratory and do not establish permanent data-source policy.

## Goal

- Load the three confirmed GPKGs locally in all map hosts. Preserve the existing two thematic overlays’ fill and zoom-dependent outline behavior, and preserve the administrative-area overlay’s existing visible style and layer controls.

## Functional Requirements

- Use `20260824_水務局既有側溝更新v3_線轉面.gpkg` for the Water Department legacy overlay. Its GeoPackage table is `line_buffer_polygon` (EPSG:3826, Polygon, 36,970 features).
- Use `20260601_水務局未調查側溝_v5.gpkg` for the possible-gutter overlay. Its GeoPackage table is `refactored` (EPSG:3826, MultiPolygon, 61,435 features). `roadServey` is the online source previously used for this layer.
- Use `桃園市行政區.gpkg` table `桃園市行政區` (geometry `MULTIPOLYGON`, SRS id 100000 / `GCS_TWD97[2020]`, 13 features) for the administrative-area layer.
- Package all three confirmed GPKGs with the app and render their features locally without requesting `legacyDitch`, `roadServey`, or `regions` feature data.
- Render legacy polygons with opaque `#156D1D` fill and possible-gutter polygons with opaque `#FA0000` fill. Preserve polygon holes and MultiPolygon members.
- Remove file extensions from the offline-source layer titles per the 1014 decision; preserve the existing labels and all other UI behavior. Make no other UI changes.
- Draw same-color outlines on both layers using a continuous zoom-dependent width profile: 2.5 tile pixels at zoom 16 and below; interpolate linearly from zoom 16 to zoom 20; use 1.5 tile pixels at zoom 20 and above. Expose endpoint widths and reference zooms as adjustable style parameters. At zoom 18 the default profile yields 2.0 tile pixels.
- Apply offline overlays in `MainActivity`, `MapWorkspaceFragment`, the gutter form map, and the point-picker map.
- Parse and render without blocking the UI thread. Avoid crashes and ANRs; release or bound provider-owned overlays and cache resources on disable or map destruction.

## Non-functional Requirements

- After installation, all three feature sources are available offline; unrelated base-map and overlay network behavior remains unchanged.
- Record packaged asset sizes and runtime responsiveness observations. There is no APK/AAB size limit for this trial.
- All App/runtime tests MUST run on an Android emulator only. Do not run App/runtime tests on a physical phone.
- Keep the GPKG source-to-layer pairing exploratory; future dataset versions and permanent mapping are outside this task.

## Acceptance Criteria

- AC-001: The APK includes the two thematic GPKGs and `桃園市行政區.gpkg` with bytes matching their recorded source hashes. All three overlays load locally without requesting `legacyDitch`, `roadServey`, or `regions` feature data.
- AC-002: Legacy polygons render opaque `#156D1D`; possible-gutter polygons render opaque `#FA0000`. EPSG:3826 geometries align with the base map and preserve polygon holes and MultiPolygon members.
- AC-003: All three overlays work in `MainActivity`, `MapWorkspaceFragment`, the gutter form map, and the point-picker map. Existing controls show and hide the corresponding overlays.
- AC-004: With any combination of the three overlays enabled, pan and zoom through supplied data in all four map hosts without crash, ANR, or UI-blocking parse/render. Record emulator observations.
- AC-005: Disabling a layer removes its content and releases or bounds provider-owned cache/resources. Re-enabling restores one correct local overlay without duplicates.
- AC-006: Offline-source layer titles have no file-extension suffix, as decided on 1014; all other UI labels and layout remain unchanged.
- AC-007: Both thematic layers display same-color outlines with a continuous profile: 2.5 tile pixels at zoom 16 and below, 2.0 at zoom 18, and 1.5 at zoom 20 and above. Endpoint widths and reference zooms are adjustable; fills and holes remain intact.
- AC-008: The Taoyuan administrative-area overlay reads the packaged `桃園市行政區.gpkg` (table `桃園市行政區`, SRS id 100000 / `GCS_TWD97[2020]`, MultiPolygon, 13 features) in all four map hosts; the `regions` WMTS is not requested, and existing region-layer controls, ordering, and visible style are preserved.

## Constraints

- Use only the three confirmed GPKG sources for this trial; do not restore the former GeoJSON possible-layer source. The administrative-area GPKG source is also trial-scoped.
- Remove file-extension suffixes from the two thematic layer-option titles per the 1014 decision; preserve the existing administrative-area title, other labels, layout, colors, controls, and unrelated map layers.
- Run App/runtime tests on an Android emulator only; physical-phone tests are out of scope.
- Do not represent this trial pairing as permanent production data-source policy.

## Open Questions

- 無。行政區沿用 WMTS 外觀已由使用者確認。
